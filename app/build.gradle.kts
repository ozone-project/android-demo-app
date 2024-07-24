plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.ozoneproject.newsallapps"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ozoneproject.newsallapps"
        minSdk = 24
        targetSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_1_8
//        targetCompatibility = JavaVersion.VERSION_1_8
//    }
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }
    buildFeatures {
        viewBinding = true
    }
//    buildToolsVersion = "33.0.1"
    buildToolsVersion = "34.0.0"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.play.services.ads)

    implementation("com.usercentrics.sdk:usercentrics-ui:2.8.0")

    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.browser:browser:1.4.0")
    implementation("androidx.media:media:1.6.0")
    implementation("com.google.ads.interactivemedia.v3:interactivemedia:3.29.0")
    implementation(files("libs/omsdk.jar"))
    implementation(files("libs/PrebidMobile-core.jar"))
    implementation(files("libs/PrebidMobile-core-sources.jar"))
    implementation(files("libs/PrebidMobile-gamEventHandlers.jar"))
    implementation(files("libs/PrebidMobile-gamEventHandlers-sources.jar"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // for instream ads:
    implementation("androidx.media3:media3-exoplayer:1.1.0")
    implementation("androidx.media3:media3-ui:1.1.0")
    implementation("androidx.media3:media3-exoplayer-ima:1.1.0")  // this is needed for ext.ima.ImaAdsLoader



}