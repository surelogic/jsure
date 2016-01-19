package com.surelogic.analysis.concurrency.heldlocks_new;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Iterables;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;
import com.surelogic.aast.promise.LockSpecificationNode;
import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.alias.IMayAlias;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.assigned.DefiniteAssignment;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.heldlocks_new.LockExpressionManager.LockExprInfo;
import com.surelogic.analysis.concurrency.heldlocks_new.LockExpressionManager.SingleThreadedData;
import com.surelogic.analysis.concurrency.heldlocks_new.LockUtils.LockMethods;
import com.surelogic.analysis.concurrency.heldlocks_new.MustHoldAnalysis.HeldLocks;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.analysis.concurrency.model.declared.ModelLock;
import com.surelogic.analysis.concurrency.model.declared.StateLock;
import com.surelogic.analysis.concurrency.model.instantiated.HeldLock;
import com.surelogic.analysis.concurrency.model.instantiated.NeededLock;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.EffectEvidenceProcessor;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.effects.Effects.ImplementedEffects;
import com.surelogic.analysis.effects.InitializationEffectEvidence;
import com.surelogic.analysis.effects.UnresolveableLocksEffectEvidence;
import com.surelogic.analysis.effects.targets.evidence.EnclosingRefEvidence;
import com.surelogic.analysis.effects.targets.evidence.EvidenceProcessor;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.analysis.visitors.FlowUnitVisitor;
import com.surelogic.analysis.visitors.InstanceInitAction;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;
import com.surelogic.dropsea.ir.drops.locks.ReturnsLockPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.HasSubQuery;
import edu.cmu.cs.fluid.java.analysis.JavaFlowAnalysisQuery;
import edu.cmu.cs.fluid.java.analysis.QueryTransformer;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaIntersectionType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.uwm.cs.fluid.java.analysis.SimpleNonnullAnalysis;

