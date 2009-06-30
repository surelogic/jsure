/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/JUCLockUsageManager.java,v 1.9 2008/04/30 20:55:48 aarong Exp $*/
package com.surelogic.analysis.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.analysis.locks.locks.HeldLock;
import com.surelogic.analysis.locks.locks.HeldLockFactory;

import edu.cmu.cs.fluid.ir.IRNode;

final class JUCLockUsageManager {
  private final Map<IRNode, LockExpressions> lockExpressions = new HashMap<IRNode, LockExpressions>();
  private final LockUtils lockUtils;
  private final AtomicReference<GlobalLockModel> sysLockModelHandle;
  private final HeldLockFactory heldLockFactory;
  
  
  
  public JUCLockUsageManager(final AtomicReference<GlobalLockModel> glmRef, final LockUtils lu,
      final HeldLockFactory hlf) {
    sysLockModelHandle = glmRef;
    lockUtils = lu;
    heldLockFactory = hlf;
  }
  
  private LockExpressions getLockExpressionsFor(final IRNode mdecl) {
    LockExpressions lockExprs = lockExpressions.get(mdecl);
    if (lockExprs == null) {
      lockExprs = new LockExpressions(mdecl, sysLockModelHandle.get(), lockUtils, heldLockFactory);
      lockExpressions.put(mdecl, lockExprs);
    }
    return lockExprs;
  }
  
  public void clear() {
    lockExpressions.clear();
  }
  
  /**
   * Find out if a method/constructor uses JUC locks.
   * 
   * @param mdecl
   *          A MethodDeclaration or ConstructorDeclaration node.
   * @return {@code true} iff the method makes use of java.util.concurrent lock
   *         objects.
   */
  public boolean usesJUCLocks(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).usesJUCLocks();
  }
  
  /**
   * Get the map of lock expressions to locks.
   */
  public Map<IRNode, Set<HeldLock>> getLockExprsToLockSets(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getLockExprsToLockSets();
  }
  
  /**
   * Get the locks that appear in lock preconditions.
   */
  public Set<HeldLock> getRequiredLocks(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getRequiredLocks();
  }
  
  /**
   * Get the locks that apply because the constructor is single threaded.
   */
  public Set<HeldLock> getSingleThreaded(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getSingleThreaded();
  }
  
  /**
   * Get the locks that apply during class initialization.
   */
  public Set<HeldLock> getClassInit(final IRNode fu) {
    return getLockExpressionsFor(fu).getClassInit();
  }
}
