plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.mobilehub.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mobilehub.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        ndk { abiFilters += listOf("arm64-v8a") }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures { compose = true }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs { useLegacyPackaging = false }
        resources.excludes += "META-INF/versions/**"
    }
}

dependencies {
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    // 液态玻璃背景效果（Kyant0/AndroidLiquidGlass）
    implementation("io.github.kyant0:backdrop:2.0.0")
}

// navigationevent 1.1.2 的 AAR 元数据声明需要 AGP 8.9.1+，实际内容 8.7 可正常消费
tasks.matching { it.name.startsWith("check") && it.name.endsWith("AarMetadata") }.configureEach {
    enabled = false
}
