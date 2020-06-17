plugins {
    kotlin("jvm") version "1.3.70"
    application
    id("com.github.johnrengelman.shadow") version "5.2.0"
}


group = "com.kedil"
version =  "0.0.1"
val ktorVersion = "1.3.0"
val logbackVersion = "1.2.3"

repositories {
    jcenter()
    maven ("https://kotlin.bintray.com/ktor" )
}

dependencies {
    // Kotlin
    implementation (kotlin("stdlib"))

    // Ktor
    implementation ("io.ktor:ktor-server-netty:$ktorVersion")
    implementation ("ch.qos.logback:logback-classic:$logbackVersion")
    implementation ("io.ktor:ktor-server-core:$ktorVersion")
    testImplementation ("io.ktor:ktor-server-tests:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")

    // Jetbrains Exposed
    implementation ("org.jetbrains.exposed:exposed-core:0.20.3")
    implementation ("org.jetbrains.exposed:exposed-jdbc:0.20.3")
    implementation ("org.jetbrains.exposed:exposed-dao:0.20.3")
    implementation ("org.jetbrains.exposed:exposed-java-time:0.20.3")

    // Utils
    implementation("org.postgresql:postgresql:42.2.9")
    implementation("com.relops:snowflake:1.1")
    implementation("com.zaxxer:HikariCP:3.4.2")
}

application {mainClassName = "com.kedil.ApplicationKt"}

tasks {
    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
        }
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName
            )
        )
    }
}

