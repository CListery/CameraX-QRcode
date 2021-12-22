import com.clistery.gradle.Plugins

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(Plugins.kotlinGradlePlugin)
    implementation(Plugins.dokkaGradlePlugin)
    implementation(Plugins.androidGradlePlugin)
}

gradlePlugin {
    plugins {
        create("clistery_plugin") {
            id = "com.clistery.gradle"
            implementationClass = "com.clistery.gradle.CPlugin"
        }
    }
}
