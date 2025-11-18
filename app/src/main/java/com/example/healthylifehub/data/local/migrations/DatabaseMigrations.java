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
     * Get all available migrations
     */
    public static Migration[] getAllMigrations() {
        return new Migration[]{
            MIGRATION_8_9,
            MIGRATION_9_10
        };
    }
}