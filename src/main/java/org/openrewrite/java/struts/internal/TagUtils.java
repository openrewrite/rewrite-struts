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
package org.openrewrite.java.struts.internal;

import org.openrewrite.xml.tree.Xml;

public class TagUtils {
    private TagUtils() {
    }

    public static String getAttribute(Xml.Tag tag, String name, String defaultValue) {
        return tag.getAttributes().stream()
                .filter(a -> a.getKey().getName().equals(name))
                .findFirst()
                .map(a -> a.getValue().getValue())
                .orElse(defaultValue);
    }
}
