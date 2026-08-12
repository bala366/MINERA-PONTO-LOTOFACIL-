plugins {
    id("com.android.application")
}

android {
    namespace = "br.com.mineradorlotofacil.oito"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.mineradorlotofacil.oito"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
