import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

// compile bytecode to Java 8 (default is Java 6)
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

plugins {
    `maven-publish`
    kotlin("jvm") version "1.3.70" 
    id("org.jetbrains.dokka") version "0.10.1"
    signing
}

group = "com.github.haifengl"
version = "2.4.0"

repositories {
    mavenCentral()
    jcenter()
    mavenLocal()
}

dependencies {
    implementation(kotlin("stdlib")) 
    implementation("com.github.haifengl:smile-core:2.4.0")
    implementation("com.github.haifengl:smile-nlp:2.4.0")
    implementation("com.github.haifengl:smile-io:2.4.0")
    // read.avro() needs org.apache.avro.Schema defintion
    compileOnly("org.apache.avro:avro:1.8.2") {
      exclude("org.slf4j", "slf4j-log4j12")
    }
}

// Copy jar to shell lib
val copyJarToShell by tasks.registering(Copy::class) {
    from(tasks.jar)
    into(file("../shell/src/universal/bin"))
}

// Run copy task after build
tasks.build {
    finalizedBy(copyJarToShell)
}

// Configure existing Dokka task to output HTML
tasks {
    val dokka by getting(DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "../docs/2.0/api/kotlin"
        configuration {
            includes = listOf("packages.md")
            externalDocumentationLink {
                url = URL("http://haifengl.github.io/api/java/")
            }
        }
    }
}

// Create dokka Jar task from dokka task output
val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    classifier = "javadoc"
    // dependsOn(tasks.dokka) not needed; dependency automatically inferred by from(tasks.dokka)
    from(tasks.dokka)
}

// Create sources Jar from main kotlin sources
val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles sources JAR"
    classifier = "sources"
    from(project.the<SourceSetContainer>()["main"].allSource)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.github.haifengl"
            artifactId = "smile-kotlin"
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJar)
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("smile-kotlin")
                description.set("Statistical Machine Intelligence and Learning Engine")
                url.set("https://haifengl.github.io//")
                licenses {
                    license {
                        name.set("GNU Lesser General Public License, Version 3")
                        url.set("https://opensource.org/licenses/LGPL-3.0")
                    }
                }
                developers {
                    developer {
                        id.set("haifengl")
                        name.set("Haifeng Li")
                        url.set("https://haifengl.github.io/")
                    }
                }
                scm {
                    connection.set("git@github.com:haifengl/smile.git")
                    developerConnection.set("scm:git:git@github.com:haifengl/smile.git")
                    url.set("https://github.com/haifengl/smile")
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                val nexusUser: String by project
                val nexusPassword: String by project
                username = nexusUser
                password = nexusPassword
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(configurations.archives.get())
    sign(publishing.publications["mavenJava"])
}
