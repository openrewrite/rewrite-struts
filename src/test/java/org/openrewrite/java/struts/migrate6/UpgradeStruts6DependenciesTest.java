/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.struts.migrate6;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeStruts6DependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.struts.migrate6.UpgradeStruts6Dependencies")
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "struts2-core-2.5"));
    }

    @DocumentExample
    @Test
    void upgradeDependencies() {
        rewriteRun(
          pomXml(
            //language=xml
            """
                <project>
                  <groupId>group</groupId>
                  <artifactId>artifact</artifactId>
                  <version>1</version>
                  <properties>
                      <struts2.version>2.5.22</struts2.version>
                  </properties>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.struts</groupId>
                          <artifactId>struts2-config-browser-plugin</artifactId>
                          <version>${struts2.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.struts</groupId>
                          <artifactId>struts2-core</artifactId>
                          <version>${struts2.version}</version>
                          <exclusions>
                              <exclusion>
                                  <groupId>com.sun</groupId>
                                  <artifactId>tools</artifactId>
                              </exclusion>
                          </exclusions>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.struts</groupId>
                          <artifactId>struts2-spring-plugin</artifactId>
                          <version>${struts2.version}</version>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.struts</groupId>
                          <artifactId>struts2-json-plugin</artifactId>
                          <version>${struts2.version}</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pomXml -> {
                assertThat(pomXml).contains("<struts2.version>6.");
                return pomXml;
            })
          )
        );
    }

    @Test
    void changeStruts1Dependency() {
        rewriteRun(
          pomXml(
            //language=xml
            """
                <project>
                  <groupId>group</groupId>
                  <artifactId>artifact</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.struts</groupId>
                          <artifactId>struts-core</artifactId>
                          <version>1.3.10</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pomXml -> {
                assertThat(pomXml).contains("<version>6.");
                return pomXml;
            })
          )
        );
    }
}
