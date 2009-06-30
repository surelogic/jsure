/*
 * Created on Sep 9, 2003
 *
 */
package edu.cmu.cs.fluid.java.bind;

import com.surelogic.ast.*;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Interface for the reference types from source code
 * @author chance
 * @see IJavaDeclaredType
 * @see IJavaTypeFormal
 */
public interface IJavaSourceRefType extends IJavaReferenceType, ISourceRefType {
  /**
   * @return the type formal declaration AST node.
   */
  public IRNode getDeclaration();
}
