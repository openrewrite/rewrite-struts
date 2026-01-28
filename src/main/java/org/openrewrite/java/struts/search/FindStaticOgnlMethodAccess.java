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
package org.openrewrite.java.struts.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.struts.table.StaticOgnlMethodAccess;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Find OGNL expressions that use static method access, which is disabled by default in Struts 6.
 * <p>
 * Static method access in OGNL uses the syntax: {@code @fully.qualified.ClassName@methodName(args)}
 * For example: {@code @com.app.Util@makeCode()} or {@code @java.lang.Math@max(1, 2)}
 * <p>
 * In Struts 6, this feature is disabled by default for security reasons. Users must either:
 * <ol>
 *   <li>Wrap static method calls in action instance methods</li>
 *   <li>Use static field access instead</li>
 *   <li>Re-enable static method access (not recommended)</li>
 * </ol>
 *
 * @see <a href="https://cwiki.apache.org/confluence/display/WW/Struts+2.5+to+6.0.0+migration#Struts2.5to6.0.0migration-Staticmethodsaccess">Struts Migration Guide - Static methods access</a>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class FindStaticOgnlMethodAccess extends Recipe {

    transient StaticOgnlMethodAccess table = new StaticOgnlMethodAccess(this);

    // Pattern to match OGNL static method access: @fully.qualified.ClassName@methodName(args)
    // Group 1: the full class name (e.g., com.app.Util)
    // Group 2: the method name (e.g., makeCode)
    private static final Pattern STATIC_METHOD_PATTERN = Pattern.compile(
            "@([a-zA-Z_][a-zA-Z0-9_.]+)@([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\("
    );

    String displayName = "Find static OGNL method access";

    String description = "Find OGNL expressions that use static method access (e.g., `@com.app.Util@makeCode()`), " +
            "which is disabled by default in Struts 6 for security reasons. " +
            "These expressions need to be migrated to use action instance methods instead.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Attribute visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                Xml.Attribute a = super.visitAttribute(attribute, ctx);

                String value = a.getValueAsString();
                if (value != null && value.contains("@") && value.contains("(")) {
                    Matcher matcher = STATIC_METHOD_PATTERN.matcher(value);
                    while (matcher.find()) {
                        String className = matcher.group(1);
                        String methodName = matcher.group(2);

                        table.insertRow(ctx, new StaticOgnlMethodAccess.Row(
                                getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString(),
                                value,
                                className,
                                methodName
                        ));

                        a = SearchResult.found(a);
                    }
                }

                return a;
            }

            @Override
            public Xml.CharData visitCharData(Xml.CharData charData, ExecutionContext ctx) {
                Xml.CharData c = super.visitCharData(charData, ctx);

                String text = c.getText();
                if (text != null && text.contains("@") && text.contains("(")) {
                    Matcher matcher = STATIC_METHOD_PATTERN.matcher(text);
                    while (matcher.find()) {
                        String className = matcher.group(1);
                        String methodName = matcher.group(2);

                        table.insertRow(ctx, new StaticOgnlMethodAccess.Row(
                                getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString(),
                                text.trim(),
                                className,
                                methodName
                        ));

                        c = SearchResult.found(c);
                    }
                }

                return c;
            }
        };
    }
}
