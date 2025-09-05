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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.struts.internal.TagUtils;
import org.openrewrite.java.struts.search.FindStrutsXml;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is a placeholder implementation that marks actions needing DMI migration.
 * Full automatic migration of DMI to explicit actions requires complex XML manipulation
 * and would benefit from a more sophisticated implementation.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateDynamicMethodInvocation extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate Dynamic Method Invocation to explicit action mappings";
    }

    @Override
    public String getDescription() {
        return "Identifies Struts configurations using Dynamic Method Invocation (DMI) and marks them for migration, " +
               "as DMI is disabled by default in Struts 6 for security reasons.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindStrutsXml(),
                new XmlIsoVisitor<ExecutionContext>() {
                    private final XPathMatcher DMI_CONSTANT = new XPathMatcher("/struts/constant[@name='struts.enable.DynamicMethodInvocation']");

                    private boolean dmiEnabled = false;

                    @Override
                    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                        dmiEnabled = false;
                        return super.visitDocument(document, ctx);
                    }

                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = super.visitTag(tag, ctx);

                        // Check if DMI is enabled
                        if (DMI_CONSTANT.matches(getCursor())) {
                            String value = TagUtils.getAttribute(t, "value", "false");
                            if ("true".equals(value)) {
                                dmiEnabled = true;
                                // Update the DMI constant to false
                                t = t.withAttributes(
                                        ListUtils.map(t.getAttributes(), attr -> {
                                            if ("value".equals(attr.getKey().getName())) {
                                                return attr.withValue(
                                                        attr.getValue().withValue("false")
                                                );
                                            }
                                            return attr;
                                        })
                                );
                            }
                        }

                        // Handle package tags that contain actions needing migration
                        if (dmiEnabled && "package".equals(t.getName())) {
                            t = migratePackageActions(t);
                        }

                        return t;
                    }

                    private Xml.Tag migratePackageActions(Xml.Tag packageTag) {
                        if (packageTag.getContent() == null) {
                            return packageTag;
                        }

                        List<Content> newContent = new ArrayList<>();

                        for (Content content : packageTag.getContent()) {
                            if (content instanceof Xml.Tag) {
                                Xml.Tag tag = (Xml.Tag) content;
                                if ("action".equals(tag.getName())) {
                                    String method = TagUtils.getAttribute(tag, "method", "");
                                    if (method.isEmpty()) {
                                        List<Xml.Tag> splitActions = splitActionByResults(tag);
                                        if (splitActions.size() > 1) {
                                            // Add all split actions instead of the original
                                            newContent.addAll(splitActions);
                                            continue;
                                        }
                                    }
                                }
                            }
                            newContent.add(content);
                        }

                        return packageTag.withContent(newContent);
                    }

                    private List<Xml.Tag> splitActionByResults(Xml.Tag action) {
                        List<Xml.Tag> actions = new ArrayList<>();
                        String actionName = TagUtils.getAttribute(action, "name", "");
                        String className = TagUtils.getAttribute(action, "class", "");

                        if (action.getContent() == null) {
                            return Collections.singletonList(action);
                        }

                        List<Content> defaultResultContent = new ArrayList<>();

                        // Collect all results
                        for (Content content : action.getContent()) {
                            if (content instanceof Xml.Tag) {
                                Xml.Tag resultTag = (Xml.Tag) content;
                                if ("result".equals(resultTag.getName())) {
                                    String resultName = resultTag.getAttributes().stream()
                                            .filter(a -> a.getKey().getName().equals("name"))
                                            .findFirst()
                                            .map(a -> a.getValue().getValue())
                                            .orElse(null);

                                    // Handle default results - common Struts result names that should stay with original action
                                    if (isDefaultResult(resultName)) {
                                        defaultResultContent.add(resultTag);
                                    } else {
                                        // Create new action for named result
                                        String newActionName = actionName + capitalize(resultName);

                                        List<Xml.Attribute> newAttributes = new ArrayList<>();
                                        newAttributes.add(new Xml.Attribute(
                                                Tree.randomId(),
                                                " ",
                                                Markers.EMPTY,
                                                new Xml.Ident(Tree.randomId(), "", Markers.EMPTY, "name"),
                                                "",
                                                new Xml.Attribute.Value(Tree.randomId(), "", Markers.EMPTY, Xml.Attribute.Value.Quote.Double, newActionName)
                                        ));
                                        newAttributes.add(new Xml.Attribute(
                                                Tree.randomId(),
                                                " ",
                                                Markers.EMPTY,
                                                new Xml.Ident(Tree.randomId(), "", Markers.EMPTY, "class"),
                                                "",
                                                new Xml.Attribute.Value(Tree.randomId(), "", Markers.EMPTY, Xml.Attribute.Value.Quote.Double, className)
                                        ));
                                        newAttributes.add(new Xml.Attribute(
                                                Tree.randomId(),
                                                " ",
                                                Markers.EMPTY,
                                                new Xml.Ident(Tree.randomId(), "", Markers.EMPTY, "method"),
                                                "",
                                                new Xml.Attribute.Value(Tree.randomId(), "", Markers.EMPTY, Xml.Attribute.Value.Quote.Double, resultName)
                                        ));

                                        Xml.Tag newAction = action
                                                .withAttributes(newAttributes)
                                                .withContent(Collections.singletonList(resultTag));

                                        actions.add(newAction);
                                    }
                                }
                            }
                        }

                        if (!defaultResultContent.isEmpty()) {
                            actions.add(0, action.withContent(defaultResultContent));
                        }

                        return actions.isEmpty() ? Collections.singletonList(action) : actions;
                    }

                    private boolean isDefaultResult(String resultName) {
                        // Common Struts result names that should stay with the original action
                        return resultName == null || 
                               resultName.isEmpty() || 
                               "success".equals(resultName) ||
                               "error".equals(resultName) ||
                               "input".equals(resultName) ||
                               "login".equals(resultName) ||
                               "none".equals(resultName);
                    }

                    private String capitalize(String str) {
                        if (str == null || str.isEmpty()) {
                            return str;
                        }
                        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
                    }
                }
        );
    }
}
