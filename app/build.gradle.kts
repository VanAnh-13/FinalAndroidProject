import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.healthylifehub"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.healthylifehub"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Read Google Web Client ID from local.properties (NOT committed to Git)
        // REQUIRED: Add this line to your local.properties: google.web.client.id=YOUR_CLIENT_ID
        // See SECURITY_SETUP.md for detailed setup instructions
        val localPropertiesFile = rootProject.file("local.properties")
        val properties = Properties()
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        val googleWebClientId = properties.getProperty("google.web.client.id")
            ?: throw GradleException(
                """
                |
                |❌ ERROR: Missing 'google.web.client.id' in local.properties
                |
                |To fix this:
                |1. Open or create 'local.properties' in project root
                |2. Add this line:
                |   google.web.client.id=YOUR_GOOGLE_WEB_CLIENT_ID
                |
                |3. Get your Web Client ID from:
                |   - Firebase Console > Project Settings > General > Web App
                |   - Or from google-services.json (oauth_client with client_type: 3)
                |
                |📖 See SECURITY_SETUP.md for detailed instructions
                |
                """.trimMargin()
            )
        
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures{
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.viewbinding)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Navigation component
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")

    // View model
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")

    // Lottie file
    implementation("com.airbnb.android:lottie:6.4.1")

    // Room component
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-rxjava3:2.6.1") // RxJava3 support for Room
    
    // Lombok MUST be processed BEFORE Room
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    
    // Room compiler AFTER Lombok
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Firebase BOM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Google Play Services Tasks (for Tasks.await)
    implementation("com.google.android.gms:play-services-tasks:18.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // RxJava
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    
    // RxJava adapter for Retrofit (optional but useful)
    implementation("com.squareup.retrofit2:adapter-rxjava3:2.11.0")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // MPAndroidChart for graphs
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Fragment
    implementation("androidx.fragment:fragment:1.6.2")

    // Navigation Drawer
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime:2.9.0")
}