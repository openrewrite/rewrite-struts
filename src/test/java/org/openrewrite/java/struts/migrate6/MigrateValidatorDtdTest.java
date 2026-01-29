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

class MigrateValidatorDtdTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateValidatorDtd());
    }

    @DocumentExample
    @Test
    void migrateValidatorDtd() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <!DOCTYPE validators PUBLIC
                      "-//Apache Struts//XWork Validator 1.0.3//EN"
                      "http://struts.apache.org/dtds/xwork-validator-1.0.3.dtd">
              <validators>
                  <field name="username">
                      <field-validator type="requiredstring">
                          <message>Username is required</message>
                      </field-validator>
                  </field>
              </validators>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <!DOCTYPE validators PUBLIC
                      "-//Apache Struts//XWork Validator 1.0.4//EN"
                      "http://struts.apache.org/dtds/xwork-validator-1.0.4.dtd">
              <validators>
                  <field name="username">
                      <field-validator type="requiredstring">
                          <message>Username is required</message>
                      </field-validator>
                  </field>
              </validators>
              """,
            spec -> spec.path("UserAction-validation.xml")
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
              <!DOCTYPE validators PUBLIC
                      "-//Apache Struts//XWork Validator 1.0.4//EN"
                      "http://struts.apache.org/dtds/xwork-validator-1.0.4.dtd">
              <validators>
                  <field name="email">
                      <field-validator type="email">
                          <message>Invalid email format</message>
                      </field-validator>
                  </field>
              </validators>
              """,
            spec -> spec.path("ContactAction-validation.xml")
          )
        );
    }

    @Test
    void noChangeForOtherDtd() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <!DOCTYPE struts PUBLIC
                      "-//Apache Software Foundation//DTD Struts Configuration 6.0//EN"
                      "https://struts.apache.org/dtds/struts-6.0.dtd">
              <struts>
                  <package name="default" extends="struts-default">
                  </package>
              </struts>
              """
          )
        );
    }
}
