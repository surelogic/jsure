/*
 * Created on Sep 9, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.ast.IArrayType;

/**
 * The type of a one-dimensional array.
 * If the array is multi-dimensional, the element type will
 * also be an array type (with fewer dimensions).
 * @author chance
 */
public interface IJavaArrayType extends IJavaReferenceType, IArrayType {
  /** Return the element type, or array type with
   * one fewer dimension.
   */
  @Override
  IJavaType getElementType();

  /** Return the number of dimensions remaining. */
  @Override
  int getDimensions();

  /** Return the base type after removing all array types */
  @Override
  IJavaType getBaseType();
}
