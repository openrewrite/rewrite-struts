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

class MigrateInterceptorDisabledParamTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateInterceptorDisabledParam());
    }

    @DocumentExample
    @Test
    void migrateExcludeMethodsToDisabled() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <package name="default" extends="struts-default">
                      <action name="save" class="com.example.SaveAction">
                          <interceptor-ref name="defaultStack">
                              <param name="validation.excludeMethods">input,back,cancel</param>
                          </interceptor-ref>
                          <result>/success.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            """
              <struts>
                  <package name="default" extends="struts-default">
                      <action name="save" class="com.example.SaveAction">
                          <interceptor-ref name="defaultStack">
                              <param name="validation.disabled">input,back,cancel</param>
                          </interceptor-ref>
                          <result>/success.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }

    @Test
    void migrateMultipleOccurrences() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <package name="default" extends="struts-default">
                      <action name="save" class="com.example.SaveAction">
                          <interceptor-ref name="defaultStack">
                              <param name="validation.excludeMethods">input</param>
                          </interceptor-ref>
                          <result>/success.jsp</result>
                      </action>
                      <action name="update" class="com.example.UpdateAction">
                          <interceptor-ref name="defaultStack">
                              <param name="validation.excludeMethods">cancel</param>
                          </interceptor-ref>
                          <result>/updated.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            """
              <struts>
                  <package name="default" extends="struts-default">
                      <action name="save" class="com.example.SaveAction">
                          <interceptor-ref name="defaultStack">
                              <param name="validation.disabled">input</param>
                          </interceptor-ref>
                          <result>/success.jsp</result>
                      </action>
                      <action name="update" class="com.example.UpdateAction">
                          <interceptor-ref name="defaultStack">
                              <param name="validation.disabled">cancel</param>
                          </interceptor-ref>
                          <result>/updated.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }

    @Test
    void noChangeWhenAlreadyMigrated() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <package name="default" extends="struts-default">
                      <action name="save" class="com.example.SaveAction">
                          <interceptor-ref name="defaultStack">
                              <param name="validation.disabled">input,back,cancel</param>
                          </interceptor-ref>
                          <result>/success.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }

    @Test
    void noChangeForOtherParams() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <package name="default" extends="struts-default">
                      <action name="save" class="com.example.SaveAction">
                          <interceptor-ref name="defaultStack">
                              <param name="fileUpload.maximumSize">2097152</param>
                          </interceptor-ref>
                          <result>/success.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }
}
