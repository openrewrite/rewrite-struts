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
package org.openrewrite.java.struts.search;

import org.openrewrite.*;
import org.openrewrite.java.struts.internal.TagUtils;
import org.openrewrite.java.struts.table.StrutsActions;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.tree.Xml;

import static java.util.stream.Collectors.joining;

public class FindStrutsActions extends Recipe {
    private final transient StrutsActions actions = new StrutsActions(this);

    @Override
    public String getDisplayName() {
        return "Find Struts actions";
    }

    @Override
    public String getDescription() {
        return "Find actions and their associated definitions.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher actionMatcher = new XPathMatcher("//action");
        return Preconditions.check(new FindTags("/struts"), new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (actionMatcher.matches(getCursor())) {
                    String pkg = getCursor().getPathAsStream(Xml.Tag.class::isInstance)
                            .map(Xml.Tag.class::cast)
                            .filter(t -> "package".equals(t.getName()))
                            .map(t -> TagUtils.getAttribute(t, "name", ""))
                            .collect(joining("."));
                    actions.insertRow(ctx, new StrutsActions.Row(
                            getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString(),
                            pkg,
                            TagUtils.getAttribute(tag, "name", "unknown"),
                            TagUtils.getAttribute(tag, "class", "unknown"),
                            TagUtils.getAttribute(tag, "method", "unknown")));
                    return SearchResult.found(tag);
                }
                return super.visitTag(tag, ctx);
            }
        });
    }

}
