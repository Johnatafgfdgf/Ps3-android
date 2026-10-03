plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
}

val releaseKeystorePath = System.getenv("KEYSTORE_PATH").orEmpty()
val releaseKeystoreAlias = System.getenv("KEYSTORE_ALIAS").orEmpty()
val releaseKeystorePassword = System.getenv("KEYSTORE_PASSWORD").orEmpty()
val releaseKeystoreFile = releaseKeystorePath.takeIf { it.isNotBlank() }?.let { file(it) }
val hasReleaseSigning = releaseKeystoreFile?.isFile == true &&
    releaseKeystoreAlias.isNotBlank() &&
    releaseKeystorePassword.isNotBlank()

android {
    namespace = "net.rpcs3"
    compileSdk = 35
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.ps3native.standard"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON",
                    "-DCMAKE_JOB_POOLS=compile=8;link=1",
                    "-DCMAKE_JOB_POOL_COMPILE=compile",
                    "-DCMAKE_JOB_POOL_LINK=link"
                )
            }
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                keyAlias = releaseKeystoreAlias
                keyPassword = releaseKeystorePassword
                storeFile = releaseKeystoreFile
                storePassword = releaseKeystorePassword
            }
        }
    }

    flavorDimensions += "branding"

    productFlavors {
        create("standard") {
            dimension = "branding"
            applicationId = "com.ps3native.standard"
        }
        create("antutu") {
            dimension = "branding"
            applicationId = "com.antutu.ABenchMark"
        }
        create("ludashi") {
            dimension = "branding"
            applicationId = "com.ludashi.benchmark"
        }
        create("pubg") {
            dimension = "branding"
            applicationId = "com.tencent.ig"
        }
    }

    buildTypes {
        debug {
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DCMAKE_BUILD_TYPE=RelWithDebInfo")
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                // Keep symbols outside the shipped APK for actionable native crash reports.
                debugSymbolLevel = "FULL"
            }
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DCMAKE_BUILD_TYPE=Release")
                }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Never silently publish a release with Android's public debug key.
            // CI can still build an unsigned release; distribution builds must
            // provide the keystore through protected environment variables.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }


    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    sourceSets["main"].assets.srcDir(
        files(layout.buildDirectory.dir("generated/rpcs3-assets"))
            .builtBy("copyRpcs3Icons", "copyRpcs3Patches")
    )

    packaging {
        // This is necessary for libadrenotools custom driver loading
        jniLibs.useLegacyPackaging = true
    }
}

androidComponents {
    // The vendor-ID flavors are retained only for developer experiments.
    // Consumer release artifacts must use our own application identity.
    beforeVariants(selector().withBuildType("release")) { variant ->
        val branding = variant.productFlavors.firstOrNull { it.first == "branding" }?.second
        if (branding != "standard") {
            variant.enable = false
        }
    }
}

base.archivesName = "ps3native"

dependencies {
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.tooling.preview.android)
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-android:3.0.3")
    implementation("io.ktor:ktor-client-cio:3.0.3")
    implementation("io.ktor:ktor-client-json:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-client-logging:3.0.3")
}

val copyRpcs3Icons by tasks.registering(Copy::class) {
    from(file("../../bin/Icons"))
    into(layout.buildDirectory.dir("generated/rpcs3-assets/Icons"))
}

val copyRpcs3Patches by tasks.registering(Copy::class) {
    from(file("../../bin/patches")) {
        include("patch.yml")
    }
    into(layout.buildDirectory.dir("generated/rpcs3-assets/patches"))
}
