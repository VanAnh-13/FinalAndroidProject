package com.example.healthylifehub.ui.profile.reports;

import android.app.ProgressDialog;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.healthylifehub.R;
import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.data.model.GeneratedReport;
import com.example.healthylifehub.databinding.ActivityExportReportsBinding;
import com.example.healthylifehub.ui.profile.reports.adapter.GeneratedReportsAdapter;
import com.example.healthylifehub.utils.report.ReportGenerator;
import com.example.healthylifehub.utils.report.ReportManager;
import com.example.healthylifehub.utils.report.ReportDataFetcher;


import java.io.File;
import java.util.concurrent.CompletableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.app.DatePickerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ExportReportsActivity implements UC-HLH-07: Export Health Reports
 * Features:
 * - Select time range (7 days, 30 days, custom)
 * - Select file format (PDF, Excel)
 * - Select report content (checkboxes)
 * - Generate report with async pipeline processing
 * - Display generated reports list
 */
public class ExportReportsActivity extends BaseActivity<ActivityExportReportsBinding> {

    private GeneratedReportsAdapter adapter;
    private ReportGenerator reportGenerator;
    private ReportManager reportManager;
    private ReportDataFetcher dataFetcher;
    private ProgressDialog progressDialog;
    
    // Custom date range
    private Date customStartDate;
    private Date customEndDate;
    private SimpleDateFormat dateFormat;

    public ExportReportsActivity() {
        super(ActivityExportReportsBinding::inflate);
    }

