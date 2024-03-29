plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    id("com.android.library")
    id("com.google.gms.google-services")
    id("kotlinx-serialization")
}

kotlin {
    android()

    //jvm("desktop")

    ios()
    iosSimulatorArm64()

    js(IR) {
        browser()
    }

    cocoapods {
        summary = "Shared code for the sample"
        homepage = "https://github.com/JetBrains/compose-jb"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "cuboc_core"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // implementation("org.mongodb:mongodb-driver-sync:4.8.1")
                implementation("dev.gitlive:firebase-firestore:1.6.2")
                implementation("dev.gitlive:firebase-auth:1.6.2")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting

        val iosMain by getting {
            dependsOn(commonMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        //val desktopMain by getting
    }
}

android {
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
