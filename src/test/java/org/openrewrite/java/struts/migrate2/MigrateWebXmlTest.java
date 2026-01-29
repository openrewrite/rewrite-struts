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
package org.openrewrite.java.struts.migrate2;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class MigrateWebXmlTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateWebXml());
    }

    @DocumentExample
    @Test
    void migrateStruts1ServletToFilter() {
        rewriteRun(
            xml(
                //language=xml
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <web-app>
                    <servlet>
                        <servlet-name>action</servlet-name>
                        <servlet-class>org.apache.struts.action.ActionServlet</servlet-class>
                        <init-param>
                            <param-name>config</param-name>
                            <param-value>/WEB-INF/struts-config.xml</param-value>
                        </init-param>
                        <load-on-startup>1</load-on-startup>
                    </servlet>
                    <servlet-mapping>
                        <servlet-name>action</servlet-name>
                        <url-pattern>*.do</url-pattern>
                    </servlet-mapping>
                </web-app>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <web-app>
                    <filter>
                        <filter-name>action</filter-name>
                        <filter-class>org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter</filter-class>
                    </filter>
                    <filter-mapping>
                        <filter-name>action</filter-name>
                        <url-pattern>/*</url-pattern>
                    </filter-mapping>
                </web-app>
                """,
                spec -> spec.path("web.xml")
            )
        );
    }

    @Test
    void noChangeForNonStrutsWebApp() {
        rewriteRun(
            xml(
                //language=xml
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <web-app>
                    <servlet>
                        <servlet-name>myServlet</servlet-name>
                        <servlet-class>com.example.MyServlet</servlet-class>
                    </servlet>
                </web-app>
                """,
                spec -> spec.path("web.xml")
            )
        );
    }
}
