package com.surelogic.analysis.concurrency.heldlocks_new;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.assigned.DefiniteAssignment;
import com.surelogic.analysis.assigned.DefiniteAssignment.ProvablyUnassignedQuery;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.analysis.concurrency.model.HeldLock;
import com.surelogic.analysis.concurrency.model.HeldLockFactory;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.evidence.NoEvidence;
import com.surelogic.analysis.visitors.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.visitors.InstanceInitAction;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.ThreadEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.Pair;
import com.surelogic.dropsea.IKeyValue;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.StartsPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquePromiseDrop;
import com.surelogic.dropsea.irfree.DiffHeuristics;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * A record of the lock expressions used in a method/constructor.  Specifically,
 * keeps separate track of
 * <ul>
 * <li>All the syntactically unique final lock expressions that appear as the
 * object expression in a call to {@code lock()} or {@code unlock()}.
 * <li>All the syntactically unique final lock expressions that appear as the
 * argument to a {@code synchronized} block.
 * <li>The set of JUC locks that are declared as preconditions.
 * <li>The set of intrinsic locks that are declared as preconditions.
 * <li>The set of JUC locks that are held if the constructor is single threaded.
 * <li>The set of intrinsic locks that are held if the constructor is single threaded.
 * <li>The set of static JUC locks that are held because we are analyzing the
 * class initializer.
 * <li>The set of static intrinsic locks that are held because we are analyzing the
 * class initializer.
 * </ul> 
 */
final class LockExpressions {
  private final static class TEB extends AbstractThisExpressionBinder {
    /* non-private so that LockExpressionVisitor can access this field
     * instead of maintaining a separate copy of it. 
     */
    IRNode enclosingFlowUnit;
    private final LinkedList<IRNode> declStack = new LinkedList<IRNode>();
    private IRNode currentRcvr;
    private final LinkedList<IRNode> rcvrStack = new LinkedList<IRNode>();
    
    public TEB(final IBinder b, final IRNode mdecl) {
      super(b);
      enclosingFlowUnit = mdecl;
      currentRcvr = JavaPromise.getReceiverNodeOrNull(mdecl);
    }

    @Override
    protected IRNode bindReceiver(final IRNode node) {
      return currentRcvr;
    }

    @Override
    protected IRNode bindQualifiedReceiver(
        final IRNode outerType, final IRNode node) {
      return JavaPromise.getQualifiedReceiverNodeByName(enclosingFlowUnit, outerType);
    }
    
    public void newDeclaration(final IRNode decl) {
      declStack.addFirst(enclosingFlowUnit);
      rcvrStack.addFirst(currentRcvr);
      enclosingFlowUnit = decl;
      currentRcvr = JavaPromise.getReceiverNodeOrNull(decl);
    }
    
    public void pop() {
      enclosingFlowUnit = declStack.removeFirst();
      currentRcvr = rcvrStack.removeFirst();
    }    
  }

  
  
  public final static class SingleThreadedData {
    public final IRNode cdecl;
    
    public final boolean isBorrowedThis;
    public final BorrowedPromiseDrop bDrop;

    public final boolean isUniqueReturn;
    public final UniquePromiseDrop uDrop;
    
    public final boolean isEffects;
    public final RegionEffectsPromiseDrop eDrop;
    public final StartsPromiseDrop teDrop;
    
    public final boolean isSingleThreaded;
    
    public SingleThreadedData(final IRNode cdecl,
        final boolean isBorrowedThis, final BorrowedPromiseDrop bDrop,
        final boolean isUniqueReturn, final UniquePromiseDrop uDrop,
        final boolean isEffects,
        final RegionEffectsPromiseDrop eDrop, final StartsPromiseDrop teDrop) {
      this.cdecl = cdecl;
      this.isBorrowedThis = isBorrowedThis;
      this.bDrop = bDrop;
      this.isUniqueReturn = isUniqueReturn;
      this.uDrop = uDrop;
      this.isEffects = isEffects;
      this.eDrop = eDrop;
      this.teDrop = teDrop;
      this.isSingleThreaded = isUniqueReturn || isBorrowedThis || isEffects;
    }
    
