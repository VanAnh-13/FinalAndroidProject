# Localization Guide

## Overview
The app now supports automatic language switching based on system language settings using Android's built-in localization framework.

## How It Works

### 1. **LocaleHelper Utility**
The `LocaleHelper` class (`app/src/main/java/com/example/healthylifehub/utils/LocaleHelper.java`) manages all language-related operations:
- Applies saved language preferences
- Handles locale switching
- Persists user language choice
- Supports "System Default" option

### 2. **Automatic Language Detection**
The app automatically detects and applies the system language when:
- User selects "System Default" in settings
- App is first installed (defaults to system language)
- System language changes (if user hasn't manually selected a language)

### 3. **BaseActivity Integration**
All activities inherit from `BaseActivity`, which automatically applies the correct locale in `attachBaseContext()`. This ensures consistent language across all screens.

## Supported Languages

Currently supported languages:
- **English** (en) - Default
- **Vietnamese** (vi) - Tiếng Việt

## Adding New Languages

To add support for a new language:

### Step 1: Create Resource Folder
Create a new values folder for your language:
```
app/src/main/res/values-{language_code}/
```

Examples:
- Spanish: `values-es`
- French: `values-fr`
- Chinese: `values-zh`
- Japanese: `values-ja`

### Step 2: Copy strings.xml
Copy the default `strings.xml` from `values/` to your new folder and translate all strings.

### Step 3: Update LocaleHelper
Add your language code to the `isLanguageSupported()` method in `LocaleHelper.java`:
```java
public static boolean isLanguageSupported(String languageCode) {
    return languageCode.equals("en") || 
           languageCode.equals("vi") ||
           languageCode.equals("es") ||  // Add your language
           languageCode.equals(LANGUAGE_SYSTEM);
}
```

### Step 4: Update Settings Dialog
Add your language to the dialog in `SettingsActivity.java`:
```java
String[] languages = {
    "English",
    "Tiếng Việt",
    "Español",  // Add your language
    getString(R.string.theme_system)
};
```

## User Experience

### Changing Language
1. Open app
2. Go to Profile → Settings
3. Tap "Language"
4. Select desired language or "System Default"
5. App automatically restarts with new language

### System Default Behavior
When "System Default" is selected:
- App uses device's system language
- If system language is not supported, falls back to English
- Changes automatically when user changes device language

## Technical Details

### Language Persistence
- Language preference is stored in SharedPreferences
- Key: `"language"` in `"app_preferences"`
- Values: `"en"`, `"vi"`, or `"system"`

### Locale Application Flow
1. App starts → `HealthyLifeHubApplication.onCreate()`
2. Applies saved language via `LocaleHelper.applyLanguage()`
3. Each activity's `attachBaseContext()` applies locale
4. All string resources automatically load from correct folder

### Android Resource Qualifiers
Android automatically selects the correct `strings.xml` based on:
1. User's manual selection (if set)
2. System language (if "system" selected)
3. Default values folder (fallback)

## Best Practices

### For Developers
1. **Always use string resources**: Never hardcode text in layouts or code
2. **Test all languages**: Verify UI doesn't break with longer translations
3. **Use plurals**: Use `<plurals>` for quantity strings
4. **Format strings**: Use `String.format()` or `getString(R.string.x, arg)` for dynamic text

### For Translators
1. **Keep formatting**: Preserve `%s`, `%d`, `%1$s` placeholders
2. **Context matters**: Understand where the string appears
3. **Length consideration**: Some languages are longer (German, Finnish)
4. **RTL support**: Consider right-to-left languages (Arabic, Hebrew)

## Testing

### Test Language Switching
1. Change language in Settings
2. Navigate through all screens
3. Verify all text is translated
4. Check for layout issues

### Test System Default
1. Select "System Default" in app
2. Change device language in system settings
3. Reopen app
4. Verify app language matches device

## Troubleshooting

### Language not changing
- Ensure activity is recreated after language change
- Check `attachBaseContext()` is called in BaseActivity
- Verify string resources exist in target language folder

### Partial translation
- Some strings may fall back to default (English)
- Check all strings are present in language-specific `strings.xml`

### Layout issues
- Text may overflow in some languages
- Use `android:ellipsize` and `android:maxLines` for long text
- Test with longest translations (usually German)

## Resources

- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [Language and Locale Resolution](https://developer.android.com/guide/topics/resources/multilingual-support)
- [String Resources](https://developer.android.com/guide/topics/resources/string-resource)
