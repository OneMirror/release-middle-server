plugins {
    id("org.springframework.boot") version "2.7.1"
    kotlin("jvm") version "1.7.0"
}

group = "bot.inker.onemirror"
version = "1.0-SNAPSHOT"


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.hibernate.orm:hibernate-core:6.1.0.Final")
    implementation("org.hibernate.orm:hibernate-hikaricp:6.1.0.Final")

    // https://github.com/undertow-io/undertow/pull/1341
    // Upgrade if this pull request released
    implementation("io.undertow:undertow-core:2.3.0.Alpha2-SNAPSHOT")

    implementation("redis.clients:jedis:4.2.3")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson:2.9.0")

    // Logger
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.apache.logging.log4j:log4j-api:2.17.2")
    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
    implementation("org.apache.logging.log4j:log4j-jul:2.17.2")
    implementation("org.apache.logging.log4j:log4j-iostreams:2.17.2")

    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.0.5")
    runtimeOnly("com.h2database:h2:1.4.200")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<ProcessResources> {
    filesMatching("bot/inker/onemirror/middle/default.properties") {
        expand(mapOf(
            "version" to project.version,
        ))
    }
}