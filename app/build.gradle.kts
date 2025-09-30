plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}

android {
    namespace = "student.projects.tholagig"
    compileSdk = 36

    defaultConfig {
        applicationId = "student.projects.tholagig"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.cardview:cardview:1.0.0")
    // Firebase
    implementation (platform("com.google.firebase:firebase-bom:32.4.0"))
    implementation ("com.google.firebase:firebase-firestore-ktx")
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
}