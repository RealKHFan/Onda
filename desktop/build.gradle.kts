import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

// The phone app's plain-Kotlin logic (matching, grouping, Deezer, server protocol, free catalogs) is shared as is.
sourceSets.main {
    kotlin.srcDir("../app/src/main/java")
    kotlin.include(
        "io/github/realkhfan/onda/core/**",
        "io/github/realkhfan/onda/data/Http.kt",
        "io/github/realkhfan/onda/data/Deezer.kt",
        "io/github/realkhfan/onda/data/Subsonic.kt",
        "io/github/realkhfan/onda/data/FreeCatalogs.kt",
        "io/github/realkhfan/onda/desktop/**",
    )
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.json:json:20240303")
    implementation("org.freedesktop.gstreamer:gst1-java-core:1.4.0") // playback through the system's GStreamer
    implementation("net.jthink:jaudiotagger:3.0.1") // reads tags and covers from your files
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

compose.desktop {
    application {
        mainClass = "io.github.realkhfan.onda.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Rpm)
            packageName = "Onda"
            packageVersion = "0.1.0"
            description = "Privacy-first music player"
            modules("java.instrument", "jdk.unsupported", "jdk.crypto.ec", "java.naming", "java.sql")
            linux { iconFile.set(project.file("src/main/resources/onda.png")) }
        }
    }
}
