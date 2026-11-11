plugins {
    id("com.android.application")
}

android {
    namespace = "com.mediabox.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mediabox.tv"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            // scaffold: เซ็น release ด้วย debug key เพื่อให้ได้ไฟล์ app-release.apk
            // ที่ติดตั้งได้จริงโดยไม่ต้องมี keystore
            // production: เปลี่ยนเป็น signingConfigs.create("release") { ... } + secrets
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// ไม่มี external dependency — ใช้ Android framework ล้วน
