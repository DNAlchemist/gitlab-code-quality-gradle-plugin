plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
    id("com.gradleup.nmcp") version "1.4.4"
    id("com.gradleup.nmcp.aggregation") version "1.4.4"
}

repositories {
    mavenCentral()
}

group = "io.github.dnalchemist"
version = "1.2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withSourcesJar()
    withJavadocJar()
}

val jaxbXjc = configurations.create("jaxbXjc") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    jaxbXjc("org.glassfish.jaxb:jaxb-xjc:4.0.5")
    jaxbXjc("org.glassfish.jaxb:jaxb-runtime:4.0.5")
    jaxbXjc("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")

    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    runtimeOnly("com.sun.xml.bind:jaxb-impl:4.0.0")
    implementation("com.google.code.gson:gson:2.10.1")

    compileOnly(gradleApi())

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

val generatedSpotbugs = layout.buildDirectory.dir("generated-sources/jaxb-spotbugs")
val generatedCheckstyle = layout.buildDirectory.dir("generated-sources/jaxb-checkstyle")

val xjcSpotbugs by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Generate JAXB model for SpotBugs XML"
    val outDir = generatedSpotbugs.get().asFile
    outputs.dir(outDir)
    inputs.file("src/main/xsd/bugcollection.xsd")
    classpath = jaxbXjc
    mainClass.set("com.sun.tools.xjc.XJCFacade")
    args(
        "-d",
        outDir.absolutePath,
        "-p",
        "io.github.dnalchemist.gitlab.codequality.spotbugs",
        file("src/main/xsd/bugcollection.xsd").absolutePath
    )
}

val xjcCheckstyle by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Generate JAXB model for Checkstyle XML"
    val outDir = generatedCheckstyle.get().asFile
    outputs.dir(outDir)
    inputs.file("src/main/xsd/checkstyle.xsd")
    classpath = jaxbXjc
    mainClass.set("com.sun.tools.xjc.XJCFacade")
    args(
        "-d",
        outDir.absolutePath,
        "-p",
        "io.github.dnalchemist.gitlab.codequality.checkstyle",
        file("src/main/xsd/checkstyle.xsd").absolutePath
    )
}

sourceSets.named("main") {
    java.srcDir(generatedSpotbugs)
    java.srcDir(generatedCheckstyle)
}

tasks.named("compileJava") {
    dependsOn(xjcSpotbugs, xjcCheckstyle)
}

tasks.named("sourcesJar") {
    dependsOn(xjcSpotbugs, xjcCheckstyle)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

gradlePlugin {
    plugins {
        create("gitlabCodeQuality") {
            id = "io.github.dnalchemist.gitlab-code-quality-gradle"
            displayName = "GitLab Code Quality"
            description =
                "Converts SpotBugs and Checkstyle XML reports into GitLab code quality JSON."
            implementationClass = "io.github.dnalchemist.gitlab.codequality.GitLabCodeQualityPlugin"
        }
    }
}

// Sonatype shut down legacy OSSRH (oss.sonatype.org / s01.oss.sonatype.org) in
// 2025. Snapshots are still uploaded with the standard `maven-publish` plugin
// (HTTP PUT) — but to the new Central Portal endpoint. Releases are uploaded
// as a signed zip bundle through the Central Portal Publisher API, which is
// what the `com.gradleup.nmcp` plugin wires up via `publishAllPublicationsToCentralPortal`.
publishing {
    repositories {
        maven {
            name = "centralSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            credentials {
                username = findProperty("centralPortalUsername")?.toString()
                password = findProperty("centralPortalPassword")?.toString()
            }
        }
    }
}

nmcpAggregation {
    centralPortal {
        username = providers.gradleProperty("centralPortalUsername").orNull
        password = providers.gradleProperty("centralPortalPassword").orNull
        // USER_MANAGED: upload + validate, then click "Publish" in the portal UI.
        // Switch to "AUTOMATIC" to release straight to Central after validation.
        publishingType = "USER_MANAGED"
    }
}

dependencies {
    // Single-project build: aggregate this project's own publications.
    nmcpAggregation(project(":"))
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    // Snapshots do not require a signature; only enforce signing for releases
    // so local/CI snapshot publishes work without a key configured.
    isRequired = !version.toString().endsWith("-SNAPSHOT")
}

afterEvaluate {
    signing {
        // Both `pluginMaven` and the plugin marker publication are registered
        // by the java-gradle-plugin in afterEvaluate, so wire signing here.
        sign(publishing.publications)
    }
    publishing {
        publications {
            named<MavenPublication>("pluginMaven") {
                pom {
                    name.set("gitlab-code-quality-gradle-plugin")
                    description.set(
                        "Gradle plugin that converts SpotBugs and Checkstyle XML into GitLab code quality JSON."
                    )
                    url.set("https://github.com/DNAlchemist/gitlab-code-quality-gradle-plugin")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("dnalchemist")
                            name.set("dnalchemist")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/DNAlchemist/gitlab-code-quality-gradle-plugin.git")
                        developerConnection.set("scm:git:git@github.com:DNAlchemist/gitlab-code-quality-gradle-plugin.git")
                        url.set("https://github.com/DNAlchemist/gitlab-code-quality-gradle-plugin")
                    }
                }
            }
        }
    }
}
