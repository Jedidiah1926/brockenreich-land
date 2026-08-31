import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.brockenreich"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.1")
    implementation(kotlin("stdlib"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("kotlin", "com.brockenreich.landplugin.libs.kotlin")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

val deployToRunServer by tasks.registering(Copy::class) {
    description = "Copies the built plugin jar into run/plugins for local server testing."
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(layout.projectDirectory.dir("run/plugins"))
}
