import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release imzalama bilgileri repoya girmez. Varsa keystore.properties'ten okunur,
// yoksa release build imzasız üretilir (HF Space debug APK ürettiği için sorun değil).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "com.waifuhtr.pixelstore"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.waifuhtr.pixelstore"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        buildConfig = true
    }

    if (hasReleaseKeystore) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            // Debug'da WebView remote debugging ve ayrıntılı log açık.
            buildConfigField("boolean", "WEBVIEW_DEBUG", "true")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "WEBVIEW_DEBUG", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    androidResources {
        // www klasöründeki dosyalar aynen paketlensin, sıkıştırma davranışı değişmesin.
        noCompress += listOf("json")
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    // WebViewAssetLoader: assetleri sabit bir https origin'inden sunar (LocalStorage origin'i sabit kalır).
    implementation("androidx.webkit:webkit:1.12.1")
}

/**
 * Paketlemeden önce www klasörünün tutarlılığını doğrular.
 * index.html yoksa veya büyük/küçük harf çakışması varsa build açık hata ile durur.
 */
val verifyWebAssets by tasks.registering {
    val wwwDir = file("src/main/assets/www")
    inputs.dir(wwwDir)
    outputs.upToDateWhen { false }
    doLast {
        val index = File(wwwDir, "index.html")
        if (!index.exists()) {
            throw GradleException("assets/www/index.html bulunamadı; WebView boş ekran açar.")
        }
        val seen = mutableMapOf<String, String>()
        wwwDir.walkTopDown().filter { it.isFile }.forEach { f ->
            val rel = f.relativeTo(wwwDir).invariantSeparatorsPath
            val prev = seen.put(rel.lowercase(), rel)
            if (prev != null && prev != rel) {
                throw GradleException(
                    "Android dosya adlarında büyük/küçük harf duyarlıdır, çakışma: '$prev' / '$rel'"
                )
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(verifyWebAssets)
}
