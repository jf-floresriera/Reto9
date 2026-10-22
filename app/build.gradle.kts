plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.reto9"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.reto9"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    
    // OSMDroid para el mapa
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    
    // Play Services Location para GPS
    implementation("com.google.android.gms:play-services-location:21.2.0")
    
    // Retrofit y Gson para API de Overpass
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}