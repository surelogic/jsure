/*
 * Created on May 26, 2004
 *
 */
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.ast.INestedType;

/**
 * Interface for a type fetched from an outer type.
 * @author boyland
 */
public interface IJavaNestedType extends IJavaDeclaredType, INestedType {
  /** Return the type this type is nested in.
   * This type, itself, may be a nested type.
   */
  @Override
  IJavaDeclaredType getOuterType();
}
