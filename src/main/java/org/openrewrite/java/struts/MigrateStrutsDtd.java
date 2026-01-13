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
package org.openrewrite.java.struts;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.struts.search.FindStrutsXml;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateStrutsDtd extends Recipe {

    @Option(displayName = "Struts version",
            description = "The Struts version to migrate to.",
            example = "6.0",
            valid = { "2.3", "2.5", "6.0" })
    String strutsVersion;

    String displayName = "Migrate DTD to a specific Struts version";

    String description = "Update Struts DTD to reflect the specified version.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindStrutsXml(), new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.DocTypeDecl visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, ExecutionContext ctx) {
                return docTypeDecl.withInternalSubset(ListUtils.map(docTypeDecl.getInternalSubset(), (n, ref) -> {
                    if (n == 0 && !ref.getName().equals("\"-//Apache Software Foundation//DTD Struts Configuration " + strutsVersion + "//EN\"")) {
                        return ref.withName("\"-//Apache Software Foundation//DTD Struts Configuration " + strutsVersion + "//EN\"");
                    }
                    if (n == 1 && !ref.getName().equals("\"https://struts.apache.org/dtds/struts-" + strutsVersion + ".dtd\"")) {
                        return ref.withName("\"https://struts.apache.org/dtds/struts-" + strutsVersion + ".dtd\"");
                    }
                    return ref;
                }));
            }
        });
    }
}
