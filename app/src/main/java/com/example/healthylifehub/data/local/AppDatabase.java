package com.example.healthylifehub.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.healthylifehub.data.local.dao.HealthMetricDao;
import com.example.healthylifehub.data.local.dao.MedicalRecordDao;
import com.example.healthylifehub.data.local.dao.NotificationHistoryDao;
import com.example.healthylifehub.data.local.dao.NotificationSettingsDao;
import com.example.healthylifehub.data.local.dao.ReminderDao;
import com.example.healthylifehub.data.local.dao.ReminderHistoryDao;
import com.example.healthylifehub.data.local.dao.SyncStatusDao;
import com.example.healthylifehub.data.local.dao.UserDao;
import com.example.healthylifehub.data.local.dao.HealthArticleDao;
import com.example.healthylifehub.data.model.HealthMetric;
import com.example.healthylifehub.data.model.MedicalRecord;
import com.example.healthylifehub.data.model.NotificationHistory;
import com.example.healthylifehub.data.model.NotificationSettings;
import com.example.healthylifehub.data.model.Reminder;
import com.example.healthylifehub.data.model.ReminderHistory;
import com.example.healthylifehub.data.model.SyncStatus;
import com.example.healthylifehub.data.model.User;
import com.example.healthylifehub.data.model.HealthArticle;
import com.example.healthylifehub.data.local.migrations.DatabaseMigrations;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Room Database for offline-first caching
 * 
 * Dùng trực tiếp Model làm Entity (đơn giản hơn, không cần Entity riêng)
 * 
 * Architecture:
 * - Local database acts as single source of truth
 * - Data flows: Firestore → Room → UI
 * - UI always reads from Room (instant, works offline)
 * - Background sync keeps Room updated from Firestore
 */
@Database(
    entities = {
        User.class,
        HealthMetric.class,
        Reminder.class,
        ReminderHistory.class,
        SyncStatus.class,
        MedicalRecord.class,
        NotificationSettings.class,
        NotificationHistory.class,
        HealthArticle.class
    },
    version = 13,  // Incremented for HealthMetric unit field
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "healthylife_hub.db";
    private static volatile AppDatabase INSTANCE;
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    
    // DAOs
    public abstract UserDao userDao();
    public abstract HealthMetricDao healthMetricDao();
    public abstract ReminderDao reminderDao();
    public abstract ReminderHistoryDao reminderHistoryDao();
    public abstract SyncStatusDao syncStatusDao();
    public abstract MedicalRecordDao medicalRecordDao();
    public abstract NotificationSettingsDao notificationSettingsDao();
    public abstract NotificationHistoryDao notificationHistoryDao();
    public abstract HealthArticleDao healthArticleDao();

    /**
     * Get database instance (Singleton pattern)
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME
                    )
                    // Allow queries on main thread for testing (remove in production)
                    // .allowMainThreadQueries()
                    .addMigrations(DatabaseMigrations.getAllMigrations())
                    .fallbackToDestructiveMigrationOnDowngrade() // Only destroy on downgrade, not missing migrations
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Clear all data (for logout)
     * Runs on background thread using Room's built-in method
     */
    public void clearAllData() {
        dbExecutor.execute(() -> {
            try {
                clearAllTables();
                android.util.Log.d("AppDatabase", "✅ Cleared all cache data");
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "Error clearing cache", e);
            }
        });
    }
    
    /**
     * Clear data for specific user (logout)
     */
    public void clearUserData(String userId) {
        dbExecutor.execute(() -> {
            try {
                healthMetricDao().deleteAllMetricsForUser(userId);
                reminderHistoryDao().deleteAll();
                reminderDao().deleteByUserId(userId);
                syncStatusDao().deleteByUserId(userId);
                medicalRecordDao().deleteByUserId(userId);
                notificationHistoryDao().deleteAllForUser(userId);
                userDao().deleteUser(userId);
                android.util.Log.d("AppDatabase", "✅ Cleared cache data for user: " + userId);
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "Error clearing user cache", e);
            }
        });
    }
}
