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
     * Requirements: 7.6 - Enhanced progress callbacks at each stage
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
        
        // Start performance tracking
        String operationId = AsyncPerformanceLogger.operation("generate_pdf_report")
            .withContext("userId", userId)
            .start();
        
        return CompletableFuture
                // Stage 1: Fetch data from Firestore (IO Thread) - 10%
                .supplyAsync(() -> {
                    callback.onProgress(10, "Đang tải dữ liệu...");
                    long stageStart = System.currentTimeMillis();
                    ReportData data = fetchReportData(userId, startDate, endDate);
                    long stageDuration = System.currentTimeMillis() - stageStart;
                    AsyncPerformanceLogger.logMetric("report_fetch_data_duration", stageDuration, "ms");
                    return data;
                }, ioExecutor)
                
                // Stage 2: Calculate statistics (Compute Thread - Parallel) - 30%, 50%
                .thenApplyAsync(reportData -> {
                    callback.onProgress(30, "Đang tính toán thống kê...");
                    long stageStart = System.currentTimeMillis();
                    
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
                    
                    long stageDuration = System.currentTimeMillis() - stageStart;
                    AsyncPerformanceLogger.logMetric("report_calculate_stats_duration", stageDuration, "ms");
                    callback.onProgress(50, "Thống kê hoàn tất");
                    return reportData;
                }, computeExecutor)
                
                // Stage 3: Generate charts (Compute Thread - Parallel) - 60%, 75%
                .thenApplyAsync(reportData -> {
                    callback.onProgress(60, "Đang tạo biểu đồ...");
                    long stageStart = System.currentTimeMillis();
                    
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
                    
                    // Store chart bitmaps in reportData for later use in PDF creation
                    // They will be recycled after PDF is created
                    List<Bitmap> chartBitmaps = new ArrayList<>();
                    try {
                        for (CompletableFuture<Bitmap> future : chartFutures) {
                            chartBitmaps.add(future.get());
                        }
                        reportData.setChartBitmaps(chartBitmaps);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    long stageDuration = System.currentTimeMillis() - stageStart;
                    AsyncPerformanceLogger.logMetric("report_generate_charts_duration", stageDuration, "ms");
                    callback.onProgress(75, "Biểu đồ hoàn tất");
                    return reportData;
                }, computeExecutor)
                
                // Stage 4: Create PDF file (IO Thread) - 80%, 90%
                .thenApplyAsync(reportData -> {
                    callback.onProgress(80, "Đang tạo file PDF...");
                    File pdfFile = createPDFFile(reportData);
                    callback.onProgress(90, "PDF hoàn tất");
                    return pdfFile;
                }, ioExecutor)
                
                // Stage 5: Upload to Firebase Storage (IO Thread) - 95%
                .thenComposeAsync(pdfFile -> {
                    callback.onProgress(95, "Đang tải lên...");
                    return uploadToStorage(userId, pdfFile);
                }, ioExecutor)
                
                // Stage 6: Save metadata to Firestore (IO Thread) - 98%, 100%
                .thenApplyAsync(downloadUrl -> {
                    callback.onProgress(98, "Đang lưu metadata...");
                    saveReportMetadata(userId, downloadUrl);
                    callback.onProgress(100, "Hoàn tất!");
                    
                    // Log successful completion with total duration
                    AsyncPerformanceLogger.logEnd(operationId, "generate_pdf_report", true);
                    
                    return downloadUrl;
                }, ioExecutor)
                
                // Error handling with logging
                .exceptionally(throwable -> {
                    // Log failed completion
                    AsyncPerformanceLogger.logEnd(operationId, "generate_pdf_report", false);
                    
                    // Log error with user context
                    AsyncErrorLogger.context()
                        .put("userId", userId)
                        .put("startDate", startDate.toString())
                        .put("endDate", endDate.toString())
                        .log("generate_pdf_report", throwable);
                    
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
     * Generate heart rate chart
     * Requirements: 7.2, 7.3
     */
    private Bitmap generateHeartRateChart(List<HealthMetric> metrics) {
        LineChart chart = new LineChart(context);
        chart.setLayoutParams(new android.view.ViewGroup.LayoutParams(800, 600));
        
        List<Entry> entries = new ArrayList<>();
        List<HealthMetric> hrMetrics = metrics.stream()
                .filter(m -> "heart_rate".equals(m.getType()))
                .collect(Collectors.toList());
        
        for (int i = 0; i < hrMetrics.size(); i++) {
            entries.add(new Entry(i, (float) hrMetrics.get(i).getValue()));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Nhịp tim");
        dataSet.setColor(Color.rgb(255, 99, 71)); // Tomato color
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getDescription().setText("Nhịp tim (BPM)");
        chart.invalidate();
        
        return renderChartToBitmap(chart);
    }
    
    /**
     * Generate weight chart
     * Requirements: 7.2, 7.3
     */
    private Bitmap generateWeightChart(List<HealthMetric> metrics) {
        LineChart chart = new LineChart(context);
        chart.setLayoutParams(new android.view.ViewGroup.LayoutParams(800, 600));
        
        List<Entry> entries = new ArrayList<>();
        List<HealthMetric> weightMetrics = metrics.stream()
                .filter(m -> "weight".equals(m.getType()))
                .collect(Collectors.toList());
        
        for (int i = 0; i < weightMetrics.size(); i++) {
            entries.add(new Entry(i, (float) weightMetrics.get(i).getValue()));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Cân nặng");
        dataSet.setColor(Color.rgb(34, 139, 34)); // Forest green
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getDescription().setText("Cân nặng (kg)");
        chart.invalidate();
        
        return renderChartToBitmap(chart);
    }
    
    /**
     * Generate temperature chart
     * Requirements: 7.2, 7.3
     */
    private Bitmap generateTemperatureChart(List<HealthMetric> metrics) {
        LineChart chart = new LineChart(context);
        chart.setLayoutParams(new android.view.ViewGroup.LayoutParams(800, 600));
        
        List<Entry> entries = new ArrayList<>();
        List<HealthMetric> tempMetrics = metrics.stream()
                .filter(m -> "temperature".equals(m.getType()))
                .collect(Collectors.toList());
        
        for (int i = 0; i < tempMetrics.size(); i++) {
            entries.add(new Entry(i, (float) tempMetrics.get(i).getValue()));
        }
        
        LineDataSet dataSet = new LineDataSet(entries, "Nhiệt độ");
        dataSet.setColor(Color.rgb(255, 140, 0)); // Dark orange
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getDescription().setText("Nhiệt độ (°C)");
        chart.invalidate();
        
        return renderChartToBitmap(chart);
    }
    
    /**
     * Stage 4: Render chart to Bitmap
     * Requirements: 7.3 - Optimized chart rendering with 800x600 dimensions and ARGB_8888 config
     */
    private Bitmap renderChartToBitmap(LineChart chart) {
        // Set chart dimensions to 800x600 for quality
        int width = 800;
        int height = 600;
        
        // Use ARGB_8888 config for quality
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
     * Stage 5: Create PDF file using iTextPDF
     */
    private File createPDFFile(ReportData reportData) {
        try {
            return createPDFWithiText(reportData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Create PDF using iTextPDF library
     * Requirements: 7.4
     * 
     * Implements bitmap recycling (Requirement 7.3):
     * - Checks !bitmap.isRecycled() before recycling
     * - Recycles all chart bitmaps after PDF creation
     * - Ensures proper memory cleanup
     */
    private File createPDFWithiText(ReportData reportData) throws Exception {
        File pdfFile = new File(context.getCacheDir(), 
            "health_report_" + System.currentTimeMillis() + ".pdf");
        
        // Track bitmaps for cleanup
        List<Bitmap> bitmapsToRecycle = new ArrayList<>();
        
        try {
            com.itextpdf.kernel.pdf.PdfWriter writer = 
                new com.itextpdf.kernel.pdf.PdfWriter(pdfFile);
            com.itextpdf.kernel.pdf.PdfDocument pdf = 
                new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = 
                new com.itextpdf.layout.Document(pdf);
            
            // Add title paragraph with formatting
            com.itextpdf.layout.element.Paragraph title = 
                new com.itextpdf.layout.element.Paragraph("Báo Cáo Sức Khỏe")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            document.add(title);
            
            // Add spacing
            document.add(new com.itextpdf.layout.element.Paragraph("\n"));
            
            // Create statistics table with data
            if (reportData.getStatistics() != null) {
                Statistics stats = reportData.getStatistics();
                
                com.itextpdf.layout.element.Table statsTable = 
                    new com.itextpdf.layout.element.Table(2);
                statsTable.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));
                
                // Header row
                statsTable.addHeaderCell(
                    new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph("Chỉ số"))
                        .setBold()
                        .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                statsTable.addHeaderCell(
                    new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph("Giá trị"))
                        .setBold()
                        .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.LIGHT_GRAY));
                
                // Data rows
                statsTable.addCell("Trung bình");
                statsTable.addCell(String.format("%.2f", stats.getMean()));
                
                statsTable.addCell("Độ lệch chuẩn");
                statsTable.addCell(String.format("%.2f", stats.getStdDev()));
                
                statsTable.addCell("Giá trị nhỏ nhất");
                statsTable.addCell(String.format("%.2f", stats.getMin()));
                
                statsTable.addCell("Giá trị lớn nhất");
                statsTable.addCell(String.format("%.2f", stats.getMax()));
                
                statsTable.addCell("Xu hướng");
                statsTable.addCell(stats.getTrend());
                
                document.add(statsTable);
                document.add(new com.itextpdf.layout.element.Paragraph("\n"));
            }
            
            // Add chart images from Bitmaps
            if (reportData.getMetrics() != null && !reportData.getMetrics().isEmpty()) {
                // Generate and add blood pressure chart
                List<HealthMetric> bpMetrics = reportData.getMetrics().stream()
                    .filter(m -> "blood_pressure".equals(m.getType()))
                    .collect(Collectors.toList());
                
                if (!bpMetrics.isEmpty()) {
                    Bitmap bpChart = generateBloodPressureChart(reportData.getMetrics());
                    bitmapsToRecycle.add(bpChart);
                    addBitmapToDocument(document, bpChart, "Biểu đồ Huyết áp");
                }
                
                // Generate and add blood sugar chart
                List<HealthMetric> bsMetrics = reportData.getMetrics().stream()
                    .filter(m -> "blood_sugar".equals(m.getType()))
                    .collect(Collectors.toList());
                
                if (!bsMetrics.isEmpty()) {
                    Bitmap bsChart = generateBloodSugarChart(reportData.getMetrics());
                    bitmapsToRecycle.add(bsChart);
                    addBitmapToDocument(document, bsChart, "Biểu đồ Đường huyết");
                }
                
                // Generate and add heart rate chart
                List<HealthMetric> hrMetrics = reportData.getMetrics().stream()
                    .filter(m -> "heart_rate".equals(m.getType()))
                    .collect(Collectors.toList());
                
                if (!hrMetrics.isEmpty()) {
                    Bitmap hrChart = generateHeartRateChart(reportData.getMetrics());
                    bitmapsToRecycle.add(hrChart);
                    addBitmapToDocument(document, hrChart, "Biểu đồ Nhịp tim");
                }
                
                // Generate and add weight chart
                List<HealthMetric> weightMetrics = reportData.getMetrics().stream()
                    .filter(m -> "weight".equals(m.getType()))
                    .collect(Collectors.toList());
                
                if (!weightMetrics.isEmpty()) {
                    Bitmap weightChart = generateWeightChart(reportData.getMetrics());
                    bitmapsToRecycle.add(weightChart);
                    addBitmapToDocument(document, weightChart, "Biểu đồ Cân nặng");
                }
                
                // Generate and add temperature chart
                List<HealthMetric> tempMetrics = reportData.getMetrics().stream()
                    .filter(m -> "temperature".equals(m.getType()))
                    .collect(Collectors.toList());
                
                if (!tempMetrics.isEmpty()) {
                    Bitmap tempChart = generateTemperatureChart(reportData.getMetrics());
                    bitmapsToRecycle.add(tempChart);
                    addBitmapToDocument(document, tempChart, "Biểu đồ Nhiệt độ");
                }
            }
            
            // Close document and return File
            document.close();
            
            return pdfFile;
            
        } finally {
            // Recycle all bitmaps after PDF creation (Requirement 7.3)
            // Check !bitmap.isRecycled() before recycling to avoid IllegalStateException
            recycleBitmaps(bitmapsToRecycle);
        }
    }
    
    /**
     * Safely recycle a list of bitmaps
     * Requirements: 7.3 - Check !bitmap.isRecycled() before recycling
     * 
     * @param bitmaps List of bitmaps to recycle
     */
    private void recycleBitmaps(List<Bitmap> bitmaps) {
        if (bitmaps == null || bitmaps.isEmpty()) {
            return;
        }
        
        int recycledCount = 0;
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                recycledCount++;
            }
        }
        
        android.util.Log.d("ReportGenerator", 
            "Recycled " + recycledCount + " bitmaps to free memory");
    }
    
    /**
     * Helper method to add bitmap to PDF document
     */
    private void addBitmapToDocument(com.itextpdf.layout.Document document, 
                                     Bitmap bitmap, String caption) throws Exception {
        // Add caption
        document.add(new com.itextpdf.layout.element.Paragraph(caption)
            .setBold()
            .setFontSize(14));
        
        // Convert bitmap to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] imageBytes = stream.toByteArray();
        stream.close();
        
        // Create image data and add to document
        com.itextpdf.io.image.ImageData imageData = 
            com.itextpdf.io.image.ImageDataFactory.create(imageBytes);
        com.itextpdf.layout.element.Image image = 
            new com.itextpdf.layout.element.Image(imageData);
        
        // Scale image to fit page width
        image.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(80));
        image.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        
        document.add(image);
        document.add(new com.itextpdf.layout.element.Paragraph("\n"));
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
