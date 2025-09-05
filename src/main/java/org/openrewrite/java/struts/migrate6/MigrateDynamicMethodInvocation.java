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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.struts.internal.TagUtils;
import org.openrewrite.java.struts.search.FindStrutsXml;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.xml.tree.Xml.Attribute.Value.Quote;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.marker.Markers.EMPTY;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateDynamicMethodInvocation extends Recipe {
    private static final XPathMatcher DMI_CONSTANT = new XPathMatcher("/struts/constant[@name='struts.enable.DynamicMethodInvocation']");

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
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = super.visitTag(tag, ctx);

                        if (DMI_CONSTANT.matches(getCursor())) {
                            String value = TagUtils.getAttribute(t, "value", "false");
                            if ("true".equals(value)) {
                                t = t.withAttributes(
                                        ListUtils.map(t.getAttributes(), it -> "value".equals(it.getKey().getName()) ? it.withValue(it.getValue().withValue("false")) : it)
                                );
                                doAfterVisit(new ActionMigrator());
                            }
                        }

                        return t;
                    }
                }
        );
    }

    private static class ActionMigrator extends XmlIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);

            if ("package".equals(t.getName())) {
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
                            if (!splitActions.isEmpty()) {
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
            String actionName = TagUtils.getAttribute(action, "name", "");
            String className = TagUtils.getAttribute(action, "class", "");

            List<Xml.Tag> actions = new ArrayList<>();
            if (action.getContent() != null) {
                for (Content content : action.getContent()) {
                    if (content instanceof Xml.Tag) {
                        Xml.Tag resultTag = (Xml.Tag) content;
                        if ("result".equals(resultTag.getName())) {
                            String resultName = TagUtils.getAttribute(resultTag, "name", "");

                            if (isDefaultResult(resultName)) {
                                actions.add(0, action.withContent(singletonList(resultTag)));
                            } else {
                                List<Xml.Attribute> newAttributes = new ArrayList<>();
                                newAttributes.add(createAttribute("name", actionName + StringUtils.capitalize(resultName)));
                                newAttributes.add(createAttribute("class", className));
                                newAttributes.add(createAttribute("method", resultName));

                                actions.add(action
                                        .withAttributes(newAttributes)
                                        .withContent(singletonList(resultTag)));
                            }
                        }
                    }
                }
            }

            return actions.isEmpty() ? singletonList(action) : actions;
        }

        private boolean isDefaultResult(String resultName) {
            return resultName.isEmpty() ||
                   "success".equals(resultName) ||
                   "error".equals(resultName) ||
                   "input".equals(resultName) ||
                   "login".equals(resultName) ||
                   "none".equals(resultName);
        }


        private Xml.Attribute createAttribute(String name, String newActionName) {
            return new Xml.Attribute(Tree.randomId(),
                    " ",
                    EMPTY,
                    new Xml.Ident(Tree.randomId(), "", EMPTY, name),
                    "",
                    new Xml.Attribute.Value(Tree.randomId(), "", EMPTY, Quote.Double, newActionName)
            );
        }
    }
}
