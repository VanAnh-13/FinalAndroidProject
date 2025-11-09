# 🐛 Bug Fix: Chỉ hiển thị huyết áp, 3 loại còn lại không hiển thị

## **Vấn đề:**
- ✅ Lưu được tất cả 4 loại chỉ số (huyết áp, đường huyết, cân nặng, nhịp tim)
- ✅ Data đã lưu vào Firestore
- ❌ **Nhưng chỉ có huyết áp hiển thị trong list**
- ❌ **3 loại còn lại không hiển thị**

---

## **Nguyên nhân:**

### **1. Format Firestore khác nhau**

**HealthMetricRepository (Save):**
```java
// Blood pressure
metricData.put("value", {systolic: 120, diastolic: 80});  // Map

// Other types
metricData.put("value", 95);  // Number trực tiếp ✅
metricData.put("value", 70);  // Number trực tiếp ✅
metricData.put("value", 75);  // Number trực tiếp ✅
```

**MetricsRepository (Load) - CŨ:**
```java
// Blood pressure
valueMap.get("systolic");  // ✅ OK

// Blood sugar
valueMap.get("level");  // ❌ SAI! Không có field "level"

// Weight
valueMap.get("weight");  // ❌ SAI! Không có field "weight"

// Heart rate
valueMap.get("bpm");  // ❌ SAI! Không có field "bpm"
```

**→ Kết quả:** Parse sai → Return "N/A" → Không hiển thị

---

## **Giải pháp:**

### **1. Update formatMetricValue()**

**Trước (SAI):**
```java
private String formatMetricValue(String type, Map<String, Object> valueMap, String unit) {
    switch (type) {
        case "blood_pressure":
            return valueMap.get("systolic") + "/" + valueMap.get("diastolic");
            
        case "blood_sugar":
            return valueMap.get("level");  // ❌ Không tồn tại
            
        case "weight":
            return valueMap.get("weight");  // ❌ Không tồn tại
            
        case "heart_rate":
            return valueMap.get("bpm");  // ❌ Không tồn tại
    }
}
```

**Sau (ĐÚNG):**
```java
private String formatMetricValue(String type, Object valueObj, String unit) {
    switch (type) {
        case "blood_pressure":
            // value = {systolic, diastolic} (Map)
            if (valueObj instanceof Map) {
                Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                return valueMap.get("systolic") + "/" + valueMap.get("diastolic");
            }
            break;
            
        case "blood_sugar":
        case "weight":
        case "heart_rate":
            // value = number (Number) ✅
            if (valueObj instanceof Number) {
                int intValue = ((Number) valueObj).intValue();
                return intValue + " " + unit;
            }
            break;
    }
}
```

---

### **2. Update loadMetrics()**

**Trước:**
```java
Map<String, Object> valueMap = (Map<String, Object>) doc.get("value");
String displayValue = formatMetricValue(type, valueMap, unit);
```

**Sau:**
```java
Object valueObj = doc.get("value");  // ✅ Lấy Object, không cast ngay
String displayValue = formatMetricValue(type, valueObj, unit);
```

---

## **Code Changes:**

### **File: MetricsRepository.java**

**1. loadMetrics() - Line 58:**
```java
// Before
Map<String, Object> valueMap = (Map<String, Object>) doc.get("value");

// After
Object valueObj = doc.get("value");
```

**2. loadMetricHistory() - Line 110:**
```java
// Before
Map<String, Object> valueMap = (Map<String, Object>) doc.get("value");

// After
Object valueObj = doc.get("value");
```

**3. formatMetricValue() - Line 135:**
```java
// Before
private String formatMetricValue(String type, Map<String, Object> valueMap, String unit)

// After
private String formatMetricValue(String type, Object valueObj, String unit) {
    try {
        switch (type) {
            case "blood_pressure":
                if (valueObj instanceof Map) {
                    Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                    Object systolic = valueMap.get("systolic");
                    Object diastolic = valueMap.get("diastolic");
                    if (systolic != null && diastolic != null) {
                        return systolic + "/" + diastolic + " " + unit;
                    }
                }
                break;
                
            case "heart_rate":
            case "blood_sugar":
            case "weight":
                if (valueObj instanceof Number) {
                    int intValue = ((Number) valueObj).intValue();
                    return intValue + " " + unit;
                }
                break;
        }
    } catch (Exception e) {
        // Return N/A if parsing fails
    }
    return "N/A";
}
```

