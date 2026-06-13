plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
}

fun restApiBaseUrlFor(buildType: String, defaultValue: String): String {
    val buildTypeProperty = "REST_API_BASE_URL_${buildType.uppercase()}"
    return providers.gradleProperty(buildTypeProperty).orNull
        ?: providers.gradleProperty("REST_API_BASE_URL").orNull
        ?: defaultValue
}

android {
    namespace = "dev.lordyorden.as_no_phish_detector"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.lordyorden.as_no_phish_detector"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val restApiBaseUrl = restApiBaseUrlFor("debug", "https://localhost:9000")
            require(restApiBaseUrl.isNotBlank()) { "REST_API_BASE_URL for debug must not be blank" }
            buildConfigField("String", "REST_API_BASE_URL", "\"$restApiBaseUrl\"")
        }

        release {
            isMinifyEnabled = false
            val restApiBaseUrl = restApiBaseUrlFor("release", "https://localhost:9000")
            require(restApiBaseUrl.isNotBlank()) { "REST_API_BASE_URL for release must not be blank" }
            buildConfigField("String", "REST_API_BASE_URL", "\"$restApiBaseUrl\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

//    kotlinOptions {
//        jvmTarget = "11"
//    }
    buildFeatures{
        viewBinding=true
        buildConfig=true
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.converter.gson)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.tink)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.tink.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.easypermissions.ktx)
    //runtimeOnly(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.service)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    // https://mvnrepository.com/artifact/com.google.firebase/firebase-messaging
    implementation(libs.firebase.messaging)
    implementation(libs.glide)
    implementation(libs.kotlin.onetimepassword)

    implementation(libs.otpinput)

    //convex
    implementation("dev.convex:android-convexmobile:0.8.0@aar") {
        isTransitive = true
    }
    implementation(libs.kotlinx.serialization.json)

    //clerk
    implementation(libs.clerk.android.api)
    implementation(libs.clerk.android.ui)

    implementation(libs.clerk.convex.kotlin)

}
