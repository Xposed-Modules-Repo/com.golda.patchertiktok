plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.golda.patchertiktok"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.golda.patchertiktok"
        minSdk = 27
        targetSdk = 36
        versionCode = 41
        versionName = "4.0"
    }
    buildFeatures {
        buildConfig = true
    }
    androidResources {
        // UI strings live in code; only the LSPosed description is localized as a resource.
        localeFilters += listOf("en", "ru", "uk", "be", "de", "es", "pt", "fr", "it", "pl", "tr", "in")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    packaging {
        resources.excludes += listOf("META-INF/**", "kotlin/**", "**.properties")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        // ModSettingsActivity is created by the hook inside TikTok, not from this manifest.
        disable += "Instantiatable"
    }
}

dependencies {
    testImplementation(libs.junit)
    compileOnly(files("libs/xposed-api-82.jar"))
    testImplementation(files("libs/xposed-api-82.jar"))
}
