package com.clistery.gradle

object Versions {

    const val kotlin = "1.5.31"
    const val dokka = "1.4.32"
    const val jfrog = "4.23.4"
    const val androidGradlePlugin = "7.0.3"
}

object Plugins{
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val dokkaGradlePlugin = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}"
    const val jfrogGradlePlugin = "org.jfrog.buildinfo:build-info-extractor-gradle:${Versions.jfrog}"
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"
}
