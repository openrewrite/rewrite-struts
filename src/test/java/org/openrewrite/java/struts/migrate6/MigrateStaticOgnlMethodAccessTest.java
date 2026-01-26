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

class MigrateStaticOgnlMethodAccessTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateStaticOgnlMethodAccess());
    }

    @DocumentExample
    @Test
    void migrateStaticMethodInJspWithMapping() {
        rewriteRun(
          // struts.xml mapping
          xml(
            //language=xml
            """
              <struts>
                  <package name="app" extends="struts-default">
                      <action name="dashboard" class="com.example.DashboardAction">
                          <result>/dashboard.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            spec -> spec.path("struts.xml")
          ),
          // JSP with static method call
          xml(
            //language=xml
            """
              <html>
                  <body>
                      <s:property value="@com.app.Util@makeCode()" />
                  </body>
              </html>
              """,
            //language=xml
            """
              <html>
                  <body>
                      <s:property <!--~~>-->value="utilMakeCode" />
                  </body>
              </html>
              """,
            spec -> spec.path("dashboard.jsp")
          )
        );
    }

    @Test
    void migrateMultipleStaticMethods() {
        rewriteRun(
          xml(
            //language=xml
            """
              <struts>
                  <package name="app" extends="struts-default">
                      <action name="report" class="com.example.ReportAction">
                          <result>/report.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            spec -> spec.path("struts.xml")
          ),
          xml(
            //language=xml
            """
              <html>
                  <body>
                      <s:property value="@com.app.Format@currency(amount)" />
                      <s:property value="@com.app.DateUtil@formatDate(today)" />
                  </body>
              </html>
              """,
            //language=xml
            """
              <html>
                  <body>
                      <s:property <!--~~>-->value="formatCurrency" />
                      <s:property <!--~~>-->value="dateUtilFormatDate" />
                  </body>
              </html>
              """,
            spec -> spec.path("report.jsp")
          )
        );
    }

    @Test
    void updateOgnlExpressionOnly() {
        // When no struts.xml mapping exists, just update the OGNL expression
        // (user will need to add wrapper method manually)
        rewriteRun(
          xml(
            //language=xml
            """
              <html>
                  <body>
                      <s:property value="@com.app.Util@makeCode()" />
                  </body>
              </html>
              """,
            //language=xml
            """
              <html>
                  <body>
                      <s:property <!--~~>-->value="utilMakeCode" />
                  </body>
              </html>
              """,
            spec -> spec.path("unmapped.jsp")
          )
        );
    }

    @Test
    void preserveStaticFieldAccess() {
        // Static field access (without parentheses) is still allowed
        rewriteRun(
          xml(
            //language=xml
            """
              <html>
                  <body>
                      <s:property value="@com.app.Constants@MAX_VALUE" />
                  </body>
              </html>
              """,
            spec -> spec.path("constants.jsp")
          )
        );
    }

    @Test
    void migrateStaticMethodInTextContent() {
        rewriteRun(
          xml(
            //language=xml
            """
              <struts>
                  <package name="app" extends="struts-default">
                      <action name="config" class="com.example.ConfigAction">
                          <param name="format">@com.app.Format@getDefault()</param>
                          <result>/config.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            //language=xml
            """
              <struts>
                  <package name="app" extends="struts-default">
                      <action name="config" class="com.example.ConfigAction">
                          <param name="format"><!--~~>-->formatGetDefault</param>
                          <result>/config.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            spec -> spec.path("struts.xml")
          )
        );
    }
}
