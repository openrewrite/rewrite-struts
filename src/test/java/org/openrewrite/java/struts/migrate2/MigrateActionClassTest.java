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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateActionClassTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateActionClass())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "struts-1.2", "struts2-core-2.5", "javax.servlet-api-4.0"));
    }

    @DocumentExample
    @Test
    void migrateBasicAction() {
        rewriteRun(
            //language=java
            java(
                """
                import org.apache.struts.action.Action;
                import org.apache.struts.action.ActionForm;
                import org.apache.struts.action.ActionForward;
                import org.apache.struts.action.ActionMapping;
                import javax.servlet.http.HttpServletRequest;
                import javax.servlet.http.HttpServletResponse;

                public class LoginAction extends Action {
                    public ActionForward execute(ActionMapping mapping, ActionForm form,
                            HttpServletRequest request, HttpServletResponse response) {
                        return mapping.findForward("success");
                    }
                }
                """,
                """
                import com.opensymphony.xwork2.ActionSupport;

                public class LoginAction extends ActionSupport {
                    public String execute() {
                        return SUCCESS;
                    }
                }
                """
            )
        );
    }

    @Test
    void noChangeForNonStrutsClass() {
        rewriteRun(
            //language=java
            java(
                """
                public class RegularClass {
                    public String execute() {
                        return "hello";
                    }
                }
                """
            )
        );
    }
}
