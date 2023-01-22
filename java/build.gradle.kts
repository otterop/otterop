val version : String by project
val junitVersion : String by project

plugins {
    `maven-publish`
    `java-library`
}

subprojects {
    version = version

    apply {
        plugin("maven-publish")
        plugin("java-library")
    }

    repositories {
        // Use Maven Central for resolving dependencies.
        mavenCentral()
    }

    dependencies {
        // Use JUnit Jupiter for testing.
        testImplementation("org.junit.jupiter:junit-jupiter:${junitVersion}")
    }

    publishing {
        publications {
            create<MavenPublication>("jar") {
                from(components["java"])
            }
        }
    }

    tasks.named<Test>("test") {
        // Use JUnit Platform for unit tests.
        useJUnitPlatform()
    }
}
