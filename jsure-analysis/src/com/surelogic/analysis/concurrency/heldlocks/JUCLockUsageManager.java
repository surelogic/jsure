package com.surelogic.analysis.concurrency.heldlocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.assigned.DefiniteAssignment;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.heldlocks.locks.HeldLock;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;


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
  private final IBinder binder;
  private final BindingContextAnalysis bca;
  private final DefiniteAssignment da;
  
  
  public JUCLockUsageManager(
      final LockUtils lu, final IBinder b, final BindingContextAnalysis bca, final DefiniteAssignment da) {
    this.lockUtils = lu;
    this.binder = b;
    this.bca = bca;
    this.da = da;
  }
  
  public LockExpressions getLockExpressionsFor(final IRNode mdecl) {
    LockExpressions lockExprs = lockExpressions.get(mdecl);
    if (lockExprs == null) {
      lockExprs = new LockExpressions(mdecl, lockUtils, binder, bca, da);
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
   * Does the method explicitly acquire or release any JUC locks?
   */
  public boolean invokesJUCLockMethods(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).invokesJUCLockMethods();
  }
  
  /**
   * Does a method/constructor use intrinsic locks
   */
  public boolean usesIntrinsicLocks(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).usesIntrinsicLocks();
  }
  
  /**
   * Does the method have any sync blocks?
   */
  public boolean usesSynchronizedBlocks(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).usesSynchronizedBlocks();
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
   * Get the single threaded data block associated with the flow unit.
   */
  public LockExpressions.SingleThreadedData getSingleThreadedData(final IRNode cdecl) {
    return getLockExpressionsFor(cdecl).getSingleThreadedData();
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
