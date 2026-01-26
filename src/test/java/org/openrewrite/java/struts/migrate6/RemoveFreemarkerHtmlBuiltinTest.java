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

import static org.openrewrite.test.SourceSpecs.text;

class RemoveFreemarkerHtmlBuiltinTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.struts.migrate6.RemoveFreemarkerHtmlBuiltin");
    }

    @DocumentExample
    @Test
    void removeHtmlBuiltinFromExpression() {
        rewriteRun(
          text(
            """
              <html>
              <head><title>${title?html}</title></head>
              <body>
                  <h1>${heading?html}</h1>
                  <p>${content?html}</p>
              </body>
              </html>
              """,
            """
              <html>
              <head><title>${title}</title></head>
              <body>
                  <h1>${heading}</h1>
                  <p>${content}</p>
              </body>
              </html>
              """,
            spec -> spec.path("template.ftl")
          )
        );
    }

    @Test
    void removeHtmlBuiltinFromNestedExpression() {
        rewriteRun(
          text(
            """
              <div>
                  <span>${user.name?html}</span>
                  <span>${user.email?html}</span>
              </div>
              """,
            """
              <div>
                  <span>${user.name}</span>
                  <span>${user.email}</span>
              </div>
              """,
            spec -> spec.path("user.ftl")
          )
        );
    }

    @Test
    void preserveOtherBuiltins() {
        rewriteRun(
          text(
            """
              <p>${text?upper_case}</p>
              <p>${text?lower_case}</p>
              <p>${text?cap_first}</p>
              """,
            spec -> spec.path("other.ftl")
          )
        );
    }

    @Test
    void removeHtmlFromChainedBuiltins() {
        rewriteRun(
          text(
            """
              <p>${text?html?upper_case}</p>
              <p>${text?trim?html}</p>
              """,
            """
              <p>${text?upper_case}</p>
              <p>${text?trim}</p>
              """,
            spec -> spec.path("chained.ftl")
          )
        );
    }

    @Test
    void noChangeForNonFtlFiles() {
        rewriteRun(
          text(
            """
              This file contains ?html but should not be modified.
              """,
            spec -> spec.path("readme.txt")
          )
        );
    }

    @Test
    void handleStrutsTagsWithHtmlBuiltin() {
        rewriteRun(
          text(
            """
              <@s.form action="save">
                  <@s.textfield name="name" label="${nameLabel?html}"/>
                  <@s.submit value="${submitLabel?html}"/>
              </@s.form>
              """,
            """
              <@s.form action="save">
                  <@s.textfield name="name" label="${nameLabel}"/>
                  <@s.submit value="${submitLabel}"/>
              </@s.form>
              """,
            spec -> spec.path("form.ftl")
          )
        );
    }
}
