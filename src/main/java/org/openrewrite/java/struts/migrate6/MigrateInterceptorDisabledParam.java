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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.struts.internal.TagUtils;
import org.openrewrite.java.struts.search.FindStrutsXml;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateInterceptorDisabledParam extends Recipe {

    String displayName = "Migrate validation.excludeMethods to validation.disabled";

    String description = "Rename the `validation.excludeMethods` interceptor param to `validation.disabled` for Struts 6 compatibility.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new FindStrutsXml(),
                new XmlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        Xml.Tag t = super.visitTag(tag, ctx);

                        if ("param".equals(t.getName()) &&
                            "validation.excludeMethods".equals(TagUtils.getAttribute(t, "name", ""))) {
                            return t.withAttributes(
                                    t.getAttributes().stream()
                                            .map(attr -> {
                                                if ("name".equals(attr.getKey().getName())) {
                                                    return attr.withValue(attr.getValue().withValue("validation.disabled"));
                                                }
                                                return attr;
                                            })
                                            .collect(Collectors.toList())
                            );
                        }

                        return t;
                    }
                }
        );
    }
}
