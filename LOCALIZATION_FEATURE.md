# 🌍 Localization Feature - HealthyLife Hub

## Overview
The HealthyLife Hub app includes a comprehensive localization system that automatically translates the app based on user preferences or system language.

## Supported Languages
- 🇬🇧 **English** (en) - Default
- 🇻🇳 **Tiếng Việt** (vi) - Complete Vietnamese translation
- 🌐 **System Default** - Follows device language

## How to Change Language

### For Users:
1. Open the app
2. Navigate to **Profile** tab (bottom navigation)
3. Tap **Settings** (Cài đặt)
4. Tap **Language** (Ngôn ngữ)
5. Select your preferred language:
   - **English**
   - **Tiếng Việt**
   - **System Default** (Theo hệ thống)
6. The app will reload automatically with the new language

## Technical Implementation

### Architecture Components

#### 1. LocaleHelper.java
**Location:** `app/src/main/java/com/example/healthylifehub/utils/LocaleHelper.java`

**Features:**
- Manages language switching
- Persists language preference using SharedPreferences
- Applies locale to app context
- Supports Android API 24+ LocaleList

**Key Methods:**
```java
// Apply language to context
Context applyLanguage(Context context)

// Save language preference
void setLanguage(Context context, String languageCode)

// Get current language
String getLanguage(Context context)
```

#### 2. BaseActivity.java
**Location:** `app/src/main/java/com/example/healthylifehub/base/BaseActivity.java`

**Features:**
- All activities inherit from BaseActivity
- Overrides `attachBaseContext()` to apply locale
- Ensures consistent language across all screens

```java
@Override
protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(LocaleHelper.applyLanguage(newBase));
}
```

#### 3. HealthyLifeHubApplication.java
**Location:** `app/src/main/java/com/example/healthylifehub/HealthyLifeHubApplication.java`

**Features:**
- Applies language settings on app startup
- Called before any activity is created

```java
@Override
public void onCreate() {
    super.onCreate();
    applyLanguageSettings();
    // ...
}
```

#### 4. SettingsActivity.java
**Location:** `app/src/main/java/com/example/healthylifehub/ui/profile/settings/SettingsActivity.java`

**Features:**
- Provides language selection UI
- Shows AlertDialog with language options
- Calls `recreate()` to refresh UI after language change

```java
private void showLanguageDialog() {
    String[] languages = { "English", "Tiếng Việt", "System Default" };
    // ... selection dialog
    HealthyLifeHubApplication.setLanguage(this, langCode);
    recreate(); // Refresh activity
}
```

### String Resources

#### English (Default)
**Location:** `app/src/main/res/values/strings.xml`  
**Lines:** 1000+ strings

#### Vietnamese
**Location:** `app/src/main/res/values-vi/strings.xml`  
**Lines:** 1000+ strings

**Example Translations:**
```xml
<!-- English -->
<string name="health_metrics">Health Metrics</string>
<string name="add_new_metric">Add New Metric</string>
<string name="blood_pressure">Blood Pressure</string>

<!-- Vietnamese -->
<string name="health_metrics">Chỉ số sức khỏe</string>
<string name="add_new_metric">Thêm chỉ số mới</string>
<string name="blood_pressure">Huyết áp</string>
```

## Testing Checklist

### Manual Testing:
- [ ] Install app on device
- [ ] Open Settings → Language
- [ ] Change to Vietnamese → Verify UI updates
- [ ] Change to English → Verify UI updates
- [ ] Change to System Default → Verify follows device language
- [ ] Restart app → Verify language persists
- [ ] Test on different Android versions (API 21+)
- [ ] Test with device set to Vietnamese
- [ ] Test with device set to English

### Screens to Verify Translation:
- [ ] Dashboard
- [ ] Health Metrics
- [ ] Reminders
- [ ] Medical Records
- [ ] Profile
- [ ] Settings
- [ ] Login/Register
- [ ] Add/Edit Metric
- [ ] Add/Edit Reminder
- [ ] Notifications

## Language Codes

| Language | Code | Folder |
|----------|------|--------|
| English | `en` | `values/` |
| Vietnamese | `vi` | `values-vi/` |
| System | `system` | Uses device locale |

## Adding New Languages

To add support for a new language:

1. Create language-specific folder:
   ```
   app/src/main/res/values-<lang_code>/
   ```

2. Copy `strings.xml` from `values/` to new folder

3. Translate all strings in the new file

4. Update `LocaleHelper.isLanguageSupported()`:
   ```java
   public static boolean isLanguageSupported(String languageCode) {
       return languageCode.equals("en") || 
              languageCode.equals("vi") ||
              languageCode.equals("<new_lang>") ||
              languageCode.equals(LANGUAGE_SYSTEM);
   }
   ```

5. Update `SettingsActivity.showLanguageDialog()` to include new language option

## Known Issues & Limitations

1. **RTL Languages**: Not yet supported (Hebrew, Arabic)
2. **Plurals**: Some languages may need plural forms
3. **Date/Time Formatting**: Uses device locale, not app language
4. **Numbers**: Uses device locale for number formatting

## Future Enhancements

- [ ] Add more languages (Spanish, French, Chinese, etc.)
- [ ] Implement RTL language support
- [ ] Add context-aware translations
- [ ] Implement translation crowdsourcing
- [ ] Add accessibility string alternatives
- [ ] Support regional variants (en-US, en-GB, etc.)

## Resources

- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [Support different languages and cultures](https://developer.android.com/training/basics/supporting-devices/languages)
- [Locale Class Documentation](https://developer.android.com/reference/java/util/Locale)
