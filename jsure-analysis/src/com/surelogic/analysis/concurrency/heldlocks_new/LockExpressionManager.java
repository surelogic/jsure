package com.surelogic.analysis.concurrency.heldlocks_new;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableSet;
import com.surelogic.analysis.assigned.DefiniteAssignment;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.StartsPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquePromiseDrop;
import com.surelogic.dropsea.irfree.DiffHeuristics;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;

public final class LockExpressionManager {
  public final static class LockExprInfo {
    private final boolean isFinal;
    private final boolean isBogus;
    private final Set<HeldLock> locks;
    
    public LockExprInfo(final boolean isFinal, final boolean isBogus, final Set<HeldLock> locks) {
      this.isFinal = isFinal;
      this.isBogus = isBogus;
      this.locks = locks;
    }
    
    public boolean isFinal() { return isFinal; }
    public boolean isBogus() { return isBogus; }
    public Set<HeldLock> getLocks() { return locks; }
    public Set<HeldLock> getRealLocks() {
      return isBogus ? ImmutableSet.<HeldLock>of() : locks;
    }
  }
  
  
  
  public final static class SingleThreadedData {
    private final IRNode cdecl;
    
    private final BorrowedPromiseDrop bDrop;
    private final UniquePromiseDrop uDrop;
    
    private final boolean isEffects;
    private final RegionEffectsPromiseDrop eDrop;
    private final StartsPromiseDrop teDrop;
    
    private final boolean isThreadConfined;
    
    public SingleThreadedData(
        final IRNode cdecl, final BorrowedPromiseDrop bDrop, final UniquePromiseDrop uDrop,
        final boolean isEffects, final RegionEffectsPromiseDrop eDrop, final StartsPromiseDrop teDrop) {
      this.cdecl = cdecl;
      this.bDrop = bDrop;
      this.uDrop = uDrop;
      this.isEffects = isEffects;
      this.eDrop = eDrop;
      this.teDrop = teDrop;
      
      this.isThreadConfined = bDrop != null || uDrop != null || isEffects;
    }
    
    public boolean isSingleThreaded() {
      return isThreadConfined;
    }
    
    public void addSingleThreadedEvidence(final ResultDrop result) {
      if (isThreadConfined) {
        final ResultFolderDrop f = ResultFolderDrop.newOrFolder(result.getNode());
        result.addTrusted(f);
        
        // Copy diff hint if any
        String diffHint = result.getDiffInfoOrNull(DiffHeuristics.ANALYSIS_DIFF_HINT);
        if (diffHint != null) {
          final IKeyValue diffInfo = KeyValueUtility.getStringInstance(DiffHeuristics.ANALYSIS_DIFF_HINT, diffHint);         
          f.addOrReplaceDiffInfo(diffInfo);
        }
        f.setMessagesByJudgement(
            Messages.CONSTRUCTOR_IS_THREADCONFINED,
            Messages.CONSTRUCTOR_IS_NOT_THREADCONFINED);
         if (uDrop != null) {
           final ResultDrop r = new ResultDrop(cdecl);
           r.setMessage(Messages.RECEIVER_IS_NOT_ALIASED);
           r.setConsistent();
           f.addTrusted(r);
           r.addTrusted(uDrop);
         }
         if (bDrop != null) {
           final ResultDrop r = new ResultDrop(cdecl);
           r.setMessage(Messages.RECEIVER_IS_NOT_ALIASED);
           r.setConsistent();
           f.addTrusted(r);
           r.addTrusted(bDrop);
         }
         if (isEffects) {
           final ResultDrop r = new ResultDrop(cdecl);
           r.setMessage(Messages.STARTS_NO_THREADS_ETC);
           r.setConsistent();
           r.addTrusted(eDrop);
           r.addTrusted(teDrop);
           f.addTrusted(r);
         }
      }
    }
  }
  
  
  
  private final Map<IRNode, LockExpressions> lockExpressions = new HashMap<IRNode, LockExpressions>();
  private final LockUtils lockUtils;
  private final IBinder binder;
  private final AtomicReference<AnalysisLockModel> analysisLockModel;
  private final BindingContextAnalysis bca;
  private final DefiniteAssignment da;

  public LockExpressionManager(final LockUtils lockUtils, final IBinder binder,
      final AtomicReference<AnalysisLockModel> analysisLockModel,
      final BindingContextAnalysis bca, final DefiniteAssignment da) {
    this.lockUtils = lockUtils;
    this.binder = binder;
    this.analysisLockModel = analysisLockModel;
    this.bca = bca;
    this.da = da;
  }
  
  // ======== Cache Management
  
  private LockExpressions getLockExpressionsFor(final IRNode mdecl) {
    LockExpressions lockExprs = lockExpressions.get(mdecl);
    if (lockExprs == null) {
      lockExprs = LockExpressions.getLockExpressionsFor(
          mdecl, lockUtils, binder, analysisLockModel, bca, da);
      lockExpressions.put(mdecl, lockExprs);
    }
    return lockExprs;
  }
  
  public void clear() {
    lockExpressions.clear();
  }

  

  // ======== Actual queries

  
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
  public Map<IRNode, LockExprInfo> getJUCLockExprsToLockSets(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getJUCLockExprsToLockSets();
  }
  
  public Map<IRNode, Set<HeldLock>> getFinalJUCLockExprs(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getFinalJUCLockExpr();
  }

  /**
   * Get the map of synchronized blocks to intrinsic locks.
   */
  public LockExprInfo getSyncBlock(final IRNode mdecl, final IRNode syncBlock) {
    return getLockExpressionsFor(mdecl).getSyncBlock(syncBlock);
  }
  
  public Map<IRNode, Set<HeldLock>> getFinalSyncBlocks(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getFinalSyncBlocks();
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
  
  public Set<HeldLock> getSynchronizedMethodLocks(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getSynchronizedMethodLocks();
  }

  /**
   * Get the single threaded data block associated with the flow unit.
   */
  public SingleThreadedData getSingleThreadedData(final IRNode cdecl) {
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
  
  /**
   * Get the lock returned, if any, by the method
   */
  public HeldLock getReturnedLock(final IRNode mdecl) {
    return getLockExpressionsFor(mdecl).getReturnedLock();
  }
  
  /**
   * Get map from return statements to returned locks.
   */
  public LockExprInfo getReturnedLocks(final IRNode mdecl, final IRNode rstmt) {
    return getLockExpressionsFor(mdecl).getReturnedLocks(rstmt);
  }
}
