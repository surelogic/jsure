package com.surelogic.analysis.concurrency.driver;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.assigned.DefiniteAssignment;
import com.surelogic.analysis.assigned.DefiniteAssignment.ProvablyUnassignedQuery;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.heldlocks_new.IntrinsicLockAnalysis;
import com.surelogic.analysis.concurrency.heldlocks_new.LockExpressionManager;
import com.surelogic.analysis.concurrency.heldlocks_new.LockUtils;
import com.surelogic.analysis.concurrency.heldlocks_new.MustHoldAnalysis;
import com.surelogic.analysis.concurrency.heldlocks_new.MustHoldAnalysis.HeldLocks;
import com.surelogic.analysis.concurrency.heldlocks_new.MustReleaseAnalysis;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.visitors.FlowUnitVisitor;
import com.surelogic.analysis.visitors.InstanceInitAction;
import com.surelogic.dropsea.ir.HintDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.JavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;

final class NewLockVisitor
extends FlowUnitVisitor<NewLockVisitor.Queries>
implements IBinderClient {
  public static final int DSC_EFFECTS = 550;
  public static final int EFFECT = 550;

  /**
   * The receiver declaration of the current instance method or constructor
   * being visited. If the current method is static or we are in the static
   * initializer, the then this is <code>null</code>.
   * 
   * <p>This is updated during the visitation and accessed by the 
   * ThisExpressionBinder instance.
   */
  private IRNode receiverDecl = null;
  
  private final ThisExpressionBinder thisExprBinder;
  private final Effects effects;
  
  private final LockUtils lockUtils;
  private final LockExpressionManager lockExprManager;
  
  /* Analyses for creating queries */
  private final BindingContextAnalysis bca;
  private final SimpleNonnullAnalysis simpleNonNull; // no queries needed for this one
  private final DefiniteAssignment definiteAssignment;
  private final IntrinsicLockAnalysis intrinsicLocks;
  private final MustHoldAnalysis mustHold;
  private final MustReleaseAnalysis mustRelease;
  
  
  
  public NewLockVisitor(
      final IBinder binder, final BindingContextAnalysis bca,
      final AtomicReference<AnalysisLockModel> analysisLockModel) {
    // Don't go inside nested types; skip annotation types
    super(true);
    
    this.thisExprBinder = new ThisExpressionBinder(binder);
    this.effects = new Effects(binder, analysisLockModel);
    
    this.bca = bca;
    this.simpleNonNull = new SimpleNonnullAnalysis(binder);
    this.definiteAssignment = new DefiniteAssignment(binder);
    
    final IMayAlias mayAlias = new TypeBasedMayAlias(binder);
    this.lockUtils = new LockUtils(analysisLockModel, thisExprBinder, effects, mayAlias);
    this.lockExprManager = new LockExpressionManager(lockUtils, binder, analysisLockModel, bca, definiteAssignment);
    this.intrinsicLocks = new IntrinsicLockAnalysis(binder, lockUtils, lockExprManager, simpleNonNull);
    this.mustHold = new MustHoldAnalysis(thisExprBinder, lockUtils, lockExprManager, simpleNonNull);
    this.mustRelease = new MustReleaseAnalysis(thisExprBinder, lockUtils, lockExprManager, simpleNonNull);
  }

  
  
  // ======================================================================
  // == Manage the query stack
  // ======================================================================
  
  final class Queries {
    private final BindingContextAnalysis.Query exprObjects;
    private final ProvablyUnassignedQuery provablyUnassigned;
    private final IntrinsicLockAnalysis.Query heldIntrinsicLocks;
    private final JavaFlowAnalysisQuery<HeldLocks> heldJUCLocks;
    private final MustHoldAnalysis.LocksForQuery lockCalls;
    private final MustReleaseAnalysis.Query unlockCalls;

    public Queries(final IRNode decl) {
      exprObjects = bca.getExpressionObjectsQuery(decl);
      provablyUnassigned = definiteAssignment.getProvablyUnassignedQuery(decl);
      heldIntrinsicLocks = intrinsicLocks.getHeldLocksQuery(decl);
      heldJUCLocks = mustHold.getHeldLocksQuery(decl);
      lockCalls = mustHold.getLocksForQuery(decl);
      unlockCalls = mustRelease.getUnlocksForQuery(decl);
    }
    
    private Queries(final Queries q, final IRNode caller) {
      exprObjects = q.exprObjects.getSubAnalysisQuery(caller);
      provablyUnassigned = q.provablyUnassigned.getSubAnalysisQuery(caller);
      heldIntrinsicLocks = q.heldIntrinsicLocks.getSubAnalysisQuery(caller);
      heldJUCLocks = q.heldJUCLocks.getSubAnalysisQuery(caller);
      lockCalls = q.lockCalls.getSubAnalysisQuery(caller);
      unlockCalls = q.unlockCalls.getSubAnalysisQuery(caller);
    }
    
    public Queries subQuery(final IRNode caller) {
      return new Queries(this, caller);
    }
  }

  // ======================================================================
  // == From IBinderClient
  // ======================================================================

  @Override
  public IBinder getBinder() {
    return thisExprBinder;
  }

  @Override
  public void clearCaches() {
    // Do nothing
  }
  
  

  // ======================================================================
  // == Query Management
  // ======================================================================

  @Override
  protected Queries createNewQuery(final IRNode decl) {
    return new Queries(decl);
  }
  
  @Override
  protected Queries createSubQuery(final IRNode caller) {
    return currentQuery().subQuery(caller);
  }
  
  
  
  // ======================================================================
  // == Wrap the receiverDecl with a this expression binder
  // ======================================================================
  
  private final class ThisExpressionBinder extends AbstractThisExpressionBinder {
    public ThisExpressionBinder(final IBinder b) {
      super(b);
    }

    @Override
    protected IRNode bindReceiver(IRNode node) {
      return receiverDecl;
    }
    
    @Override
    protected IRNode bindQualifiedReceiver(IRNode outerType, IRNode node) {
      return JavaPromise.getQualifiedReceiverNodeByName(getEnclosingDecl(), outerType);
    }    
  }

  
  
  // ======================================================================
  // == Visit
  // ======================================================================
  
  private void reportEffects(final IRNode mdecl) {
    final Set<Effect> fx = effects.getImplementationEffects(mdecl, bca);
    for (final Effect e : fx) {
      final HintDrop drop = HintDrop.newInformation(e.getSource());
      drop.setCategorizingMessage(DSC_EFFECTS);
      drop.setMessage(EFFECT, e.toString());
    }
  }
  
  @Override
  protected void handleMethodDeclaration(final IRNode mdecl) {
    // Manage the receiver Declaration
    final IRNode oldReceiverDecl = receiverDecl;
    try {
      receiverDecl = JavaPromise.getReceiverNodeOrNull(mdecl);
      
      reportEffects(mdecl);

      doAcceptForChildren(mdecl);
    } finally {
      receiverDecl = oldReceiverDecl;
    }
  }
  
  @Override
  protected void handleConstructorDeclaration(final IRNode cdecl) {
    // Manage the receiver Declaration
    final IRNode oldReceiverDecl = receiverDecl;
    try {
      receiverDecl = JavaPromise.getReceiverNodeOrNull(cdecl);
      
      reportEffects(cdecl);

      doAcceptForChildren(cdecl);
    } finally {
      receiverDecl = oldReceiverDecl;
    }
  }

  @Override
  protected void handleClassInitDeclaration(final IRNode classBody, final IRNode node) {
    reportEffects(node);
  }
  
  @Override
  protected InstanceInitAction getAnonClassInitAction(
      final IRNode expr, final IRNode classBody) {
    // Manage the receiver Declaration
    return new InstanceInitAction() {
      final IRNode oldReceiverDecl = receiverDecl;
      
      @Override
      public void tryBefore() {
        receiverDecl = JavaPromise.getReceiverNodeOrNull(getEnclosingDecl());
      }
      
      @Override
      public void finallyAfter() {
        receiverDecl = oldReceiverDecl;
      }
      
      @Override
      public void afterVisit() {
        // does nothing
      }
    };
  }
}
