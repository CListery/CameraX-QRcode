import com.clistery.gradle.AppConfig

plugins {
    id("com.android.library")
    id("kre-publish")
    id("org.jetbrains.dokka")
}

val androidJavadocs by tasks.register<Javadoc>("androidJavadocs") {
    options {
        encoding = Charsets.UTF_8.displayName()
        source = android.sourceSets.flatMap { it.java.srcDirs }.first().absolutePath
        classpath =
            classpath.plus(project.files(android.bootClasspath.joinToString(File.pathSeparator)))
        exclude(listOf("**/*.kt", "**/BuildConfig.java", "**/R.java"))
        isFailOnError = true
        if (this is StandardJavadocDocletOptions) {
            links("http://docs.oracle.com/javase/8/docs/api/")
            linksOffline("http://d.android.com/reference", "${android.sdkDirectory}/docs/reference")
        }
    }
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTask> {
    // dependsOn(androidJavadocs)
    dokkaSourceSets {
        named("main") {
            noStdlibLink.set(true)
            noAndroidSdkLink.set(true)
            noJdkLink.set(true)
            includeNonPublic.set(true)
            skipEmptyPackages.set(true)
        }
    }
    offlineMode.set(true)
    // outputDirectory.set(androidJavadocs.destinationDir)
}
val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}
val dokkaHtmlJar by tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}
val androidSourcesJar by tasks.register<Jar>("androidSourcesJar") {
    from(android.sourceSets.flatMap { it.java.srcDirs })
    archiveClassifier.set("sources")
}

publishing {
    publications {
        maybeCreate<MavenPublication>("-Release").apply {
            groupId = AppConfig.GROUP_ID
            artifactId = AppConfig.ARTIFACT_ID
            version = AppConfig.versionName

            suppressAllPomMetadataWarnings()

            artifact(dokkaJavadocJar)
            artifact(androidSourcesJar)
            afterEvaluate { artifact(tasks.getByName("bundleReleaseAar")) }
        }
    }
}
