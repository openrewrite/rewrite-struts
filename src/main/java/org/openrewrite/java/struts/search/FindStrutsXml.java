/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.struts.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.search.FindTags;

public class FindStrutsXml extends Recipe {
    @Override
    public String getDisplayName() {
        return "Find struts XML files";
    }

    @Override
    public String getDescription() {
        return "Struts XML files may have any name, and may be outside a resources directory, so " +
               "the true test is to look at the content of the file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new FindTags("/struts").getVisitor();
    }
}
