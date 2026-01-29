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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateValidatorDtd extends Recipe {

    String displayName = "Migrate xwork-validator DTD to 1.0.4";

    String description = "Update xwork-validator DTD from 1.0.3 to 1.0.4 for Struts 6 compatibility.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.DocTypeDecl visitDocTypeDecl(Xml.DocTypeDecl docTypeDecl, ExecutionContext ctx) {
                return docTypeDecl.withInternalSubset(ListUtils.map(docTypeDecl.getInternalSubset(), (n, ref) -> {
                    if (n == 0 && ref.getName().contains("XWork Validator 1.0.3")) {
                        return ref.withName(ref.getName().replace("1.0.3", "1.0.4"));
                    }
                    if (n == 1 && ref.getName().contains("xwork-validator-1.0.3.dtd")) {
                        return ref.withName(ref.getName().replace("1.0.3", "1.0.4"));
                    }
                    return ref;
                }));
            }
        };
    }
}
