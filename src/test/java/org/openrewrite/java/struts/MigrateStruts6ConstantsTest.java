/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.struts;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.srcMainResources;
import static org.openrewrite.xml.Assertions.xml;

class MigrateStruts6ConstantsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
            .scanYamlResources()
            .build()
            .activateRecipes("org.openrewrite.java.struts.migrate6.MigrateStruts6Constants"))
          .expectedCyclesThatMakeChanges(1).cycles(1);
    }

    @Test
    void inStrutsXml() {
        rewriteRun(
          srcMainResources(
            xml(
              //language=xml
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE struts PUBLIC
                    "-//Apache Software Foundation//DTD Struts Configuration 2.5//EN"
                    "http://struts.apache.org/dtds/struts-2.5.dtd">
                <struts>
                    <constant name="devMode" value="true" />
                </struts>
                """,
              //language=xml
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE struts PUBLIC
                    "-//Apache Software Foundation//DTD Struts Configuration 2.5//EN"
                    "http://struts.apache.org/dtds/struts-2.5.dtd">
                <struts>
                    <constant name="struts.devMode" value="true" />
                </struts>
                """
            )
          )
        );
    }
}
