plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(project(":java:reactor-handler"))
    implementation("io.projectreactor.netty:reactor-netty:0.+")
}

sourceSets {
    main {
        java {
            srcDir("src/expected/java")
        }
    }
}
