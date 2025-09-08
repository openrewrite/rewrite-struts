/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateAwareInterfacesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.struts.migrate6.MigrateAwareInterfaces")
          .parser(JavaParser.fromJavaVersion()
            .classpath("javax.servlet-api")
            .addClasspathEntry(JavaParser.dependenciesFromResources(new InMemoryExecutionContext(), "struts2-core-2.5").getFirst()));
    }

    @DocumentExample
    @Test
    void migrateSessionAware() {
        rewriteRun(
          //language=java
          java(
            """
              import com.opensymphony.xwork2.ActionInvocation;
              import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
              import org.apache.struts2.interceptor.SessionAware;

              import java.util.Map;

              public class CustomSecurityInterceptor extends AbstractInterceptor implements SessionAware {

                  private Map<String, Object> session;

                  @Override
                  public void setSession(Map<String, Object> session) {
                      this.session = session;
                  }

                  @Override
                  public String intercept(ActionInvocation invocation) throws Exception {
                      return invocation.invoke();
                  }
              }
              """,
            """
              import com.opensymphony.xwork2.ActionInvocation;
              import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
              import org.apache.struts2.action.SessionAware;

              import java.util.Map;

              public class CustomSecurityInterceptor extends AbstractInterceptor implements SessionAware {

                  private Map<String, Object> session;

                  @Override
                  public void withSession(Map<String, Object> session) {
                      this.session = session;
                  }

                  @Override
                  public String intercept(ActionInvocation invocation) throws Exception {
                      return invocation.invoke();
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateOnlyApplicationAware() {
        rewriteRun(
          //language=java
          java(
            """
              import com.opensymphony.xwork2.ActionInvocation;
              import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
              import org.apache.struts2.interceptor.ApplicationAware;

              import java.util.Map;

              public class ApplicationInterceptor extends AbstractInterceptor implements ApplicationAware {
                  private Map<String, Object> application;

                  @Override
                  public void setApplication(Map<String, Object> application) {
                      this.application = application;
                  }

                  @Override
                  public String intercept(ActionInvocation invocation) throws Exception {
                      return invocation.invoke();
                  }
              }
              """,
            """
              import com.opensymphony.xwork2.ActionInvocation;
              import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
              import org.apache.struts2.action.ApplicationAware;

              import java.util.Map;

              public class ApplicationInterceptor extends AbstractInterceptor implements ApplicationAware {
                  private Map<String, Object> application;

                  @Override
                  public void withApplication(Map<String, Object> application) {
                      this.application = application;
                  }

                  @Override
                  public String intercept(ActionInvocation invocation) throws Exception {
                      return invocation.invoke();
                  }
              }
              """
          )
        );
    }

    @Test
    void migratePrincipalAware() {
        rewriteRun(
          //language=java
          java(
            """
              import com.opensymphony.xwork2.ActionInvocation;
              import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
              import org.apache.struts2.interceptor.PrincipalAware;
              import org.apache.struts2.interceptor.PrincipalProxy;

              public class PrincipalInterceptor extends AbstractInterceptor implements PrincipalAware {
                  private PrincipalProxy principalProxy;

                  @Override
                  public void setPrincipalProxy(PrincipalProxy principalProxy) {
                      this.principalProxy = principalProxy;
                  }

                  @Override
                  public String intercept(ActionInvocation invocation) throws Exception {
                      return invocation.invoke();
                  }
              }
              """,
            """
              import com.opensymphony.xwork2.ActionInvocation;
              import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
              import org.apache.struts2.action.PrincipalAware;
              import org.apache.struts2.interceptor.PrincipalProxy;

              public class PrincipalInterceptor extends AbstractInterceptor implements PrincipalAware {
                  private PrincipalProxy principalProxy;

                  @Override
                  public void withPrincipalProxy(PrincipalProxy principalProxy) {
                      this.principalProxy = principalProxy;
                  }

                  @Override
                  public String intercept(ActionInvocation invocation) throws Exception {
                      return invocation.invoke();
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateServletRequestAndResponseAware() {
        rewriteRun(
          //language=java
          java(
            """
              import com.opensymphony.xwork2.ActionInvocation;
              import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
              import org.apache.struts2.interceptor.ServletRequestAware;
              import org.apache.struts2.interceptor.ServletResponseAware;

              import javax.servlet.http.HttpServletRequest;
              import javax.servlet.http.HttpServletResponse;

              public class ServletInterceptor extends AbstractInterceptor implements ServletRequestAware, ServletResponseAware {
                  private HttpServletRequest request;
                  private HttpServletResponse response;

                  @Override
                  public void setServletRequest(HttpServletRequest request) {
                      this.request = request;
                  }

                  @Override
                  public void setServletResponse(HttpServletResponse response) {
                      this.response = response;
                  }

                  @Override
                  public String intercept(ActionInvocation invocation) throws Exception {
                      return invocation.invoke();
                  }
              }
              """,
            """
              import com.opensymphony.xwork2.ActionInvocation;
              import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
              import org.apache.struts2.action.ServletRequestAware;
              import org.apache.struts2.action.ServletResponseAware;

              import javax.servlet.http.HttpServletRequest;
              import javax.servlet.http.HttpServletResponse;

              public class ServletInterceptor extends AbstractInterceptor implements ServletRequestAware, ServletResponseAware {
                  private HttpServletRequest request;
                  private HttpServletResponse response;

                  @Override
                  public void withServletRequest(HttpServletRequest request) {
                      this.request = request;
                  }

                  @Override
                  public void withServletResponse(HttpServletResponse response) {
                      this.response = response;
                  }

                  @Override
                  public String intercept(ActionInvocation invocation) throws Exception {
                      return invocation.invoke();
                  }
              }
              """
          )
        );
    }
}
