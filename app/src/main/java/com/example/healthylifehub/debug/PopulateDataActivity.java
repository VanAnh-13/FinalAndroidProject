package com.example.healthylifehub.debug;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.healthylifehub.R;
import com.example.healthylifehub.data.local.AppDatabase;
import com.example.healthylifehub.data.local.dao.HealthMetricDao;
import com.example.healthylifehub.data.model.HealthMetric;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DEBUG ACTIVITY - Populate Room DB with demo data
 * Chạy activity này để tạo data trực tiếp vào Room DB
 */
public class PopulateDataActivity extends AppCompatActivity {
    
    private TextView tvStatus;
    private Button btnPopulate;
    private ExecutorService executor;
    private HealthMetricDao dao;
    private String userId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_populate_data);
        
        tvStatus = findViewById(R.id.tvStatus);
        btnPopulate = findViewById(R.id.btnPopulate);
        
        executor = Executors.newSingleThreadExecutor();
        dao = AppDatabase.getInstance(this).healthMetricDao();
        
        // Get current user
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            tvStatus.setText("User: " + userId + "\n\nNhấn nút để tạo data");
        } else {
            tvStatus.setText("ERROR: Chưa login!");
            btnPopulate.setEnabled(false);
            return;
        }
        
        btnPopulate.setOnClickListener(v -> populateData());
    }
    
    private void populateData() {
        btnPopulate.setEnabled(false);
        tvStatus.setText("Đang tạo data THỰC TẾ...");
        
        executor.execute(() -> {
            try {
                Random random = new Random();
                int count = 0;
                
                // Generate 90 days of realistic health metrics
                long now = System.currentTimeMillis();
                long dayInMillis = 24 * 60 * 60 * 1000;
                
                for (int i = 0; i < 90; i++) {
                    long dayStart = now - (i * dayInMillis);
                    
                    // === MORNING BLOOD PRESSURE (7:00 AM) - DAILY ===
                    HealthMetric morningBP = new HealthMetric();
                    morningBP.setId("bp_morning_" + i);
                    morningBP.setUserId(userId);
                    morningBP.setType("blood_pressure");
                    
                    // Realistic blood pressure with slight variation
                    int baseSystolic = 125;
                    int baseDiastolic = 82;
                    morningBP.setSystolic(baseSystolic + random.nextInt(15) - 7); // 118-132
                    morningBP.setDiastolic(baseDiastolic + random.nextInt(10) - 5); // 77-87
                    
                    long morningTime = dayStart - (17 * 3600000); // 7:00 AM
                    morningBP.setMeasuredAt(new Date(morningTime));
                    morningBP.setNotes(i % 7 == 0 ? "Đo sau khi thức dậy, trước khi uống thuốc" : "Đo buổi sáng");
                    morningBP.setNeedsSync(false);
                    morningBP.setSynced(true);
                    morningBP.setLastSyncedAt(new Date());
                    
                    dao.insertMetric(morningBP);
                    count++;
                    
                    // === EVENING BLOOD PRESSURE (7:00 PM) - EVERY OTHER DAY ===
                    if (i % 2 == 0) {
                        HealthMetric eveningBP = new HealthMetric();
                        eveningBP.setId("bp_evening_" + i);
                        eveningBP.setUserId(userId);
                        eveningBP.setType("blood_pressure");
                        
                        // Evening BP slightly higher
                        eveningBP.setSystolic(baseSystolic + 5 + random.nextInt(15) - 7); // 123-137
                        eveningBP.setDiastolic(baseDiastolic + 3 + random.nextInt(10) - 5); // 80-90
                        
                        long eveningTime = dayStart - (5 * 3600000); // 7:00 PM
                        eveningBP.setMeasuredAt(new Date(eveningTime));
                        eveningBP.setNotes("Đo buổi tối trước khi ngủ");
                        eveningBP.setNeedsSync(false);
                        eveningBP.setSynced(true);
                        eveningBP.setLastSyncedAt(new Date());
                        
                        dao.insertMetric(eveningBP);
                        count++;
                    }
                    
                    // === BLOOD SUGAR (8:00 AM) - 3 TIMES A WEEK ===
                    if (i % 3 == 0) {
                        HealthMetric bloodSugar = new HealthMetric();
                        bloodSugar.setId("sugar_" + i);
                        bloodSugar.setUserId(userId);
                        bloodSugar.setType("blood_sugar");
                        
                        // Realistic fasting blood sugar: 85-110 mg/dL
                        double sugarValue = 95 + random.nextInt(20) - 10;
                        bloodSugar.setValue(sugarValue);
                        bloodSugar.setSystolic(0);
                        bloodSugar.setDiastolic(0);
                        
                        long sugarTime = dayStart - (16 * 3600000); // 8:00 AM
                        bloodSugar.setMeasuredAt(new Date(sugarTime));
                        bloodSugar.setNotes(sugarValue < 100 ? "Đo lúc đói, chỉ số bình thường" : "Đo sau ăn 2 giờ");
                        bloodSugar.setNeedsSync(false);
                        bloodSugar.setSynced(true);
                        bloodSugar.setLastSyncedAt(new Date());
                        
                        dao.insertMetric(bloodSugar);
                        count++;
                    }
                    
                    // === WEIGHT (6:00 AM) - ONCE A WEEK (SUNDAY) ===
                    if (i % 7 == 0) {
                        HealthMetric weight = new HealthMetric();
                        weight.setId("weight_" + i);
                        weight.setUserId(userId);
                        weight.setType("weight");
                        
                        // Realistic weight with gradual change: 66.5 - 69.5 kg
                        double baseWeight = 68.0;
                        double weightTrend = -0.02 * i; // Gradual weight loss
                        double weightValue = baseWeight + weightTrend + (random.nextDouble() * 1.0 - 0.5);
                        weight.setValue(Math.round(weightValue * 10.0) / 10.0); // Round to 1 decimal
                        weight.setSystolic(0);
                        weight.setDiastolic(0);
                        
                        long weightTime = dayStart - (18 * 3600000); // 6:00 AM
                        weight.setMeasuredAt(new Date(weightTime));
                        weight.setNotes("Cân buổi sáng sau khi đi vệ sinh, trước khi ăn sáng");
                        weight.setNeedsSync(false);
                        weight.setSynced(true);
                        weight.setLastSyncedAt(new Date());
                        
                        dao.insertMetric(weight);
                        count++;
                    }
                    
                    // === HEART RATE (7:30 AM) - DAILY ===
                    HealthMetric heartRate = new HealthMetric();
                    heartRate.setId("hr_" + i);
                    heartRate.setUserId(userId);
                    heartRate.setType("heart_rate");
                    
                    // Realistic resting heart rate: 65-80 bpm
                    double hrValue = 72 + random.nextInt(12) - 6;
                    heartRate.setValue(hrValue);
                    heartRate.setSystolic(0);
                    heartRate.setDiastolic(0);
                    
                    long hrTime = dayStart - (16 * 3600000 + 1800000); // 7:30 AM
                    heartRate.setMeasuredAt(new Date(hrTime));
                    heartRate.setNotes("Nhịp tim nghỉ ngơi, đo sau khi thức dậy 5 phút");
                    heartRate.setNeedsSync(false);
                    heartRate.setSynced(true);
                    heartRate.setLastSyncedAt(new Date());
                    
                    dao.insertMetric(heartRate);
                    count++;
                    
                    // === TEMPERATURE (9:00 AM) - TWICE A WEEK ===
                    if (i % 4 == 0) {
                        HealthMetric temp = new HealthMetric();
                        temp.setId("temp_" + i);
                        temp.setUserId(userId);
                        temp.setType("temperature");
                        
                        // Normal body temperature: 36.3 - 36.8°C
                        double tempValue = 36.5 + (random.nextDouble() * 0.4 - 0.2);
                        temp.setValue(Math.round(tempValue * 10.0) / 10.0);
                        temp.setSystolic(0);
                        temp.setDiastolic(0);
                        
                        long tempTime = dayStart - (15 * 3600000); // 9:00 AM
                        temp.setMeasuredAt(new Date(tempTime));
                        temp.setNotes("Nhiệt độ cơ thể bình thường");
                        temp.setNeedsSync(false);
                        temp.setSynced(true);
                        temp.setLastSyncedAt(new Date());
                        
                        dao.insertMetric(temp);
                        count++;
                    }
                }
                
                final int finalCount = count;
                runOnUiThread(() -> {
                    tvStatus.setText("✅ HOÀN THÀNH!\n\n" +
                        "Đã tạo " + finalCount + " health metrics\n" +
                        "Thời gian: 90 ngày\n\n" +
                        "Chi tiết:\n" +
                        "- Huyết áp: ~135 bản ghi\n" +
                        "- Đường huyết: ~30 bản ghi\n" +
                        "- Cân nặng: ~13 bản ghi\n" +
                        "- Nhịp tim: 90 bản ghi\n" +
                        "- Nhiệt độ: ~23 bản ghi\n\n" +
                        "Bây giờ mở app và xem Health Metrics!");
                    Toast.makeText(this, "Tạo data thành công!", Toast.LENGTH_LONG).show();
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    tvStatus.setText("❌ LỖI:\n" + e.getMessage() + "\n\n" + 
                        android.util.Log.getStackTraceString(e));
                    btnPopulate.setEnabled(true);
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
