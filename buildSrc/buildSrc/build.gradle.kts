plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    // gradlePluginPortal()
}

// force compilation of Dependencies.kt so it can be referenced in buildSrc/build.gradle.kts
sourceSets.main {
    java {
        setSrcDirs(setOf(projectDir.parentFile.resolve("src/main/kotlin")))
        include("com/clistery/gradle/Dependencies.kt")
        include("com/clistery/gradle/AppConfig.kt")
    }
}
