import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
}

allprojects {
    repositories {
        jcenter()
    }

    dependencyLocking {
        lockAllConfigurations()
    }

    tasks.register("resolveAndLockAll") {
        doFirst {
            require(gradle.startParameter.isWriteDependencyLocks)
        }
        doLast {
            configurations.filter {
                // Add any custom filtering on the configurations to be resolved
                it.isCanBeResolved
            }.forEach { it.resolve() }
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    targetCompatibility = "1.8"
    sourceCompatibility = "1.8"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.fasterxml.jackson.core:jackson-core:2.+")
    implementation("org.yaml:snakeyaml:1.+")
    implementation("commons-cli:commons-cli:1.+")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.+")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.+")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.+")
    testImplementation("org.eclipse.jgit:org.eclipse.jgit:5.+")
    testImplementation("ch.qos.logback:logback-classic:1.2.+")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("testCasesDir", "$projectDir/src/test/test-cases")
}
