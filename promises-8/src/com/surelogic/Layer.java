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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a named layer as well as the type set that types in the layer may
 * refer to. A layer is a global entity, and it may be referred to by other
 * annotations. Its name is qualified by the name of the package that it
 * annotates. If the reference is within the same package the name does not need
 * to be qualified.
 * <p>
 * To place more than one {@code Layer} annotation on a package, use the
 * {@link Layers} annotation. It is a modeling error for a package to have both
 * a {@link Layer} and a {@link Layers} annotation.
 * 
 * <h3>Semantics:</h3>
 * 
 * Places a structural constraint on the program's implementation. Types mapped
 * into the layer are allowed to reference only types within the type sets or
 * layers explicitly defined in the layer's <tt>may refer to</tt> clause.
 * References to any declaration within the same compilation unit, or
 * <tt>.java</tt> file, are implicitly allowed. References to any declaration
 * within the <tt>java.lang</tt> package are implicitly allowed.
 * 
 * <h3>Examples:</h3>
 * 
 * Declaring a layered model for an adventure-style game.
 * 
 * <pre>
 * &#064;TypeSets({ @TypeSet(&quot;UTIL=java.util &amp; !(java.util.{Enumeration, Hashtable, Vector})&quot;),
 *     &#064;TypeSet(&quot;XML=org.jdom | UTIL | java.{io, net}&quot;) })
 * &#064;Layers({ @Layer(&quot;MODEL may refer to UTIL&quot;), @Layer(&quot;PERSISTENCE may refer to MODEL | XML&quot;),
 *     &#064;Layer(&quot;CONTROLLER may refer to MODEL | PERSISTENCE | java.io.File&quot;) })
 * package com.surelogic.smallworld;
 * </pre>
 * 
 * The {@code UTIL} typeset allows the use of all the types in the
 * {@code java.util} package except for the {@code Enumeration},
 * {@code Hashtable}, and {@code Vector} classes. The {@code XML} typeset allows
 * references to any type in the {@code org.jdom} package, any type in the
 * {@code UTIL} typeset, and any type in the {@code java.io} and
 * {@code java.net} packages.
 * <p>
 * Types mapped into the {@code MODEL} layer with the {@link InLayer} annotation
 * may refer to any type in the {@code UTIL} typeset.
 * <p>
 * Types mapped into the {@code PERSISTENCE} layer with the {@link InLayer}
 * annotation may refer to any type mapped into the {@code MODEL} layer and any
 * type in the {@code XML} typeset.
 * <p>
 * Types mapped into the {@code CONTROLLER} layer with the {@link InLayer}
 * annotation may refer to any type mapped into the {@code MODEL} layer, the
 * {@code PERSISTENCE} layer, and the {@code java.io.File} type.
 * 
 * @see InLayer
 * @see Layers
 * @see TypeSet
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
@Repeatable(Layers.class)
public @interface Layer {
  /**
   * The name of the layer together with the may refer to type set. This set is
   * declared using a constructive syntax shared with the other annotations. The
   * attribute is restricted to strings that match the following grammar:
   * <p>
   * value = name "<tt>is</tt>" ["<tt>above</tt>" name *("<tt>,</tt> name) "
   * <tt>;</tt>"] "<tt>may</tt>" "<tt>refer</tt>" "<tt>to</tt>"
   * no_layer_type_set_expr
   * <p>
   * no_layer_type_set_expr = no_layer_type_set_disjunct *("<tt>|</tt>"
   * no_layer_type_set_disjunct) <i>; Set union</i>
   * <p>
   * no_layer_type_set_disjunct = no_layer_type_set_conjunct *("<tt>&amp;</tt>"
   * no_layer_type_set_conjunct) <i>; Set intersection</i>
   * <p>
   * no_layer_type_set_conjunct = ["<tt>!</tt>"] no_layer_type_set_leaf <i>; Set
   * complement</i>
   * <p>
   * no_layer_type_set_leaf = dotted_name <i>; Package name, type name, or
   * no_layer_type set name</i> <br>
   * no_layer_type_set_leaf /= dotted_name "<tt>+</tt>" <i>; Package tree</i> <br>
   * no_layer_type_set_leaf /= dotted_name "<tt>.</tt>" "<tt>{</tt>" name *("
   * <tt>,</tt>" name) "<tt>}</tt>" <i>; Union of packages/types</i> <br>
   * no_layer_type_set_leaf /= "<tt>(</tt>" no_layer_type_set_expr "<tt>)</tt>"
   * <p>
   * The "<tt>is above</tt>" clause names those layers are immediately below
   * this layer; that is, those layers that are allowed to be referenced by
   * types in this layer.
   * <p>
   * The union, intersection, and complement operators, as well as the
   * parentheses have the obvious meanings, and standard precedence order. A
   * package name signifies all the types in that package; a named type
   * indicates a specific type. <em>A dotted name is not allowed to refer to 
   * layer.</em> A named type set stands for the type set specified by the given
   * name, as defined by a {@code @TypeSet} annotation. <em>The named type 
   * set is not allowed to refer to a layer, nor is any type set that it recursively
   * references.</em> The package tree suffix "<tt>+</tt>" indicates that all
   * the types in the package and its subpackages are part of the set. The
   * braces "<tt>{</tt>" "<tt>}</tt>" are syntactic sugar used to enumerate a
   * union of packages/types that share the same prefix.
   * 
   * @return a value following the syntax described above.
   */
  public String value();
}
