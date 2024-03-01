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
package org.openrewrite.java.struts.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.srcMainResources;
import static org.openrewrite.xml.Assertions.xml;

public class FindStrutsActionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindStrutsActions());
    }

    @Test
    void findActions() {
        rewriteRun(
          srcMainResources(
            xml(
              //language=xml
              """
                <struts>
                    <package name="basicstruts2" extends="struts-default">
                        <action name="index">
                            <result>/index.jsp</result>
                        </action>
                        <action name="hello" class="org.apache.struts.helloworld.action.HelloWorldAction" method="execute">
                            <result name="success">/HelloWorld.jsp</result>
                        </action>
                    </package>
                </struts>
                """,
              //language=xml
              """
                <struts>
                    <package name="basicstruts2" extends="struts-default">
                        <!--~~>--><action name="index">
                            <result>/index.jsp</result>
                        </action>
                        <!--~~>--><action name="hello" class="org.apache.struts.helloworld.action.HelloWorldAction" method="execute">
                            <result name="success">/HelloWorld.jsp</result>
                        </action>
                    </package>
                </struts>
                """,
              spec -> spec.path("struts.xml")
            )
          )
        );
    }
}
