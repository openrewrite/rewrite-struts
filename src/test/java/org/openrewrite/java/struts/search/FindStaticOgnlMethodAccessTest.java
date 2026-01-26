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
package org.openrewrite.java.struts.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class FindStaticOgnlMethodAccessTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindStaticOgnlMethodAccess());
    }

    @DocumentExample
    @Test
    void findStaticMethodAccessInStrutsTag() {
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
                      <s:property <!--~~>-->value="@com.app.Util@makeCode()" />
                  </body>
              </html>
              """,
            spec -> spec.path("index.jsp")
          )
        );
    }

    @Test
    void findMultipleStaticMethodAccess() {
        rewriteRun(
          xml(
            //language=xml
            """
              <html>
                  <body>
                      <s:property value="@com.app.Util@makeCode()" />
                      <s:property value="@java.lang.Math@max(1, 2)" />
                      <s:if test="@com.app.Security@isAdmin()">
                          <p>Admin content</p>
                      </s:if>
                  </body>
              </html>
              """,
            //language=xml
            """
              <html>
                  <body>
                      <s:property <!--~~>-->value="@com.app.Util@makeCode()" />
                      <s:property <!--~~>-->value="@java.lang.Math@max(1, 2)" />
                      <s:if <!--~~>-->test="@com.app.Security@isAdmin()">
                          <p>Admin content</p>
                      </s:if>
                  </body>
              </html>
              """,
            spec -> spec.path("dashboard.jsp")
          )
        );
    }

    @Test
    void findStaticMethodWithArguments() {
        rewriteRun(
          xml(
            //language=xml
            """
              <html>
                  <body>
                      <s:property value="@com.app.Format@currency(amount)" />
                      <s:property value="@com.app.DateUtil@format(date, 'yyyy-MM-dd')" />
                  </body>
              </html>
              """,
            //language=xml
            """
              <html>
                  <body>
                      <s:property <!--~~>-->value="@com.app.Format@currency(amount)" />
                      <s:property <!--~~>-->value="@com.app.DateUtil@format(date, 'yyyy-MM-dd')" />
                  </body>
              </html>
              """,
            spec -> spec.path("report.jsp")
          )
        );
    }

    @Test
    void ignoreNonStaticOgnlExpressions() {
        rewriteRun(
          xml(
            //language=xml
            """
              <html>
                  <body>
                      <s:property value="user.name" />
                      <s:property value="getCode()" />
                      <s:property value="items[0]" />
                      <s:textfield name="email" label="Email Address" />
                  </body>
              </html>
              """,
            spec -> spec.path("form.jsp")
          )
        );
    }

    @Test
    void findStaticMethodAccessInStrutsXml() {
        rewriteRun(
          xml(
            //language=xml
            """
              <struts>
                  <package name="app" extends="struts-default">
                      <action name="report" class="com.app.ReportAction">
                          <param name="format">@com.app.Format@DEFAULT()</param>
                          <result>/report.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            //language=xml
            """
              <struts>
                  <package name="app" extends="struts-default">
                      <action name="report" class="com.app.ReportAction">
                          <param name="format"><!--~~>-->@com.app.Format@DEFAULT()</param>
                          <result>/report.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            spec -> spec.path("struts.xml")
          )
        );
    }

    @Test
    void ignoreStaticFieldAccess() {
        // Static field access (without parentheses) is still allowed in Struts 6
        rewriteRun(
          xml(
            //language=xml
            """
              <html>
                  <body>
                      <s:property value="@com.app.Constants@MAX_VALUE" />
                      <s:property value="@java.lang.Integer@MAX_VALUE" />
                  </body>
              </html>
              """,
            spec -> spec.path("constants.jsp")
          )
        );
    }
}
