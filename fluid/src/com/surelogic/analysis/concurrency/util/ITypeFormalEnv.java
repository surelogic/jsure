package com.surelogic.analysis.concurrency.util;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public interface ITypeFormalEnv {
  /**
   * Test if the given type formal is annotated to be containable.
   * 
   * @param formal
   *          The type formal to test
   * @param exclusive
   *          <code>true</code> if the type formal must not be allowed to be
   *          annotated with anything else.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is containable or <code>null</code> if the type
   *         formal is not containable.
   */
  public PromiseDrop<? extends IAASTRootNode> isContainable(
      IJavaTypeFormal formal, boolean exclusive);

  /**
   * Test if the given type formal is annotated to be immutable.
   * 
   * @param formal
   *          The type formal to test
   * @param exclusive
   *          <code>true</code> if the type formal must not be allowed to be
   *          annotated with anything else.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is immutable or <code>null</code> if the type
   *         formal is not immutable.
   */
  public PromiseDrop<? extends IAASTRootNode> isImmutable(
      IJavaTypeFormal formal, boolean exclusive);

  /**
   * Test if the given type formal is annotated to be reference object.
   * 
   * @param formal
   *          The type formal to test
   * @param exclusive
   *          <code>true</code> if the type formal must not be allowed to be
   *          annotated with anything else.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is reference object or <code>null</code> if the type
   *         formal is not reference object.
   */
  public PromiseDrop<? extends IAASTRootNode> isReferenceObject(
      IJavaTypeFormal formal, boolean exclusive);

  /**
   * Test if the given type formal is annotated to be thread safe.  If the 
   * type formal is annotated immutable, then it is also considered to be
   * thread safe.
   * 
   * @param formal
   *          The type formal to test
   * @param exclusive
   *          <code>true</code> if the type formal must not be allowed to be
   *          annotated with anything else.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is thread safe or <code>null</code> if the type
   *         formal is not thread safe.
   */
  public PromiseDrop<? extends IAASTRootNode> isThreadSafe(
      IJavaTypeFormal formal, boolean exclusive);

  /**
   * Test if the given type formal is annotated to be value object.
   * 
   * @param formal
   *          The type formal to test
   * @param exclusive
   *          <code>true</code> if the type formal must not be allowed to be
   *          annotated with anything else.
   * 
   * @return The promise drop of the annotation that allows us to conclude that
   *         this type formal is value object or <code>null</code> if the type
   *         formal is not value object.
   */
  public PromiseDrop<? extends IAASTRootNode> isValueObject(
      IJavaTypeFormal formal, boolean exclusive);
}