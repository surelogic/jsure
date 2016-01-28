package com.surelogic.analysis.testing;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.surelogic.analysis.AbstractAnalysisSharingAnalysis;
import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.assigned.DefiniteAssignment;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.driver.LockModelBuilder;
import com.surelogic.analysis.concurrency.heldlocks_new.IntrinsicLockAnalysis;
import com.surelogic.analysis.concurrency.heldlocks_new.LockExpressionManager;
import com.surelogic.analysis.concurrency.heldlocks_new.LockUtils;
import com.surelogic.analysis.concurrency.heldlocks_new.MustHoldAnalysis;
import com.surelogic.analysis.concurrency.heldlocks_new.MustHoldAnalysis.HeldLocks;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.Effects.ImplementedEffects;
import com.surelogic.analysis.visitors.FlowUnitVisitor;
import com.surelogic.analysis.visitors.InstanceInitAction;
import com.surelogic.analysis.visitors.SuperVisitor;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.HasSubQuery;
import edu.cmu.cs.fluid.java.analysis.JavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.analysis.QueryTransformer;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;

public final class EffectsAndLocksDumpModule
extends AbstractAnalysisSharingAnalysis<BindingContextAnalysis, MainVisitor, CUDrop> {
	public EffectsAndLocksDumpModule() {
		super(false, "EffectsAndLocksDumpModule", BindingContextAnalysis.factory);
	}

  @Override
  protected MainVisitor constructIRAnalysis(final IBinder binder) {
    return new MainVisitor(
        binder, getSharedAnalysis(), LockModelBuilder.getLockModel());
  }

  @Override
  protected boolean doAnalysisOnAFile(
      final IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
    final Driver driver = new Driver();
    driver.doAccept(compUnit);
    return true;
  }
  
  @Override
  protected void clearCaches() {
    // Nothing to do
  }
  
  
  
  private final class Driver extends SuperVisitor {
    public Driver() {
      super(SkipAnnotations.YES);
    }
    
    @Override
    protected List<FlowUnitVisitor<?>> createSubVisitors() {
      return ImmutableList.<FlowUnitVisitor<?>>of(getAnalysis());
    }


    
    private JavaComponentFactory jcf = null;
    
    @Override
    protected void enteringEnclosingDecl(
        final IRNode newDecl, final IRNode anonClassDecl) {
      jcf = JavaComponentFactory.startUse();
    }
    
    @Override
    protected final void leavingEnclosingDecl(
        final IRNode oldDecl, final IRNode returningTo) {
      JavaComponentFactory.finishUse(jcf);
      jcf = null;
    }
  }
}


final class MainVisitor
extends FlowUnitVisitor<MainVisitor.Queries>
implements IBinderClient {
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
  private final IntrinsicLockAnalysis intrinsicLocks;
  private final MustHoldAnalysis mustHold;
  
  
  
  public MainVisitor(
      final IBinder binder, final BindingContextAnalysis bca,
      final AtomicReference<AnalysisLockModel> analysisLockModel) {
    // Don't go inside nested types; skip annotation types
    super(SkipAnnotations.YES);
    
    this.thisExprBinder = new ThisExpressionBinder(binder);
    this.effects = new Effects(binder, analysisLockModel);
    
    this.bca = bca;
    final SimpleNonnullAnalysis simpleNonNull = new SimpleNonnullAnalysis(binder);
    final DefiniteAssignment definiteAssignment = new DefiniteAssignment(binder);
    final IMayAlias mayAlias = new TypeBasedMayAlias(binder);
    
    this.lockUtils = new LockUtils(analysisLockModel, thisExprBinder, effects, mayAlias);
    this.lockExprManager = new LockExpressionManager(lockUtils, binder, analysisLockModel, bca, definiteAssignment);
    this.intrinsicLocks = new IntrinsicLockAnalysis(binder, lockUtils, lockExprManager, simpleNonNull);
    this.mustHold = new MustHoldAnalysis(thisExprBinder, lockUtils, lockExprManager, simpleNonNull);
  }

  
  
  // ======================================================================
  // == Manage the query stack
  // ======================================================================
  
  final class Queries implements HasSubQuery {
    private final IntrinsicLockAnalysis.Query heldIntrinsicLocks;
    private final JavaFlowAnalysisQuery<HeldLocks> heldJUCLocks;

    public Queries(final IRNode decl) {
      heldIntrinsicLocks = intrinsicLocks.getHeldLocksQuery(decl);
      heldJUCLocks = mustHold.getHeldLocksQuery(decl);
    }
    
    private Queries(final Queries q, final IRNode caller) {
      heldIntrinsicLocks = q.heldIntrinsicLocks.getSubAnalysisQuery(caller);
      heldJUCLocks = q.heldJUCLocks.getSubAnalysisQuery(caller);
    }
    
    @Override
    public Queries getSubAnalysisQuery(final IRNode caller) {
      return new Queries(this, caller);
    }
    
    public Iterable<HeldLock> getHeldLocks(final IRNode node) {
      final Set<HeldLock> intrinsic = heldIntrinsicLocks.getResultFor(node);
      final HeldLocks juc = heldJUCLocks.getResultFor(node);
      return Iterables.concat(
          intrinsic, juc.classInitLocks, juc.singleThreadedLocks,
          juc.heldLocks);
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
    return currentQuery().getSubAnalysisQuery(caller);
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
      return JavaPromise.getQualifiedReceiverNodeByName(getEnclosingSyntacticDecl(), outerType);
    }    
  }

  
  
  // ======================================================================
  // == The lock-checking magic is here
  // ======================================================================
  
  private void reportEffects(final IRNode mdecl) {
    final ImplementedEffects implementationEffects =
        effects.getImplementationEffects(mdecl, bca);
    for (final Effect e : implementationEffects) {
      final IRNode src = e.getSource();
      final QueryTransformer qt = implementationEffects.getTransformerFor(src);
      final Queries queries = qt.transform(currentQuery());
      
      HintDrop.newInformation(src, Messages.DSC_EFFECTS, Messages.EFFECT, e.toString());

      final Iterable<HeldLock> heldLocks = queries.getHeldLocks(src);
      for (final HeldLock heldLock : heldLocks) {
        HintDrop.newInformation(
            src, Messages.DSC_EFFECTS, Messages.HOLDS_LOCKS,
            heldLock.toString(), DebugUnparser.toString(heldLock.getSource()));
      }
    }
  }

  
  
  // ======================================================================
  // == Visit
  // ======================================================================
  
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
        receiverDecl = JavaPromise.getReceiverNodeOrNull(getEnclosingSyntacticDecl());
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
