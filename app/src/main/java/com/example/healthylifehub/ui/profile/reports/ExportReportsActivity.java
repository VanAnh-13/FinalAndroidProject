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
import com.example.healthylifehub.utils.ReportGenerator;
import com.example.healthylifehub.utils.report.ReportManager;
import com.example.healthylifehub.utils.report.ReportDataFetcher;
import com.example.healthylifehub.data.repository.HealthMetricRepository;
import com.example.healthylifehub.data.repository.RemindersRepository;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.Reminder;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
    private HealthMetricRepository healthMetricRepository;
    private RemindersRepository remindersRepository;
    private ProgressDialog progressDialog;

    public ExportReportsActivity() {
        super(ActivityExportReportsBinding::inflate);
    }

    @Override
    public void initData() {
        // Initialize report generator
        reportGenerator = new ReportGenerator(this);
        reportManager = new ReportManager(this);
        dataFetcher = new ReportDataFetcher(this);
        healthMetricRepository = new HealthMetricRepository(this);
        remindersRepository = new RemindersRepository();
        
        // Initialize adapter
        adapter = new GeneratedReportsAdapter(new GeneratedReportsAdapter.OnReportClickListener() {
            @Override
            public void onReportClick(GeneratedReport report) {
                // TODO: Open report with Intent
                Toast.makeText(ExportReportsActivity.this, 
                    "Mở: " + report.getTitle(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMoreClick(GeneratedReport report) {
                // TODO: Show bottom sheet with options (Share, Delete, etc.)
                Toast.makeText(ExportReportsActivity.this, 
                    "Tùy chọn: " + report.getTitle(), Toast.LENGTH_SHORT).show();
            }
        });
        
        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Đang tạo báo cáo");
        progressDialog.setMessage("Vui lòng đợi...");
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
                // TODO: Show date range picker dialog
                Toast.makeText(this, "Chọn khoảng thời gian tùy chỉnh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGeneratedReports() {
        // TODO: Load from Firestore
        List<GeneratedReport> reports = new ArrayList<>();
        
        // Sample data
        reports.add(new GeneratedReport(
            "Báo cáo Sức khỏe - 30/05/2024",
            "PDF - 2.1 MB",
            "30/05/2024",
            GeneratedReport.ReportType.PDF
        ));
        
        reports.add(new GeneratedReport(
            "Dữ liệu chỉ số - 25/05/2024",
            "Excel - 500 KB",
            "25/05/2024",
            GeneratedReport.ReportType.EXCEL
        ));

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
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get selected time range
        Date endDate = new Date();
        Date startDate = calculateStartDate();
        
        // Get selected file format
        int selectedFormat = getBinding().rgFileFormat.getCheckedRadioButtonId();
        boolean isPdf = selectedFormat == R.id.rbPdf;
        
        // Validate selected content
        if (!validateSelectedContent()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một nội dung báo cáo", 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress dialog
        progressDialog.show();
        
        // Start async pipeline processing
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
                            // Completed
                            progressDialog.dismiss();
                            Toast.makeText(ExportReportsActivity.this, 
                                "Báo cáo đã được tạo thành công!", Toast.LENGTH_SHORT).show();
                            
                            // Refresh reports list
                            loadGeneratedReports();
                        } else {
                            // Update progress
                            progressDialog.setProgress(progress);
                            progressDialog.setMessage(message);
                        }
                    });
                }
            }
        ).exceptionally(throwable -> {
            runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(ExportReportsActivity.this, 
                    "Lỗi: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
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
        } else {
            // Custom - default to 30 days for now
            // TODO: Implement custom date picker
            calendar.add(Calendar.DAY_OF_MONTH, -30);
        }
        
        return calendar.getTime();
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
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get selected time range
        Date endDate = new Date();
        Date startDate = calculateStartDate();
        
        // Get selected file format
        int selectedFormat = getBinding().rgFileFormat.getCheckedRadioButtonId();
        boolean isPdf = selectedFormat == R.id.rbPdf;
        
        // Validate selected content
        if (!validateSelectedContent()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất một nội dung báo cáo", 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress
        progressDialog.setMessage("Đang tải dữ liệu...");
        progressDialog.show();
        
        // Fetch data and generate report
        CompletableFuture<Void> reportFuture = dataFetcher.fetchHealthMetrics(currentUser.getUid(), startDate, endDate)
            .thenAccept(metrics -> {
                runOnUiThread(() -> {
                    progressDialog.setMessage("Đang tạo báo cáo...");
                });
                
                // Generate report based on format
                if (isPdf) {
                    reportManager.generateHealthMetricsPDF(metrics, startDate, endDate, new ReportManager.ReportCallback() {
                        @Override
                        public void onSuccess(File file) {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(ExportReportsActivity.this, 
                                    "Báo cáo đã được tạo thành công!", Toast.LENGTH_SHORT).show();
                                
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
                                    "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                                    "Báo cáo đã được tạo thành công!", Toast.LENGTH_SHORT).show();
                                
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
                                    "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                }
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(ExportReportsActivity.this, 
                        "Lỗi khi tải dữ liệu: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                });
                return null;
            });
    }
    
    /**
     * Show options to open or share report
     */
    private void showReportOptions(File file) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Báo cáo đã sẵn sàng")
            .setMessage("Bạn muốn làm gì với báo cáo?")
            .setPositiveButton("Mở", (dialog, which) -> {
                reportManager.openReport(file);
            })
            .setNegativeButton("Chia sẻ", (dialog, which) -> {
                reportManager.shareReport(file);
            })
            .setNeutralButton("Đóng", null)
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
