plugins {
	kotlin("multiplatform")
	kotlin("native.cocoapods")
	id("com.android.library")
}

kotlin {
	android()

	jvm("desktop")

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
			}
		}

		val androidMain by getting

		val iosMain by getting {
			dependsOn(commonMain)
		}
		val iosSimulatorArm64Main by getting {
			dependsOn(iosMain)
		}

		val desktopMain by getting
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