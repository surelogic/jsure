/*
 * Copyright (c) 2015 SureLogic, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link TypeSet} annotations. It is a
 * modeling error for an entity to have both a {@link TypeSets} and a
 * {@link TypeSet} annotation.
 * 
 * <h3>Semantics:</h3>
 * 
 * This annotation holds a list of {@link TypeSet} declarations without imposing
 * any further constraint on the program's implementation.
 * 
 * <h3>Examples:</h3>
 * 
 * Declaring two type sets on the same package. This annotation is only
 * permitted in a <code>package-info.java</code> file.
 * 
 * <pre>
 * &#064;TypeSets({ @TypeSet(&quot;UTIL = java.util &amp; !(java.util.{Enumeration, Hashtable, Vector})&quot;),
 *     &#064;TypeSet(&quot;XML = org.jdom+ | UTIL | java.{io, net}&quot;) })
 * package example;
 * 
 * import com.surelogic.*;
 * </pre>
 * 
 * @see TypeSet
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface TypeSets {
  /**
   * The {@link TypeSet} annotations to apply to the package.
   * 
   * @return the {@link TypeSet} annotations to apply to the package.
   */
  TypeSet[] value();
}
