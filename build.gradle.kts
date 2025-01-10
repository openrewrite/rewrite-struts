plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Automatically refactor Apache Struts."

sourceSets {
    create("testWithStruts6") {
        java {
            compileClasspath += sourceSets.getByName("main").output
            runtimeClasspath += sourceSets.getByName("main").output
        }
    }
}

configurations {
    getByName("testWithStruts6RuntimeOnly") {
        isCanBeResolved = true
        extendsFrom(getByName("testRuntimeOnly"))
    }
    getByName("testWithStruts6Implementation") {
        isCanBeResolved = true
        extendsFrom(getByName("testImplementation"))
    }
}

val rewriteVersion = "latest.release"
dependencies {
    implementation(platform("org.openrewrite:rewrite-bom:8.41.1"))

    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-xml")
    runtimeOnly("org.openrewrite:rewrite-java-17")

    implementation("org.openrewrite.recipe:rewrite-java-dependencies:1.24.1")
    implementation("org.openrewrite.meta:rewrite-analysis:2.13.1")

    // Refaster style recipes need the rewrite-templating annotation processor and dependency for generated recipes
    // https://github.com/openrewrite/rewrite-templating/releases
    annotationProcessor("org.openrewrite:rewrite-templating:1.19.1")
    implementation("org.openrewrite:rewrite-templating:1.19.1")

    // Need to have a slf4j binding to see any output enabled from the parser.
    runtimeOnly("ch.qos.logback:logback-classic:1.2.+")

    testImplementation("org.openrewrite:rewrite-maven")
    testImplementation("org.openrewrite:rewrite-test")
    "testWithStruts6Implementation"("org.apache.struts:struts2-core:latest.release")
}
