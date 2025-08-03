import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("maven-publish")
}

android {
    namespace = "com.xq.stickylayoutmanager"
    compileSdk = 33

    defaultConfig {
        minSdk = 16

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        //consumerProguardFiles = "consumer-rules.pro"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.core:core-ktx:1.16.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.gitlab.hooshkar"
                artifactId = "sticky-layout-manager"
                version = "1.0.1"
            }
        }

        repositories {
            maven {
                val projectId = gradleLocalProperties(rootDir, providers).getProperty("projectId")
                val gitLabPrivateToken = gradleLocalProperties(rootDir, providers).getProperty("gitLabPrivateToken")

                url = uri("https://gitlab.hooshkar.com/api/v4/projects/$projectId/packages/maven")
                name = "Maven"
                credentials(HttpHeaderCredentials::class) {
                    name = "Deploy-Token" // this isn't token name!
                    value = gitLabPrivateToken
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    }
}