package com.example.healthylifehub.utils.report;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
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
    
    private static final String TAG = "ReportManager";
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
            
            context.startActivity(Intent.createChooser(shareIntent, 
                context.getString(com.example.healthylifehub.R.string.share_report)));
        } catch (Exception e) {
            Toast.makeText(context, 
                context.getString(com.example.healthylifehub.R.string.error_sharing_report, e.getMessage()), 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Open report file
     * If no app is available to open Excel files, redirect to Microsoft 365 Online
     */
    public void openReport(File file) {
        try {
            Log.d(TAG, "Attempting to open report file: " + file.getAbsolutePath());
            Log.d(TAG, "File exists: " + file.exists() + ", Can read: " + file.canRead());
            
            Uri uri = FileProvider.getUriForFile(context, 
                context.getPackageName() + ".fileprovider", file);
            Log.d(TAG, "FileProvider URI: " + uri);
            
            String mimeType = getMimeType(file);
            Log.d(TAG, "MIME type: " + mimeType);
            
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(uri, mimeType);
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Check if there's an app that can handle this intent
            if (openIntent.resolveActivity(context.getPackageManager()) != null) {
                // Open directly if app exists
                context.startActivity(openIntent);
                Log.d(TAG, "Successfully started activity to open file");
            } else {
                // No specific app found
                Log.d(TAG, "No app found to open file");
                
                // Check if it's an Excel file
                if (isExcelFile(file)) {
                    // Show dialog to redirect to Microsoft 365 Online
                    showMicrosoft365RedirectDialog(file);
                } else {
                    // For other file types, show chooser
                    showFileOpenChooser(openIntent);
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "FileProvider error - file path might not be in configured paths", e);
            Toast.makeText(context, 
                context.getString(com.example.healthylifehub.R.string.error_file_path, e.getMessage()), 
                Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found to handle the intent", e);
            
            // Check if it's an Excel file and offer Microsoft 365 redirect
            if (isExcelFile(file)) {
                showMicrosoft365RedirectDialog(file);
            } else {
                Toast.makeText(context, 
                    context.getString(com.example.healthylifehub.R.string.error_no_app_to_open), 
                    Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening report file", e);
            Toast.makeText(context, 
                context.getString(com.example.healthylifehub.R.string.error_cannot_open_file, e.getMessage()), 
                Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Check if file is an Excel file
     */
    private boolean isExcelFile(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".xlsx") || fileName.endsWith(".xls");
    }
    
    /**
     * Show dialog to redirect to Microsoft 365 Online
     */
    private void showMicrosoft365RedirectDialog(File file) {
        new android.app.AlertDialog.Builder(context)
            .setTitle(context.getString(com.example.healthylifehub.R.string.open_excel_file))
            .setMessage(context.getString(com.example.healthylifehub.R.string.no_excel_app_message))
            .setPositiveButton(context.getString(com.example.healthylifehub.R.string.open_microsoft_365), (dialog, which) -> {
                openInMicrosoft365Online();
            })
            .setNegativeButton(context.getString(com.example.healthylifehub.R.string.download_excel_app), (dialog, which) -> {
                openPlayStoreForExcel();
            })
            .setNeutralButton(context.getString(com.example.healthylifehub.R.string.share), (dialog, which) -> {
                shareReport(file);
            })
            .show();
    }
    
    /**
     * Open Microsoft 365 Online (Excel Online)
     */
    private void openInMicrosoft365Online() {
        try {
            // Open Microsoft 365 web app
            String url = "https://www.office.com/launch/excel";
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(browserIntent);
            
            Toast.makeText(context, 
                context.getString(com.example.healthylifehub.R.string.opening_microsoft_365), 
                Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Error opening Microsoft 365 Online", e);
            Toast.makeText(context, 
                context.getString(com.example.healthylifehub.R.string.cannot_open_browser), 
                Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Open Google Play Store to download Microsoft Excel app
     */
    private void openPlayStoreForExcel() {
        try {
            // Try to open Play Store app
            Intent intent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("market://details?id=com.microsoft.office.excel"));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // If Play Store app is not available, open in browser
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("https://play.google.com/store/apps/details?id=com.microsoft.office.excel"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(context, 
                    context.getString(com.example.healthylifehub.R.string.cannot_open_play_store), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Show file open chooser
     */
    private void showFileOpenChooser(Intent openIntent) {
        try {
            Intent chooserIntent = Intent.createChooser(openIntent, 
                context.getString(com.example.healthylifehub.R.string.open_excel_file));
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooserIntent);
            Log.d(TAG, "Chooser opened successfully");
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Even chooser failed, suggesting download");
            Toast.makeText(context, 
                context.getString(com.example.healthylifehub.R.string.no_app_for_pdf), 
                Toast.LENGTH_LONG).show();
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
