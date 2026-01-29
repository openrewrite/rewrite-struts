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
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateTilesListener extends Recipe {

    private static final String OLD_LISTENER = "org.apache.tiles.web.startup.TilesListener";
    private static final String NEW_LISTENER = "org.apache.struts2.tiles.StrutsTilesListener";

    String displayName = "Migrate TilesListener to StrutsTilesListener";

    String description = "Update `org.apache.tiles.web.startup.TilesListener` to `org.apache.struts2.tiles.StrutsTilesListener` in web.xml for Struts 6 compatibility.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);

                if ("listener-class".equals(t.getName())) {
                    List<? extends Content> content = t.getContent();
                    if (content != null && content.size() == 1 && content.get(0) instanceof Xml.CharData) {
                        Xml.CharData charData = (Xml.CharData) content.get(0);
                        if (OLD_LISTENER.equals(charData.getText().trim())) {
                            return t.withContent(singletonList(charData.withText(
                                    charData.getText().replace(OLD_LISTENER, NEW_LISTENER))));
                        }
                    }
                }

                return t;
            }
        };
    }
}
