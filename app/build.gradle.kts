plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.pcshutdown"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pcshutdown"
        minSdk = 31
        targetSdk = 34
        versionCode = 3
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildToolsVersion = "34.0.0"
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.jcraft:jsch:0.1.55")
    implementation("androidx.security:security-crypto:1.1.0-beta01")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.arch.core:core-runtime:2.2.0")
    implementation("com.pranavpandey.android:dynamic-toasts:4.3.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}