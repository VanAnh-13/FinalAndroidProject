package com.example.healthylifehub.ui.profile.privacy;

import android.content.Intent;
import android.view.View;

import com.example.healthylifehub.base.BaseActivity;
import com.example.healthylifehub.databinding.ActivityLegalDocumentBinding;

/**
 * LegalDocumentActivity - Display Privacy Policy or Terms of Service
 */
public class LegalDocumentActivity extends BaseActivity<ActivityLegalDocumentBinding> {
    
    private String documentType;
    
    public LegalDocumentActivity() {
        super(ActivityLegalDocumentBinding::inflate);
    }
    
    @Override
    public void initData() {
        Intent intent = getIntent();
        documentType = intent.getStringExtra("document_type");
        
        if (documentType == null) {
            documentType = "privacy_policy"; // Default
        }
    }
    
    @Override
    public void bindData() {
        // Setup toolbar based on document type
        setSupportActionBar(getBinding().toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            
            if ("privacy_policy".equals(documentType)) {
                getSupportActionBar().setTitle("Chính sách bảo mật");
                loadPrivacyPolicy();
            } else {
                getSupportActionBar().setTitle("Điều khoản sử dụng");
                loadTermsOfService();
            }
        }
    }
    
    @Override
    public void setOnClick() {
        getBinding().toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void loadPrivacyPolicy() {
        String privacyPolicy = buildPrivacyPolicyContent();
        getBinding().tvContent.setText(privacyPolicy);
    }
    
    private void loadTermsOfService() {
        String termsOfService = buildTermsOfServiceContent();
        getBinding().tvContent.setText(termsOfService);
    }
    
    private String buildPrivacyPolicyContent() {
        return "# CHÍNH SÁCH BẢO MẬT\n" +
                "**HealthyLife Hub**\n\n" +
                
                "## 1. Thu thập thông tin\n" +
                "Chúng tôi thu thập thông tin bạn cung cấp khi:\n" +
                "• Đăng ký tài khoản\n" +
                "• Nhập dữ liệu sức khỏe\n" +
                "• Sử dụng các tính năng của ứng dụng\n\n" +
                
                "## 2. Sử dụng thông tin\n" +
                "Thông tin được sử dụng để:\n" +
                "• Cung cấp dịch vụ theo dõi sức khỏe\n" +
                "• Gửi nhắc nhở và thông báo\n" +
                "• Cải thiện chất lượng ứng dụng\n" +
                "• Phân tích xu hướng sức khỏe\n\n" +
                
                "## 3. Bảo vệ dữ liệu\n" +
                "Chúng tôi cam kết:\n" +
                "• Mã hóa dữ liệu nhạy cảm\n" +
                "• Sử dụng kết nối bảo mật (HTTPS)\n" +
                "• Không chia sẻ thông tin cá nhân\n" +
                "• Tuân thủ các tiêu chuẩn bảo mật\n\n" +
                
                "## 4. Quyền của bạn\n" +
                "Bạn có quyền:\n" +
                "• Xem và chỉnh sửa thông tin cá nhân\n" +
                "• Yêu cầu xóa dữ liệu\n" +
                "• Xuất dữ liệu cá nhân\n" +
                "• Rút lại sự đồng ý\n\n" +
                
                "## 5. Cookies và theo dõi\n" +
                "Ứng dụng sử dụng:\n" +
                "• Cookies cần thiết cho chức năng\n" +
                "• Analytics để cải thiện dịch vụ\n" +
                "• Không theo dõi trên các ứng dụng khác\n\n" +
                
                "## 6. Chia sẻ dữ liệu\n" +
                "Chúng tôi KHÔNG:\n" +
                "• Bán thông tin cá nhân\n" +
                "• Chia sẻ với bên thứ ba không liên quan\n" +
                "• Sử dụng cho mục đích quảng cáo\n\n" +
                
                "## 7. Lưu trữ dữ liệu\n" +
                "• Dữ liệu được lưu trữ trên Firebase (Google)\n" +
                "• Tuân thủ GDPR và các luật bảo mật\n" +
                "• Backup định kỳ để đảm bảo an toàn\n\n" +
                
                "## 8. Liên hệ\n" +
                "Để biết thêm thông tin hoặc khiếu nại:\n" +
                "Email: privacy@healthylifehub.com\n" +
                "Địa chỉ: [Địa chỉ công ty]\n\n" +
                
                "**Cập nhật lần cuối: " + new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date()) + "**";
    }
    