    public void addSingleThreadedEvidence(final ResultDrop result) {
     final ResultFolderDrop f = ResultFolderDrop.newOrFolder(result.getNode());
     result.addTrusted(f);
     
     // Copy diff hint if any
     String diffHint = result.getDiffInfoOrNull(DiffHeuristics.ANALYSIS_DIFF_HINT);
     if (diffHint != null) {
    	 final IKeyValue diffInfo = KeyValueUtility.getStringInstance(DiffHeuristics.ANALYSIS_DIFF_HINT, diffHint);         
    	 f.addOrReplaceDiffInfo(diffInfo);
     }
     f.setMessagesByJudgement(Messages.CONSTRUCTOR_IS_THREADCONFINED,
         Messages.CONSTRUCTOR_IS_NOT_THREADCONFINED);
      if (isUniqueReturn) {
        final ResultDrop r = new ResultDrop(cdecl);
        r.setMessage(Messages.RECEIVER_IS_NOT_ALIASED);
        r.setConsistent();
        f.addTrusted(r);
        r.addTrusted(uDrop);
      }
      if (isBorrowedThis) {
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
  
  /**
   * Map from final lock expressions used as the object expressions to lock() or
   * unlock() calls to the JUC locks they resolve to.
   */
  private final ImmutableMap<IRNode, Set<HeldLock>> jucLockExprsToLockSets;

  /** Any JUC locks that are declared in a lock precondition */
  private final ImmutableSet<HeldLock> jucRequiredLocks;

  /**
   * The JUC locks that apply because we are analyzing a singled-threaded 
   * constructor.
   */
  private final ImmutableSet<HeldLock> jucSingleThreaded;

  /**
   * The JUC locks that apply because we are analyzing the class initializer.
   */
  private final ImmutableSet<HeldLock> jucClassInit;
 
  /**
   * Map from the synchronized blocks found in the flow unit to the 
   * set of locks acquired by each block.
   */
  private final ImmutableMap<IRNode, Set<HeldLock>> syncBlocks;
  
  /**
   * The set of intrinsic locks that are held throughout the scope of the
   * method.  These are the locks known to be held because of
   * lock preconditions, the method being synchronized, the constructor
   * being single-threaded, or the flow unit being the class initializer.
   * These locks do not need to be tracked by the flow analysis because
   * they cannot be released during the lifetime of the flow unit.
   */
  private final ImmutableSet<HeldLock> intrinsicAssumedLocks;
 
  /**
   * Information for determining whether a constructor is proven single-threaded.
   * If the flow unit is not a constructor this is {@value null}.
   */
  private final SingleThreadedData singleThreadedData;
  
  
  
  /**
   * <code>enclosingMethodDecl</code> is always a top-level method: a ConstructorDeclaration,
   * MethodDeclaration, or ClassInitDeclaration.  It is never an
   * AnonClassExpression or InitDeclaration.
   */
  private LockExpressions(
      final ImmutableSet<HeldLock> intrinsicAssumedLocks,
      final ImmutableSet<HeldLock> jucClassInit,
      final ImmutableMap<IRNode, Set<HeldLock>> jucLockExprsToLockSet,
      final ImmutableSet<HeldLock> jucRequiredLocks,
      final ImmutableSet<HeldLock> jucSingleThreaded,
      final SingleThreadedData singleThreadedData,
      final ImmutableMap<IRNode, Set<HeldLock>> syncBlocks) {
    this.intrinsicAssumedLocks = intrinsicAssumedLocks;
    this.jucClassInit = jucClassInit;
    this.jucLockExprsToLockSets = jucLockExprsToLockSet;
    this.jucRequiredLocks = jucRequiredLocks;
    this.jucSingleThreaded = jucSingleThreaded;
    this.singleThreadedData = singleThreadedData;
    this.syncBlocks = syncBlocks;
  }
  
  
  
  public static LockExpressions getLockExpressionsFor(
      final IRNode mdecl, final LockUtils lu, final IBinder b,
      final AtomicReference<AnalysisLockModel> analysisLockModel,
      final BindingContextAnalysis bca, final DefiniteAssignment da) {
    final LockExpressionVisitor visitor = 
        new LockExpressionVisitor(mdecl, analysisLockModel, lu, b, bca, da);
    visitor.doAccept(mdecl);
    return new LockExpressions(visitor.intrinsicAssumedLocks.build(),
        visitor.jucClassInit.build(), visitor.jucLockExprsToLockSets.build(),
        visitor.jucRequiredLocks.build(), visitor.jucSingleThreaded.build(),
        visitor.getSingleThreadedData(), visitor.syncBlocks.build());
  }
  
  
  
  
  /**
   * Does the method use any JUC locks?
   */
  public boolean usesJUCLocks() {
    return !jucLockExprsToLockSets.isEmpty()
        || !jucRequiredLocks.isEmpty()
        || !jucSingleThreaded.isEmpty()
        || !jucClassInit.isEmpty();
  }
  
  /**
   * Does the method explicitly acquire or release any JUC locks?
   */
  public boolean invokesJUCLockMethods() {
    return !jucLockExprsToLockSets.isEmpty();
  }
  
  /**
   * Does the method use any intrinsic locks?
   */
  public boolean usesIntrinsicLocks() {
    return !syncBlocks.isEmpty()
        || !intrinsicAssumedLocks.isEmpty();
  }
  
  /**
   * Does the method have any sync blocks?
   */
  public boolean usesSynchronizedBlocks() {
    return !syncBlocks.isEmpty();
  }
  
  /**
   * Get the map of lock expressions to JUC locks.
   */
  public Map<IRNode, Set<HeldLock>> getJUCLockExprsToLockSets() {
    return jucLockExprsToLockSets;
  }
  
  public Map<IRNode, Set<HeldLock>> getSyncBlocks() {
    return syncBlocks;
  }
  
  /**
   * Get the JUC locks that appear in lock preconditions.
   */
  public Set<HeldLock> getJUCRequiredLocks() {
    return jucRequiredLocks;
  }
  
  /**
   * Get the intrinsic locks that are held throughout the lifetime of the
   * flow unit.
   */
  public Set<HeldLock> getIntrinsicAssumedLocks() {
    return intrinsicAssumedLocks;
  }
  
  /**
   * Get the single threaded data block for the flow unit.
   */
  public SingleThreadedData getSingleThreadedData() {
    return singleThreadedData;
  }
  
  /**
   * Get the JUC locks that are held because of being a singleThreaded constructor
   */
  public Set<HeldLock> getJUCSingleThreaded() {
    return jucSingleThreaded;
  }
  
  /**
   * Get the JUC locks that are held because we are inside a class initializer
   */
  public Set<HeldLock> getJUCClassInit() {
    return jucClassInit;
  }

  
  
  /**
   * A tree visitor that we run over a method/constructor body to find all the
   * occurrences of syntactically unique final lock expressions. 
   */
  private static final class LockExpressionVisitor
  extends AbstractJavaAnalysisDriver<Pair<BindingContextAnalysis.Query, ProvablyUnassignedQuery>> {
    private final AtomicReference<AnalysisLockModel> analysisLockModel;
    private final IRNode enclosingMethodDecl;
    private final BindingContextAnalysis bca;
    private final DefiniteAssignment definiteAssignment;
    private final LockUtils lockUtils;
    private final HeldLockFactory heldLockFactory;
    private final TEB thisExprBinder;
    
    // ========== These get built as a side-effect of visitation ==========
    
    /**
     * Map from final lock expressions used as the object expressions to lock() or
     * unlock() calls to the JUC locks they resolve to.
     */
    private final ImmutableMap.Builder<IRNode, Set<HeldLock>> jucLockExprsToLockSets = ImmutableMap.builder();

    /** Any JUC locks that are declared in a lock precondition */
    private final ImmutableSet.Builder<HeldLock> jucRequiredLocks = ImmutableSet.builder(); 
        

    /**
     * The JUC locks that apply because we are analyzing a singled-threaded 
     * constructor.
     */
    private final ImmutableSet.Builder<HeldLock> jucSingleThreaded = ImmutableSet.builder();

    /**
     * The JUC locks that apply because we are analyzing the class initializer.
     */
    private final ImmutableSet.Builder<HeldLock> jucClassInit = ImmutableSet.builder();
   
    /**
     * Map from the synchronized blocks found in the flow unit to the 
     * set of locks acquired by each block.
     */
    private final ImmutableMap.Builder<IRNode, Set<HeldLock>> syncBlocks = ImmutableMap.builder();
    
    /**
     * The set of intrinsic locks that are held throughout the scope of the
     * method.  These are the locks known to be held because of
     * lock preconditions, the method being synchronized, the constructor
     * being single-threaded, or the flow unit being the class initializer.
     * These locks do not need to be tracked by the flow analysis because
     * they cannot be released during the lifetime of the flow unit.
     */
    private final ImmutableSet.Builder<HeldLock> intrinsicAssumedLocks = ImmutableSet.builder();
   
    /**
     * Information for determining whether a constructor is proven single-threaded.
     * If the flow unit is not a constructor this is {@value null}.
     */
    private SingleThreadedData singleThreadedData = null;
    

    
    public LockExpressionVisitor(final IRNode mdecl, 
        final AtomicReference<AnalysisLockModel> analysisLockModel, final LockUtils lu,
        final IBinder b, final BindingContextAnalysis bca, final DefiniteAssignment da) {
      super(false, mdecl, true);
      this.analysisLockModel = analysisLockModel;
      this.enclosingMethodDecl = mdecl;
      this.bca = bca;
      definiteAssignment = da;
      lockUtils = lu;
      thisExprBinder = new TEB(b, mdecl);
      heldLockFactory = new HeldLockFactory(thisExprBinder);
    }
    
    
    
    
    /**
     * Determine if a constructor can be considered to be single-threaded
     * 
     * @param cdecl
     *          The constructor declaration node of the constructor to be tested.
     * @param rcvrDecl
     *          The receiver declaration node associated with the constructor
     *          declaration.
     */
    /* Could move this to LockExpressions, but I like it better here because
     * LockUtils contains all the methods for complex operations involving lock
     * semantics.
     */
    private LockExpressions.SingleThreadedData isConstructorSingleThreaded(
        final IRNode cdecl, final IRNode rcvrDecl) {
      // get the receiver and see if it is declared to be borrowed
      final BorrowedPromiseDrop bDrop = UniquenessRules.getBorrowed(rcvrDecl);
      final boolean isBorrowedThis = bDrop != null;
      
      // See if the return value is declared to be unique
      final IRNode returnNode = JavaPromise.getReturnNodeOrNull(cdecl);
      final UniquePromiseDrop uDrop = UniquenessRules.getUnique(returnNode);
      final boolean isUniqueReturn = uDrop != null;

      /*
       * See if the declared *write* effects are < "writes this.Instance" (Can
       * read whatever it wants. Want to prevent the object from writing a
       * reference to itself into another object.
       */
      final Effect writesInstance = Effect.write(null,
          new InstanceTarget(
              rcvrDecl, RegionModel.getInstanceRegion(cdecl), NoEvidence.INSTANCE),
          Effect.NO_LOCKS);

      final RegionEffectsPromiseDrop eDrop = MethodEffectsRules.getRegionEffectsDrop(cdecl);
      final StartsPromiseDrop teDrop = ThreadEffectsRules.getStartsSpec(cdecl);
      boolean isEffectsWork = teDrop != null && eDrop != null;
      if (isEffectsWork) {
        final List<Effect> declFx = Effects.getDeclaredMethodEffects(cdecl, cdecl);
        if (declFx != null) {
          final Iterator<Effect> iter = declFx.iterator();
          while (isEffectsWork && iter.hasNext()) {
            final Effect effect = iter.next();
            if (effect.isWrite()) {
              isEffectsWork &= effect.isCheckedBy(thisExprBinder, writesInstance);
            }
          }
        }
      }
      return new SingleThreadedData(cdecl, isBorrowedThis, bDrop,
          isUniqueReturn, uDrop, isEffectsWork, eDrop, teDrop);
    }

    
    
    
    public SingleThreadedData getSingleThreadedData() {
      return singleThreadedData;
    }

    
    
    @Override
    protected Pair<BindingContextAnalysis.Query, ProvablyUnassignedQuery> createNewQuery(final IRNode decl) {
      // SHould only get here for the original method we are called with
      return new Pair<BindingContextAnalysis.Query, ProvablyUnassignedQuery>(
          bca.getExpressionObjectsQuery(decl),
          definiteAssignment.getProvablyUnassignedQuery(decl));
    }

    @Override
    protected Pair<BindingContextAnalysis.Query, ProvablyUnassignedQuery> createSubQuery(final IRNode caller) {
      final Pair<BindingContextAnalysis.Query, ProvablyUnassignedQuery> current = currentQuery();
      return new Pair<BindingContextAnalysis.Query, ProvablyUnassignedQuery>(
          current.first().getSubAnalysisQuery(caller),
          current.second().getSubAnalysisQuery(caller));
    }

    
    
    @Override
    protected InstanceInitAction getAnonClassInitAction(
        final IRNode anonClass, final IRNode classBody) {
      return new InstanceInitAction() {
        @Override
        public void tryBefore() {
          try {
            thisExprBinder.newDeclaration(JavaPromise.getInitMethodOrNull(anonClass));
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
        
        @Override
        public void finallyAfter() {
          thisExprBinder.pop();
        }
        
        @Override
        public void afterVisit() { // do nothing
        }
      };
    }
    
    @Override
    protected void handleConstructorDeclaration(final IRNode cdecl) {
      final RequiresLockPromiseDrop requiresLock = LockRules.getRequiresLock(cdecl);
      analysisLockModel.get().getHeldLocksFromRequiresLock(
          requiresLock, cdecl, intrinsicAssumedLocks, jucRequiredLocks, heldLockFactory);
      
      final IRNode rcvr = JavaPromise.getReceiverNodeOrNull(cdecl);
      singleThreadedData = isConstructorSingleThreaded(cdecl, rcvr);
      if (singleThreadedData.isSingleThreaded) {
        analysisLockModel.get().getHeldLocksFromSingleThreadedConstructor(cdecl, intrinsicAssumedLocks, jucSingleThreaded, heldLockFactory);
      }
      
      // Analyze the body of the constructor
      doAcceptForChildren(cdecl);
    }

    @Override
    protected void handleMethodDeclaration(final IRNode mdecl) {
      final RequiresLockPromiseDrop requiresLock = LockRules.getRequiresLock(mdecl);
      analysisLockModel.get().getHeldLocksFromRequiresLock(
          requiresLock, mdecl, intrinsicAssumedLocks, jucRequiredLocks, heldLockFactory);
      analysisLockModel.get().getHeldLocksFromSynchronizedMethod(mdecl, intrinsicAssumedLocks, heldLockFactory);
      
      doAcceptForChildren(mdecl);
    }
    
    @Override
    protected void handleClassInitDeclaration(
        final IRNode classBody, final IRNode classInitDecl) {
      // Get the locks held due to class initialization assumptions
      analysisLockModel.get().getHeldLocksFromClassInitialization(
          classInitDecl, intrinsicAssumedLocks, jucClassInit, heldLockFactory);
    }
    
    @Override
    public void handleMethodCall(final IRNode mcall) {
      if (lockUtils.isMethodFromJavaUtilConcurrentLocksLock(mcall)) {
        final MethodCall call = (MethodCall) JJNode.tree.getOperator(mcall);
        final IRNode lockExpr = call.get_Object(mcall);
        final Set<HeldLock> locks = processLockExpression(false, lockExpr, null);
        if (locks != null) jucLockExprsToLockSets.put(lockExpr, locks);
      }
      doAcceptForChildren(mcall);
    }

    @Override
    public Void visitSynchronizedStatement(final IRNode syncBlock) {
      final IRNode lockExpr = SynchronizedStatement.getLock(syncBlock);
      final Set<HeldLock> locks = processLockExpression(true, lockExpr, syncBlock);
      if (locks != null) syncBlocks.put(syncBlock, locks);
      doAcceptForChildren(syncBlock);
      return null;
    }
    
    private Set<HeldLock> processLockExpression(final boolean convertAsIntrinsic,
        final IRNode lockExpr, final IRNode syncBlock) {
      if (lockUtils.isFinalExpression(
          lockExpr, thisExprBinder.enclosingFlowUnit, syncBlock,
          currentQuery().first(), currentQuery().second())) {
        // Get the locks for the lock expression
        final ImmutableSet.Builder<HeldLock> lockSet = ImmutableSet.builder();
        lockUtils.convertLockExpr(
            convertAsIntrinsic, lockExpr, heldLockFactory, syncBlock,
            currentQuery().second(), enclosingMethodDecl, lockSet);
        final Set<HeldLock> result = lockSet.build();
        if (result.isEmpty() && !convertAsIntrinsic) {
          return ImmutableSet.<HeldLock>of(heldLockFactory.createBogusLock(lockExpr));
        }
        return result;
      }
      return null;
    }    
  }
}
