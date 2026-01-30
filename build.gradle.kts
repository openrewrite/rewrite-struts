plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
    id("org.openrewrite.build.moderne-source-available-license") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Automatically refactor Apache Struts."

recipeDependencies {
    testParserClasspath("struts:struts:1.2.9")
    testParserClasspath("javax.servlet:javax.servlet-api:4.0.1")
    parserClasspath("org.apache.struts:struts2-core:2.5.22")
    parserClasspath("org.apache.struts:struts2-core:6.0.3")
}

val rewriteVersion = rewriteRecipe.rewriteVersion.get()
dependencies {
    implementation(platform("org.openrewrite:rewrite-bom:$rewriteVersion"))

    implementation("org.openrewrite:rewrite-java")
    implementation("org.openrewrite:rewrite-xml")
    runtimeOnly("org.openrewrite:rewrite-java-21")

    implementation("org.openrewrite.recipe:rewrite-java-dependencies:$rewriteVersion")
    implementation("org.openrewrite.recipe:rewrite-migrate-java:${rewriteVersion}")

    testImplementation("org.openrewrite:rewrite-maven")
    testImplementation("org.openrewrite:rewrite-test")

    testRuntimeOnly("javax.servlet:javax.servlet-api:4.0.1")
}
