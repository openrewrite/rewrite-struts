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
package org.openrewrite.java.struts.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class StaticOgnlMethodAccess extends DataTable<StaticOgnlMethodAccess.Row> {

    public StaticOgnlMethodAccess(Recipe recipe) {
        super(recipe,
                "Static OGNL method access",
                "Locations where OGNL expressions use static method access, which is disabled by default in Struts 6.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source file",
                description = "The source file containing the OGNL expression.")
        String sourceFile;

        @Column(displayName = "OGNL expression",
                description = "The full OGNL expression containing the static method access.")
        String expression;

        @Column(displayName = "Static class",
                description = "The fully qualified class name being accessed statically.")
        String staticClass;

        @Column(displayName = "Static method",
                description = "The static method being called.")
        String staticMethod;
    }
}