public final class NewLockVisitor
extends FlowUnitVisitor<NewLockVisitor.Queries>
implements IBinderClient {
  private static final int PRECONDITION_NOT_ASSURED_CATEGORY = 2007;
  private static final int RETURNS_LOCK_ASSURED_CATEGORY = 2008;
  private static final int RETURNS_LOCK_NOT_ASSURED_CATEGORY = 2009;
  private static final int SHARED_UNPROTECTED_CATEGORY = 2010;
  private static final int UNIDENTIFIABLE_LOCK_CATEGORY = 2011;
  private static final int REDUNDANT_CATEGORY = 2012;
  private static final int NON_FINAL_CATEGORY = 2013;
  private static final int MIXED_JUC_INTRINSIC = 2014;
  private static final int LOCK_UNLOCK_MATCHES = 2015;
  
  private static final int UNRESOLVEABLE_LOCK_SPEC = 2018;
  private static final int ON_BEHALF_OF_CONSTRUCTOR = 2020;
  private static final int ANONYMOUS_CLASS_ENCLOSING_REF = 2025;
  
  private static final int GOOD_RETURN = 2030;
  private static final int BAD_RETURN = 2031;
  
  private static final int SHARED_UNPROTECTED_RECEIVER = 2035;
  private static final int SHARED_UNPROTECTED_FIELD_REF= 2036;
  
  private static final int UNIDENTIFIABLE_SYNCHRONIZED_METHOD = 2040;
  private static final int UNIDENTIFIABLE_STATIC_SYNCHRONIZED_METHOD = 2041;
  private static final int UNIDENTIFIABLE_LOCK_EXPR = 2042;
  
  private static final int REDUNDANT_SYNC = 2045;
  
  private static final int NON_FINAL_LOCK_EXPR = 2050;
  
  private static final int SYNCED_LOCK_OBJECT = 2055;
  
//  private static final int DSC_EFFECTS = 550;
//  private static final int EFFECT = 550;
  
  private static final int LOCK_DIFFERENT_NUMBER = 2060;
  private static final int LOCK_NO_MATCHES = 2061;
  private static final int LOCK_MATCH = 2062;
  private static final int UNLOCK_DIFFERENT_NUMBER = 2063;
  private static final int UNLOCK_NO_MATCHES = 2064;
  private static final int UNLOCK_MATCH = 2065;

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
  
  private final AtomicReference<AnalysisLockModel> analysisLockModel;
  private final LockUtils lockUtils;
  private final LockExpressionManager lockExprManager;
  
  /* Analyses for creating queries */
  private final BindingContextAnalysis bca;
//  private final SimpleNonnullAnalysis simpleNonNull; // no queries needed for this one
//  private final DefiniteAssignment definiteAssignment;
  private final IntrinsicLockAnalysis intrinsicLocks;
  private final MustHoldAnalysis mustHold;
  private final MustReleaseAnalysis mustRelease;
  
  
  
  public NewLockVisitor(
      final IBinder binder, final BindingContextAnalysis bca,
      final AtomicReference<AnalysisLockModel> analysisLockModel) {
    // Don't go inside nested types; skip annotation types
    super(true);
    
    this.analysisLockModel = analysisLockModel;
    this.thisExprBinder = new ThisExpressionBinder(binder);
    this.effects = new Effects(binder, analysisLockModel);
    
    this.bca = bca;
//    this.simpleNonNull = new SimpleNonnullAnalysis(binder);
//    this.definiteAssignment = new DefiniteAssignment(binder);
    
    final SimpleNonnullAnalysis simpleNonNull = new SimpleNonnullAnalysis(binder);
    final DefiniteAssignment definiteAssignment = new DefiniteAssignment(binder);
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
  
  final class Queries implements HasSubQuery {
//    private final BindingContextAnalysis.Query exprObjects;
//    private final ProvablyUnassignedQuery provablyUnassigned;
    private final IntrinsicLockAnalysis.Query heldIntrinsicLocks;
    private final JavaFlowAnalysisQuery<HeldLocks> heldJUCLocks;
    private final MustHoldAnalysis.LocksForQuery lockCalls;
    private final MustReleaseAnalysis.Query unlockCalls;

    public Queries(final IRNode decl) {
//      exprObjects = bca.getExpressionObjectsQuery(decl);
//      provablyUnassigned = definiteAssignment.getProvablyUnassignedQuery(decl);
      heldIntrinsicLocks = intrinsicLocks.getHeldLocksQuery(decl);
      heldJUCLocks = mustHold.getHeldLocksQuery(decl);
      lockCalls = mustHold.getLocksForQuery(decl);
      unlockCalls = mustRelease.getUnlocksForQuery(decl);
    }
    
    private Queries(final Queries q, final IRNode caller) {
//      exprObjects = q.exprObjects.getSubAnalysisQuery(caller);
//      provablyUnassigned = q.provablyUnassigned.getSubAnalysisQuery(caller);
      heldIntrinsicLocks = q.heldIntrinsicLocks.getSubAnalysisQuery(caller);
      heldJUCLocks = q.heldJUCLocks.getSubAnalysisQuery(caller);
      lockCalls = q.lockCalls.getSubAnalysisQuery(caller);
      unlockCalls = q.unlockCalls.getSubAnalysisQuery(caller);
    }
    
    @Override
    public Queries getSubAnalysisQuery(final IRNode caller) {
      return new Queries(this, caller);
    }
    
    public Set<IRNode> getUnlocksFor(final IRNode lockCall) {
      return unlockCalls.getResultFor(lockCall);
    }
    
    public Set<IRNode> getLocksFor(final IRNode unlockCall) {
      return lockCalls.getResultFor(unlockCall);
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
      return JavaPromise.getQualifiedReceiverNodeByName(getEnclosingDecl(), outerType);
    }    
  }

  
  
  // ======================================================================
  // == Helpers
  // ======================================================================
  
  private HeldLock isSatisfied(final NeededLock neededLock, final Iterable<HeldLock> heldLocks) {
    for (final HeldLock heldLock : heldLocks) {
      if (heldLock.mustSatisfy(neededLock, thisExprBinder)) {
        return heldLock;
      }
    }
    final NeededLock derivedFrom = neededLock.getDerivedFrom();
    if (derivedFrom != null) {
      return isSatisfied(derivedFrom, heldLocks);
    } else {
      return null;
    }
  }

  /**
   * Determine whether a class can be considered to protect itself. Returns
   * true} if one of the following is true:
   * <ul>
   * <li>The class, or one of its ancestors, is annotated with
   * <code>@ThreadSafe</code>
   * <li>The class, or one of its ancestors, is annotated with
   * <code>@Immutable</code>
   * <li>The class, or one of its ancestors, declares at least one region or
   * policy lock</code>
   * </ul>
   */
  private boolean isSafeType(final IJavaType type) {
    boolean isSafe = false;
    if (type instanceof IJavaDeclaredType) {
      final IJavaDeclaredType declaredType = (IJavaDeclaredType) type;
      final IRNode typeDeclarationNode = declaredType.getDeclaration();
      final boolean isThreadSafe = LockRules
          .isThreadSafe(typeDeclarationNode);
      isSafe = isThreadSafe || analysisLockModel.get().classDeclaresLocks(type);
    } else if (type instanceof IJavaTypeFormal) {
      final IJavaTypeFormal jtf = (IJavaTypeFormal) type;
      isSafe = isSafeType(jtf.getExtendsBound(thisExprBinder.getTypeEnvironment()));
    } else if (type instanceof IJavaIntersectionType) {
      final IJavaIntersectionType iType = (IJavaIntersectionType) type;
      isSafe = isSafeType(iType.getPrimarySupertype())
          || isSafeType(iType.getSecondarySupertype());
    }
    return isSafe;
  }
  
  private void receiverIsSafeObject(final IRNode actualRcvr) {
    // First see if the referenced type is safe
    if (!isSafeType(thisExprBinder.getJavaType(actualRcvr))) { // not safe
      if (FieldRef.prototype.includes(actualRcvr)) {
        final IRNode fieldDecl = this.thisExprBinder.getBinding(actualRcvr);
        // If the field is unique, it is a safe object
        if (!UniquenessUtils.isUnique(fieldDecl)) {
          /* See if the field is protected: either directly, or
           * because the the field is final or volatile and the class
           * contains lock annotations.
           */
          if (TypeUtil.isJSureFinal(fieldDecl) || TypeUtil.isVolatile(fieldDecl)) {
            final IJavaType actualRcvrType =
                thisExprBinder.getJavaType(FieldRef.getObject(actualRcvr));
            if (analysisLockModel.get().classDeclaresLocks(actualRcvrType)) {
              /* final/volatile field in a lock protected class, so the
               * object referenced by the field may be accessed concurrently.
               */
              final HintDrop info = HintDrop.newWarning(
                  actualRcvr, SHARED_UNPROTECTED_CATEGORY,
                  SHARED_UNPROTECTED_RECEIVER, DebugUnparser.toString(actualRcvr));
              for (final ModelLock<?, ?> ml : analysisLockModel.get().getAllDeclaredLocksIn(actualRcvrType, false)) {
                ml.getSourceAnnotation().addDependent(info);
              }
            }
          } else {
            final StateLock<?, ?> neededLock =
                analysisLockModel.get().getLockForFieldRef(actualRcvr);
            if (neededLock != null) {
              // Lock protected field
              final HintDrop info = HintDrop.newWarning(
                  actualRcvr, SHARED_UNPROTECTED_CATEGORY,
                  SHARED_UNPROTECTED_RECEIVER, DebugUnparser.toString(actualRcvr));
              neededLock.getSourceAnnotation().addDependent(info);
            }
          }
        }
      }
    }
  }

  private void dereferencesSafeObject(
      final IRNode objExpr, final IRNode fieldRef, final boolean isArrayRef) {
    /* fieldRef == e.f or e[...] */
    /* We only continue if fieldRef is e'.f'.f or e'.f'[...] */
    if (FieldRef.prototype.includes(objExpr)) {
      /*
       * Things are only interesting if the outer region f is not protected. So
       * we don't proceed if f' is unique (and thus f is aggregated into the
       * state of the referring object), f is protected by a lock or if f is
       * volatile or final. Array reference is never protected
       */
      final IRNode fDecl = thisExprBinder.getBinding(fieldRef);
      final IRNode fPrimeDecl = thisExprBinder.getBinding(objExpr);
      if (!UniquenessUtils.isUnique(fPrimeDecl) &&
          (isArrayRef ||
              (!TypeUtil.isJSureFinal(fDecl) && !TypeUtil.isVolatile(fDecl) &&
                  analysisLockModel.get().getLockForFieldRef(fieldRef) == null))) {
        /*
         * Now check if f' is in a protected region. There are three cases: (1)
         * f' is a final or volatile field in a class that contains lock
         * declarations. (2) f' is a field in a region associated with a lock.
         * (3) Otherwise, we assume f' is not meant to be accessed concurrently,
         * so we don't have to issue a warning.
         * 
         * In the first case we report the warning under EACH lock that is
         * declared in the class. In the second case we report the warning under
         * the lock that protects f'.
         */
        final IJavaType ePrimeType = thisExprBinder.getJavaType(FieldRef.getObject(objExpr));
        if (TypeUtil.isJSureFinal(fPrimeDecl) || TypeUtil.isVolatile(fPrimeDecl)) {
          // Field is final or volatile, see if the class contains locks
          if (analysisLockModel.get().classDeclaresLocks(ePrimeType)) {
            /*
             * For each lock declared in the class of e'.f', attach a warning
             * that it is not protecting the field f.
             * 
             * Propose that the field be aggregated into those regions. Really
             * this needs to be an OR. The end user should only be allowed to
             * choose one of these.
             */
            final HintDrop info = HintDrop.newWarning(
                fieldRef, SHARED_UNPROTECTED_CATEGORY,
                SHARED_UNPROTECTED_FIELD_REF, DebugUnparser.toString(fieldRef));

            // Propose the unique annotation
            for (final ModelLock<?, ?> ml : analysisLockModel.get().getAllDeclaredLocksIn(ePrimeType, false)) {
              ml.getSourceAnnotation().addDependent(info);
              
              if (ml instanceof StateLock) {
                final String simpleRegionName = ((StateLock<?, ?>) ml).getRegion().getName();
                if ("Instance".equals(simpleRegionName)) {
                  info.addProposal(
                      new Builder(Unique.class, fPrimeDecl, fieldRef).build());
                } else {
                  info.addProposal(
                      new Builder(UniqueInRegion.class, fPrimeDecl, fieldRef).setValue(simpleRegionName).build());
                }
              }
            }
          }
        } else { // Field f' is non-final, non-volatile
          final StateLock<?, ?> fPrimeLock = 
              analysisLockModel.get().getLockForFieldRef(objExpr);
          if (fPrimeLock != null) { // Field is non-final, non-volatile, and is associated with a lock
            final HintDrop info = HintDrop.newWarning(
                fieldRef, SHARED_UNPROTECTED_CATEGORY,
                SHARED_UNPROTECTED_FIELD_REF, DebugUnparser.toString(fieldRef));
            fPrimeLock.getSourceAnnotation().addDependent(info);

            /*
             * Propose that the field be @Unique and aggregated.
             */
            final String simpleRegionName = fPrimeLock.getRegion().getName();
            if ("Instance".equals(simpleRegionName)) {
              info.addProposal(
                  new Builder(Unique.class, fPrimeDecl, fieldRef).build());
            } else {
              info.addProposal(
                  new Builder(UniqueInRegion.class, fPrimeDecl, fieldRef).setValue(simpleRegionName).build());
            }
          }
        }
      }
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
      
//      // ======== DEBUG ========
//      HintDrop.newInformation(src, DSC_EFFECTS, EFFECT, e.toString());
//      // ======== DEBUG ========

      
      /* Look for unresolveable locks. */
      new EffectEvidenceProcessor() {
        @Override
        public void visitUnresolveableLocksEffectEvidence(final UnresolveableLocksEffectEvidence e) {
          for (final LockSpecificationNode lockSpec : e.getUnresolveableSpecs()) {
            final ResultDrop rd = ResultsBuilder.createResult(
                false, e.getRequiresLock(), src, 
                UNRESOLVEABLE_LOCK_SPEC, lockSpec, 
                DebugUnparser.toString(src));
            rd.setCategorizingMessage(PRECONDITION_NOT_ASSURED_CATEGORY);
          }
        }
      }.accept(e.getEvidence());

      
      
      // Show the held locks if the effect has needed locks
      if (!e.getNeededLocks().isEmpty()) {
        final Iterable<HeldLock> heldLocks = queries.getHeldLocks(src);
        for (final NeededLock neededLock : e.getNeededLocks()) {
          final HeldLock satisfyingLock = isSatisfied(neededLock, heldLocks);
          final boolean success = satisfyingLock != null;
          final ResultDrop resultDrop = ResultsBuilder.createResult(
              success, neededLock.getAssuredPromise(), src,
              neededLock.getReason().getResultMessage(success),
              neededLock.unparseForMessage(), DebugUnparser.toString(src));
          resultDrop.setCategorizingMessage(
              neededLock.getReason().getCategory(success));
          
          // ==== Add evidence and supporting information ====
          
          // Thread-confined constructor information
          /* Cannot get this at the top of the method before the loop because
           * in cases where the flow-unit is an init-block, the queries in the
           * lockExprManager are not set up correctly (not sure why).  We need
           * to have the LockExpressiosn object created by the flow analyses that
           * occur when we query the held locks (via the transformed query),
           * and then everything works okay.
           */
          final SingleThreadedData singleThreaded = lockExprManager.getSingleThreadedData(mdecl);
          if (singleThreaded != null) {
            singleThreaded.addSingleThreadedEvidence(resultDrop);
          }
          
          // Add held locks as supporting information
          for (final HeldLock heldLock : heldLocks) {
            if (heldLock.getSupportingPromise() != null) {
              resultDrop.addTrusted(heldLock.getSupportingPromise());
            }
            resultDrop.addInformationHint(heldLock.getSource(),
                heldLock.getReason().getInformationMessage(), heldLock);
          }
          
          /* Add constructor initialization information to help disambiguate
           * field initialization results. 
           */
          new EffectEvidenceProcessor() {
            @Override
            public void visitInitializationEffectEvidence(final InitializationEffectEvidence e) {
              final IRNode constructorDecl = e.getConstructorDeclaration();
              resultDrop.addInformationHint(
                  constructorDecl, ON_BEHALF_OF_CONSTRUCTOR,
                  JavaNames.genMethodConstructorName(constructorDecl));
            }
          }.accept(e.getEvidence());
          
          /* Add "held as" information that describes the mapping of 
           * qualified receivers inside of anonymous classes into 
           * references in the outer context.
           */
          new EvidenceProcessor(true) {
            @Override
            public void visitEnclosingRefEvidence(final EnclosingRefEvidence ere) {
              resultDrop.addInformationHint(
                  ere.getLink(), ANONYMOUS_CLASS_ENCLOSING_REF,
                  EnclosingRefEvidence.unparseRef(ere.getOriginal()),
                  EnclosingRefEvidence.unparseRef(ere.getEnclosingRef()));
            }
          }.accept(e.getTarget().getEvidence());
          
          // ==== Add proposals ==== 
          
          if (!success) {
            if (singleThreaded == null) { // method
              resultDrop.addProposal(
                  neededLock.getProposedRequiresLock(getEnclosingDecl(), src));
            } else { // constructor
              if (neededLock.isStatic()) {
                resultDrop.addProposal(
                    neededLock.getProposedRequiresLock(getEnclosingDecl(), src));
              } else {
                resultDrop.addProposal(
                    new Builder(Unique.class, getEnclosingDecl(), src).setValue("return").build());
              }
            }
          }
        }
        
        
        
//        // ======== TESTING & DEBUGGING --- GET RID OF THIS LATER ========
//        for (final HeldLock heldLock : heldLocks) {
//          HintDrop.newInformation(src, DSC_EFFECTS,
//              551, heldLock.toString(), DebugUnparser.toString(heldLock.getSource()));
//        }
//        // ===============================================================
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
      
      // Check locks
      reportEffects(mdecl);
      
      /* If the method is synchronized, but now programmer-declared locks are 
       * associated with it, then we add a warning.  This check is sleazy because
       * we count on the fact that non-static methods will always resolve the
       * receiver to at least the lock MUTEX.  So for static methods, we warn
       * when there are 0 locks, but for instance methods we warn when there
       * is only 1 lock.
       */
      if (JavaNode.getModifier(mdecl, JavaNode.SYNCHRONIZED)) {
        final int numLocks = lockExprManager.getSynchronizedMethodLocks(mdecl).size();
        if (TypeUtil.isStatic(mdecl)) {
          if (numLocks == 0) {
            HintDrop.newWarning(
                mdecl, UNIDENTIFIABLE_LOCK_CATEGORY,
                UNIDENTIFIABLE_STATIC_SYNCHRONIZED_METHOD,
                JavaNames.genMethodConstructorName(mdecl),
                JavaNames.getTypeName(VisitUtil.getEnclosingType(mdecl)));
          }
        } else {
          if (numLocks == 1) {
            HintDrop.newWarning(
                mdecl, UNIDENTIFIABLE_LOCK_CATEGORY,
                UNIDENTIFIABLE_SYNCHRONIZED_METHOD,
                JavaNames.genMethodConstructorName(mdecl));
          }
        }
      }

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
  
  @Override
  public Void visitReturnStatement(final IRNode rstmt) {
    final IRNode mdecl = getEnclosingDecl();
    final HeldLock returnsLock = lockExprManager.getReturnedLock(mdecl);
    if (returnsLock != null) { // Method as a @ReturnsLock annotation
      final ReturnsLockPromiseDrop pd = LockUtils.getReturnedLock(mdecl);
      final LockExprInfo lockExprInfo = lockExprManager.getReturnedLocks(mdecl, rstmt);
      if (lockExprInfo.isFinal()) {
        boolean correct = false;
        for (final HeldLock lock : lockExprInfo.getLocks()) {
          if (returnsLock.mustAlias(lock, thisExprBinder)) {
            correct = true;
            break;
          }
        }
        
        if (correct) {
          final ResultDrop resultDrop = ResultsBuilder.createResult(
              true, pd, rstmt, GOOD_RETURN, returnsLock);
          resultDrop.setCategorizingMessage(RETURNS_LOCK_ASSURED_CATEGORY);
        } else {
          final ResultDrop resultDrop = ResultsBuilder.createResult(
              false, pd, rstmt, BAD_RETURN, returnsLock);
          resultDrop.setCategorizingMessage(RETURNS_LOCK_NOT_ASSURED_CATEGORY);
        }
      } else { // Non-final lock expression
        final IRNode lockExpr = ReturnStatement.getValue(rstmt);
        final HintDrop info = HintDrop.newWarning(
            lockExpr, NON_FINAL_CATEGORY, NON_FINAL_LOCK_EXPR,
            DebugUnparser.toString(lockExpr));
        
        pd.addDependent(info);
        for (final HeldLock l : lockExprInfo.getLocks()) {
          l.getLockPromise().addDependent(info);
        }
      }
    }
    return null;
  }
  
  @Override
  public void handleMethodCall(final IRNode expr) {
    final IRNode methodDecl = this.thisExprBinder.getBinding(expr);
    final MethodCall call = (MethodCall) JJNode.tree.getOperator(expr);
    final IRNode rcvrObject = call.get_Object(expr);
    
    /* Check if the call is to a JUC lock()/unlock().  The map returned by
     * LockExpressionManager only has lock-expressions from lock()/unlock() 
     * calls in it, so if the rcvrObject is not in the map then it is "regular"
     * method call.
     */
    final LockExprInfo lockExprInfo =
        lockExprManager.getJUCLockExprsToLockSets(getEnclosingDecl()).get(rcvrObject);
    if (lockExprInfo != null) { // JUC Lock call
      if (!lockExprInfo.isFinal()) { // non-final lock expression
        final HintDrop info = HintDrop.newWarning(
            rcvrObject, NON_FINAL_CATEGORY, NON_FINAL_LOCK_EXPR,
            DebugUnparser.toString(rcvrObject));
        for (final HeldLock l : lockExprInfo.getLocks()) {
          l.getLockPromise().addDependent(info);
        }
      } else {
        if (lockExprInfo.isBogus()) { // unidentifiable lock
          HintDrop.newWarning(
              rcvrObject, UNIDENTIFIABLE_LOCK_CATEGORY,
              UNIDENTIFIABLE_LOCK_EXPR, DebugUnparser.toString(rcvrObject));
        }
        
        final String methodName = MethodCall.getMethod(expr);
        final LockMethods whichMethod = LockMethods.whichLockMethod(methodName);
        if (whichMethod.isLock()) { // lock(), tryLock(), or lockInterruptably()
          final Set<IRNode> unlocks = currentQuery().getUnlocksFor(expr);
          if (unlocks == null) {
            final HintDrop hint = HintDrop.newWarning(
                expr, LOCK_UNLOCK_MATCHES, LOCK_DIFFERENT_NUMBER, methodName);
            for (final HeldLock lock : lockExprInfo.getRealLocks()) {
              lock.getLockPromise().addDependent(hint);
            }
          } else if (unlocks.isEmpty()) {
            final HintDrop hint = HintDrop.newWarning(
                expr, LOCK_UNLOCK_MATCHES, LOCK_NO_MATCHES, methodName);
            for (final HeldLock lock : lockExprInfo.getRealLocks()) {
              lock.getLockPromise().addDependent(hint);
            }
          } else {
            for (final IRNode where : unlocks) {
              int lineNumber = -1;
              final IJavaRef javaRef = JavaNode.getJavaRef(where);
              if (javaRef != null) lineNumber = javaRef.getLineNumber();
              final HintDrop hint = HintDrop.newInformation(
                  expr, LOCK_UNLOCK_MATCHES, LOCK_MATCH, methodName, lineNumber);
              for (final HeldLock lock : lockExprInfo.getRealLocks()) {
                lock.getLockPromise().addDependent(hint);
              }
            }
          }
        } else {
          /* unlock() [LockExpressions already filtered out
           * NOT_A_LOCK_METHOD and IDENTICALLY_NAMED_METHOD] 
           */
          final Set<IRNode> locks = currentQuery().getLocksFor(expr);
          if (locks == null) {
            final HintDrop hint = HintDrop.newWarning(
                expr, LOCK_UNLOCK_MATCHES, UNLOCK_DIFFERENT_NUMBER);
            for (final HeldLock lock : lockExprInfo.getRealLocks()) {
              lock.getLockPromise().addDependent(hint);
            }
          } else if (locks.isEmpty()) {
            final HintDrop hint = HintDrop.newWarning(
                expr, LOCK_UNLOCK_MATCHES, UNLOCK_NO_MATCHES);
            for (final HeldLock lock : lockExprInfo.getRealLocks()) {
              lock.getLockPromise().addDependent(hint);
            }
          } else {
            for (final IRNode where : locks) {
              int lineNumber = -1;
              final IJavaRef javaRef = JavaNode.getJavaRef(where);
              if (javaRef != null) lineNumber = javaRef.getLineNumber();
              final HintDrop hint = HintDrop.newInformation(
                  expr, LOCK_UNLOCK_MATCHES, UNLOCK_MATCH,
                  MethodCall.getMethod(where), lineNumber);
              for (final HeldLock lock : lockExprInfo.getRealLocks()) {
                lock.getLockPromise().addDependent(hint);
              }
            }
          }
        }
      }
    } else {
      if (!TypeUtil.isStatic(methodDecl)) {
        /*
         * Check if the receiver is a "safe" object. Already weeded out
         * lock()/unlock().  Still need to rule out readLock()/writeLock().
         */
        if (!lockUtils.isMethodFromJavaUtilConcurrentLocksReadWriteLock(expr)) {
          receiverIsSafeObject(rcvrObject);
        }
      }
    }
    
    // Continue into the expression
    doAcceptForChildren(expr);
  }
  
  @Override
  public Void visitArrayRefExpression(final IRNode arrayRef) {
    dereferencesSafeObject(ArrayRefExpression.getArray(arrayRef), arrayRef, true);
    
    // continue into the expression
    doAcceptForChildren(arrayRef);
    return null;
  }
  
  @Override
  public Void visitFieldRef(final IRNode fieldRef) {
    dereferencesSafeObject(FieldRef.getObject(fieldRef), fieldRef, false);
    
    // continue into the expression
    doAcceptForChildren(fieldRef);
    return null;
  }
  
  @Override
  public Void visitSynchronizedStatement(final IRNode syncStmt) {
    final IRNode lockExpr = SynchronizedStatement.getLock(syncStmt);
    final LockExprInfo acquiringLocks =
        lockExprManager.getSyncBlock(getEnclosingDecl(), syncStmt);

    if (lockUtils.isJavaUtilConcurrentLockObject(lockExpr)) {
      HintDrop.newWarning(
          lockExpr, MIXED_JUC_INTRINSIC, SYNCED_LOCK_OBJECT, DebugUnparser.toString(lockExpr));
    } else {
      if (acquiringLocks.isFinal()) {
        final Set<HeldLock> lockSet = acquiringLocks.getLocks();
        
        /* Get the locks held at the point of the lock expression, and see if 
         * any of them are also acquired by the sync statement.
         */
        final Iterable<HeldLock> heldLocks = currentQuery().getHeldLocks(lockExpr);
        for (final HeldLock heldLock : heldLocks) {
          for (final HeldLock acquiredLock : lockSet) {
            if (acquiredLock.mustAlias(heldLock, thisExprBinder)) {
              final HintDrop info = HintDrop.newWarning(
                  syncStmt, REDUNDANT_CATEGORY, REDUNDANT_SYNC, acquiredLock);
              acquiredLock.getLockPromise().addDependent(info);
            }
          }
        }
        
        /* If the set of acquired locks is empty, or only contains MUTEX, then
         * we put out an unidentifiable lock warning.
         */
        final boolean unidentifiable =
            (lockSet.size() == 0) ||
            ((lockSet.size() == 1) &&
                lockSet.iterator().next().getLockPromise() ==
                analysisLockModel.get().getJavaLangObjectMutex());
        if (unidentifiable) {
          HintDrop.newWarning(
              lockExpr, UNIDENTIFIABLE_LOCK_CATEGORY,
              UNIDENTIFIABLE_LOCK_EXPR, DebugUnparser.toString(lockExpr));
        }
      } else { // Non-final lock expression
        final HintDrop info = HintDrop.newWarning(
            lockExpr, NON_FINAL_CATEGORY, 
            NON_FINAL_LOCK_EXPR, DebugUnparser.toString(lockExpr));
        for (final HeldLock l : acquiringLocks.getLocks()) {
          l.getLockPromise().addDependent(info);
        }
      }
    }
    
    // continue into the expression
    doAcceptForChildren(syncStmt);
    return null;
  }
}
