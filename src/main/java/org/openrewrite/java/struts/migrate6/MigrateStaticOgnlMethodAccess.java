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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.struts.internal.TagUtils;
import org.openrewrite.java.struts.table.StaticOgnlMethodAccess;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migrate OGNL static method access to action wrapper methods.
 * <p>
 * In Struts 6, static method access via OGNL (e.g., {@code @com.app.Util@makeCode()}) is disabled
 * by default for security reasons. This recipe:
 * <ol>
 *   <li>Scans struts.xml to map JSPs to Action classes</li>
 *   <li>Finds static method access patterns in JSP/XML files</li>
 *   <li>Adds wrapper methods to the corresponding Action classes</li>
 *   <li>Updates OGNL expressions to use the new wrapper properties</li>
 * </ol>
 *
 * @see <a href="https://cwiki.apache.org/confluence/display/WW/Struts+2.5+to+6.0.0+migration#Struts2.5to6.0.0migration-Staticmethodsaccess">Struts Migration Guide</a>
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateStaticOgnlMethodAccess extends ScanningRecipe<MigrateStaticOgnlMethodAccess.Accumulator> {

    transient StaticOgnlMethodAccess table = new StaticOgnlMethodAccess(this);

    // Pattern: @fully.qualified.ClassName@methodName(args)
    private static final Pattern STATIC_METHOD_PATTERN = Pattern.compile(
            "@([a-zA-Z_][a-zA-Z0-9_.]+)@([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)"
    );

    private static final XPathMatcher ACTION_MATCHER = new XPathMatcher("//action");
    private static final XPathMatcher RESULT_MATCHER = new XPathMatcher("//action/result");

    String displayName = "Migrate static OGNL method access to action wrapper methods";

    String description = "Migrates OGNL expressions using static method access (e.g., `@com.app.Util@makeCode()`) " +
            "to use action wrapper methods instead. Static method access is disabled by default in Struts 6 " +
            "for security reasons.";

    @Value
    public static class StaticMethodCall {
        String className;
        String methodName;
        String arguments;
        String fullExpression;

        public String getWrapperMethodName() {
            // Convert com.app.Util.makeCode to utilMakeCode
            String simpleName = className.contains(".") ?
                    className.substring(className.lastIndexOf('.') + 1) :
                    className;
            return StringUtils.uncapitalize(simpleName) + StringUtils.capitalize(methodName);
        }

        public String getWrapperPropertyName() {
            return getWrapperMethodName();
        }
    }

    @Value
    public static class Accumulator {
        // Map from JSP path (normalized) to Action class FQN
        Map<String, Set<String>> jspToActionClasses = new HashMap<>();

        // Map from Action class FQN to static methods it needs wrappers for
        Map<String, Set<StaticMethodCall>> actionToStaticMethods = new HashMap<>();

        // All static method calls found (for data table reporting)
        List<StaticMethodCall> allStaticMethodCalls = new ArrayList<>();

        // Source files containing static method calls (path -> calls)
        Map<String, Set<StaticMethodCall>> sourceFileToStaticMethods = new HashMap<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof Xml.Document) {
                    Xml.Document doc = (Xml.Document) tree;
                    String sourcePath = doc.getSourcePath().toString();

                    // Check if this is a struts.xml file
                    if (sourcePath.endsWith("struts.xml") || sourcePath.contains("struts")) {
                        new StrutsXmlScanner(acc).visit(doc, ctx);
                    }

                    // Check for static method access in any XML/JSP file
                    new StaticMethodScanner(acc, sourcePath).visit(doc, ctx);
                }
                return tree;
            }
        };
    }

    private static class StrutsXmlScanner extends XmlIsoVisitor<ExecutionContext> {
        private final Accumulator acc;
        private String currentActionClass;

        StrutsXmlScanner(Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);

            if (ACTION_MATCHER.matches(getCursor())) {
                currentActionClass = TagUtils.getAttribute(t, "class", "");
            }

            if (RESULT_MATCHER.matches(getCursor()) && currentActionClass != null && !currentActionClass.isEmpty()) {
                // Get the JSP path from the result content or attribute
                String jspPath = null;
                if (t.getContent() != null) {
                    for (Content content : t.getContent()) {
                        if (content instanceof Xml.CharData) {
                            jspPath = ((Xml.CharData) content).getText().trim();
                            break;
                        }
                    }
                }
                if (StringUtils.isNullOrEmpty( jspPath )) {
                    jspPath = TagUtils.getAttribute(t, "name", "");
                }

                if (StringUtils.isNotEmpty( jspPath )) {
                    // Normalize JSP path (remove leading slash, etc.)
                    String normalizedPath = normalizeJspPath(jspPath);
                    acc.getJspToActionClasses()
                            .computeIfAbsent(normalizedPath, k -> new HashSet<>())
                            .add(currentActionClass);
                }
            }

            return t;
        }

        private String normalizeJspPath(String path) {
            // Remove leading slash and normalize
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        }
    }

    private static class StaticMethodScanner extends XmlIsoVisitor<ExecutionContext> {
        private final Accumulator acc;
        private final String sourcePath;

        StaticMethodScanner(Accumulator acc, String sourcePath) {
            this.acc = acc;
            this.sourcePath = sourcePath;
        }

        @Override
        public Xml.Attribute visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
            Xml.Attribute a = super.visitAttribute(attribute, ctx);
            scanForStaticMethods(a.getValueAsString());
            return a;
        }

        @Override
        public Xml.CharData visitCharData(Xml.CharData charData, ExecutionContext ctx) {
            Xml.CharData c = super.visitCharData(charData, ctx);
            scanForStaticMethods(c.getText());
            return c;
        }

        private void scanForStaticMethods(String text) {
            if (text == null || !text.contains("@")) {
                return;
            }

            Matcher matcher = STATIC_METHOD_PATTERN.matcher(text);
            while (matcher.find()) {
                StaticMethodCall call = new StaticMethodCall(
                        matcher.group(1),  // className
                        matcher.group(2),  // methodName
                        matcher.group(3),  // arguments
                        matcher.group(0)   // full expression
                );

                acc.getAllStaticMethodCalls().add(call);
                acc.getSourceFileToStaticMethods()
                        .computeIfAbsent(sourcePath, k -> new HashSet<>())
                        .add(call);

                // Map to action classes if we know them
                String normalizedPath = sourcePath;
                if (normalizedPath.startsWith("/")) {
                    normalizedPath = normalizedPath.substring(1);
                }
                // Try various path normalizations
                for (String jspPath : acc.getJspToActionClasses().keySet()) {
                    if (normalizedPath.endsWith(jspPath) || jspPath.endsWith(normalizedPath) ||
                        normalizedPath.contains(jspPath) || jspPath.contains(normalizedPath)) {
                        for (String actionClass : acc.getJspToActionClasses().get(jspPath)) {
                            acc.getActionToStaticMethods()
                                    .computeIfAbsent(actionClass, k -> new HashSet<>())
                                    .add(call);
                        }
                    }
                }
            }
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (tree instanceof J.CompilationUnit) {
                    // Handle Java files - add wrapper methods to Action classes
                    return new ActionClassVisitor(acc).visit(tree, ctx);
                } else if (tree instanceof Xml.Document) {
                    // Handle XML/JSP files - update OGNL expressions
                    Xml.Document doc = (Xml.Document) tree;
                    String sourcePath = doc.getSourcePath().toString();

                    // Report findings to data table
                    Set<StaticMethodCall> calls = acc.getSourceFileToStaticMethods().get(sourcePath);
                    if (calls != null) {
                        for (StaticMethodCall call : calls) {
                            table.insertRow(ctx, new StaticOgnlMethodAccess.Row(
                                    sourcePath,
                                    call.getFullExpression(),
                                    call.getClassName(),
                                    call.getMethodName()
                            ));
                        }
                    }

                    return new OgnlExpressionUpdater(acc).visit(tree, ctx);
                }
                return tree;
            }
        };
    }

    private static class ActionClassVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final Accumulator acc;

        ActionClassVisitor(Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);

            if (cd.getType() == null) {
                return cd;
            }

            String fqn = cd.getType().getFullyQualifiedName();
            Set<StaticMethodCall> staticMethods = acc.getActionToStaticMethods().get(fqn);

            if (staticMethods == null || staticMethods.isEmpty()) {
                return cd;
            }

            // Add wrapper methods for each static method call
            for (StaticMethodCall call : staticMethods) {
                // Check if method already exists
                boolean methodExists = cd.getBody().getStatements().stream()
                        .filter(J.MethodDeclaration.class::isInstance)
                        .map(J.MethodDeclaration.class::cast)
                        .anyMatch(m -> {
                            String methodName = "get" + StringUtils.capitalize(call.getWrapperMethodName());
                            return m.getSimpleName().equals(methodName);
                        });

                if (!methodExists) {
                    String wrapperMethodName = "get" + StringUtils.capitalize(call.getWrapperMethodName());
                    String returnType = "Object"; // Safe default; could be improved with type inference

                    // Build the wrapper method
                    String methodCode = String.format(
                            "public %s %s() { return %s.%s(%s); }",
                            returnType,
                            wrapperMethodName,
                            call.getClassName(),
                            call.getMethodName(),
                            call.getArguments()
                    );

                    JavaTemplate template = JavaTemplate.builder(methodCode)
                            .contextSensitive()
                            .imports(call.getClassName())
                            .build();

                    cd = template.apply(
                            updateCursor(cd),
                            cd.getBody().getCoordinates().lastStatement()
                    );

                    // Add import for the static class
                    maybeAddImport(call.getClassName());
                }
            }

            return cd;
        }
    }

    private static class OgnlExpressionUpdater extends XmlIsoVisitor<ExecutionContext> {
        private final Accumulator acc;

        OgnlExpressionUpdater(Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public Xml.Attribute visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
            Xml.Attribute a = super.visitAttribute(attribute, ctx);

            String value = a.getValueAsString();
            if (value != null && value.contains("@")) {
                String newValue = replaceStaticMethods(value);
                if (!newValue.equals(value)) {
                    a = a.withValue(
                            a.getValue().withValue(newValue)
                    );
                    a = SearchResult.found(a);
                }
            }

            return a;
        }

        @Override
        public Xml.CharData visitCharData(Xml.CharData charData, ExecutionContext ctx) {
            Xml.CharData c = super.visitCharData(charData, ctx);

            String text = c.getText();
            if (text != null && text.contains("@")) {
                String newText = replaceStaticMethods(text);
                if (!newText.equals(text)) {
                    c = c.withText(newText);
                    c = SearchResult.found(c);
                }
            }

            return c;
        }

        private String replaceStaticMethods(String text) {
            Matcher matcher = STATIC_METHOD_PATTERN.matcher(text);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String className = matcher.group(1);
                String methodName = matcher.group(2);
                // arguments are in group 3 but we don't need them for the property name

                StaticMethodCall call = new StaticMethodCall(className, methodName, "", "");
                String replacement = call.getWrapperPropertyName();

                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);

            return sb.toString();
        }
    }
}
