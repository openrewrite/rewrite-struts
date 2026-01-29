/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class MigrateTilesListenerTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateTilesListener());
    }

    @DocumentExample
    @Test
    void migrateTilesListener() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                       http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
                       version="4.0">

                  <listener>
                      <listener-class>org.apache.tiles.web.startup.TilesListener</listener-class>
                  </listener>

                  <filter>
                      <filter-name>struts2</filter-name>
                      <filter-class>org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter</filter-class>
                  </filter>

              </web-app>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
                       http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
                       version="4.0">

                  <listener>
                      <listener-class>org.apache.struts2.tiles.StrutsTilesListener</listener-class>
                  </listener>

                  <filter>
                      <filter-name>struts2</filter-name>
                      <filter-class>org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter</filter-class>
                  </filter>

              </web-app>
              """,
            spec -> spec.path("web.xml")
          )
        );
    }

    @Test
    void noChangeWhenAlreadyMigrated() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app version="4.0">
                  <listener>
                      <listener-class>org.apache.struts2.tiles.StrutsTilesListener</listener-class>
                  </listener>
              </web-app>
              """,
            spec -> spec.path("web.xml")
          )
        );
    }

    @Test
    void noChangeForOtherListeners() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app version="4.0">
                  <listener>
                      <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
                  </listener>
              </web-app>
              """,
            spec -> spec.path("web.xml")
          )
        );
    }

    @Test
    void migrateMultipleListeners() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app version="4.0">
                  <listener>
                      <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
                  </listener>
                  <listener>
                      <listener-class>org.apache.tiles.web.startup.TilesListener</listener-class>
                  </listener>
              </web-app>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app version="4.0">
                  <listener>
                      <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
                  </listener>
                  <listener>
                      <listener-class>org.apache.struts2.tiles.StrutsTilesListener</listener-class>
                  </listener>
              </web-app>
              """,
            spec -> spec.path("web.xml")
          )
        );
    }
}
