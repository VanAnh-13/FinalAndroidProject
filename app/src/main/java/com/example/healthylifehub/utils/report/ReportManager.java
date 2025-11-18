package com.example.healthylifehub.utils.report;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.Reminder;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manager for generating and sharing reports
 */
public class ReportManager {
    
    private final Context context;
    private final ExecutorService executorService;
    
    public interface ReportCallback {
        void onSuccess(File file);
        void onError(Exception e);
    }
    
    public ReportManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Generate health metrics report in Excel format
     */
    public void generateHealthMetricsExcel(List<HealthMetric> metrics, Date startDate, Date endDate, ReportCallback callback) {
        executorService.execute(() -> {
            try {
                File file = ExcelReportGenerator.generateHealthMetricsReport(context, metrics, startDate, endDate);
                callback.onSuccess(file);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Generate health metrics report in PDF format
     */
    public void generateHealthMetricsPDF(List<HealthMetric> metrics, Date startDate, Date endDate, ReportCallback callback) {
        executorService.execute(() -> {
            try {
                File file = PDFReportGenerator.generateHealthMetricsReport(context, metrics, startDate, endDate);
                callback.onSuccess(file);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Generate reminders report in Excel format
     */
    public void generateRemindersExcel(List<Reminder> reminders, ReportCallback callback) {
        executorService.execute(() -> {
            try {
                File file = ExcelReportGenerator.generateRemindersReport(context, reminders);
                callback.onSuccess(file);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Generate reminders report in PDF format
     */
    public void generateRemindersPDF(List<Reminder> reminders, ReportCallback callback) {
        executorService.execute(() -> {
            try {
                File file = PDFReportGenerator.generateRemindersReport(context, reminders);
                callback.onSuccess(file);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
    
    /**
     * Share report file
     */
    public void shareReport(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, 
                context.getPackageName() + ".fileprovider", file);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(getMimeType(file));
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ báo cáo"));
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi khi chia sẻ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Open report file
     */
    public void openReport(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(context, 
                context.getPackageName() + ".fileprovider", file);
            
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(uri, getMimeType(file));
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(openIntent);
        } catch (Exception e) {
            Toast.makeText(context, "Không thể mở file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getMimeType(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "*/*";
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
