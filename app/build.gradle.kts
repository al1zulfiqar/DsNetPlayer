
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.digitalsignage.dsnetcoreplayer"
    compileSdk = 34
    packaging {
        jniLibs {
            excludes.add("META-INF/DEPENDENCIES")
        }
        resources {
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("META-INF/LICENSE")
            excludes.add("META-INF/NOTICE")
        }
    }
    defaultConfig {
        applicationId = "com.digitalsignage.dsnetcoreplayer"
        minSdk = 24
        targetSdk = 34
        versionCode = 70
        versionName = "8.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Core ExoPlayer dependency
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")




    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")


    implementation(files("libs/aspose-words-24.3-android.via.java.jar"))
    implementation ("com.dmitryborodin:pdfview-android:1.1.0")
    implementation ("com.joanzapata.pdfview:android-pdfview:1.0.4@aar")
    implementation ("com.github.chrisbanes:PhotoView:2.3.0")

    // Add these to handle XML parsing


    implementation ("com.google.mlkit:text-recognition:16.0.0-beta1")
    implementation ("com.google.android.gms:play-services-mlkit-text-recognition:16.0.0-beta1")




    // Retrofit dependencies
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation(libs.gson)
    implementation ("androidx.work:work-runtime-ktx:2.8.1")

    implementation ("androidx.room:room-runtime:2.6.1")
    implementation ("androidx.room:room-ktx:2.2.1")
    kapt ("androidx.room:room-compiler:2.6.1")

    // For Kotlin coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

// For ViewModel and LiveData
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation ("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")

    implementation ("androidx.activity:activity-ktx:1.7.2")

    implementation ("androidx.fragment:fragment-ktx:1.7.0")

    implementation ("commons-io:commons-io:2.11.0")
    implementation ("org.greenrobot:eventbus:3.3.1")

    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.github.bumptech.glide:glide:4.14.2")
    implementation ("com.github.bumptech.glide:okhttp3-integration:4.12.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")
    // Check for the latest version
    annotationProcessor ("com.github.bumptech.glide:compiler:4.14.2")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}