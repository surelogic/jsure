/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/JUCLockUsageManager.java,v 1.9 2008/04/30 20:55:48 aarong Exp $*/
package com.surelogic.analysis.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.locks.locks.HeldLock;
import com.surelogic.analysis.locks.locks.HeldLockFactory;

import edu.cmu.cs.fluid.ir.IRNode;


/**
 * The purpose of this class is to cache basic information gleaned from a 
 * method declaration that is used to initialize the control flow anlayses
 * that track lock usage.  The analyses all need the same information, and it 
 * would be stupid to recreate the information multiple times.
 * 
 * <p>A {@link LockExpressions} object is created internally for each method that
 * is analyzed.  Method declaration and constructor declaration nodes are used
 * to look up the object and get the information it contains.
 */
final class JUCLockUsageManager {
  private final Map<IRNode, LockExpressions> lockExpressions = new HashMap<IRNode, LockExpressions>();
  private final LockUtils lockUtils;
  private final HeldLockFactory heldLockFactory;
  
  
  public JUCLockUsageManager(final LockUtils lu, final HeldLockFactory hlf) {
    lockUtils = lu;
    heldLockFactory = hlf;
  }
  
  private LockExpressions getLockExpressionsFor(final IRNode mdecl) {
    LockExpressions lockExprs = lockExpressions.get(mdecl);
    if (lockExprs == null) {
      lockExprs = new LockExpressions(mdecl, lockUtils, heldLockFactory);
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
   * Does a method/constructor use intrinsic locks
   */
  public boolean usesIntrinsicLocks(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).usesIntrinsicLocks();
  }
  
  /**
   * Get the map of lock expressions to JUC locks.
   */
  public Map<IRNode, Set<HeldLock>> getJUCLockExprsToLockSets(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getJUCLockExprsToLockSets();
  }

  /**
   * Get the map of synchronized blocks to intrinsic locks.
   */
  public Map<IRNode, Set<HeldLock>> getSyncBlocks(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getSyncBlocks();
  }
  
  /**
   * Get the JUC locks that appear in lock preconditions.
   */
  public Set<HeldLock> getJUCRequiredLocks(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getJUCRequiredLocks();
  }
  
  /**
   * Get the intrinsic locks that are held through out the lifetime of 
   * the flow unit.
   */
  public Set<HeldLock> getIntrinsicAssumedLocks(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getIntrinsicAssumedLocks();
  }
  
  /**
   * Get the JUC locks that apply because the constructor is single threaded.
   */
  public Set<HeldLock> getJUCSingleThreaded(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getJUCSingleThreaded();
  }
  
  /**
   * Get the JUC locks that apply during class initialization.
   */
  public Set<HeldLock> getJUCClassInit(final IRNode fu) {
    return getLockExpressionsFor(fu).getJUCClassInit();
  }
}
