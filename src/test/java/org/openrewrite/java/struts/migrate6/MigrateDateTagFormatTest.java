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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;

class MigrateDateTagFormatTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateDateTagFormat());
    }

    @DocumentExample
    @Test
    void migratesUPatternToDayOfWeek() {
        rewriteRun(
          text(
            """
              <%@ taglib prefix="s" uri="/struts-tags" %>
              <html>
              <body>
                  <s:date name="myDate" format="u" />
                  <s:date name="myDate" format="EEEE, u" />
              </body>
              </html>
              """,
            """
              <%@ taglib prefix="s" uri="/struts-tags" %>
              <html>
              <body>
                  <s:date name="myDate" format="e" />
                  <s:date name="myDate" format="EEEE, e" />
              </body>
              </html>
              """,
            spec -> spec.path("src/main/webapp/page.jsp")
          )
        );
    }

    @Test
    void migratesYYYYToYyyyForCalendarYear() {
        rewriteRun(
          text(
            """
              <%@ taglib prefix="s" uri="/struts-tags" %>
              <s:date name="myDate" format="MM/dd/YYYY" />
              <s:date name="myDate" format="YYYY-MM-dd HH:mm:ss" />
              """,
            """
              <%@ taglib prefix="s" uri="/struts-tags" %>
              <s:date name="myDate" format="MM/dd/yyyy" />
              <s:date name="myDate" format="yyyy-MM-dd HH:mm:ss" />
              """,
            spec -> spec.path("src/main/webapp/dates.jsp")
          )
        );
    }

    @Test
    void preservesYYYYForWeekBasedDatePattern() {
        rewriteRun(
          text(
            """
              <%@ taglib prefix="s" uri="/struts-tags" %>
              <s:date name="myDate" format="YYYY-'W'ww-e" />
              <s:date name="myDate" format="YYYY'W'ww" />
              """,
            spec -> spec.path("src/main/webapp/weekdates.jsp")
          )
        );
    }

    @Test
    void handlesMultiplePatternsInSameFile() {
        rewriteRun(
          text(
            """
              <%@ taglib prefix="s" uri="/struts-tags" %>
              <html>
              <body>
                  <!-- Date with day of week -->
                  <s:date name="date1" format="u EEEE" />

                  <!-- ISO date with wrong year pattern -->
                  <s:date name="date2" format="YYYY-MM-dd" />

                  <!-- Combined issues -->
                  <s:date name="date3" format="YYYY/MM/dd u" />
              </body>
              </html>
              """,
            """
              <%@ taglib prefix="s" uri="/struts-tags" %>
              <html>
              <body>
                  <!-- Date with day of week -->
                  <s:date name="date1" format="e EEEE" />

                  <!-- ISO date with wrong year pattern -->
                  <s:date name="date2" format="yyyy-MM-dd" />

                  <!-- Combined issues -->
                  <s:date name="date3" format="yyyy/MM/dd e" />
              </body>
              </html>
              """,
            spec -> spec.path("src/main/webapp/mixed.jsp")
          )
        );
    }

    @Test
    void handlesQuotedLiterals() {
        rewriteRun(
          text(
            """
              <s:date name="myDate" format="'Year:' YYYY" />
              <s:date name="myDate" format="'Today is' u" />
              """,
            """
              <s:date name="myDate" format="'Year:' yyyy" />
              <s:date name="myDate" format="'Today is' e" />
              """,
            spec -> spec.path("webapp/literals.jsp")
          )
        );
    }

    @Test
    void preservesCompatiblePatterns() {
        rewriteRun(
          text(
            """
              <%@ taglib prefix="s" uri="/struts-tags" %>
              <s:date name="myDate" format="yyyy-MM-dd" />
              <s:date name="myDate" format="HH:mm:ss" />
              <s:date name="myDate" format="yyyy-MM-dd'T'HH:mm:ss.SSS" />
              <s:date name="myDate" format="EEEE, MMMM d, yyyy" />
              """,
            spec -> spec.path("src/main/webapp/compatible.jsp")
          )
        );
    }

    @Test
    void handlesSingleQuotedFormatAttribute() {
        rewriteRun(
          text(
            """
              <s:date name="myDate" format='YYYY-MM-dd u' />
              """,
            """
              <s:date name="myDate" format='yyyy-MM-dd e' />
              """,
            spec -> spec.path("page.jsp")
          )
        );
    }

    @Test
    void ignoresNonJspFiles() {
        rewriteRun(
          text(
            """
              <s:date name="myDate" format="YYYY-MM-dd u" />
              """,
            spec -> spec.path("src/main/java/Example.java")
          )
        );
    }

    @Test
    void handlesFreeMarkerTemplates() {
        rewriteRun(
          text(
            """
              <@s.date name="myDate" format="YYYY-MM-dd" />
              <@s.date name="myDate" format="u EEEE" />
              """,
            """
              <@s.date name="myDate" format="yyyy-MM-dd" />
              <@s.date name="myDate" format="e EEEE" />
              """,
            spec -> spec.path("templates/page.ftl")
          )
        );
    }

    @Test
    void unitTestMigrateFormatPattern() {
        // Test u -> e conversion
        assertThat(MigrateDateTagFormat.migrateFormatPattern("u")).isEqualTo("e");
        assertThat(MigrateDateTagFormat.migrateFormatPattern("uu")).isEqualTo("ee");
        assertThat(MigrateDateTagFormat.migrateFormatPattern("EEEE u")).isEqualTo("EEEE e");

        // Test YYYY -> yyyy conversion
        assertThat(MigrateDateTagFormat.migrateFormatPattern("YYYY")).isEqualTo("yyyy");
        assertThat(MigrateDateTagFormat.migrateFormatPattern("YYYY-MM-dd")).isEqualTo("yyyy-MM-dd");
        assertThat(MigrateDateTagFormat.migrateFormatPattern("MM/dd/YYYY")).isEqualTo("MM/dd/yyyy");

        // Test YYYY preservation for week-based patterns
        assertThat(MigrateDateTagFormat.migrateFormatPattern("YYYY-'W'ww")).isEqualTo("YYYY-'W'ww");
        assertThat(MigrateDateTagFormat.migrateFormatPattern("YYYY'W'ww-e")).isEqualTo("YYYY'W'ww-e");

        // Test quoted literal preservation
        assertThat(MigrateDateTagFormat.migrateFormatPattern("'u' u")).isEqualTo("'u' e");
        assertThat(MigrateDateTagFormat.migrateFormatPattern("'YYYY' YYYY")).isEqualTo("'YYYY' yyyy");

        // Test compatible patterns unchanged
        assertThat(MigrateDateTagFormat.migrateFormatPattern("yyyy-MM-dd")).isEqualTo("yyyy-MM-dd");
        assertThat(MigrateDateTagFormat.migrateFormatPattern("HH:mm:ss")).isEqualTo("HH:mm:ss");
        assertThat(MigrateDateTagFormat.migrateFormatPattern("EEEE, MMMM d")).isEqualTo("EEEE, MMMM d");
    }
}