---

## **Firestore Data Format:**

### **Blood Pressure:**
```json
{
  "type": "blood_pressure",
  "value": {
    "systolic": 120,
    "diastolic": 80
  },
  "unit": "mmHg",
  "measuredAt": "2025-11-08T10:00:00Z",
  "note": "Morning measurement"
}
```

### **Blood Sugar:**
```json
{
  "type": "blood_sugar",
  "value": 95,  ← Number trực tiếp
  "unit": "mg/dL",
  "measuredAt": "2025-11-08T10:00:00Z",
  "note": "After breakfast"
}
```

### **Weight:**
```json
{
  "type": "weight",
  "value": 70,  ← Number trực tiếp
  "unit": "kg",
  "measuredAt": "2025-11-08T10:00:00Z",
  "note": ""
}
```

### **Heart Rate:**
```json
{
  "type": "heart_rate",
  "value": 75,  ← Number trực tiếp
  "unit": "bpm",
  "measuredAt": "2025-11-08T10:00:00Z",
  "note": "Resting"
}
```

---

## **Testing:**

### **Before Fix:**
```
✅ Blood Pressure: 120/80 mmHg  → Hiển thị
❌ Blood Sugar: N/A             → Không hiển thị
❌ Weight: N/A                  → Không hiển thị
❌ Heart Rate: N/A              → Không hiển thị
```

### **After Fix:**
```
✅ Blood Pressure: 120/80 mmHg  → Hiển thị
✅ Blood Sugar: 95 mg/dL        → Hiển thị
✅ Weight: 70 kg                → Hiển thị
✅ Heart Rate: 75 bpm           → Hiển thị
```

---

## **Root Cause Analysis:**

### **Why it happened:**

1. **Inconsistent data format assumptions**
   - Save code: Lưu `value` là Number cho non-BP metrics
   - Load code: Expect `value` là Map với nested fields

2. **No type checking**
   - Code cast trực tiếp `(Map<String, Object>)` mà không check type
   - Khi `value` là Number → Cast fail → Exception → Return "N/A"

3. **Silent failure**
   - Try-catch bắt exception nhưng không log
   - UI chỉ thấy "N/A" mà không biết lỗi gì

---

## **Prevention:**

### **1. Add logging:**
```java
} catch (Exception e) {
    Log.e(TAG, "Error parsing metric: " + type, e);  // ✅ Log lỗi
    return "N/A";
}
```

### **2. Add type checking:**
```java
if (valueObj instanceof Map) {
    // Handle Map
} else if (valueObj instanceof Number) {
    // Handle Number
} else {
    Log.w(TAG, "Unknown value type: " + valueObj.getClass());
}
```

### **3. Unit tests:**
```java
@Test
public void testFormatBloodSugar() {
    Object value = 95;  // Number
    String result = formatMetricValue("blood_sugar", value, "mg/dL");
    assertEquals("95 mg/dL", result);
}

@Test
public void testFormatBloodPressure() {
    Map<String, Object> value = new HashMap<>();
    value.put("systolic", 120);
    value.put("diastolic", 80);
    String result = formatMetricValue("blood_pressure", value, "mmHg");
    assertEquals("120/80 mmHg", result);
}
```

---

## **Summary:**

| Issue | Status |
|-------|--------|
| Blood Pressure hiển thị | ✅ OK (đã OK từ trước) |
| Blood Sugar không hiển thị | ✅ **FIXED** |
| Weight không hiển thị | ✅ **FIXED** |
| Heart Rate không hiển thị | ✅ **FIXED** |

**Root cause:** Parse sai format Firestore  
**Solution:** Update parser để handle cả Map và Number  
**Status:** ✅ **RESOLVED**

---

**Bây giờ tất cả 4 loại chỉ số đều hiển thị đúng!** 🎉
