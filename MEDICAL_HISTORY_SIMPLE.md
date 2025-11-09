# ✅ Chức năng Tiền sử Bệnh án - Phiên bản đơn giản

## 🎯 **Đã hoàn thành**

✅ Load tiền sử bệnh án từ Firestore  
✅ Pre-fill 5 fields khi có data  
✅ Save tất cả fields vào 1 String  
✅ Loading state khi đang lưu  
✅ Error handling  
✅ Success feedback  

---

## 📊 **Architecture (Đơn giản)**

```
MedicalHistoryActivity
    ↓
UserRepository (đã có sẵn)
    ↓
Firestore: users/{userId}/profile/medicalHistory (String)
```

**Không cần:**
- ❌ Model riêng
- ❌ ViewModel riêng
- ❌ Nested object

---

## 💾 **Firestore Structure**

```json
{
  "users": {
    "{userId}": {
      "profile": {
        "medicalHistory": "Tiểu đường||Penicillin||Metformin||Phẫu thuật 2015||Bố mẹ tim mạch"
      }
    }
  }
}
```

**Format:** `field1||field2||field3||field4||field5`

---

## 💻 **Code Implementation**

### **1. UserRepository - Methods đã có**

```java
/**
 * Load medical history (String)
 */
public LiveData<String> loadMedicalHistory() {
    return getProfileField("profile.medicalHistory");
}

/**
 * Update medical history (String)
 */
public CompletableFuture<Boolean> updateMedicalHistory(String medicalHistory) {
    Map<String, Object> updateData = new HashMap<>();
    updateData.put("profile.medicalHistory", medicalHistory);
    updateData.put("updatedAt", Timestamp.now());
    
    return db.collection(COLLECTION_USERS)
        .document(userId)
        .set(updateData, SetOptions.merge());
}
```

### **2. MedicalHistoryActivity**

```java
public class MedicalHistoryActivity extends BaseActivity<ActivityMedicalHistoryBinding> {

    private UserRepository repository;
    private boolean isSaving = false;

    @Override
    public void initData() {
        repository = new UserRepository();
        loadMedicalHistory();
    }
    
    /**
     * Load và split String thành 5 fields
     */
    private void loadMedicalHistory() {
        repository.loadMedicalHistory().observe(this, historyText -> {
            if (historyText != null && !historyText.isEmpty()) {
                String[] parts = historyText.split("\\|\\|");
                if (parts.length >= 5) {
                    getBinding().etChronicConditions.setText(parts[0]);
                    getBinding().etAllergies.setText(parts[1]);
                    getBinding().etCurrentMedications.setText(parts[2]);
                    getBinding().etPastSurgeries.setText(parts[3]);
                    getBinding().etFamilyHistory.setText(parts[4]);
                }
            }
        });
    }
    
    /**
     * Combine 5 fields thành 1 String và save
     */
    private void saveMedicalHistory() {
        if (isSaving) return;
        
        String conditions = getBinding().etChronicConditions.getText().toString().trim();
        String allergies = getBinding().etAllergies.getText().toString().trim();
        String medications = getBinding().etCurrentMedications.getText().toString().trim();
        String surgeries = getBinding().etPastSurgeries.getText().toString().trim();
        String familyHistory = getBinding().etFamilyHistory.getText().toString().trim();
        
        // Combine với separator ||
        String combinedHistory = conditions + "||" + allergies + "||" + 
                                medications + "||" + surgeries + "||" + familyHistory;
        
        isSaving = true;
        getBinding().btnSaveHistory.setEnabled(false);
        getBinding().btnSaveHistory.setText("Đang lưu...");
        
        repository.updateMedicalHistory(combinedHistory)
            .thenAccept(success -> {
                runOnUiThread(() -> {
                    isSaving = false;
                    getBinding().btnSaveHistory.setEnabled(true);
                    getBinding().btnSaveHistory.setText("Lưu");
                    
                    if (success) {
                        Toast.makeText(this, "✅ Đã lưu tiền sử bệnh án", 
                            Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showErrorDialog("Lỗi khi lưu. Vui lòng thử lại.");
                    }
                });
            })
            .exceptionally(throwable -> {
                runOnUiThread(() -> {
                    isSaving = false;
                    getBinding().btnSaveHistory.setEnabled(true);
                    getBinding().btnSaveHistory.setText("Lưu");
                    showErrorDialog("Lỗi: " + throwable.getMessage());
                });
                return null;
            });
    }
}
```

---

## 🎨 **UI Fields**

| Field | Description | Example |
|-------|-------------|---------|
| **etChronicConditions** | Bệnh mãn tính | "Tiểu đường type 2" |
| **etAllergies** | Dị ứng | "Penicillin, hải sản" |
| **etCurrentMedications** | Thuốc đang dùng | "Metformin 500mg" |
| **etPastSurgeries** | Phẫu thuật đã qua | "Phẫu thuật ruột thừa (2015)" |
| **etFamilyHistory** | Tiền sử gia đình | "Bố mẹ có tiền sử tim mạch" |

---

## 🔄 **Data Flow**

### **Load:**
```
1. Open screen
2. Repository.loadMedicalHistory()
3. Get String: "A||B||C||D||E"
4. Split by "||"
5. Pre-fill 5 fields
```

### **Save:**
```
1. Get text from 5 fields
2. Combine: field1 + "||" + field2 + "||" + ...
3. Repository.updateMedicalHistory(combined)
4. Save to Firestore
5. Success → Toast + Close
```

---

## 🧪 **Testing**

### **Test 1: First time user**
```
✅ Open screen → All fields empty
✅ Fill data → Click "Lưu"
✅ Success toast
✅ Reopen → Fields pre-filled
```

### **Test 2: Edit existing**
```
✅ Open screen → Fields pre-filled
✅ Modify → Click "Lưu"
✅ Success toast
✅ Reopen → Updated data shown
```

### **Test 3: Empty fields**
```
✅ Leave all empty → Click "Lưu"
✅ Saves as "||||"
✅ Works fine
```

---

## ✅ **Advantages**

| Feature | Status |
|---------|--------|
| **Simple** | ✅ Chỉ 1 String field |
| **No model** | ✅ Không cần class mới |
| **No ViewModel** | ✅ Activity gọi Repository trực tiếp |
| **Works offline** | ✅ Firestore offline cache |
| **Easy to maintain** | ✅ Code ngắn gọn |

---

## 📝 **Summary**

**Đã implement:**
- ✅ Load từ Firestore
- ✅ Pre-fill 5 fields
- ✅ Save combined String
- ✅ Loading state
- ✅ Error handling
- ✅ Success feedback

**Không cần:**
- ❌ MedicalHistory model
- ❌ MedicalHistoryViewModel
- ❌ Nested Firestore object

**Kết quả:** Đơn giản, gọn gàng, dễ maintain! 🎉

---

**Status:** ✅ **HOÀN THÀNH**
