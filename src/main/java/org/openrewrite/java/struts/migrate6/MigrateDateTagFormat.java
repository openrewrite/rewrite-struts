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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migrates date format patterns in Struts {@code <s:date>} tags from SimpleDateFormat
 * syntax to DateTimeFormatter syntax.
 * <p>
 * In Struts 6.0, the date tag switched from using {@code SimpleDateFormat} to
 * {@code DateTimeFormatter}. While many patterns are compatible, some pattern letters
 * have different meanings:
 * <ul>
 *   <li>{@code u} - SimpleDateFormat: day-number-of-week (1-7). DateTimeFormatter: year.
 *       Converted to {@code e} (localized day-of-week).</li>
 *   <li>{@code YYYY} - Week-based-year. Often misused in SimpleDateFormat for calendar year.
 *       Converted to {@code yyyy} when not part of a week-based date pattern.</li>
 * </ul>
 *
 * @see <a href="https://cwiki.apache.org/confluence/display/WW/Struts+2.5+to+6.0.0+migration#Struts2.5to6.0.0migration-Thedatetag">Struts Migration Guide - Date Tag</a>
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateDateTagFormat extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate Struts date tag format patterns";
    }

    @Override
    public String getDescription() {
        return "Converts SimpleDateFormat patterns in `<s:date>` tags to DateTimeFormatter-compatible patterns. " +
               "Struts 6.0 uses DateTimeFormatter instead of SimpleDateFormat, which has different pattern letter meanings.";
    }

    // Patterns to match Struts date tags with format attribute:
    // - JSP: <s:date ... format="..." ...> or <s:date ... format='...' ...>
    // - FreeMarker: <@s.date ... format="..." ...> or <@s.date ... format='...' ...>
    // We use separate patterns for double and single quotes to correctly handle
    // date format literals that contain single quotes (e.g., "'Year:' yyyy")
    private static final Pattern DATE_TAG_DOUBLE_QUOTE_PATTERN = Pattern.compile(
            "(<(?:s:date|@s\\.date)\\b[^>]*\\bformat\\s*=\\s*)(\")((?:[^\"]|\\\\\")*)(\")",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATE_TAG_SINGLE_QUOTE_PATTERN = Pattern.compile(
            "(<(?:s:date|@s\\.date)\\b[^>]*\\bformat\\s*=\\s*)(')((?:[^']|\\\\')*)(')");
    private static final Pattern[] DATE_TAG_PATTERNS = {DATE_TAG_DOUBLE_QUOTE_PATTERN, DATE_TAG_SINGLE_QUOTE_PATTERN};

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext ctx) {
                String sourcePath = text.getSourcePath().toString();
                // Only process JSP and FreeMarker template files
                if (!sourcePath.endsWith(".jsp") && !sourcePath.endsWith(".ftl")) {
                    return text;
                }

                String content = text.getText();
                boolean modified = false;

                // Apply both patterns (double and single quote variants)
                for (Pattern pattern : DATE_TAG_PATTERNS) {
                    StringBuffer result = new StringBuffer();
                    Matcher matcher = pattern.matcher(content);

                    while (matcher.find()) {
                        String prefix = matcher.group(1);    // <s:date ... format=
                        String quote = matcher.group(2);     // " or '
                        String format = matcher.group(3);    // the format pattern
                        String newFormat = migrateFormatPattern(format);

                        if (!newFormat.equals(format)) {
                            modified = true;
                            matcher.appendReplacement(result, Matcher.quoteReplacement(prefix + quote + newFormat + quote));
                        } else {
                            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
                        }
                    }
                    matcher.appendTail(result);
                    content = result.toString();
                }

                if (modified) {
                    return text.withText(content);
                }
                return text;
            }
        };
    }

    /**
     * Migrates a SimpleDateFormat pattern to DateTimeFormatter-compatible syntax.
     *
     * @param pattern the original SimpleDateFormat pattern
     * @return the migrated DateTimeFormatter pattern
     */
    static String migrateFormatPattern(String pattern) {
        StringBuilder result = new StringBuilder();
        boolean inQuote = false;

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);

            if (c == '\'') {
                // Toggle quote mode - characters inside quotes are literal
                inQuote = !inQuote;
                result.append(c);
                continue;
            }

            if (inQuote) {
                // Inside quotes, preserve as-is
                result.append(c);
                continue;
            }

            if (c == 'u') {
                // SimpleDateFormat: day-number-of-week (1=Monday, 7=Sunday)
                // DateTimeFormatter: year (same as 'y' for positive years)
                // Convert to 'e' (localized day-of-week number)
                result.append('e');
            } else if (c == 'Y') {
                // Count consecutive Y's
                int count = 1;
                while (i + count < pattern.length() && pattern.charAt(i + count) == 'Y') {
                    count++;
                }

                // Check if this is part of a week-based date pattern (YYYY-'W'ww or similar)
                // by looking for nearby 'w' (week-of-week-based-year) pattern
                String remaining = pattern.substring(i);
                boolean isWeekBasedPattern = remaining.contains("w") || remaining.contains("W");

                if (isWeekBasedPattern) {
                    // Preserve week-based-year for actual week date patterns
                    for (int j = 0; j < count; j++) {
                        result.append('Y');
                    }
                } else {
                    // Convert to calendar year - user likely meant yyyy
                    for (int j = 0; j < count; j++) {
                        result.append('y');
                    }
                }
                i += count - 1; // Skip the Y's we just processed
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
