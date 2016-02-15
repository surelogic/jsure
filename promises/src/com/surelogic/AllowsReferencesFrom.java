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
 * Constrains the set of types that are allowed to reference the annotated
 * program element. The value is a {@link TypeSet} or <tt>"nothing"</tt>.
 * References from the same compilation unit are implicitly allowed despite the
 * passed type set. In fact, <tt>"nothing"</tt> restricts references to only
 * those within same compilation unit.
 * <p>
 * This annotation may target a type, a method, a constructor, or a field.
 * 
 * <h3>Semantics:</h3>
 * 
 * Places a structural constraint on the program's implementation. Only
 * explicitly listed types are allowed to reference the annotated program
 * element. References from within the same compilation unit, or <tt>.java</tt>
 * file, are implicitly allowed.
 * 
 * <h3>Examples:</h3>
 * 
 * The snippet below declares that a constructor may only be called from within
 * a particular type.
 * 
 * <pre>
 * package com.surelogic.smallworld.model;
 * 
 * public class Place {
 * 
 *   &#064;AllowsReferencesFrom(&quot;World&quot;)
 *   Place(World world, String name, String description) { ... }
 *   ...
 * }
 * </pre>
 * 
 * The constructor for the {@code Place} class shown above is allowed, by the
 * Java language, to be invoked from any type within the
 * {@code com.surelogic.smallworld.model} package. The
 * {@link AllowsReferencesFrom} annotation restricts this visibility further to
 * only the {@code World} class in the same package.
 * 
 * <p>
 * The annotation below declares that the publicly visible <tt>altitude</tt>
 * field is may only be accessed from the <tt>org.controls.Altimeter</tt> class.
 * 
 * <pre>
 * public class Aircraft {
 * 
 *   &#064;AllowsReferencesFrom(&quot;org.controls.Altimeter&quot;)
 *   public double altitude;
 *   ...
 * }
 * </pre>
 * 
 * <p>
 * It is rare that code would use <tt>"nothing"</tt> rather than a type set. It
 * is, however, useful in the case of a type that only contains a <tt>main</tt>
 * method. The example below makes sure that main is not invoked by another type
 * in the system.
 * 
 * <pre>
 * &#064;AllowsReferencesFrom(&quot;nothing&quot;)
 * class MainProgram {
 *   public static void main(String[] args) {
 *     Program.go();
 *   }
 * }
 * </pre>
 * 
 * @see MayReferTo
 * @see TypeSet
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD })
public @interface AllowsReferencesFrom {
  /**
   * The set of types that may refer to the type. This set is declared using a
   * constructive syntax shared with several other annotations. The attribute is
   * restricted to strings that match the following grammar:
   * 
   * <p>
   * value =<br>
   * &nbsp;"nothing" / <i>; referenced only in the same compilation unit</i><br>
   * &nbsp;type_set_expr
   * 
   * <p>
   * type_set_expr = type_set_disjunct *("<tt>|</tt>" type_set_disjunct) <i>;
   * Set union</i>
   * 
   * <p>
   * type_set_disjunct = type_set_conjunct *("<tt>&amp;</tt>" type_set_conjunct)
   * <i>; Set intersection</i>
   * 
   * <p>
   * type_set_conjunct = ["<tt>!</tt>"] type_set_leaf <i>; Set complement</i>
   * 
   * <p>
   * type_set_leaf = dotted_name <i>; Package name, layer name, type name, or
   * type set name</i> <br>
   * type_set_leaf /= dotted_name "<tt>+</tt>" <i>; Package tree</i> <br>
   * type_set_leaf /= dotted_name "<tt>.</tt>" "<tt>{</tt>" name *("<tt>,</tt>
   * " name) "<tt>}</tt>" <i>; Union of packages/types</i> <br>
   * type_set_leaf /= "<tt>(</tt>" type_set_expr "<tt>)</tt>"
   * 
   * <p>
   * The union, intersection, and complement operators, as well as the
   * parentheses have the obvious meanings, and standard precedence order. A
   * package name signifies all the types in that package; a named type
   * indicates a specific type. A named layer stands for all the types in the
   * layer. A named type set stands for the type set specified by the given
   * name, as defined by a {@code @TypeSet} annotation. The package tree suffix
   * "<tt>+</tt>" indicates that all the types in the package and its
   * subpackages are part of the set. The braces "<tt>{</tt>" "<tt>}</tt>" are
   * syntactic sugar used to enumerate a union of packages/types that share the
   * same prefix.
   * 
   * @return the set of types that may refer to the type.
   */
  public String value();
}
