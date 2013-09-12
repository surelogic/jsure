package com.surelogic.analysis.concurrency.heldlocks.locks;

import java.util.Set;

import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

/**
 * Interface for locks that are needed by a program.  That is, analysis 
 * generates "needed locks" when analyzing field expressions, etc., and 
 * compares them against locks that are known to be held.   
 * The needed lock is identified by its lock declaration node. 
 * 
 * <p>A lock also includes a pointer back to the syntactic entity that
 * needs the lock.  This "source" node is either
 * <ul>
 *   <li>A FieldRef or ArrayRefExpression IRNode of the field access that
 *       requires the lock
 *   <li>A MethodCall, ConstructorCall, or NewExpression IRNode of the
 *       call that requires the lock
 * </ul>
 */
public interface NeededLock extends ILock {
  Object getUniqueIdentifier();
  
  public boolean isSatisfiedByLockSet(
      Set<HeldLock> lockSet, ThisExpressionBinder thisExprBinder, IBinder binder);
  
  /**
   * Is it possible that this lock has an alias in the calling context?
   * If <code>false</code>, then {@link #getAliasInCallingContext(com.surelogic.analysis.MethodCallUtils.EnclosingRefs, NeededLockFactory)}
   * will definitely return <code>null</code>.  If <code>true</code>,
   * then it is possible that {@code getAliasInCallingContext} may return
   * a non-<code>null</code> value.
   */
  public boolean mayHaveAliasInCallingContext();
  
  /**
   * Get the alias by which this lock is known in the calling context, if any.
   * This has to do with lock references from within the initialization blocks
   * of anonymous classes.  Such locks are assured when the associated
   * anonymous class expression is analyzed, and thus there is a calling context:
   * the method scope in which the anonymous class expression is contained.  
   * The qualified receiver expressions used in the anonymous class initializers
   * may be known to be aliased to particular expressions in the calling context
   * because of Java rules about how enclosing references are bound.  
   * 
   * @param enclosingRefs
   *          The enclosing references object as obtained from
   *          {@link MethodCallUtils#getEnclosingInstanceReferences(IBinder, ThisExpressionBinder, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode, edu.cmu.cs.fluid.ir.IRNode).
   * @param lockFactory
   *          The needed lock factory to use.
   * @return The alternative lock, or <code>null</code> if there is no
   *         alternative lock.
   */
  public NeededLock getAliasInCallingContext(
      MethodCallUtils.EnclosingRefs enclosingRefs, 
      NeededLockFactory lockFactory);
  
  public boolean isFieldExprOfThis(IBinder b, IRNode varDecl);
}
