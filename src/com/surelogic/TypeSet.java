package com.surelogic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Declares a named set of types to be used in {@link MayReferTo} and
 * {@link Layer} annotations.  A type set is a global entity, and may be
 * referred to by other annotations.  Its name is qualified by the name of the 
 * package that it annotates.
 * 
 * <p>To place more than one {@code TypeSet} annotation on a package, use the 
 * {@link TypeSets} annotation.  It is an error for a package to have both a 
 * {@code TypeSet} and a {@code TypeSets} annotation.
 * 
 * @see TypeSets
 */
@Documented
@Target(ElementType.PACKAGE)
public @interface TypeSet {
  /**
   * The named set of types. This set is declared using a constructive syntax
   * shared with the other annotations.  The attribute is
   * restricted to strings that match the following grammar:
   * 
   * <p>
   * value = name "<tt>=</tt>" type_set_expr
   * 
   * <p>type_set_expr = type_set_disjunct *("<tt>|</tt>" type_set_disjunct) <i>; Set
   * union</i>
   * 
   * <p>
   * type_set_disjunct = type_set_conjunct *("<tt>&</tt>" type_set_conjunct)
   * <i>; Set intersection</i>
   * 
   * <p>
   * type_set_conjunct = ["<tt>!</tt>"] type_set_leaf <i>; Set complement</i>
   * 
   * <p>
   * type_set_leaf = dotted_name <i>; Package name, layer name, type name, or type set name</i>
   * <br>
   * type_set_leaf /= dotted_name "<tt>+</tt>" <i>; Package tree</i> <br>
   * type_set_leaf /= dotted_name "<tt>.</tt>" "<tt>{</tt>" name *("<tt>,</tt>"
   * name) "<tt>}</tt>" <i>; Union of packages/types</i> <br>
   * type_set_leaf /= "<tt>(</tt>" type_set_expr "<tt>)</tt>"
   * 
   * <p>
   * The union, intersection, and complement operators, as well as the
   * parentheses have the obvious meanings, and standard precedence order. A
   * package name signifies all the types in that package; a named type
   * indicates a specific type. A named layer stands for all the types in the layer. A named type set stands for the type set
   * specified by the given name, as defined by a {@code @TypeSet} annotation. The package tree suffix "<tt>+</tt>"
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
