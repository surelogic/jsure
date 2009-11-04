package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Declares a named layer as well as the type set that types in the layer may
 * refer to. A layer is a global entity, and it may be referred to by other
 * annotations. Its name is qualified by the name of the package that it
 * annotates.
 * 
 * <p>To place more than one {@code Layer} annotation on a package, use the 
 * {@link Layers} annotation.  It is an error for a package to have both a 
 * {@code Layer} and a {@code Layers} annotation.
 * 
 * @see Layers
 * @see TypeSet
 */
@Documented
@Target(ElementType.PACKAGE)
public @interface Layer {
  /**
   * The name of the layer together with the may refer to type set. This set is
   * declared using a constructive syntax shared with the other annotations. The
   * attribute is restricted to strings that match the following grammar:
   * 
   * <p>
   * value = name "<tt>is</tt>" ["<tt>above</tt>" name *("<tt>,</tt> name) "<tt>;</tt>"]
   * "<tt>may</tt>" "<tt>refer</tt>" "<tt>to</tt>" no_layer_type_set_expr
   * 
   * <p>
   * no_layer_type_set_expr = no_layer_type_set_disjunct *("<tt>|</tt>" no_layer_type_set_disjunct)
   * <i>; Set union</i>
   * 
   * <p>
   * no_layer_type_set_disjunct = no_layer_type_set_conjunct *("<tt>&</tt>" no_layer_type_set_conjunct)
   * <i>; Set intersection</i>
   * 
   * <p>
   * no_layer_type_set_conjunct = ["<tt>!</tt>"] no_layer_type_set_leaf <i>; Set complement</i>
   * 
   * <p>
   * no_layer_type_set_leaf = dotted_name <i>; Package name, type name, or
   * no_layer_type set name</i> <br>
   * no_layer_type_set_leaf /= dotted_name "<tt>+</tt>" <i>; Package tree</i> <br>
   * no_layer_type_set_leaf /= dotted_name "<tt>.</tt>" "<tt>{</tt>" name *("<tt>,</tt>"
   * name) "<tt>}</tt>" <i>; Union of packages/types</i> <br>
   * no_layer_type_set_leaf /= "<tt>(</tt>" no_layer_type_set_expr "<tt>)</tt>"
   * 
   * 
   * <p>The "<tt>is above</tt>" clause names those layers are immediately below
   * this layer; that is, those layers that are allowed to be referenced by
   * types in this layer.
   * 
   *
   * <p>
   * The union, intersection, and complement operators, as well as the
   * parentheses have the obvious meanings, and standard precedence order. A
   * package name signifies all the types in that package; a named type
   * indicates a specific type. <em>A dotted name is not allowed to refer to 
   * layer.</em>  A named type set stands for the type set specified by the given
   * name, as defined by a {@code @TypeSet} annotation.  <em>The named type 
   * set is not allowed to refer to a layer, nor is any type set that it recursively
   * references.</em>  The package tree suffix "<tt>+</tt>"
   * indicates that all the types in the package and its subpackages are part of
   * the set. The braces "<tt>{</tt>" "<tt>}</tt>" are syntactic sugar
   * used to enumerate a union of packages/types that share the same prefix.
   */
  public String value();
  
  /**
   * When {@code true}, indicates that this annotation has priority over any
   * annotations that apply to the same node that originate from scoped promises.
   */
  boolean override() default true;
}
