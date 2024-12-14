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
package org.openrewrite.java.struts.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class StrutsActions extends DataTable<StrutsActions.Row> {

    public StrutsActions(Recipe recipe) {
        super(recipe,
                "Struts actions",
                "Definition of struts action.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Source file",
                description = "The source file that the action is defined in.")
        String sourceFile;

        @Column(displayName = "Package",
                description = "The package of the action.")
        String pkg;

        @Column(displayName = "Action name",
                description = "The name of the action.")
        String name;

        @Column(displayName = "Class",
                description = "The action class.")
        String className;

        @Column(displayName = "Method name",
                description = "The method name of the action method.")
        String methodName;
    }
}
