package com.example.healthylifehub.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;

import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.ReportData;
import com.example.healthylifehub.data.model.Statistics;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * ReportGenerator implements UC-HLH-07: Export Health Reports
 * Uses CompletableFuture pipeline for async processing:
 * Stage 1: Fetch data from Firestore (IO Thread)
 * Stage 2: Calculate statistics (Compute Thread - Parallel)
 * Stage 3: Generate charts (Compute Thread - Parallel)
 * Stage 4: Render charts to Bitmap (Compute Thread)
 * Stage 5: Create PDF (IO Thread)
 * Stage 6: Upload to Firebase Storage (IO Thread)
 */
public class ReportGenerator {
    
    private final Context context;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    
    // Thread pools for different types of operations
    private final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
    private final ExecutorService computeExecutor = Executors.newFixedThreadPool(2);
    
    public interface ProgressCallback {
        void onProgress(int progress, String message);
    }
    
    public ReportGenerator(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }
    
    /**
     * Main method to generate PDF report with full pipeline
     * @param userId User ID
     * @param startDate Start date for report
     * @param endDate End date for report
     * @param callback Progress callback
     * @return CompletableFuture with download URL
     */
    public CompletableFuture<String> generatePDFReport(
            String userId,
            Date startDate,
            Date endDate,
            ProgressCallback callback) {
        
        return CompletableFuture
                // Stage 1: Fetch data from Firestore (IO Thread)
                .supplyAsync(() -> {
                    callback.onProgress(10, "Đang tải dữ liệu...");
                    return fetchReportData(userId, startDate, endDate);
                }, ioExecutor)
                
                // Stage 2: Calculate statistics (Compute Thread - Parallel)
                .thenApplyAsync(reportData -> {
                    callback.onProgress(30, "Đang tính toán thống kê...");
                    
                    // Parallel computation of statistics
                    CompletableFuture<Statistics> statsFuture = CompletableFuture
                            .supplyAsync(() -> calculateStatistics(reportData.getMetrics()), 
                                    computeExecutor);
                    
                    // Wait for statistics
                    try {
                        Statistics stats = statsFuture.get();
                        reportData.setStatistics(stats);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    callback.onProgress(50, "Thống kê hoàn tất");
                    return reportData;
                }, computeExecutor)
                
                // Stage 3: Generate charts (Compute Thread - Parallel)
                .thenApplyAsync(reportData -> {
                    callback.onProgress(60, "Đang tạo biểu đồ...");
                    
                    // Generate chart bitmaps in parallel
                    List<CompletableFuture<Bitmap>> chartFutures = new ArrayList<>();
                    
                    // Blood pressure chart
                    chartFutures.add(CompletableFuture.supplyAsync(
                            () -> generateBloodPressureChart(reportData.getMetrics()),
                            computeExecutor));
                    
                    // Blood sugar chart
                    chartFutures.add(CompletableFuture.supplyAsync(
                            () -> generateBloodSugarChart(reportData.getMetrics()),
                            computeExecutor));
                    
                    // Wait for all charts
                    CompletableFuture.allOf(chartFutures.toArray(new CompletableFuture[0])).join();
                    
                    callback.onProgress(75, "Biểu đồ hoàn tất");
                    return reportData;
                }, computeExecutor)
                
                // Stage 4: Create PDF file (IO Thread)
                .thenApplyAsync(reportData -> {
                    callback.onProgress(80, "Đang tạo file PDF...");
                    File pdfFile = createPDFFile(reportData);
                    callback.onProgress(90, "PDF hoàn tất");
                    return pdfFile;
                }, ioExecutor)
                
                // Stage 5: Upload to Firebase Storage (IO Thread)
                .thenComposeAsync(pdfFile -> {
                    callback.onProgress(95, "Đang tải lên...");
                    return uploadToStorage(userId, pdfFile);
                }, ioExecutor)
                
                // Stage 6: Save metadata to Firestore (IO Thread)
                .thenApplyAsync(downloadUrl -> {
                    callback.onProgress(98, "Đang lưu metadata...");
                    saveReportMetadata(userId, downloadUrl);
                    callback.onProgress(100, "Hoàn tất!");
                    return downloadUrl;
                }, ioExecutor)
                
                // Error handling
                .exceptionally(throwable -> {
                    callback.onProgress(-1, "Lỗi: " + throwable.getMessage());
                    return null;
                });
    }
    
    /**
     * Stage 1: Fetch data from Firestore
     */
    private ReportData fetchReportData(String userId, Date startDate, Date endDate) {
        ReportData reportData = new ReportData();
        
        try {
            // Fetch health metrics
            Task<QuerySnapshot> metricsTask = firestore
                    .collection("users").document(userId)
                    .collection("healthMetrics")
                    .whereGreaterThanOrEqualTo("measuredAt", startDate)
                    .whereLessThanOrEqualTo("measuredAt", endDate)
                    .get();
            
            Tasks.await(metricsTask);
            
            List<HealthMetric> metrics = metricsTask.getResult()
                    .toObjects(HealthMetric.class);
            reportData.setMetrics(metrics);
            
            // TODO: Fetch medical records, medicines, user profile
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return reportData;
    }
    
    /**
     * Stage 2: Calculate statistics
     */
    private Statistics calculateStatistics(List<HealthMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return new Statistics(0, 0, 0, 0, 0, "STABLE");
        }
        
        // Calculate mean
        double sum = metrics.stream()
                .mapToDouble(HealthMetric::getValue)
                .sum();
        double mean = sum / metrics.size();
        
        // Calculate standard deviation
        double variance = metrics.stream()
                .mapToDouble(m -> Math.pow(m.getValue() - mean, 2))
                .sum() / metrics.size();
        double stdDev = Math.sqrt(variance);
        
        // Calculate min and max
        double min = metrics.stream()
                .mapToDouble(HealthMetric::getValue)
                .min()
                .orElse(0);
        
        double max = metrics.stream()
                .mapToDouble(HealthMetric::getValue)
                .max()
                .orElse(0);
        
        Statistics stats = new Statistics(mean, stdDev, min, max, 0, "STABLE");
        
        // Calculate trend
        if (metrics.size() >= 2) {
            double firstValue = metrics.get(0).getValue();
            double lastValue = metrics.get(metrics.size() - 1).getValue();
            
            if (lastValue > firstValue * 1.1) {
                stats.setTrend("INCREASING");
            } else if (lastValue < firstValue * 0.9) {
                stats.setTrend("DECREASING");
            } else {
                stats.setTrend("STABLE");
            }
        }
        
        return stats;
    }
    
    /**
     * Stage 3: Generate blood pressure chart
     */
    private Bitmap generateBloodPressureChart(List<HealthMetric> metrics) {
        LineChart chart = new LineChart(context);
        chart.setLayoutParams(new android.view.ViewGroup.LayoutParams(800, 600));
        
        List<Entry> entries = new ArrayList<>();
        List<HealthMetric> bpMetrics = metrics.stream()
                .filter(m -> "blood_pressure".equals(m.getType()))
                .collect(Collectors.toList());
        
        for (int i = 0; i < bpMetrics.size(); i++) {
            entries.add(new Entry(i, (float) bpMetrics.get(i).getSystolic()));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Huyết áp");
        dataSet.setColor(Color.RED);
        dataSet.setValueTextColor(Color.BLACK);
        
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
        
        return renderChartToBitmap(chart);
    }
    
    /**
     * Stage 3: Generate blood sugar chart
     */
    private Bitmap generateBloodSugarChart(List<HealthMetric> metrics) {
        LineChart chart = new LineChart(context);
        chart.setLayoutParams(new android.view.ViewGroup.LayoutParams(800, 600));
        
        List<Entry> entries = new ArrayList<>();
        List<HealthMetric> bsMetrics = metrics.stream()
                .filter(m -> "blood_sugar".equals(m.getType()))
                .collect(Collectors.toList());
        
        for (int i = 0; i < bsMetrics.size(); i++) {
            entries.add(new Entry(i, (float) bsMetrics.get(i).getValue()));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Đường huyết");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);
        
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.invalidate();
        
        return renderChartToBitmap(chart);
    }
    
    /**
     * Stage 4: Render chart to Bitmap
     */
    private Bitmap renderChartToBitmap(LineChart chart) {
        int width = 800;
        int height = 600;
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        
        chart.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
        );
        chart.layout(0, 0, width, height);
        chart.draw(canvas);
        
        return bitmap;
    }
    
    /**
     * Stage 5: Create PDF file
     * Note: This is a simplified version. In production, use iTextPDF library
     */
    private File createPDFFile(ReportData reportData) {
        try {
            File pdfFile = new File(context.getCacheDir(), "health_report_" + System.currentTimeMillis() + ".pdf");
            
            // TODO: Use iTextPDF to create proper PDF
            // For now, just create a placeholder file
            FileOutputStream fos = new FileOutputStream(pdfFile);
            fos.write("Health Report Placeholder".getBytes());
            fos.close();
            
            return pdfFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Stage 6: Upload to Firebase Storage
     */
    private CompletableFuture<String> uploadToStorage(String userId, File pdfFile) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        StorageReference storageRef = storage.getReference()
                .child("reports")
                .child(userId)
                .child(pdfFile.getName());
        
        Uri fileUri = Uri.fromFile(pdfFile);
        UploadTask uploadTask = storageRef.putFile(fileUri);
        
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String downloadUrl = task.getResult().toString();
                future.complete(downloadUrl);
                
                // Delete temp file
                pdfFile.delete();
            } else {
                future.completeExceptionally(task.getException());
            }
        });
        
        return future;
    }
    
    /**
     * Stage 7: Save report metadata to Firestore
     */
    private void saveReportMetadata(String userId, String downloadUrl) {
        try {
            Task<com.google.firebase.firestore.DocumentReference> task = firestore
                    .collection("users").document(userId)
                    .collection("reports")
                    .add(new java.util.HashMap<String, Object>() {{
                        put("downloadUrl", downloadUrl);
                        put("createdAt", new Date());
                        put("type", "PDF");
                    }});
            
            Tasks.await(task);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Cleanup resources
     */
    public void shutdown() {
        ioExecutor.shutdown();
        computeExecutor.shutdown();
    }
}
