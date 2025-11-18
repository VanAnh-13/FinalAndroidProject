package com.example.healthylifehub.ui.shared;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import com.example.healthylifehub.R;

/**
 * NetworkDialog - Thông báo mạng đơn giản ở giữa màn hình
 * Có thể tắt được, hiển thị trạng thái mạng với icon và màu sắc
 */
public class NetworkDialog {
    
    private Dialog dialog;
    private TextView tvMessage;
    private ImageView ivIcon;
    private CardView cardView;
    private Context context;
    
    public NetworkDialog(Context context) {
        this.context = context;
        createDialog();
    }
    
    private void createDialog() {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Inflate custom layout
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_network_status, null);
        dialog.setContentView(view);
        
        // Find views
        tvMessage = view.findViewById(R.id.tv_message);
        ivIcon = view.findViewById(R.id.iv_icon);
        cardView = view.findViewById(R.id.card_view);
        ImageView ivClose = view.findViewById(R.id.iv_close);
        
        // Setup close button
        ivClose.setOnClickListener(v -> dismiss());
        
        // Setup dialog properties
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.CENTER);
            
            WindowManager.LayoutParams params = window.getAttributes();
            params.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
        
        // Auto dismiss after 3 seconds for success messages
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
    }
    
    /**
     * Hiển thị trạng thái offline
     */
    public void showOffline() {
        tvMessage.setText(context.getString(R.string.network_offline));
        ivIcon.setImageResource(R.drawable.ic_wifi_off);
        cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.holo_red_light));
        
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }
    
    /**
     * Hiển thị trạng thái online
     */
    public void showOnline(String networkType) {
        String message = "✅ Đã kết nối " + networkType + "\nDữ liệu sẽ được đồng bộ";
        tvMessage.setText(message);
        ivIcon.setImageResource(R.drawable.ic_wifi_on);
        cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
        
        if (!dialog.isShowing()) {
            dialog.show();
        }
        
        // Auto dismiss after 2 seconds for success
        tvMessage.postDelayed(this::dismiss, 2000);
    }
    
    /**
     * Hiển thị trạng thái đang kết nối
     */
    public void showConnecting() {
        tvMessage.setText(context.getString(R.string.network_connecting));
        ivIcon.setImageResource(R.drawable.ic_sync);
        cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.holo_orange_light));
        
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }
    
    /**
     * Hiển thị lỗi với custom message
     */
    public void showError(String message) {
        tvMessage.setText("❌ " + message);
        ivIcon.setImageResource(R.drawable.ic_error);
        cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.holo_red_light));
        
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }
    
    /**
     * Hiển thị thành công với custom message
     */
    public void showSuccess(String message) {
        tvMessage.setText("✅ " + message);
        ivIcon.setImageResource(R.drawable.ic_check);
        cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.holo_green_light));
        
        if (!dialog.isShowing()) {
            dialog.show();
        }
        
        // Auto dismiss after 2 seconds
        tvMessage.postDelayed(this::dismiss, 2000);
    }
    
    /**
     * Ẩn dialog
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    /**
     * Kiểm tra dialog có đang hiển thị không
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
    
    /**
     * Cleanup khi không dùng nữa
     */
    public void destroy() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }
}