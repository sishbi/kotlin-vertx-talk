import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.google.protobuf.gradle.id
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

plugins {
    kotlin("jvm") version "1.9.23"
    application
    id("com.google.protobuf") version "0.9.4"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"
    id("com.github.ben-manes.versions") version "0.51.0"
}

group = "sishbi.vertx"
version = "1.0.0-SNAPSHOT"

defaultTasks("clean", "build")
repositories {
    mavenCentral()
}

val javaVersion = 21
val javaVendor: JvmVendorSpec = JvmVendorSpec.ADOPTIUM

val coroutinesVersion = "1.8.0"
val grpcVersion = "1.63.0"
val jacksonVersion = "2.17.1"
val javaxAnnotationVersion = "1.3.2"
val junitJupiterVersion = "5.10.2"
val flywayVersion = "10.12.0"
val kotlinLoggingVersion = "3.0.5"
val kotlinProtoVersion = "1.4.1"
val log4jVersion = "2.23.1"
val postgreVersion = "42.7.3"
val protoBufVersion = "4.26.1"
val scramVersion = "2.1"
val slf4jVersion = "2.0.13"
val testContainersVersion = "1.19.7"
val vertxVersion = "4.5.7"

val mainVerticleName = "sishbi.vertx.kotlin.MainVerticleKt"

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
        vendor.set(javaVendor)
    }
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
        vendor.set(javaVendor)
    }
}

application {
    mainClass.set(mainVerticleName)
}

dependencies {
    implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
    implementation(platform("org.apache.logging.log4j:log4j-bom:$log4jVersion"))
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:$coroutinesVersion"))
    implementation("io.vertx:vertx-web-client")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-grpc-server")
    implementation("io.vertx:vertx-pg-client")
    implementation("io.vertx:vertx-sql-client-templates")
    implementation("io.vertx:vertx-lang-kotlin-coroutines")
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("io.vertx:vertx-grpc-client")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j")
    implementation("com.ongres.scram:client:$scramVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("io.github.microutils:kotlin-logging-jvm:$kotlinLoggingVersion")
    implementation("com.google.protobuf:protobuf-java:$protoBufVersion")
    implementation("com.google.protobuf:protobuf-kotlin:$protoBufVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-kotlin-stub:$kotlinProtoVersion")
    implementation("javax.annotation:javax.annotation-api:$javaxAnnotationVersion")
    implementation("org.postgresql:postgresql:$postgreVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    testImplementation("io.vertx:vertx-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
    testImplementation("org.testcontainers:postgresql:$testContainersVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(PASSED, SKIPPED, FAILED)
    }
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:$protoBufVersion"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${kotlinProtoVersion}:jdk8@jar"
        }
        id("vertx") {
            artifact = "io.vertx:vertx-grpc-protoc-plugin2:${vertxVersion}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc") { }
                id("grpckt") { }
                id("vertx") { }
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCaseAsciiOnly().contains(it) }
    val regex = "^[0-9,.v-]+$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure { checkForGradleUpdate = false }
tasks.withType<DependencyUpdatesTask> { rejectVersionIf { isNonStable(candidate.version) } }
task("doAll") { dependsOn("useLatestVersions", "clean", "test") }
