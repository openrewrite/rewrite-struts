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

class MigrateStrutsConfigTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateStrutsConfig());
    }

    @DocumentExample
    @Test
    void migrateBasicStrutsConfig() {
        rewriteRun(
            xml(
                //language=xml
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE struts-config PUBLIC
                    "-//Apache Software Foundation//DTD Struts Configuration 1.2//EN"
                    "http://struts.apache.org/dtds/struts-config_1_2.dtd">
                <struts-config>
                    <action-mappings>
                        <action path="/login" type="com.example.LoginAction">
                            <forward name="success" path="/welcome.jsp"/>
                        </action>
                    </action-mappings>
                </struts-config>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE struts-config PUBLIC
                    "-//Apache Software Foundation//DTD Struts Configuration 1.2//EN"
                    "http://struts.apache.org/dtds/struts-config_1_2.dtd">
                <struts>
                    <package name="default" extends="struts-default">
                        <action name="login" class="com.example.LoginAction">
                            <result name="success">/welcome.jsp</result>
                        </action>
                    </package>
                </struts>
                """,
                spec -> spec.path("struts-config.xml").afterRecipe(cu ->
                    // Verify the file was renamed
                    org.assertj.core.api.Assertions.assertThat(cu.getSourcePath().getFileName().toString())
                        .isEqualTo("struts.xml"))
            )
        );
    }

    @Test
    void migrateMultipleActions() {
        rewriteRun(
            xml(
                //language=xml
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <struts-config>
                    <action-mappings>
                        <action path="/login" type="com.example.LoginAction">
                            <forward name="success" path="/welcome.jsp"/>
                            <forward name="error" path="/error.jsp"/>
                        </action>
                        <action path="/logout" type="com.example.LogoutAction">
                            <forward name="success" path="/goodbye.jsp"/>
                        </action>
                    </action-mappings>
                </struts-config>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <struts>
                    <package name="default" extends="struts-default">
                        <action name="login" class="com.example.LoginAction">
                            <result name="success">/welcome.jsp</result>
                            <result name="error">/error.jsp</result>
                        </action>
                        <action name="logout" class="com.example.LogoutAction">
                            <result name="success">/goodbye.jsp</result>
                        </action>
                    </package>
                </struts>
                """,
                spec -> spec.path("struts-config.xml")
            )
        );
    }

    @Test
    void noChangeForStruts2Config() {
        rewriteRun(
            xml(
                //language=xml
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <struts>
                    <package name="default" extends="struts-default">
                        <action name="hello" class="com.example.HelloAction">
                            <result name="success">/hello.jsp</result>
                        </action>
                    </package>
                </struts>
                """,
                spec -> spec.path("struts.xml")
            )
        );
    }
}
