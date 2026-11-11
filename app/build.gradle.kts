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
        versionCode = 4
        versionName = "1.3"
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

    buildFeatures {
        buildConfig = false
    }

    // ให้ unit test อ่านไฟล์ใน assets ได้ (ใช้ทดสอบ parser กับ playlist จริงที่ bundle มา)
    sourceSets {
        getByName("test") {
            resources.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Media3 / ExoPlayer — ตัวเล่นวิดีโอ + HLS
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    testImplementation("junit:junit:4.13.2")
}
