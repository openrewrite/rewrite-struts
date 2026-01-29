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
package org.openrewrite.java.struts.migrate2;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateActionClass extends Recipe {

    String displayName = "Migrate Struts 1 Action to Struts 2 ActionSupport";

    String description = "Migrates Struts 1.x Action classes to Struts 2.x ActionSupport, " +
            "transforming the execute method signature and return statements.";

    private static final String STRUTS1_ACTION = "org.apache.struts.action.Action";
    private static final String STRUTS2_ACTION_SUPPORT = "com.opensymphony.xwork2.ActionSupport";

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                // Change the base class type
                new ChangeType(STRUTS1_ACTION, STRUTS2_ACTION_SUPPORT, true),
                // Transform method signatures and return statements
                new MigrateExecuteMethod()
        );
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class MigrateExecuteMethod extends Recipe {
        String displayName = "Migrate Struts 1 execute method";
        String description = "Transforms execute method signature and return statements.";

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(
                    new UsesType<>(STRUTS2_ACTION_SUPPORT, false),
                    new JavaIsoVisitor<ExecutionContext>() {

                        @Override
                        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                            J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

                            // Check if this is the Struts 1 execute method (4 parameters)
                            if (isStruts1ExecuteMethod(md)) {
                                // Remove all parameters
                                md = md.withParameters(emptyList());

                                // Change return type to String
                                if (md.getReturnTypeExpression() != null) {
                                    md = md.withReturnTypeExpression(
                                            new J.Identifier(
                                                    md.getReturnTypeExpression().getId(),
                                                    md.getReturnTypeExpression().getPrefix(),
                                                    md.getReturnTypeExpression().getMarkers(),
                                                    emptyList(),
                                                    "String",
                                                    JavaType.Primitive.String,
                                                    null
                                            )
                                    );
                                }

                                // Remove unused imports
                                maybeRemoveImport("org.apache.struts.action.ActionMapping");
                                maybeRemoveImport("org.apache.struts.action.ActionForm");
                                maybeRemoveImport("org.apache.struts.action.ActionForward");
                                maybeRemoveImport("javax.servlet.http.HttpServletRequest");
                                maybeRemoveImport("javax.servlet.http.HttpServletResponse");
                            }

                            return md;
                        }

                        @Override
                        public J.Return visitReturn(J.Return return_, ExecutionContext ctx) {
                            J.Return r = super.visitReturn(return_, ctx);

                            // Transform mapping.findForward("x") to "x" or SUCCESS/ERROR constants
                            if (r.getExpression() instanceof J.MethodInvocation) {
                                J.MethodInvocation mi = (J.MethodInvocation) r.getExpression();
                                if ("findForward".equals(mi.getSimpleName()) && !mi.getArguments().isEmpty()) {
                                    J expression = mi.getArguments().get(0);
                                    if (expression instanceof J.Literal) {
                                        J.Literal literal = (J.Literal) expression;
                                        String value = (String) literal.getValue();

                                        // Use ActionSupport constants for common values
                                        String replacement = getConstantForValue(value);
                                        if (replacement != null) {
                                            return r.withExpression(
                                                    new J.Identifier(
                                                            r.getExpression().getId(),
                                                            r.getExpression().getPrefix(),
                                                            r.getExpression().getMarkers(),
                                                            emptyList(),
                                                            replacement,
                                                            JavaType.Primitive.String,
                                                            null
                                                    )
                                            );
                                        }
                                        // Keep as string literal for non-standard forward names
                                        return r.withExpression(literal.withPrefix(r.getExpression().getPrefix()));
                                    }
                                }
                            }

                            return r;
                        }

                        private String getConstantForValue(String value) {
                            switch (value) {
                                case "success":
                                    return "SUCCESS";
                                case "error":
                                    return "ERROR";
                                case "input":
                                    return "INPUT";
                                case "login":
                                    return "LOGIN";
                                case "none":
                                    return "NONE";
                                default:
                                    return null;
                            }
                        }

                        private boolean isStruts1ExecuteMethod(J.MethodDeclaration md) {
                            if (!"execute".equals(md.getSimpleName())) {
                                return false;
                            }
                            if (md.getParameters().size() != 4) {
                                return false;
                            }

                            // Check for Struts 1 execute signature:
                            // execute(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)
                            String[] expectedTypes = {
                                    "org.apache.struts.action.ActionMapping",
                                    "org.apache.struts.action.ActionForm",
                                    "javax.servlet.http.HttpServletRequest",
                                    "javax.servlet.http.HttpServletResponse"
                            };

                            for (int i = 0; i < md.getParameters().size(); i++) {
                                if (!(md.getParameters().get(i) instanceof J.VariableDeclarations)) {
                                    return false;
                                }
                                J.VariableDeclarations param = (J.VariableDeclarations) md.getParameters().get(i);
                                JavaType type = param.getType();
                                if (!TypeUtils.isOfClassType(type, expectedTypes[i])) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
            );
        }
    }
}
