package com.example.healthylifehub.ui.profile.help;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityHelpSupportBinding;

/**
 * HelpSupportActivity - Help & Support center
 */
public class HelpSupportActivity extends BaseActivity<ActivityHelpSupportBinding> {
    
    public HelpSupportActivity() {
        super(ActivityHelpSupportBinding::inflate);
    }
    
    @Override
    public void initData() {
        // Setup initial data if needed
    }
    
    @Override
    public void bindData() {
        // Setup toolbar
        setSupportActionBar(getBinding().toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Trợ giúp & Hỗ trợ");
        }
        
        // Load app version
        loadAppInfo();
    }
    
    @Override
    public void setOnClick() {
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());
        
        // FAQ
        getBinding().actionFaq.setOnClickListener(v -> {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_faq_coming_soon), Toast.LENGTH_SHORT).show();
        });
        
        // Getting Started Guide
        getBinding().actionGettingStarted.setOnClickListener(v -> {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_getting_started_coming_soon), Toast.LENGTH_SHORT).show();
        });
        
        // Feature Tutorials
        getBinding().actionTutorials.setOnClickListener(v -> {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_tutorials_coming_soon), Toast.LENGTH_SHORT).show();
        });
        
        // Troubleshooting
        getBinding().actionTroubleshooting.setOnClickListener(v -> {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_troubleshooting_coming_soon), Toast.LENGTH_SHORT).show();
        });
        
        // Contact Support
        getBinding().actionContactSupport.setOnClickListener(v -> showContactOptions());
        
        // Send Feedback
        getBinding().actionSendFeedback.setOnClickListener(v -> sendFeedback());
        
        // Rate App
        getBinding().actionRateApp.setOnClickListener(v -> rateApp());
        
        // Report Bug
        getBinding().actionReportBug.setOnClickListener(v -> reportBug());
        
        // Community
        getBinding().actionCommunity.setOnClickListener(v -> openCommunity());
        
        // Video Tutorials
        getBinding().actionVideoTutorials.setOnClickListener(v -> openVideoTutorials());
    }
    
    private void loadAppInfo() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            
            getBinding().tvAppVersion.setText("Phiên bản " + versionName + " (" + versionCode + ")");
        } catch (Exception e) {
            getBinding().tvAppVersion.setText("Phiên bản: Không xác định");
        }
    }
    
    private void showContactOptions() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Liên hệ hỗ trợ");
        
        String[] options = {
            "📧 Gửi email hỗ trợ",
            "💬 Chat trực tuyến", 
            "📞 Gọi điện hỗ trợ",
            "📱 Hỗ trợ qua Zalo"
        };
        
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Email
                    sendSupportEmail();
                    break;
                case 1: // Chat
                    Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_chat_developing), Toast.LENGTH_SHORT).show();
                    break;
                case 2: // Phone
                    callSupport();
                    break;
                case 3: // Zalo
                    openZaloSupport();
                    break;
            }
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    
    private void sendSupportEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:support@healthylifehub.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ HealthyLife Hub - Android");
        
        String body = "Xin chào đội ngũ hỗ trợ,\n\n" +
                     "Tôi cần hỗ trợ về:\n" +
                     "[Mô tả vấn đề của bạn ở đây]\n\n" +
                     "--- Thông tin thiết bị ---\n" +
                     "Phiên bản app: " + getAppVersion() + "\n" +
                     "Thiết bị: " + android.os.Build.MODEL + "\n" +
                     "Android: " + android.os.Build.VERSION.RELEASE + "\n\n" +
                     "Cảm ơn!";
        
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Gửi email hỗ trợ"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_email_app_not_found), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void sendFeedback() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:feedback@healthylifehub.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Góp ý HealthyLife Hub");
        
        String body = "Xin chào,\n\n" +
                     "Tôi muốn góp ý về ứng dụng HealthyLife Hub:\n\n" +
                     "✅ Điều tôi thích:\n" +
                     "[Viết ở đây]\n\n" +
                     "🔧 Điều cần cải thiện:\n" +
                     "[Viết ở đây]\n\n" +
                     "💡 Tính năng mới đề xuất:\n" +
                     "[Viết ở đây]\n\n" +
                     "Cảm ơn!";
        
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Gửi góp ý"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_email_app_not_found), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void reportBug() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:bugs@healthylifehub.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Báo lỗi HealthyLife Hub");
        
        String body = "Báo cáo lỗi:\n\n" +
                     "🐛 Mô tả lỗi:\n" +
                     "[Mô tả chi tiết lỗi]\n\n" +
                     "📝 Các bước tái tạo:\n" +
                     "1. [Bước 1]\n" +
                     "2. [Bước 2]\n" +
                     "3. [Bước 3]\n\n" +
                     "📱 Kết quả mong đợi:\n" +
                     "[Điều gì nên xảy ra]\n\n" +
                     "❌ Kết quả thực tế:\n" +
                     "[Điều gì đã xảy ra]\n\n" +
                     "--- Thông tin thiết bị ---\n" +
                     "App version: " + getAppVersion() + "\n" +
                     "Device: " + android.os.Build.MODEL + "\n" +
                     "Android: " + android.os.Build.VERSION.RELEASE + "\n" +
                     "RAM: " + getTotalRAM() + "\n";
        
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Báo lỗi"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_email_app_not_found), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void callSupport() {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:+84123456789"));
        
        try {
            startActivity(callIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_cannot_make_call), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openZaloSupport() {
        try {
            // Try to open Zalo app with specific phone number
            Intent zaloIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://zalo.me/0123456789"));
            startActivity(zaloIntent);
        } catch (Exception e) {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_cannot_open_zalo), Toast.LENGTH_LONG).show();
        }
    }
    
    private void rateApp() {
        try {
            Intent rateIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("market://details?id=" + getPackageName()));
            startActivity(rateIntent);
        } catch (android.content.ActivityNotFoundException e) {
            // Fallback to Play Store website
            Intent webIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
            startActivity(webIntent);
        }
    }
    
    private void openCommunity() {
        // Open Facebook group or community forum
        Intent communityIntent = new Intent(Intent.ACTION_VIEW, 
            Uri.parse("https://facebook.com/groups/healthylifehub"));
        
        try {
            startActivity(communityIntent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_cannot_open_community), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openVideoTutorials() {
        // Open YouTube channel
        Intent youtubeIntent = new Intent(Intent.ACTION_VIEW, 
            Uri.parse("https://youtube.com/@healthylifehub"));
        
        try {
            startActivity(youtubeIntent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, getString(com.example.healthylifehub.R.string.toast_cannot_open_youtube), Toast.LENGTH_SHORT).show();
        }
    }
    
    private String getAppVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private String getTotalRAM() {
        android.app.ActivityManager actManager = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        android.app.ActivityManager.MemoryInfo memInfo = new android.app.ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        long totalMemory = memInfo.totalMem;
        return android.text.format.Formatter.formatFileSize(this, totalMemory);
    }
}