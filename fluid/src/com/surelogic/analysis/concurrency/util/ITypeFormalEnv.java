package com.surelogic.analysis.concurrency.util;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public interface ITypeFormalEnv {
  /**
   * Test if the given type formal is annotated to be containable.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is containable or <code>null</code> if the type
   *         formal is not containable.
   */
  public PromiseDrop<? extends IAASTRootNode> isContainable(IJavaTypeFormal formal);

  /**
   * Test if the given type formal is annotated to be immutable.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is immutable or <code>null</code> if the type
   *         formal is not immutable.
   */
  public PromiseDrop<? extends IAASTRootNode> isImmutable(IJavaTypeFormal formal);

  /**
   * Test if the given type formal is annotated to be thread safe.  If the 
   * type formal is annotated immutable, then it is also considered to be
   * thread safe.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is thread safe or <code>null</code> if the type
   *         formal is not thread safe.
   */
  public PromiseDrop<? extends IAASTRootNode> isThreadSafe(IJavaTypeFormal formal);
}