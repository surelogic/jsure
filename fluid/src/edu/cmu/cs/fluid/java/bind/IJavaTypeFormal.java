/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/bind/IJavaTypeFormal.java,v 1.5 2007/07/10 22:16:30 aarong Exp $
 */
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.ast.ITypeFormal;

import edu.cmu.cs.fluid.ir.IRNode;


/**
 * The type of something determined by the type parameter of
 * an uninstantiated parameterized class. In other words, when type-checking
 * the body of a parameterized class, this kind of type is used by
 * the type parameter.  It is substituted when the class is parameterized.
 * @author boyland
 */
public interface IJavaTypeFormal extends IJavaSourceRefType, IJavaTypeVariable, ITypeFormal {
  /**
   * Get the IRNode associated with this type formal.
   * Don't use this method to get the bound.
   * @see getExtendsBounds
   * @return the type formal declaration AST node.
   */
  @Override
  public IRNode getDeclaration();
  
  /**
   * Get the bounds for this type variable as a single IJavaType.
   * Multiple bounds result in an intersection type.
   * @param te type environment (needed)
   * @return extends bounds.
   */
  public IJavaReferenceType getExtendsBound(ITypeEnvironment te);

  /**
   * Get the extends bound.
   * @deprecated
   * <em>Warning</em>: This code will crash.
   * Getting the bound requires a type environment.
   * Use {@link #getSuperclass(ITypeEnvironment)} instead.
   * @see com.surelogic.ast.ITypeFormal#getExtendsBound()
   */
  @Override
  @Deprecated
  public IJavaReferenceType getExtendsBound();
}
