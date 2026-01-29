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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.Markers.EMPTY;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateWebXml extends Recipe {
    private static final String STRUTS1_ACTION_SERVLET = "org.apache.struts.action.ActionServlet";
    private static final String STRUTS2_FILTER = "org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter";

    String displayName = "Migrate web.xml from Struts 1 to Struts 2";

    String description = "Converts Struts 1 ActionServlet configuration to Struts 2 StrutsPrepareAndExecuteFilter.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindTags("/web-app"),
                new XmlIsoVisitor<ExecutionContext>() {
                    private String servletName = null;
                    private boolean foundStruts1Servlet = false;

                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = super.visitTag(tag, ctx);

                        // Detect Struts 1 servlet and get its name
                        if ("servlet".equals(t.getName())) {
                            String servletClass = findChildTagValue(t, "servlet-class");
                            if (STRUTS1_ACTION_SERVLET.equals(servletClass)) {
                                foundStruts1Servlet = true;
                                servletName = findChildTagValue(t, "servlet-name");
                                // Transform servlet to filter
                                return transformServletToFilter(t);
                            }
                        }

                        // Transform servlet-mapping to filter-mapping for Struts
                        if ("servlet-mapping".equals(t.getName()) && foundStruts1Servlet) {
                            String mappingServletName = findChildTagValue(t, "servlet-name");
                            if (servletName != null && servletName.equals(mappingServletName)) {
                                return transformServletMappingToFilterMapping(t);
                            }
                        }

                        return t;
                    }

                    private String findChildTagValue(Xml.Tag tag, String childName) {
                        if (tag.getContent() != null) {
                            for (Content content : tag.getContent()) {
                                if (content instanceof Xml.Tag) {
                                    Xml.Tag child = (Xml.Tag) content;
                                    if (childName.equals(child.getName())) {
                                        return child.getValue().orElse("");
                                    }
                                }
                            }
                        }
                        return null;
                    }

                    private Xml.Tag transformServletToFilter(Xml.Tag servlet) {
                        // Get filter name from servlet-name or default to struts2
                        String filterName = findChildTagValue(servlet, "servlet-name");
                        if (StringUtils.isNullOrEmpty(filterName)) {
                            filterName = "struts2";
                        }

                        List<Content> newContent = new ArrayList<>();
                        String prefix = "\n        ";

                        // Add filter-name
                        newContent.add(createSimpleTag("filter-name", filterName, prefix, servlet));

                        // Add filter-class
                        newContent.add(createSimpleTag("filter-class", STRUTS2_FILTER, prefix, servlet));

                        return servlet
                                .withName("filter")
                                .withContent(newContent)
                                .withClosing(servlet.getClosing() != null ?
                                        servlet.getClosing().withName("filter") : null);
                    }

                    private Xml.Tag transformServletMappingToFilterMapping(Xml.Tag servletMapping) {
                        // Get filter name
                        String filterName = findChildTagValue(servletMapping, "servlet-name");
                        if (StringUtils.isNullOrEmpty(filterName)) {
                            filterName = "struts2";
                        }

                        // Get url pattern - convert *.do to /*
                        String urlPattern = findChildTagValue(servletMapping, "url-pattern");
                        if (urlPattern == null || "*.do".equals(urlPattern)) {
                            urlPattern = "/*";
                        }

                        List<Content> newContent = new ArrayList<>();
                        String prefix = "\n        ";

                        // Add filter-name
                        newContent.add(createSimpleTag("filter-name", filterName, prefix, servletMapping));

                        // Add url-pattern
                        newContent.add(createSimpleTag("url-pattern", urlPattern, prefix, servletMapping));

                        return servletMapping
                                .withName("filter-mapping")
                                .withContent(newContent)
                                .withClosing(servletMapping.getClosing() != null ?
                                        servletMapping.getClosing().withName("filter-mapping") : null);
                    }

                    private Xml.Tag createSimpleTag(String name, String value, String prefix, Xml.Tag template) {
                        List<Content> content = new ArrayList<>();
                        content.add(new Xml.CharData(randomId(), "", EMPTY, false, value, ""));

                        // Find an existing child tag to use as a template
                        if (template.getContent() != null) {
                            for (Content c : template.getContent()) {
                                if (c instanceof Xml.Tag) {
                                    Xml.Tag childTemplate = (Xml.Tag) c;
                                    return childTemplate
                                            .withName(name)
                                            .withPrefix(prefix)
                                            .withAttributes(new ArrayList<>())
                                            .withContent(content)
                                            .withClosing(childTemplate.getClosing() != null ?
                                                    childTemplate.getClosing().withName(name).withPrefix("") : null);
                                }
                            }
                        }

                        // Fallback - shouldn't normally happen as web.xml always has child tags
                        return template
                                .withName(name)
                                .withPrefix(prefix)
                                .withAttributes(new ArrayList<>())
                                .withContent(content);
                    }
                }
        );
    }
}
