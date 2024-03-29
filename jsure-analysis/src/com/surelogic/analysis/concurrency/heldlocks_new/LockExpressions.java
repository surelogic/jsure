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
import com.surelogic.analysis.concurrency.heldlocks_new.LockExpressionManager.LockExprInfo;
import com.surelogic.analysis.concurrency.heldlocks_new.LockExpressionManager.LockExprInfo.SyncedJUC;
import com.surelogic.analysis.concurrency.heldlocks_new.LockExpressionManager.SingleThreadedData;
import com.surelogic.analysis.concurrency.heldlocks_new.LockExpressionManager.LockExprInfo.Bogus;
import com.surelogic.analysis.concurrency.heldlocks_new.LockExpressionManager.LockExprInfo.Final;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLockFactory;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock.Reason;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.NoEffectEvidence;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.evidence.NoEvidence;
import com.surelogic.analysis.visitors.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.visitors.InstanceInitAction;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.ThreadEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.Pair;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.StartsPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
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
    private IRNode enclosingFlowUnit;
    private IRNode currentRcvr;
    private final LinkedList<IRNode> rcvrStack = new LinkedList<IRNode>();
    private final LinkedList<IRNode> enclosingStack = new LinkedList<IRNode>();
    
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
      rcvrStack.addFirst(currentRcvr);
      currentRcvr = JavaPromise.getReceiverNodeOrNull(decl);
      
      enclosingStack.addFirst(decl);
      enclosingFlowUnit = decl;
    }
    
    public void pop() {
      currentRcvr = rcvrStack.removeFirst();
      enclosingFlowUnit = enclosingStack.removeFirst();
    }    
  }


  
  /**
   * Map from final lock expressions used as the object expressions to lock() or
   * unlock() calls to the JUC locks they resolve to.
   */
  private final ImmutableMap<IRNode, LockExprInfo> jucLockExprsToLockSets;

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
  private final ImmutableMap<IRNode, LockExprInfo> syncBlocks;
  
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
   * The set of locks represented by the method begin synchronized.  These
   * are included in {@link #intrinsicAssumedLocks}, but separated here for 
   * ease of checking other properties in the lock analysis.
   */
  private final ImmutableSet<HeldLock> synchronizedMethodLocks;
 
  /**
   * Information for determining whether a constructor is proven single-threaded.
   * If the flow unit is not a constructor this is {@value null}.
   */
  private final SingleThreadedData singleThreadedData;
  
  /**
   * The lock, if any, that the method is declared to return.
   */
  private final HeldLock returnedLock;
  
  /**
   * The map from return statements to lock sets.  Only meaningful
   * if the {@link #returnedLock} is not null.
   */
  private final Map<IRNode, LockExprInfo> returnStatements;
  
  
  
  /**
   * <code>enclosingMethodDecl</code> is always a top-level method: a ConstructorDeclaration,
   * MethodDeclaration, or ClassInitDeclaration.  It is never an
   * AnonClassExpression or InitDeclaration.
   */
  private LockExpressions(
      final ImmutableSet<HeldLock> intrinsicAssumedLocks,
      final ImmutableSet<HeldLock> synchronizedMethodLocks,
      final ImmutableSet<HeldLock> jucClassInit,
      final ImmutableMap<IRNode, LockExprInfo> jucLockExprsToLockSet,
      final ImmutableSet<HeldLock> jucRequiredLocks,
      final ImmutableSet<HeldLock> jucSingleThreaded,
      final SingleThreadedData singleThreadedData,
      final ImmutableMap<IRNode, LockExprInfo> syncBlocks,
      final HeldLock returnedLock,
      final Map<IRNode, LockExprInfo> returnStatements) {
    this.intrinsicAssumedLocks = intrinsicAssumedLocks;
    this.synchronizedMethodLocks = synchronizedMethodLocks;
    this.jucClassInit = jucClassInit;
    this.jucLockExprsToLockSets = jucLockExprsToLockSet;
    this.jucRequiredLocks = jucRequiredLocks;
    this.jucSingleThreaded = jucSingleThreaded;
    this.singleThreadedData = singleThreadedData;
    this.syncBlocks = syncBlocks;
    this.returnedLock = returnedLock;
    this.returnStatements = returnStatements;
  }
  
  
  
  public static LockExpressions getLockExpressionsFor(
      final IRNode mdecl, final LockUtils lu, final IBinder b,
      final AtomicReference<AnalysisLockModel> analysisLockModel,
      final BindingContextAnalysis bca, final DefiniteAssignment da) {
    final LockExpressionVisitor visitor = 
        new LockExpressionVisitor(mdecl, analysisLockModel, lu, b, bca, da);
    visitor.doAccept(mdecl);
    return new LockExpressions(visitor.intrinsicAssumedLocks.build(),
        visitor.synchronizedMethodLocks,
        visitor.jucClassInit.build(), visitor.jucLockExprsToLockSets.build(),
        visitor.jucRequiredLocks.build(), visitor.jucSingleThreaded.build(),
        visitor.getSingleThreadedData(), visitor.syncBlocks.build(),
        visitor.returnedLock, visitor.returnStatements.build());
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
  public Map<IRNode, LockExprInfo> getJUCLockExprsToLockSets() {
    return jucLockExprsToLockSets;
  }
  
  public Map<IRNode, Set<HeldLock>> getFinalJUCLockExpr() {
    final ImmutableMap.Builder<IRNode, Set<HeldLock>> builder = ImmutableMap.builder();
    for (final Map.Entry<IRNode, LockExprInfo> entry : jucLockExprsToLockSets.entrySet()) {
      if (entry.getValue().isFinal()) {
        builder.put(entry.getKey(), entry.getValue().getLocks());
      }
    }
    return builder.build();
  }
  
  public LockExprInfo getSyncBlock(final IRNode syncBlock) {
    return syncBlocks.get(syncBlock);
  }
  
  public Map<IRNode, Set<HeldLock>> getFinalSyncBlocks() {
    final ImmutableMap.Builder<IRNode, Set<HeldLock>> builder = ImmutableMap.builder();
    for (final Map.Entry<IRNode, LockExprInfo> entry : syncBlocks.entrySet()) {
      if (entry.getValue().isFinal()) {
        builder.put(entry.getKey(), entry.getValue().getLocks());
      }
    }
    return builder.build();
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
  
  public Set<HeldLock> getSynchronizedMethodLocks() {
    return synchronizedMethodLocks;
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

  public HeldLock getReturnedLock() {
    return returnedLock;
  }
  
  public LockExprInfo getReturnedLocks(final IRNode rstmt) {
    return returnStatements.get(rstmt);
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
    private final ImmutableMap.Builder<IRNode, LockExprInfo> jucLockExprsToLockSets = ImmutableMap.builder();

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
    private final ImmutableMap.Builder<IRNode, LockExprInfo> syncBlocks = ImmutableMap.builder();
    
    /**
     * The set of intrinsic locks that are held throughout the scope of the
     * method.  These are the locks known to be held because of
     * lock preconditions, the method being synchronized, the constructor
     * being single-threaded, or the flow unit being the class initializer.
     * These locks do not need to be tracked by the flow analysis because
     * they cannot be released during the lifetime of the flow unit.
     */
    private final ImmutableSet.Builder<HeldLock> intrinsicAssumedLocks = ImmutableSet.builder();
   
    private ImmutableSet<HeldLock> synchronizedMethodLocks = ImmutableSet.of();
    
    /**
     * Information for determining whether a constructor is proven single-threaded.
     * If the flow unit is not a constructor this is {@value null}.
     */
    private SingleThreadedData singleThreadedData = null;
    
    private HeldLock returnedLock;
    private final ImmutableMap.Builder<IRNode, LockExprInfo> returnStatements = ImmutableMap.builder();

    
    
    public LockExpressionVisitor(final IRNode mdecl, 
        final AtomicReference<AnalysisLockModel> analysisLockModel, final LockUtils lu,
        final IBinder b, final BindingContextAnalysis bca, final DefiniteAssignment da) {
      super(VisitInsideTypes.NO, mdecl, SkipAnnotations.YES, CreateTransformer.NO);
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
    private SingleThreadedData isConstructorSingleThreaded(
        final IRNode cdecl, final IRNode rcvrDecl) {
      // get the receiver and see if it is declared to be borrowed
      final BorrowedPromiseDrop bDrop = UniquenessRules.getBorrowed(rcvrDecl);
      
      // See if the return value is declared to be unique
      final IRNode returnNode = JavaPromise.getReturnNodeOrNull(cdecl);
      final UniquePromiseDrop uDrop = UniquenessRules.getUnique(returnNode);

      /*
       * See if the declared *write* effects are < "writes this.Instance" (Can
       * read whatever it wants. Want to prevent the object from writing a
       * reference to itself into another object.
       */
      final Effect writesInstance = Effect.write(null,
          new InstanceTarget(
              rcvrDecl, RegionModel.getInstanceRegion(cdecl), NoEvidence.INSTANCE),
          NoEffectEvidence.INSTANCE);

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
      return new SingleThreadedData(cdecl, bDrop, uDrop, isEffectsWork, eDrop, teDrop);
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
      if (singleThreadedData.isSingleThreaded()) {
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
      
      final ImmutableSet.Builder<HeldLock> builder = ImmutableSet.builder();
      analysisLockModel.get().getHeldLocksFromSynchronizedMethod(mdecl, builder, heldLockFactory);
      synchronizedMethodLocks = builder.build();
      intrinsicAssumedLocks.addAll(synchronizedMethodLocks);
      
      returnedLock = analysisLockModel.get().getHeldLockFromReturnsLock(
          LockUtils.getReturnedLock(mdecl), mdecl, true, mdecl,
          Reason.BOGUS, null, heldLockFactory);
      
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
        final LockExprInfo locks = processLockExpression(
            false, lockExpr, lockExpr, Reason.JUC_LOCK_CALL, null);
        jucLockExprsToLockSets.put(lockExpr, locks);
      }
      doAcceptForChildren(mcall);
    }

    @Override
    public Void visitSynchronizedStatement(final IRNode syncBlock) {
      final IRNode lockExpr = SynchronizedStatement.getLock(syncBlock);
      final LockExprInfo locks = processLockExpression(
          true, lockExpr, syncBlock, Reason.SYNCHRONIZED_STATEMENT, syncBlock);
      syncBlocks.put(syncBlock, locks);
      doAcceptForChildren(syncBlock);
      return null;
    }
    
    @Override
    public Void visitReturnStatement(final IRNode rstmt) {
      if (returnedLock != null) {
        // Convert the return statement as a lock expression so it can be checked later
        final LockExprInfo retLocks = processLockExpressionAllowJUC(
            true, ReturnStatement.getValue(rstmt), rstmt, Reason.BOGUS, null);
        returnStatements.put(rstmt, retLocks);
      }
      doAcceptForChildren(rstmt);
      return null;
    }
    
    private LockExprInfo processLockExpressionAllowJUC(
        final boolean convertAsIntrinsic, final IRNode lockExpr,
        final IRNode src, final Reason reason, final IRNode syncBlock) {
      final boolean isFinal = lockUtils.isFinalExpression(
          lockExpr, thisExprBinder.enclosingFlowUnit, syncBlock,
          currentQuery().first(), currentQuery().second());
      // Get the locks for the lock expression
      final ImmutableSet.Builder<HeldLock> lockSet = ImmutableSet.builder();
      lockUtils.convertLockExpr(
          convertAsIntrinsic, lockExpr, heldLockFactory, src, reason,
          currentQuery().second(), enclosingMethodDecl, lockSet);
      final Set<HeldLock> result = lockSet.build();
      
      if (!isFinal) {
        return new LockExprInfo(Final.NO, Bogus.NO, SyncedJUC.NO, result);
      } else {
        if (result.isEmpty() && !convertAsIntrinsic) {
          return new LockExprInfo(Final.YES, Bogus.YES, SyncedJUC.NO,
              ImmutableSet.<HeldLock>of(heldLockFactory.createBogusLock(lockExpr)));
        } else {
          return new LockExprInfo(Final.YES, Bogus.NO, SyncedJUC.NO, result);
        }
      }
    }     
  
    private LockExprInfo processLockExpression(
        final boolean convertAsIntrinsic, final IRNode lockExpr,
        final IRNode src, final Reason reason, final IRNode syncBlock) {
      if (convertAsIntrinsic && lockUtils.isJavaUtilConcurrentLockObject(lockExpr)) {
        final boolean isFinal = lockUtils.isFinalExpression(
            lockExpr, thisExprBinder.enclosingFlowUnit, syncBlock,
            currentQuery().first(), currentQuery().second());
        return new LockExprInfo(isFinal ? Final.YES : Final.NO,
            Bogus.NO, SyncedJUC.YES, ImmutableSet.<HeldLock>of());
      } else { // !convertAsIntrinsic || !isJavaUtilConcurrentLockObject
        return processLockExpressionAllowJUC(
            convertAsIntrinsic, lockExpr, src, reason, syncBlock);
      }
    }    
  }
}
