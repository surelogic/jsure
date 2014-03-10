/*
 * Created on May 26, 2004
 *
 */
package edu.cmu.cs.fluid.java.bind;

import java.util.List;

import com.surelogic.ast.IDeclaredType;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Interface for a type that refers to a declared class or interface.
 * @author boyland chance
 */
public interface IJavaDeclaredType extends IJavaSourceRefType, IDeclaredType {
  /** Return the IRNode this type is referring to.
   * This may be a class or interface declaration or an
   * anonymous class declaration.
   */
  @Override
  IRNode getDeclaration();

  /** In Java 1.5 and later, return the type parameters
   * as an immutable list of
   * {@link edu.cmu.cs.fluid.java.bind.IJavaType} objects.
   */
  List<IJavaType> getTypeParameters();
  
  /** Return the superclass type of this type.
   * Unlike the version in {@link IJavaType}, the return type is known to
   * be a declared type.
   * @see edu.cmu.cs.fluid.java.bind.IJavaType#getSuperclass(edu.cmu.cs.fluid.java.bind.ITypeEnvironment)
   */
  @Override
  IJavaDeclaredType getSuperclass(ITypeEnvironment tEnv);
  
  @Override
  IJavaDeclaredType subst(IJavaTypeSubstitution subst);
  
  /** Return the depth of nesting for this type.
   * 0 if not nested.
   */
  int getNesting();
  
  /**
   * @return the outer class (if any)
   */
  IJavaDeclaredType getOuterType();
  
  /**
   * Returns whether this would be considered a raw type
   */
  boolean isRawType(ITypeEnvironment tEnv);
}
