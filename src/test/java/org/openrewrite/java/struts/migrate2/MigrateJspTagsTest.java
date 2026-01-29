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

import static org.openrewrite.test.SourceSpecs.text;

class MigrateJspTagsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/struts2.yml",
                "org.openrewrite.java.struts.migrate2.MigrateJspTags");
    }

    @DocumentExample
    @Test
    void migrateHtmlFormTags() {
        rewriteRun(
            text(
                """
                <%@ page language="java" %>
                <html:form action="/login">
                    <html:text property="username"/>
                    <html:password property="password"/>
                    <html:submit value="Login"/>
                </html:form>
                """,
                """
                <%@ page language="java" %>
                <s:form action="/login">
                    <s:textfield name="username"/>
                    <s:password name="password"/>
                    <s:submit value="Login"/>
                </s:form>
                """,
                spec -> spec.path("login.jsp")
            )
        );
    }

    @Test
    void migrateBeanWriteTag() {
        rewriteRun(
            text(
                """
                <bean:write name="user" property="name"/>
                <bean:write property="message"/>
                """,
                """
                <s:property value="user.name"/>
                <s:property value="message"/>
                """,
                spec -> spec.path("display.jsp")
            )
        );
    }

    @Test
    void migrateLogicIterateTag() {
        rewriteRun(
            text(
                """
                <logic:iterate id="item" name="items">
                    <bean:write name="item" property="name"/>
                </logic:iterate>
                """,
                """
                <s:iterator value="items" var="item">
                    <s:property value="item.name"/>
                </s:iterator>
                """,
                spec -> spec.path("list.jsp")
            )
        );
    }

    @Test
    void migrateLogicPresentTag() {
        rewriteRun(
            text(
                """
                <logic:present name="user">
                    Welcome!
                </logic:present>
                <logic:notPresent name="user">
                    Please login.
                </logic:notPresent>
                """,
                """
                <s:if test="user != null">
                    Welcome!
                </s:if>
                <s:if test="user == null">
                    Please login.
                </s:if>
                """,
                spec -> spec.path("welcome.jsp")
            )
        );
    }

    @Test
    void migrateHtmlErrorsTag() {
        rewriteRun(
            text(
                """
                <html:errors/>
                """,
                """
                <s:actionerror/><s:fielderror/>
                """,
                spec -> spec.path("form.jsp")
            )
        );
    }

    @Test
    void migrateHtmlLinkTag() {
        rewriteRun(
            text(
                """
                <html:link action="/viewUser">View User</html:link>
                """,
                """
                <s:a action="/viewUser">View User</s:a>
                """,
                spec -> spec.path("nav.jsp")
            )
        );
    }

    @Test
    void noChangeForNonJspFile() {
        rewriteRun(
            text(
                """
                <html:form action="/login">
                    <html:text property="username"/>
                </html:form>
                """,
                spec -> spec.path("template.html")
            )
        );
    }
}
