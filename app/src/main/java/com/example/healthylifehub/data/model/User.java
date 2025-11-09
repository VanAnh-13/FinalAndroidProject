package com.example.healthylifehub.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.healthylifehub.data.local.converter.DateConverter;
import com.example.healthylifehub.data.local.converter.ProfileConverter;

import java.util.Date;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User - Vừa là Model vừa là Room Entity
 * 
 * Note: Room cần constructor với tất cả fields hoặc no-arg constructor + getters/setters
 * Lombok @Data sẽ generate getters/setters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor  // Room có thể dùng constructor này
@Entity(tableName = "users")
@TypeConverters({DateConverter.class, ProfileConverter.class})
public class User {
    @PrimaryKey(autoGenerate = false)
    @NonNull
    private String uid;
    
    private String email;
    private String displayName;
    private String photoUrl;
    private long createdAt;
    private long lastLogin;
    
    // Additional fields for Room
    private String role;
    private Map<String, Object> profile;
    private Date updatedAt;
    
    // Sync metadata
    private Date lastSyncedAt;
    private boolean needsSync;

    // Custom constructor for app usage
    @Ignore
    public User(String uid, String email, String displayName, String photoUrl) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.createdAt = System.currentTimeMillis();
        this.lastLogin = System.currentTimeMillis();
        this.needsSync = true;
        this.lastSyncedAt = new Date();
    }
}
