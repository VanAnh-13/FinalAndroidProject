# 🔧 Cleanup Firestore - Xóa field trùng lặp

## **Vấn đề:**

Firestore có 2 fields trùng lặp:
```json
{
  "profile": {
    "medicalHistory": "..." // ✅ ĐÚNG - Nested
  },
  "profile.medicalHistory": "..." // ❌ SAI - Root level
}
```

---

## **Cách fix:**

### **Option 1: Xóa thủ công trên Firebase Console**

1. Mở Firebase Console: https://console.firebase.google.com
2. Chọn project → Firestore Database
3. Vào collection `users`
4. Chọn document user: `p8mAS7boUYV4CcAkg8LwZwKVskf1`
5. **Xóa field:** `profile.medicalHistory` (field ở root level)
6. **Giữ lại:** `profile` → `medicalHistory` (nested)

---

### **Option 2: Code cleanup (Chạy 1 lần)**

Thêm vào `UserRepository.java`:

```java
/**
 * Cleanup duplicate medical history field (run once)
 */
public CompletableFuture<Void> cleanupDuplicateMedicalHistory() {
    return CompletableFuture.runAsync(() -> {
        String userId = getCurrentUserId();
        if (userId == null) return;
        
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("profile.medicalHistory", FieldValue.delete());
            
            Task<Void> task = db.collection(COLLECTION_USERS)
                    .document(userId)
                    .update(updates);
            
            while (!task.isComplete()) {
                Thread.sleep(100);
            }
            
            if (task.isSuccessful()) {
                Log.d("UserRepository", "✅ Cleaned up duplicate field");
            }
        } catch (Exception e) {
            Log.e("UserRepository", "❌ Cleanup failed", e);
        }
    }, executorService);
}
```

Gọi trong Activity:
```java
// Run once to cleanup
repository.cleanupDuplicateMedicalHistory();
```

---

## **Sau khi fix:**

Firestore structure đúng:
```json
{
  "userId": "p8mAS7boUYV4CcAkg8LwZwKVskf1",
  "email": "gaa@gmail.com",
  "displayName": "WDFSEFS",
  "profile": {
    "fullName": "WDFSEFS",
    "dateOfBirth": "2005-11-08",
    "gender": "female",
    "height": 123,
    "weight": 88,
    "bloodType": "B-",
    "medicalHistory": "dssdasaddsa||sddsadsasdads||dssddsasdads||sadsaddds||sadsadasd"
  },
  "updatedAt": "2025-11-08T17:08:07Z"
}
```

**Không còn:** `profile.medicalHistory` ở root level ❌

---

## **Giải thích lỗi:**

### **Trước (SAI):**
```java
updateData.put("profile.medicalHistory", value);
// → Tạo field "profile.medicalHistory" ở ROOT level
```

### **Sau (ĐÚNG):**
```java
Map<String, Object> profileData = snapshot.get("profile");
profileData.put("medicalHistory", value);
updateData.put("profile", profileData);
// → Update nested: profile { medicalHistory: value }
```

---

**Recommendation:** Xóa thủ công trên Firebase Console (nhanh nhất) ✅
