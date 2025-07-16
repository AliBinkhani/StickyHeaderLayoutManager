import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("maven-publish")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.gitlab.hooshkar"
                artifactId = "sticky-layout-manager"
                version = "1.0.0"
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