    @Override
    public void initData() {
        // Initialize report generator
        reportGenerator = new ReportGenerator(this);
        reportManager = new ReportManager(this);
        dataFetcher = new ReportDataFetcher(this);
        
        // Initialize date format
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        
        // Initialize adapter
        adapter = new GeneratedReportsAdapter(new GeneratedReportsAdapter.OnReportClickListener() {
            @Override
            public void onReportClick(GeneratedReport report) {
                // TODO: Open report with Intent
                Toast.makeText(ExportReportsActivity.this, 
                    getString(R.string.toast_clicked, report.getTitle()), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMoreClick(GeneratedReport report) {
                // TODO: Show bottom sheet with options (Share, Delete, etc.)
                Toast.makeText(ExportReportsActivity.this, 
                    getString(R.string.toast_more_options), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Initialize progress dialog
        // Requirements: 7.6 - Display ProgressBar with percentage and stage messages
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.creating_report_title));
        progressDialog.setMessage(getString(R.string.please_wait));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
    }

    @Override
    public void bindData() {
        // Setup toolbar
        setSupportActionBar(getBinding().toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Setup RecyclerView
        getBinding().rvGeneratedReports.setLayoutManager(new LinearLayoutManager(this));
        getBinding().rvGeneratedReports.setAdapter(adapter);

        // Load generated reports
        loadGeneratedReports();
    }

    @Override
    public void setOnClick() {
        // Toolbar back button
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());

        // Create Report button - Main action with new report system
        getBinding().btnCreateReport.setOnClickListener(v -> {
            createReportWithNewSystem();
        });

        // Time range radio buttons
        getBinding().rgTimeRange.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCustom) {
                // Show date picker button when Custom is selected
                getBinding().btnSelectDateRange.setVisibility(View.VISIBLE);
            } else {
                // Hide date picker button for other options
                getBinding().btnSelectDateRange.setVisibility(View.GONE);
            }
        });
        
        // Date range picker button
        getBinding().btnSelectDateRange.setOnClickListener(v -> showDateRangePicker());
    }
    
    /**
     * Show date range picker dialog
     * First pick start date, then end date
     */
    private void showDateRangePicker() {
        Calendar calendar = Calendar.getInstance();
        
        // Show start date picker first
        DatePickerDialog startDatePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar startCal = Calendar.getInstance();
            startCal.set(year, month, dayOfMonth, 0, 0, 0);
            customStartDate = startCal.getTime();
            
            // Then show end date picker
            showEndDatePicker();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        
        startDatePicker.setTitle(getString(R.string.select_start_date));
        startDatePicker.show();
    }
    
    /**
     * Show end date picker after start date is selected
     */
    private void showEndDatePicker() {
        Calendar calendar = Calendar.getInstance();
        
        DatePickerDialog endDatePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar endCal = Calendar.getInstance();
            endCal.set(year, month, dayOfMonth, 23, 59, 59);
            customEndDate = endCal.getTime();
            
            // Validate date range
            if (customEndDate.before(customStartDate)) {
                Toast.makeText(this, getString(R.string.error_end_date_before_start), Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Update button text to show selected range
            String dateRangeText = dateFormat.format(customStartDate) + " - " + dateFormat.format(customEndDate);
            getBinding().btnSelectDateRange.setText(dateRangeText);
            
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        
        endDatePicker.setTitle(getString(R.string.select_end_date));
        endDatePicker.show();
    }

    private void loadGeneratedReports() {
        // TODO: Load from Firestore
        List<GeneratedReport> reports = new ArrayList<>();

        if (reports.isEmpty()) {
            getBinding().emptyState.setVisibility(View.VISIBLE);
            getBinding().rvGeneratedReports.setVisibility(View.GONE);
        } else {
            getBinding().emptyState.setVisibility(View.GONE);
            getBinding().rvGeneratedReports.setVisibility(View.VISIBLE);
            adapter.setReports(reports);
        }
    }

    /**
     * UC-HLH-07: Create report with async pipeline processing
     * Pipeline stages:
     * 1. Fetch data from Firestore (IO Thread)
     * 2. Calculate statistics (Compute Thread - Parallel)
     * 3. Generate charts (Compute Thread - Parallel)
     * 4. Render charts to Bitmap (Compute Thread)
     * 5. Create PDF (IO Thread)
     * 6. Upload to Firebase Storage (IO Thread)
     * 7. Save metadata to Firestore (IO Thread)
     */
    private void createReportWithPipeline() {
        // Validate user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.toast_please_login), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get selected time range
        Date startDate = calculateStartDate();
        Date endDate = calculateEndDate();
        
        // Validate custom date range if selected
        int selectedTimeRange = getBinding().rgTimeRange.getCheckedRadioButtonId();
        if (selectedTimeRange == R.id.rbCustom && (customStartDate == null || customEndDate == null)) {
            Toast.makeText(this, getString(R.string.toast_select_custom_time), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get selected file format
        int selectedFormat = getBinding().rgFileFormat.getCheckedRadioButtonId();
        boolean isPdf = selectedFormat == R.id.rbPdf;
        
        // Validate selected content
        if (!validateSelectedContent()) {
            Toast.makeText(this, getString(R.string.toast_select_report_content), 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress dialog
        progressDialog.show();
        
        // Start async pipeline processing
        // Requirements: 7.6 - Display ProgressBar with percentage and show stage messages
        // (fetching, calculating, rendering, etc.)
        reportGenerator.generatePDFReport(
            currentUser.getUid(),
            startDate,
            endDate,
            new ReportGenerator.ProgressCallback() {
                @Override
                public void onProgress(int progress, String message) {
                    runOnUiThread(() -> {
                        if (progress < 0) {
                            // Error occurred
                            progressDialog.dismiss();
                            Toast.makeText(ExportReportsActivity.this, 
                                message, Toast.LENGTH_LONG).show();
                        } else if (progress >= 100) {
                            // Completed - Requirements: 7.6 - Enable share button when complete
                            progressDialog.dismiss();
                            Toast.makeText(ExportReportsActivity.this, 
                                getString(R.string.report_generated_success), Toast.LENGTH_SHORT).show();
                            
                            // Refresh reports list
                            loadGeneratedReports();
                            
                            // Enable share functionality
                            // TODO: Show share button or dialog
                        } else {
                            // Update progress with percentage and stage message
                            // Requirements: 7.6 - Show stage messages (fetching, calculating, rendering, etc.)
                            progressDialog.setProgress(progress);
                            progressDialog.setMessage(message + " (" + progress + "%)");
                        }
                    });
                }
            }
        ).exceptionally(throwable -> {
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(ExportReportsActivity.this, 
                    getString(R.string.error, throwable.getMessage()), Toast.LENGTH_LONG).show();
            });
            return null;
        });
    }
    
    /**
     * Calculate start date based on selected time range
     */
    private Date calculateStartDate() {
        Calendar calendar = Calendar.getInstance();
        int selectedTimeRange = getBinding().rgTimeRange.getCheckedRadioButtonId();
        
        if (selectedTimeRange == R.id.rbLast7Days) {
            calendar.add(Calendar.DAY_OF_MONTH, -7);
        } else if (selectedTimeRange == R.id.rbLast30Days) {
            calendar.add(Calendar.DAY_OF_MONTH, -30);
        } else if (selectedTimeRange == R.id.rbCustom && customStartDate != null) {
            // Use custom start date
            return customStartDate;
        } else {
            // Default to 30 days if custom not set
            calendar.add(Calendar.DAY_OF_MONTH, -30);
        }
        
        return calendar.getTime();
    }
    
    /**
     * Calculate end date based on selected time range
     */
    private Date calculateEndDate() {
        int selectedTimeRange = getBinding().rgTimeRange.getCheckedRadioButtonId();
        
        if (selectedTimeRange == R.id.rbCustom && customEndDate != null) {
            return customEndDate;
        }
        
        return new Date(); // Default to now
    }
    
    /**
     * Validate that at least one content option is selected
     */
    private boolean validateSelectedContent() {
        return getBinding().cbPersonalInfo.isChecked() ||
               getBinding().cbHealthMetrics.isChecked() ||
               getBinding().cbTrendCharts.isChecked() ||
               getBinding().cbMedicalHistory.isChecked() ||
               getBinding().cbMedicineList.isChecked() ||
               getBinding().cbAnalysis.isChecked();
    }
    
    /**
     * Create report with new system (using local database)
     */
    private void createReportWithNewSystem() {
        // Validate user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.toast_please_login), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get selected time range
        Date startDate = calculateStartDate();
        Date endDate = calculateEndDate();
        
        // Validate custom date range if selected
        int selectedTimeRange = getBinding().rgTimeRange.getCheckedRadioButtonId();
        if (selectedTimeRange == R.id.rbCustom && (customStartDate == null || customEndDate == null)) {
            Toast.makeText(this, getString(R.string.toast_select_custom_time), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get selected file format
        int selectedFormat = getBinding().rgFileFormat.getCheckedRadioButtonId();
        boolean isPdf = selectedFormat == R.id.rbPdf;
        
        // Validate selected content
        if (!validateSelectedContent()) {
            Toast.makeText(this, getString(R.string.toast_select_report_content), 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress
        progressDialog.setMessage(getString(R.string.loading_data));
        progressDialog.show();
        
        // Fetch data and generate report
        CompletableFuture<Void> reportFuture = dataFetcher.fetchHealthMetrics(currentUser.getUid(), startDate, endDate)
            .thenAccept(metrics -> {
                runOnUiThread(() -> {
                    progressDialog.setMessage(getString(R.string.creating_report_message));
                });
                
                // Generate report based on format
                if (isPdf) {
                    reportManager.generateHealthMetricsPDF(metrics, startDate, endDate, new ReportManager.ReportCallback() {
                        @Override
                        public void onSuccess(File file) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(ExportReportsActivity.this, 
                                    getString(R.string.report_generated_success), Toast.LENGTH_SHORT).show();
                                
                                // Ask user to open or share
                                showReportOptions(file);
                                
                                // Refresh list
                                loadGeneratedReports();
                            });
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(ExportReportsActivity.this, 
                                    getString(R.string.error, e.getMessage()), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                } else {
                    reportManager.generateHealthMetricsExcel(metrics, startDate, endDate, new ReportManager.ReportCallback() {
                        @Override
                        public void onSuccess(File file) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(ExportReportsActivity.this, 
                                    getString(R.string.report_generated_success), Toast.LENGTH_SHORT).show();
                                
                                // Ask user to open or share
                                showReportOptions(file);
                                
                                // Refresh list
                                loadGeneratedReports();
                            });
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(ExportReportsActivity.this, 
                                    getString(R.string.error, e.getMessage()), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                }
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ExportReportsActivity.this, 
                        getString(R.string.error_loading_data, throwable.getMessage()), Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    /**
     * Show options to open or share report
     */
    private void showReportOptions(File file) {
        new android.app.AlertDialog.Builder(this)
            .setTitle(R.string.report_ready_title)
            .setMessage(R.string.report_ready_message)
            .setPositiveButton(R.string.open, (dialog, which) -> {
                reportManager.openReport(file);
            })
            .setNegativeButton(R.string.share, (dialog, which) -> {
                reportManager.shareReport(file);
            })
            .setNeutralButton(R.string.close, null)
            .show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup resources
        if (reportGenerator != null) {
            reportGenerator.shutdown();
        }
        if (reportManager != null) {
            reportManager.shutdown();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
