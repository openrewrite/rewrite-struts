/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://docs.moderne.io/licensing/moderne-source-available-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.struts.migrate7;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class RenameOpenSymphonyToStruts2Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.struts.migrate7.RenameOpenSymphonyToStruts2")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "struts2-core-6.0"));
    }

    @DocumentExample
    @Test
    void migrateCoreActionAndContextTypes() {
        rewriteRun(
          java(
            """
              import com.opensymphony.xwork2.Action;
              import com.opensymphony.xwork2.ActionSupport;
              import com.opensymphony.xwork2.ActionChainResult;

              class MyAction extends ActionSupport implements Action {
                  ActionChainResult result;
              }
              """,
            spec -> spec.after(after -> {
                assertThat(after).contains("org.apache.struts2.action.Action");
                assertThat(after).contains("org.apache.struts2.result.ActionChainResult");
                return after;
            })
          )
        );
    }

    @Test
    void migrateInterceptorAndValidationTypes() {
        rewriteRun(
          java(
            """
              import com.opensymphony.xwork2.interceptor.Interceptor;
              import com.opensymphony.xwork2.interceptor.ValidationAware;
              import com.opensymphony.xwork2.Preparable;

              class MyInterceptor implements Interceptor, ValidationAware, Preparable {
              }
              """,
            spec -> spec.after(after -> {
                assertThat(after).contains("org.apache.struts2.interceptor.Interceptor");
                assertThat(after).contains("org.apache.struts2.interceptor.ValidationAware");
                assertThat(after).contains("org.apache.struts2.Preparable");
                return after;
            })
          )
        );
    }

    @Test
    void migrateValueStackAndUtilityTypes() {
        rewriteRun(
          java(
            """
              import com.opensymphony.xwork2.util.ValueStack;
              import com.opensymphony.xwork2.util.AnnotationUtils;

              class UtilUser {
                  ValueStack stack;
                  AnnotationUtils utils;
              }
              """,
            spec -> spec.after(after -> {
                assertThat(after).contains("org.apache.struts2.util.ValueStack");
                assertThat(after).contains("org.apache.struts2.util.AnnotationUtils");
                return after;
            })
          )
        );
    }

    @Test
    void migrateTextAndLocalizationTypes() {
        rewriteRun(
          java(
            """
              import com.opensymphony.xwork2.TextProvider;
              import com.opensymphony.xwork2.CompositeTextProvider;
              import com.opensymphony.xwork2.DefaultTextProvider;
              import com.opensymphony.xwork2.LocalizedTextProvider;
              import com.opensymphony.xwork2.ResourceBundleTextProvider;
              import com.opensymphony.xwork2.TextProviderFactory;
              import com.opensymphony.xwork2.TextProviderSupport;
              import com.opensymphony.xwork2.util.GlobalLocalizedTextProvider;
              import com.opensymphony.xwork2.util.StrutsLocalizedTextProvider;

              class TextUser extends DefaultTextProvider implements TextProvider {
                  CompositeTextProvider composite;
                  LocalizedTextProvider localized;
                  ResourceBundleTextProvider bundle;
                  TextProviderFactory factory;
                  TextProviderSupport support;
                  GlobalLocalizedTextProvider globalProvider;
                  StrutsLocalizedTextProvider strutsProvider;
              }
              """,
            spec -> spec.after(after ->
              assertThat(after)
                .doesNotContain("com.opensymphony.xwork2")
                .contains("import org.apache.struts2.text.*;")
                .actual())
          )
        );

    }

    @Test
    void migrateLocaleTypes() {
        rewriteRun(
          java(
            """
              import com.opensymphony.xwork2.DefaultLocaleProvider;
              import com.opensymphony.xwork2.DefaultLocaleProviderFactory;
              import com.opensymphony.xwork2.LocaleProvider;
              import com.opensymphony.xwork2.LocaleProviderFactory;

              class LocaleUser {
                  DefaultLocaleProvider provider;
                  DefaultLocaleProviderFactory providerFactory;
                  LocaleProvider localeProvider;
                  LocaleProviderFactory localeProviderFactory;
              }
              """,
            """
              import org.apache.struts2.locale.DefaultLocaleProvider;
              import org.apache.struts2.locale.DefaultLocaleProviderFactory;
              import org.apache.struts2.locale.LocaleProvider;
              import org.apache.struts2.locale.LocaleProviderFactory;

              class LocaleUser {
                  DefaultLocaleProvider provider;
                  DefaultLocaleProviderFactory providerFactory;
                  LocaleProvider localeProvider;
                  LocaleProviderFactory localeProviderFactory;
              }
              """
          )
        );
    }

    @Test
    void catchAllChangePackageHandlesUnlistedTypes() {
        rewriteRun(
          java(
            """
              import com.opensymphony.xwork2.util.ValueStack;

              class InternalUser {
                  ValueStack valuestack;
              }
              """,
            """
              import org.apache.struts2.util.ValueStack;

              class InternalUser {
                  ValueStack valuestack;
              }
              """
          )
        );
    }
}
