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
import org.openrewrite.java.struts.internal.TagUtils;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.Tree.randomId;
import static org.openrewrite.marker.Markers.EMPTY;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateStrutsConfig extends Recipe {

    String displayName = "Migrate struts-config.xml to struts.xml";

    String description = "Transforms Struts 1.x struts-config.xml to Struts 2.x struts.xml format.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindTags("/struts-config").getVisitor(),
                new XmlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                        Xml.Document doc = super.visitDocument(document, ctx);

                        // Rename the file from struts-config.xml to struts.xml
                        Path sourcePath = doc.getSourcePath();
                        if ("struts-config.xml".equals(sourcePath.getFileName().toString())) {
                            Path newPath = sourcePath.getParent() != null ?
                                    sourcePath.getParent().resolve("struts.xml") :
                                    Paths.get("struts.xml");
                            doc = doc.withSourcePath(newPath);
                        }

                        return doc;
                    }

                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = super.visitTag(tag, ctx);

                        // Transform root element from struts-config to struts
                        if ("struts-config".equals(t.getName())) {
                            t = t.withName("struts");

                            // Wrap action-mappings content in a package element
                            if (t.getContent() != null) {
                                List<Content> newContent = new ArrayList<>();
                                List<Content> actions = new ArrayList<>();

                                for (Content content : t.getContent()) {
                                    if (content instanceof Xml.Tag) {
                                        Xml.Tag contentTag = (Xml.Tag) content;
                                        if ("action-mappings".equals(contentTag.getName())) {
                                            // Collect actions from action-mappings (already transformed by visitor)
                                            if (contentTag.getContent() != null) {
                                                actions.addAll(contentTag.getContent());
                                            }
                                        } else if (!"form-beans".equals(contentTag.getName()) &&
                                                !"global-forwards".equals(contentTag.getName()) &&
                                                !"message-resources".equals(contentTag.getName()) &&
                                                !"plug-in".equals(contentTag.getName()) &&
                                                !"controller".equals(contentTag.getName())) {
                                            // Keep other elements (but filter out Struts 1 specific ones)
                                            newContent.add(content);
                                        }
                                    } else {
                                        newContent.add(content);
                                    }
                                }

                                // Create package element wrapping actions
                                if (!actions.isEmpty()) {
                                    Xml.Tag packageTag = createPackageTag(actions);
                                    if (packageTag != null) {
                                        newContent.add(packageTag);
                                    }
                                }

                                t = t.withContent(newContent);
                            }
                        }

                        // Transform action element (path -> name, type -> class)
                        if ("action".equals(t.getName()) && t.getAttributes().stream()
                                .anyMatch(a -> "path".equals(a.getKeyAsString()))) {
                            t = transformAction(t);
                        }

                        // Transform forward element to result
                        if ("forward".equals(t.getName())) {
                            t = transformForward(t);
                        }

                        return t;
                    }

                    private Xml.Tag createPackageTag(List<Content> actions) {
                        List<Xml.Attribute> attrs = new ArrayList<>();
                        attrs.add(createAttribute("name", "default"));
                        attrs.add(createAttribute("extends", "struts-default"));

                        // Use the first action as a template if available
                        if (!actions.isEmpty() && actions.get(0) instanceof Xml.Tag) {
                            Xml.Tag template = (Xml.Tag) actions.get(0);
                            return template
                                    .withName("package")
                                    .withPrefix("\n    ")
                                    .withAttributes(attrs)
                                    .withContent(actions)
                                    .withClosing(template.getClosing() != null ?
                                            template.getClosing().withName("package").withPrefix("\n    ") : null);
                        }
                        return null;
                    }

                    private Xml.Tag transformAction(Xml.Tag action) {
                        // Transform action attributes:
                        // path="/login" -> name="login"
                        // type="com.example.LoginAction" -> class="com.example.LoginAction"
                        List<Xml.Attribute> newAttrs = new ArrayList<>();

                        String path = TagUtils.getAttribute(action, "path", "");
                        if (!path.isEmpty()) {
                            // Remove leading slash from path to get name
                            String name = path.startsWith("/") ? path.substring(1) : path;
                            newAttrs.add(createAttribute("name", name));
                        }

                        String type = TagUtils.getAttribute(action, "type", "");
                        if (!type.isEmpty()) {
                            newAttrs.add(createAttribute("class", type));
                        }

                        // Content is already transformed by visitor (forwards -> results)
                        Xml.Tag result = action.withAttributes(newAttrs);

                        // Update closing tag if present
                        if (result.getClosing() != null) {
                            result = result.withClosing(result.getClosing().withName("action"));
                        }

                        return result;
                    }

                    private Xml.Tag transformForward(Xml.Tag forward) {
                        // Transform forward to result:
                        // <forward name="success" path="/welcome.jsp"/> -> <result name="success">/welcome.jsp</result>
                        List<Xml.Attribute> attrs = new ArrayList<>();

                        String name = TagUtils.getAttribute(forward, "name", "");
                        if (!name.isEmpty()) {
                            attrs.add(createAttribute("name", name));
                        }

                        String path = TagUtils.getAttribute(forward, "path", "");

                        // Create result tag with path as content
                        List<Content> content = new ArrayList<>();
                        if (!path.isEmpty()) {
                            content.add(new Xml.CharData(randomId(), "", EMPTY, false, path, ""));
                        }

                        Xml.Tag result = forward
                                .withName("result")
                                .withAttributes(attrs);

                        if (!content.isEmpty()) {
                            result = result.withContent(content);
                        }

                        // Update closing tag if present
                        if (result.getClosing() != null) {
                            result = result.withClosing(result.getClosing().withName("result"));
                        }

                        return result;
                    }

                    private Xml.Attribute createAttribute(String name, String value) {
                        return new Xml.Attribute(
                                randomId(),
                                " ",
                                EMPTY,
                                new Xml.Ident(randomId(), "", EMPTY, name),
                                "",
                                new Xml.Attribute.Value(randomId(), "", EMPTY, Xml.Attribute.Value.Quote.Double, value)
                        );
                    }
                }
        );
    }
}