    private String buildTermsOfServiceContent() {
        return "# ĐIỀU KHOẢN SỬ DỤNG\n" +
                "**HealthyLife Hub**\n\n" +
                
                "## 1. Chấp nhận điều khoản\n" +
                "Bằng việc sử dụng ứng dụng, bạn đồng ý tuân thủ các điều khoản này.\n\n" +
                
                "## 2. Mô tả dịch vụ\n" +
                "HealthyLife Hub cung cấp:\n" +
                "• Theo dõi chỉ số sức khỏe\n" +
                "• Nhắc nhở uống thuốc\n" +
                "• Quản lý hồ sơ y tế\n" +
                "• Phân tích và báo cáo sức khỏe\n\n" +
                
                "## 3. Tài khoản người dùng\n" +
                "Bạn có trách nhiệm:\n" +
                "• Cung cấp thông tin chính xác\n" +
                "• Bảo mật thông tin đăng nhập\n" +
                "• Thông báo khi phát hiện truy cập trái phép\n" +
                "• Sử dụng ứng dụng một cách hợp pháp\n\n" +
                
                "## 4. Quy định sử dụng\n" +
                "NGHIÊM CẤM:\n" +
                "• Sử dụng cho mục đích bất hợp pháp\n" +
                "• Tấn công hệ thống hoặc người dùng khác\n" +
                "• Chia sẻ thông tin y tế của người khác\n" +
                "• Tạo tài khoản giả mạo\n\n" +
                
                "## 5. Tuyên bố từ chối trách nhiệm\n" +
                "Ứng dụng chỉ mang tính tham khảo:\n" +
                "• KHÔNG thay thế ý kiến bác sĩ\n" +
                "• KHÔNG chẩn đoán hoặc điều trị bệnh\n" +
                "• Luôn tham khảo chuyên gia y tế\n" +
                "• Chúng tôi không chịu trách nhiệm về quyết định y tế\n\n" +
                
                "## 6. Quyền sở hữu trí tuệ\n" +
                "• Ứng dụng và nội dung thuộc bản quyền của chúng tôi\n" +
                "• Bạn được cấp phép sử dụng cá nhân\n" +
                "• Không được sao chép, phân phối\n" +
                "• Dữ liệu bạn nhập vẫn thuộc về bạn\n\n" +
                
                "## 7. Chấm dứt dịch vụ\n" +
                "Chúng tôi có quyền:\n" +
                "• Tạm ngừng tài khoản vi phạm\n" +
                "• Thay đổi hoặc ngừng dịch vụ\n" +
                "• Thông báo trước khi có thay đổi lớn\n\n" +
                
                "## 8. Giới hạn trách nhiệm\n" +
                "Chúng tôi không chịu trách nhiệm cho:\n" +
                "• Thiệt hại do sử dụng thông tin từ ứng dụng\n" +
                "• Mất dữ liệu do lỗi thiết bị\n" +
                "• Gián đoạn dịch vụ không mong muốn\n\n" +
                
                "## 9. Thay đổi điều khoản\n" +
                "• Chúng tôi có thể cập nhật điều khoản\n" +
                "• Thông báo qua ứng dụng hoặc email\n" +
                "• Tiếp tục sử dụng nghĩa là chấp nhận thay đổi\n\n" +
                
                "## 10. Luật áp dụng\n" +
                "Các điều khoản tuân thủ pháp luật Việt Nam.\n\n" +
                
                "## 11. Liên hệ\n" +
                "Email: legal@healthylifehub.com\n" +
                "Điện thoại: [Số điện thoại]\n\n" +
                
                "**Cập nhật lần cuối: " + new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date()) + "**";
    }
}