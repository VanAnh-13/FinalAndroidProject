# Firebase Setup Instructions for HealthyLife Hub

## Prerequisites
- Android Studio installed
- Google account

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"**
3. Enter project name: `HealthyLife Hub`
4. Enable/disable Google Analytics (optional)
5. Click **"Create project"**

## Step 2: Add Android App to Firebase

1. In Firebase Console, click the Android icon (or **"Add app"**)
2. Enter your Android package name: `com.example.healthylifehub`
3. App nickname (optional): `HealthyLife Hub`
4. Debug signing certificate SHA-1 (for Google Sign-In):
   
   **To get SHA-1:**
   
   Open terminal in your project folder and run:
   ```bash
   # On Windows (Git Bash or PowerShell):
   ./gradlew signingReport
   
   # Or using keytool directly:
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
   
   Copy the **SHA-1** fingerprint and paste it in Firebase Console.

5. Click **"Register app"**

## Step 3: Download google-services.json

1. After registering, download the `google-services.json` file
2. Move it to your project's `app/` folder:
   ```
   Base/
   ├── app/
   │   ├── google-services.json  <-- Place here
   │   ├── build.gradle.kts
   │   └── src/
   ```

## Step 4: Enable Authentication Methods

1. In Firebase Console, go to **Authentication** → **Sign-in method**
2. Enable **Email/Password**:
   - Click on "Email/Password"
   - Toggle **Enable**
   - Click **Save**

3. Enable **Google Sign-In**:
   - Click on "Google"
   - Toggle **Enable**
   - Select support email
   - Click **Save**

## Step 5: Get Web Client ID for Google Sign-In

1. In Firebase Console, go to **Project Settings** (gear icon)
2. Scroll down to **"Your apps"** section
3. Find **Web client (auto created by Google Service)**
4. Copy the **Web client ID** (looks like: `123456789-abcdefg.apps.googleusercontent.com`)

5. Open `AuthRepository.java` and replace:
   ```java
   .requestIdToken("YOUR_WEB_CLIENT_ID")
   ```
   with your actual Web client ID:
   ```java
   .requestIdToken("123456789-abcdefg.apps.googleusercontent.com")
   ```

## Step 6: Enable Firestore Database

1. In Firebase Console, go to **Firestore Database**
2. Click **"Create database"**
3. Select **Start in test mode** (for development)
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if request.time < timestamp.date(2025, 12, 31);
       }
     }
   }
   ```
4. Choose a location (e.g., `asia-southeast1` for Vietnam)
5. Click **Enable**

## Step 7: Enable Firebase Storage (Optional - for future use)

1. In Firebase Console, go to **Storage**
2. Click **"Get started"**
3. Start in **test mode**
4. Choose the same location as Firestore
5. Click **Done**

## Step 8: Verify Setup

1. Sync Gradle in Android Studio
2. Build the project:
   ```bash
   ./gradlew build
   ```
3. If successful, you're ready to run the app!

## Firestore Security Rules (Production - Update Later)

After testing, update Firestore rules to secure your database:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - only authenticated users can read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Health metrics - only owner can access
    match /users/{userId}/healthMetrics/{metricId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Reminders - only owner can access
    match /users/{userId}/reminders/{reminderId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Medical records - only owner can access
    match /users/{userId}/medicalRecords/{recordId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Medicines - only owner can access
    match /users/{userId}/medicines/{medicineId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

## Troubleshooting

### Error: "google-services.json not found"
- Make sure `google-services.json` is in `app/` folder
- Sync Gradle again

### Error: "SHA-1 certificate fingerprint"
- Generate SHA-1 using the command above
- Add it to Firebase Console → Project Settings → SHA certificate fingerprints

### Google Sign-In not working
- Verify Web client ID is correct in `AuthRepository.java`
- Make sure Google Sign-In is enabled in Firebase Console
- Check SHA-1 is added

### Build errors
- Clean project: `Build → Clean Project`
- Rebuild: `Build → Rebuild Project`
- Invalidate caches: `File → Invalidate Caches / Restart`

## Test Accounts (Create in Firebase Console)

For testing, create test accounts:
1. Go to **Authentication** → **Users** → **Add user**
2. Email: `test@healthylife.com`
3. Password: `test123456`

## Next Steps

After setup is complete:
1. ✅ Test Email/Password login
2. ✅ Test Google Sign-In
3. ✅ Verify user data is saved to Firestore
4. 🚀 Start building other features!

---

**Important Notes:**
- Never commit `google-services.json` to public repositories
- Update security rules before production
- Use environment variables for sensitive data in production
