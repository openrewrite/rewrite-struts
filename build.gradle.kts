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

val rewriteVersion = rewriteRecipe.rewriteVersion.get()

dependencies {
    // The bom version can also be set to a specific version
    // https://github.com/openrewrite/rewrite-recipe-bom/releases
    implementation(platform("org.openrewrite.recipe:rewrite-recipe-bom:$rewriteVersion"))

    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite.recipe:rewrite-java-dependencies")
    implementation("org.openrewrite.meta:rewrite-analysis")
    implementation("org.openrewrite:rewrite-xml")
    runtimeOnly("org.openrewrite:rewrite-java-17")

    // Refaster style recipes need the rewrite-templating annotation processor and dependency for generated recipes
    // https://github.com/openrewrite/rewrite-templating/releases
    annotationProcessor("org.openrewrite:rewrite-templating:$rewriteVersion")
    implementation("org.openrewrite:rewrite-templating")

    // Need to have a slf4j binding to see any output enabled from the parser.
    runtimeOnly("ch.qos.logback:logback-classic:1.2.+")

    "testWithStruts6Implementation"("org.apache.struts:struts2-core:latest.release")
}

configure<PublishingExtension> {
    publications {
        named("nebula", MavenPublication::class.java) {
            suppressPomMetadataWarningsFor("runtimeElements")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "moderne"
            url = uri("https://us-west1-maven.pkg.dev/moderne-dev/moderne-recipe")
        }
    }
}
