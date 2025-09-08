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

class MigrateDynamicMethodInvocationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateDynamicMethodInvocation());
    }

    @DocumentExample
    @Test
    void migrateSimpleActionWithMultipleResults() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="true"/>

                  <package name="crud" extends="struts-default">
                      <action name="user" class="com.example.UserAction">
                          <result name="list">/users.jsp</result>
                          <result name="edit">/editUser.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="false"/>

                  <package name="crud" extends="struts-default">
                      <action name="userList" class="com.example.UserAction" method="list">
                          <result name="list">/users.jsp</result>
                      </action>
                      <action name="userEdit" class="com.example.UserAction" method="edit">
                          <result name="edit">/editUser.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }

    @Test
    void migrateActionWithDefaultResult() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="true"/>

                  <package name="app" extends="struts-default">
                      <action name="product" class="com.example.ProductAction">
                          <result>/product.jsp</result>
                          <result name="view">/viewProduct.jsp</result>
                          <result name="edit">/editProduct.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="false"/>

                  <package name="app" extends="struts-default">
                      <action name="product" class="com.example.ProductAction">
                          <result>/product.jsp</result>
                      </action>
                      <action name="productView" class="com.example.ProductAction" method="view">
                          <result name="view">/viewProduct.jsp</result>
                      </action>
                      <action name="productEdit" class="com.example.ProductAction" method="edit">
                          <result name="edit">/editProduct.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }

    @Test
    void migrateActionWithSuccessResult() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="true"/>

                  <package name="app" extends="struts-default">
                      <action name="account" class="com.example.AccountAction">
                          <result name="success">/account.jsp</result>
                          <result name="create">/createAccount.jsp</result>
                          <result name="delete">/deleteAccount.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="false"/>

                  <package name="app" extends="struts-default">
                      <action name="account" class="com.example.AccountAction">
                          <result name="success">/account.jsp</result>
                      </action>
                      <action name="accountCreate" class="com.example.AccountAction" method="create">
                          <result name="create">/createAccount.jsp</result>
                      </action>
                      <action name="accountDelete" class="com.example.AccountAction" method="delete">
                          <result name="delete">/deleteAccount.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }

    @Test
    void doNotMigrateWhenDMIAlreadyDisabled() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="false"/>

                  <package name="crud" extends="struts-default">
                      <action name="user" class="com.example.UserAction" method="list">
                          <result>/users.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }

    @Test
    void doNotMigrateWhenDMINotConfigured() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <package name="crud" extends="struts-default">
                      <action name="user" class="com.example.UserAction">
                          <result name="list">/users.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }

    @Test
    void migrateActionWithErrorResult() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="true"/>

                  <package name="app" extends="struts-default">
                      <action name="order" class="com.example.OrderAction">
                          <result name="error">/error.jsp</result>
                          <result name="submit">/submitOrder.jsp</result>
                          <result name="cancel">/cancelOrder.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="false"/>

                  <package name="app" extends="struts-default">
                      <action name="order" class="com.example.OrderAction">
                          <result name="error">/error.jsp</result>
                      </action>
                      <action name="orderSubmit" class="com.example.OrderAction" method="submit">
                          <result name="submit">/submitOrder.jsp</result>
                      </action>
                      <action name="orderCancel" class="com.example.OrderAction" method="cancel">
                          <result name="cancel">/cancelOrder.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }

    @Test
    void preserveOtherConstants() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <constant name="struts.devMode" value="true"/>
                  <constant name="struts.enable.DynamicMethodInvocation" value="true"/>
                  <constant name="struts.i18n.encoding" value="UTF-8"/>
              </struts>
              """,
            """
              <struts>
                  <constant name="struts.devMode" value="true"/>
                  <constant name="struts.enable.DynamicMethodInvocation" value="false"/>
                  <constant name="struts.i18n.encoding" value="UTF-8"/>
              </struts>
              """
          )
        );
    }

    @Test
    void handleMultiplePackages() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="true"/>

                  <package name="admin" extends="struts-default">
                      <action name="user" class="com.example.admin.UserAction">
                          <result name="list">/admin/users.jsp</result>
                          <result name="edit">/admin/editUser.jsp</result>
                      </action>
                  </package>

                  <package name="public" extends="struts-default">
                      <action name="product" class="com.example.ProductAction">
                          <result name="view">/product.jsp</result>
                          <result name="buy">/buyProduct.jsp</result>
                      </action>
                  </package>
              </struts>
              """,
            """
              <struts>
                  <constant name="struts.enable.DynamicMethodInvocation" value="false"/>

                  <package name="admin" extends="struts-default">
                      <action name="userList" class="com.example.admin.UserAction" method="list">
                          <result name="list">/admin/users.jsp</result>
                      </action>
                      <action name="userEdit" class="com.example.admin.UserAction" method="edit">
                          <result name="edit">/admin/editUser.jsp</result>
                      </action>
                  </package>

                  <package name="public" extends="struts-default">
                      <action name="productView" class="com.example.ProductAction" method="view">
                          <result name="view">/product.jsp</result>
                      </action>
                      <action name="productBuy" class="com.example.ProductAction" method="buy">
                          <result name="buy">/buyProduct.jsp</result>
                      </action>
                  </package>
              </struts>
              """
          )
        );
    }

    @Test
    void migrateWhenConstantAfterPackage() {
        rewriteRun(
          //language=xml
          xml(
            """
              <struts>
                  <package name="crud" extends="struts-default">
                      <action name="user" class="com.example.UserAction">
                          <result name="edit">/editUser.jsp</result>
                      </action>
                  </package>

                  <constant name="struts.enable.DynamicMethodInvocation" value="true"/>
              </struts>
              """,
            """
              <struts>
                  <package name="crud" extends="struts-default">
                      <action name="userEdit" class="com.example.UserAction" method="edit">
                          <result name="edit">/editUser.jsp</result>
                      </action>
                  </package>

                  <constant name="struts.enable.DynamicMethodInvocation" value="false"/>
              </struts>
              """
          )
        );
    }
}
