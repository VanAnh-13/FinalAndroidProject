package com.example.healthylifehub.data.local.migrations;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Database migrations for smart reminder system enhancements
 */
public class DatabaseMigrations {
    
    /**
     * Migration from version 8 to 9
     * Adds smart reminder system fields and ReminderHistory table
     */
    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add new fields to reminders table
            database.execSQL("ALTER TABLE reminders ADD COLUMN deadline INTEGER");
            database.execSQL("ALTER TABLE reminders ADD COLUMN totalExpected INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE reminders ADD COLUMN completedCount INTEGER NOT NULL DEFAULT 0");
            
            // Create reminder_history table
            database.execSQL("CREATE TABLE IF NOT EXISTS reminder_history (" +
                    "id TEXT PRIMARY KEY NOT NULL, " +
                    "reminderId TEXT NOT NULL, " +
                    "actionType TEXT NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "scheduledTime INTEGER NOT NULL, " +
                    "FOREIGN KEY(reminderId) REFERENCES reminders(reminderId) ON DELETE CASCADE)");
            
            // Create indices for better query performance
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reminder_history_reminderId ON reminder_history(reminderId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reminder_history_timestamp ON reminder_history(timestamp)");
            
            // Update existing reminders to calculate totalExpected based on frequency
            // For existing reminders without deadline, set totalExpected to 0 (unlimited)
            database.execSQL("UPDATE reminders SET totalExpected = 0 WHERE deadline IS NULL");
            
            // For reminders with frequency "once", set totalExpected to 1
            database.execSQL("UPDATE reminders SET totalExpected = 1 WHERE frequency = 'once' AND deadline IS NOT NULL");
        }
    };
    
    /**
     * Migration from version 9 to 10
     * Adds missing fields to notification_settings table
     */
    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add missing fields to notification_settings table
            database.execSQL("ALTER TABLE notification_settings ADD COLUMN reminderSoundUri TEXT");
            database.execSQL("ALTER TABLE notification_settings ADD COLUMN reminderVibrationPattern TEXT");
        }
    };
    
    /**
     * Migration from version 10 to 11
     * Adds notification_history table
     */
    public static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create notification_history table without indices and without default value for isRead
            database.execSQL("CREATE TABLE IF NOT EXISTS notification_history (" +
                    "notificationId TEXT PRIMARY KEY NOT NULL, " +
                    "userId TEXT, " +
                    "title TEXT, " +
                    "message TEXT, " +
                    "type TEXT, " +
                    "relatedId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "isRead INTEGER NOT NULL, " +
                    "actionTaken TEXT)");
        }
    };
    
    /**
     * Migration from version 11 to 12
     * Fixes notification_history table schema - removes indices and default values
     * to match the entity definition
     */
    public static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Drop existing table with indices and recreate without them
            database.execSQL("DROP TABLE IF EXISTS notification_history");
            
            // Create notification_history table matching entity definition
            database.execSQL("CREATE TABLE IF NOT EXISTS notification_history (" +
                    "notificationId TEXT PRIMARY KEY NOT NULL, " +
                    "userId TEXT, " +
                    "title TEXT, " +
                    "message TEXT, " +
                    "type TEXT, " +
                    "relatedId TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "isRead INTEGER NOT NULL, " +
                    "actionTaken TEXT)");
        }
    };
    
    /**
     * Migration from version 12 to 13
     * Added 'unit' column to health_metrics table
     */
    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add unit column to health_metrics table
            database.execSQL("ALTER TABLE health_metrics ADD COLUMN unit TEXT");
        }
    };
    
    /**
     * Get all available migrations
     */
    public static Migration[] getAllMigrations() {
        return new Migration[]{
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13
        };
    }
}