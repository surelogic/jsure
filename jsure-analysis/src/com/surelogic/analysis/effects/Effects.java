package com.surelogic.analysis.effects;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableSet;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.java.TypeExpressionNode;
import com.surelogic.aast.java.VariableUseExpressionNode;
import com.surelogic.aast.promise.AnyInstanceExpressionNode;
import com.surelogic.aast.promise.EffectSpecificationNode;
import com.surelogic.aast.promise.EffectsSpecificationNode;
import com.surelogic.aast.promise.ImplicitQualifierNode;
import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.bca.BindingContext;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel.LockGenerator;
import com.surelogic.analysis.concurrency.model.NeededLock;
import com.surelogic.analysis.concurrency.model.NeedsNoLock;
import com.surelogic.analysis.effects.targets.evidence.AggregationEvidence;
import com.surelogic.analysis.effects.targets.evidence.AnonClassEvidence;
import com.surelogic.analysis.effects.targets.evidence.BCAEvidence;
import com.surelogic.analysis.effects.targets.evidence.CallEvidence;
import com.surelogic.analysis.effects.targets.evidence.EmptyEvidence;
import com.surelogic.analysis.effects.targets.evidence.MappedArgumentEvidence;
import com.surelogic.analysis.effects.targets.evidence.NoEvidence;
import com.surelogic.analysis.effects.targets.evidence.QualifiedReceiverConversionEvidence;
import com.surelogic.analysis.effects.targets.evidence.UnknownReferenceConversionEvidence;
import com.surelogic.analysis.effects.targets.evidence.EmptyEvidence.Reason;
import com.surelogic.analysis.effects.targets.AnyInstanceTarget;
import com.surelogic.analysis.effects.targets.ClassTarget;
import com.surelogic.analysis.effects.targets.EmptyTarget;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.LocalTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.analysis.visitors.InstanceInitAction;
import com.surelogic.analysis.visitors.JavaSemanticsVisitor;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;
import com.surelogic.javac.Projects;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.AnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.NestedEnumDeclaration;
import edu.cmu.cs.fluid.java.operator.OpAssignExpression;
import edu.cmu.cs.fluid.java.operator.PostDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PostIncrementExpression;
import edu.cmu.cs.fluid.java.operator.PreDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PreIncrementExpression;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.OpUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Interface to the region effects analysis.
 */
public final class Effects implements IBinderClient {
  private final IBinder binder;
  private IIRProject allRegionProj = null;
  private RegionModel allRegion;
  
  private final AtomicReference<AnalysisLockModel> lockModel;
  
  
  
  public Effects(final IBinder binder, final AtomicReference<AnalysisLockModel> lm) {
    this.binder = binder;
    this.lockModel = lm;
  }


  
  // ----------------------------------------------------------------------
  // IBinderClient methods
  // ----------------------------------------------------------------------
  
  @Override
  public void clearCaches() {
    // Do nothing
  }

  @Override
  public IBinder getBinder() {
    return binder;
  }

  
  
  //----------------------------------------------------------------------
  // -- Utility methods
  //----------------------------------------------------------------------

  private RegionModel getAllRegion(final IRNode context) {
	  final IIRProject p = Projects.getEnclosingProject(context);
	  if (p != allRegionProj) {
		  // Update, since it's the wrong project
		  allRegion = RegionModel.getAllRegion(p);
		  allRegionProj = p;
	  }
	  return allRegion;
  }

  public static String unparseForPromise(final Set<Effect> fx) {
    if (fx.isEmpty()) {
      return "none";
    } else {
      // Added to unparse in a consistent order
      final List<Effect> sorted = new ArrayList<Effect>(fx);
      Collections.sort(sorted, new Comparator<Effect>() {
        @Override
        public int compare(Effect o1, Effect o2) {
          // Not efficient due to unparse
          return o1.unparseForPromise().compareTo(o2.unparseForPromise());
        }
      });

      final StringBuilder reads = new StringBuilder("reads ");
      final StringBuilder writes = new StringBuilder("writes ");
      boolean hasRead = false;
      boolean hasWrite = false;
      for (final Effect e : sorted) {
        final String unparsed = e.unparseForPromise();
        if (e.isRead()) {
          if (hasRead) reads.append(", ");
          else hasRead = true;
          reads.append(unparsed);
        } else {
          if (hasWrite) writes.append(", ");
          else hasWrite = true;
          writes.append(unparsed);
        }
      }

      /* We already checked if the effects are empty,
       * so must have at least one of these
       */
      if (!hasRead) {
        return writes.toString();
      } else if (!hasWrite) {
        return reads.toString();
      } else {
        return reads.toString() + "; " + writes.toString();
      }
    }
  }  
  
  
  
  //----------------------------------------------------------------------
  // -- Get the effects of an expression
  //----------------------------------------------------------------------

  /**
   * Construct a new effects query to be used on a particular flow unit.
   * 
   * @param flowUnit
   *          The method or constructor declaration that encloses the nodes that
   *          we will ultimately visit. This <em>must</em> be a
   *          MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
   *          ClassInitDeclaration node. If the nodes we are going to visit are
   *          inside an instance initializer block or instance field declaration
   *          of a non-anonymous class, then this should be the
   *          ConstructorDeclaration node of the constructor on whose behalf
   *          they are being analyzed. If the nodes we are going to visit are
   *          inside the instance initializer or field declaration of an
   *          anonymous class expression, this should be the InitDeclaration of
   *          the anonymous class.
   * @param query
   *          The BCA query to use. This is needs to have the proper
   *          relationship to <code>flowUnit</code>. In particular, when the
   *          node being analyzed is inside an instance initializer or field
   *          declaration, or is inside an instance initializer or field
   *          declaration of an anonymous class expression, then this should be
   *          the appropriate sub query object. In cases of highly nested
   *          anonymous classes, this should be the appropriate sub-sub-query.
   */
  public Query getEffectsQuery(
      final IRNode flowUnit, final BindingContextAnalysis.Query query) {
    return new Query(flowUnit, query);
  }

  /**
   * Get the effects of a method/constructor implementation.  This 
   * method is equivalent too, but hopefully slightly faster than,
   * <code>this.getEffectsQuery(flowUnit, bca.getExpressionObjectsQuery(flowUnit)).getResultsFor(flowUnit)</code>.
   * 
   * @param decl A MethodDeclaration or ConstructorDeclaration node.
   * @param bac The BCA analysis to use.
   * @return The effects of the method as implemented, not declared.
   */
  public Set<Effect> getImplementationEffects(
      final IRNode flowUnit, final BindingContextAnalysis bca) {
    final EffectsVisitor visitor = new EffectsVisitor(
        flowUnit, bca.getExpressionObjectsQuery(flowUnit));
    visitor.doAccept(flowUnit);
    return Collections.unmodifiableSet(visitor.getTheEffects());
  }
  
  /**
   * Remove the maskable effects from a set of effects.
   */
  public Set<Effect> maskEffects(final Set<Effect> effects) {
    if (effects.isEmpty()) {
      return Collections.emptySet();
    } else {
      final Set<Effect> newEffects = new HashSet<Effect>();
      for (final Effect e : effects) {
        final Effect masked = e.mask(binder);
        if (masked != null) newEffects.add(masked);
      }
      return Collections.unmodifiableSet(newEffects);
    }
  }

  
  
  //----------------------------------------------------------------------
  // -- Get the effects of a method declaration
  //----------------------------------------------------------------------

  /**
   * Get the declared effects for a method/constructor or <code>null</code> if
   * no effects are declared.  Use the method {@link #getMethodEffects(IRNode, IRNode)}
   * if you need to compensate for methods that do not declare effects.
   * 
   * @param mDecl
   *          a MethodDeclaration or ConstructorDeclaration
   * @param callSite
   *          Node for the effect source. Should be one of MethodCall,
   *          NewExpression, ConstructorCall, MethodDeclaration, or
   *          ConstructorDeclaration.
   */
  public static List<Effect> getDeclaredMethodEffects(
      final IRNode mDecl, final IRNode callSite) {
    final RegionEffectsPromiseDrop promisedEffects =
        MethodEffectsRules.getRegionEffectsDrop(mDecl);
    if (promisedEffects == null) { // No promises, return null
      return null;
    } else {
      final List<Effect> result = new ArrayList<Effect>();
      // Convert IRNode representation of effects in Effect objects
      getEffectsFromSpecificationNode(
          mDecl, promisedEffects.getEffects(), result, callSite);
      if (result.isEmpty()) {
        result.add(Effect.empty(callSite, new EmptyEvidence(Reason.DECLARES_NO_EFFECTS)));
      }
      return Collections.unmodifiableList(result);
    }
  }

  private static void getEffectsFromSpecificationNode(final IRNode mDecl,
      final Iterable<EffectsSpecificationNode> promisedEffects,
      final List<Effect> result, final IRNode callSite) {
    for(final EffectsSpecificationNode effList : promisedEffects) {
      for(final EffectSpecificationNode peff : effList.getEffectList()) {
        final RegionModel region = peff.getRegion().resolveBinding().getModel();
        final boolean isRead = !peff.getIsWrite();
        final ExpressionNode pContext = peff.getContext();
        
        final Target targ;
        if (pContext instanceof ImplicitQualifierNode) {
          if (region.isStatic()) { // Static region -> class target
            targ = new ClassTarget(region, NoEvidence.INSTANCE);
          } else { // Instance region -> qualify with receiver
            // We bind the receiver ourselves, so this is safe
            targ = new InstanceTarget(
                JavaPromise.getReceiverNode(mDecl), region, NoEvidence.INSTANCE);
          }
        } else if (pContext instanceof AnyInstanceExpressionNode) {
          final IJavaType type = 
            ((AnyInstanceExpressionNode) pContext).getType().resolveType().getJavaType();
          targ = new AnyInstanceTarget(
              (IJavaReferenceType) type, region, NoEvidence.INSTANCE);
        } else if (pContext instanceof QualifiedThisExpressionNode) {
          final QualifiedThisExpressionNode qthis =
            (QualifiedThisExpressionNode) pContext;
          final IRNode canonicalReceiver =
            JavaPromise.getQualifiedReceiverNodeByName(mDecl, qthis.resolveType().getNode());
          // We just bound the receiver ourselves, so this is safe
          targ = new InstanceTarget(
              canonicalReceiver, region, NoEvidence.INSTANCE);
        } else if (pContext instanceof TypeExpressionNode) {
          targ = new ClassTarget(region, NoEvidence.INSTANCE);
        } else if (pContext instanceof ThisExpressionNode) {
          // We bind the receiver ourselves, so this is safe
          targ = new InstanceTarget(
              JavaPromise.getReceiverNode(mDecl), region, NoEvidence.INSTANCE);
        } else if (pContext instanceof VariableUseExpressionNode) {
          // The object expression cannot be a receiver, so this is safe
          targ = new InstanceTarget(
              ((VariableUseExpressionNode) pContext).resolveBinding().getNode(),
              region, NoEvidence.INSTANCE);
        } else {
          // Shouldn't happen, but we need to ensure that blank final targ is initialized
          targ = null;
        }
        final Effect eff = Effect.effect(callSite, isRead, targ, new NeedsNoLock(callSite));
        result.add(eff);
      }
    }
  }

  /**
   * Get the declared effects for a method/constructor or
   * <code>writes(All)</code> if no effects are declared.
   * 
   * <P>
   * This method compensates for unannotated methods.  We still
   * need a system-wide approach to dealing with this.
   * 
   * @param mDecl
   *          a MethodDeclaration or ConstructorDeclaration
   * @param callSite
   *          Node for the effect source. Should be one of MethodCall,
   *          NewExpression, ConstructorCall, MethodDeclaration, or
   *          ConstructorDeclaration.
   */
  public List<Effect> getMethodEffects(
      final IRNode mDecl, final IRNode callSite) {
    List<Effect> effects = getDeclaredMethodEffects(mDecl, callSite);
    if (effects == null) {
      final Target anything =
          new ClassTarget(getAllRegion(callSite), NoEvidence.INSTANCE);
      // get the RequiredLocks
      effects = Collections.singletonList(
          Effect.write(callSite, anything, new NeedsNoLock(callSite)));
    }
    return effects;
  }

  
  
  //----------------------------------------------------------------------
  // -- Get the effects of a method call
  //----------------------------------------------------------------------

  /**
   * Get the effects of a method/constructor call.
   * 
   * @param call
   *          The node of the call. This node must have an operator type that
   *          implements {@link #CallInterface}.
   * @param caller
   *          The node of the method declaration or constructor declaration that
   *          contains the call.
   * @return An unmodifiable set of effects.
   */
  public Set<Effect> getMethodCallEffects(
      final BindingContextAnalysis.Query bcaQuery,
      final IRNode call, final IRNode caller) {
    final IRNode rcvr = JavaPromise.getReceiverNodeOrNull(caller);
    final ThisExpressionBinder teb = new AbstractThisExpressionBinder(binder) {
      @Override
      protected IRNode bindReceiver(final IRNode node) {
        return rcvr;
      }
      
      @Override
      protected IRNode bindQualifiedReceiver(final IRNode outerType, final IRNode node) {
        return JavaPromise.getQualifiedReceiverNodeByName(caller, outerType);
      }
    };
    return getMethodCallEffects(bcaQuery, teb, call, caller);
  }
  
  /**
   * Get the effects of a specific method/constructor call. The effects are
   * fully integrated into the context of the caller, that is, region
   * aggregation is taken into account, and BindingContextAnalysis is used to
   * replace uses of local variables in instance targets. Technically speaking,
   * the effects are properly elaborated.
   * 
   * @param bcaQuery
   *          Binding context analysis query focused to the flow unit
   *          represented by <code>callingMethodDecl</code>. It is up to the
   *          caller to make sure these values are consistent. Although we could
   *          instead take the BCA analysis itself and force the query to be
   *          consistent here, we do not because not doing so allows the query
   *          to be cached by the callers and thus not created over and over
   *          again for each use.
   * @param call
   *          The node representing the method/constructor call
   * @param targetFactory
   *          The target factory must insure that ThisExpression or
   *          QualifiedThisExpression IRNodes that are passed to
   *          {@link TargetFactory#createInstanceTarget(IRNode, IRegion)} are
   *          property bound. Currently this means that the targetFactory had
   *          better be an instance of {@link ThisBindingTargetFactory}.
   */
  private Set<Effect> getMethodCallEffects(
      final BindingContextAnalysis.Query bcaQuery,
      final ThisExpressionBinder thisExprBinder, final IRNode call,
      final IRNode callingMethodDecl) {
    // Get the node of the method/constructor declaration
    final IRNode mdecl = thisExprBinder.getBinding(call);
    if (mdecl == null) {
    	return Collections.emptySet();
    }
    final Operator op = JJNode.tree.getOperator(mdecl);
  
    // Don't process pseudo-method calls that make up Java 5 annotations
    if (AnnotationElement.prototype.includes(op)) {
      return Collections.emptySet();
    }
  
    final Map<IRNode, IRNode> table =
      MethodCallUtils.constructFormalToActualMap(thisExprBinder, call, mdecl, callingMethodDecl);
    
    // === Step 2: Instantiate the declared effects based on the substitution map
    
    // go through list and replace each effect on p with effects on table[p]
    final Set<Effect> methodEffects = new HashSet<Effect>();
    for (final Effect eff : getMethodEffects(mdecl, call)) {
      final Target t = eff.getTarget();
      if (t instanceof InstanceTarget) {
        final IRNode ref = t.getReference();
        final IRNode val = table.get(ref);
        if (val != null) {
          final IRNode objectExpr = thisExprBinder.bindThisExpression(val);
          final Target newTarg = new InstanceTarget(
              objectExpr, t.getRegion(), new MappedArgumentEvidence(mdecl, ref, val));
          elaborateInstanceTarget(
              bcaQuery, thisExprBinder, lockModel.get(),
              call, eff.isRead(), newTarg,
              lockModel.get().getNeededLock(thisExprBinder, newTarg, call, !eff.isRead(), objectExpr),
              methodEffects);
        } else { // See if ref is a QualifiedReceiverDeclaration
          if (QualifiedReceiverDeclaration.prototype.includes(JJNode.tree.getOperator(ref))) {
            final IRNode type = QualifiedReceiverDeclaration.getType(thisExprBinder, ref);
            final IJavaReferenceType javaType = JavaTypeFactory.getMyThisType(type);
            final Target newTarg = new AnyInstanceTarget(
                javaType, t.getRegion(),
                new QualifiedReceiverConversionEvidence(mdecl, ref, javaType)); 
            methodEffects.add(Effect.effect(call, eff.isRead(), newTarg, new NeedsNoLock(call)));
          } else {
            throw new IllegalStateException("Unmappable instance target: " + t);
          }
        }
      } else { // It's an effect on static state or any instance
        methodEffects.add(eff.changeSource(call, new CallEvidence(mdecl)));
      }
    }
  
    return Collections.unmodifiableSet(methodEffects);
  }

  
  
  // ----------------------------------------------------------------------
  // Target elaboration methods
  // ----------------------------------------------------------------------
  
  /**
   * XXX: Only public so that lock assurance can have access to it.  This itself
   * is questionable.  I really need to replace the lock assurance with a flow
   * analysis that uses regular effects results, instead of one that duplicates
   * the work of effects analysis.  This would eliminate the need for this
   * method to be public or even to exist at all.
   */
  // XXX: To be eliminated
  public static Set<Effect> elaborateEffect(
      final BindingContextAnalysis.Query bcaQuery,
      final ThisExpressionBinder thisExprBinder,
      final IRNode src, final boolean isRead, final Target target) {
    if (target instanceof InstanceTarget) {
      final Set<Effect> elaboratedEffects = new HashSet<Effect>();
      // XXX: Using bogus lock model and empty lock
      elaborateInstanceTarget(
          bcaQuery, thisExprBinder, new AnalysisLockModel(thisExprBinder), src,
          isRead, target, new NeedsNoLock(src), elaboratedEffects);
      return Collections.unmodifiableSet(elaboratedEffects);
    } else {
      return Collections.singleton(Effect.effect(src, isRead, target, Effect.NO_LOCKS));
    }
  }
  
  

  private static void elaborateInstanceTarget(
      final BindingContextAnalysis.Query bcaQuery,
      final ThisExpressionBinder thisExprBinder,
      final AnalysisLockModel lockModel,
      final IRNode srcExpr, final boolean isRead,
      final Target initTarget, final NeededLock initLock,
      final Set<Effect> outEffects) {
    final TargetElaborator te = new TargetElaborator(
        srcExpr, !isRead, bcaQuery, thisExprBinder, lockModel);
    te.elaborateTarget(initTarget, initLock);
    te.getEffects(srcExpr, isRead, outEffects);
  }

  
  
  
  // ----------------------------------------------------------------------
  // Nested types
  // ----------------------------------------------------------------------
  
  /**
   * Class stores the details about the particular visitation being performed.
   * Initialized by one of the public entry methods:
   * {@link #getEffects} {@link #getLHSEffects}, {@link #getMethodCallEffects},
   * or {@link #getRawMethodCallEffects}.
   */
  private static final class Context {
    /**
     * The set of accumulated effects.  This field has a value only when a 
     * traversal is being performed; otherwise it is <code>null</code>.
     */
    private final Set<Effect> theEffects;
    
    /**
     * The MethodDeclaration, ConstructorDeclaration, or ClassInitDeclaration
     * being analyzed.
     */
    private final IRNode enclosingMethod;
    
    /**
     * The receiver declaration node of the constructor/method/field
     * initializer/class initializer currently being analyzed. Every expression we
     * want to analyze should be inside one of these things. We need to keep track
     * of this because the {@link #initHelper instance initialization  helper}
     * re-enters this analysis on behalf of constructor declarations, and we want
     * any field declarations and instance initializers to report their receivers
     * in terms of the current constructor; this makes life easier for consumers
     * of the effect results.
     */
    private final IRNode theReceiverNode;
    
    /**
     * The current binding context analysis query engine.  This BCA is focused
     * to the flow unit represented by {@link #enclosingMethod}.
     */
    private final BindingContextAnalysis.Query bcaQuery;
    
    /**
     * This field is checked on entry to an expression to determine if the effect
     * should be a write effect. It is always immediately restored to
     * <code>false</code> after being checked.
     * 
     * <p>
     * The field represents whether the expression is the left-hand side of an
     * assignment expression. Has the following relationship with the
     * <code>read</code> parameter of
     * {@link Effect#newEffect(IRNode, boolean, Target)}:
     * <code>read == !isLHS</code> because if it's on the LHS it is being
     * assigned to.
     * 
     * <p>
     * This field is set by when visiting the parent of the lhs node. It is thus
     * important that the parent node set this flag immediately before visiting the
     * node that represents the left-hand side of the assignment expression.
     */
    private boolean isLHS;
  
    
    
    private Context(final Set<Effect> effects, final IRNode method,
        final IRNode rcvr, final BindingContextAnalysis.Query query,
        final boolean lhs) {
      this.theEffects = effects;
      this.enclosingMethod = method;
      this.theReceiverNode = rcvr;
      this.bcaQuery = query;
      this.isLHS = lhs;
    }
  
    public static Context forNormalMethod(
        final BindingContextAnalysis.Query query, final IRNode enclosingMethod) {
      return new Context(new HashSet<Effect>(), enclosingMethod,
          JavaPromise.getReceiverNodeOrNull(enclosingMethod),
          query, false);
    }
    
    public static Context forACE(
        final Context oldContext, final IRNode anonClassExpr,
        final IRNode enclosingDecl, final IRNode rcvr) {
      return new Context(new HashSet<Effect>(), enclosingDecl, rcvr,
          oldContext.bcaQuery.getSubAnalysisQuery(anonClassExpr), false);
    }
    
    public static Context forConstructorCall(final Context oldContext, final IRNode ccall) {
      // Purposely alias the effects set
      return new Context(oldContext.theEffects, 
          oldContext.enclosingMethod, oldContext.theReceiverNode,
          oldContext.bcaQuery.getSubAnalysisQuery(ccall), oldContext.isLHS);
    }
    
    
    
    public boolean isRead() {
      final boolean isRead = !this.isLHS;
      this.isLHS = false;
      return isRead;
    }
  }



  private final class EffectsVisitor extends JavaSemanticsVisitor {
    private final RegionModel INSTANCE_REGION;

    /**
     * The binder to use.
     */
    private final ThisExpressionBinder thisExprBinder;
    
    /**
     * Information about the current method/constructor declaration
     * being visited.  This is set by the constructor, and temporarily
     * reset by {@link #visitAnonClassExpression(IRNode)} to allow the
     * visitor to be reentered by an InstanceInitVisitor without destroying
     * the parent analysis.
     */
    private Context context;
    
    //----------------------------------------------------------------------

    /**
     * Construct a new Effects Visitor.
     * 
     * @param b
     *          The Binder to use to look up names.
     * @param flowUnit
     *          The method or constructor declaration that encloses the nodes that
     *          we will ultimately visit. This <em>must</em> be a
     *          MethodDeclaration, ConstructorDeclaration, or InitDeclaration
     *          node. It does not make sense right now for it to be a
     *          ClassInitDeclaration. If the nodes we are going to visit are
     *          inside an instance initializer block or instance field declaration
     *          of a non-anonymous class, then this should be the
     *          ConstructorDeclaration node of the constructor on whose behalf
     *          they are being analyzed. If the nodes we are going to visit are
     *          inside the instance initializer or field declaration of an
     *          anonymous class expression, this should be the InitDeclaration of
     *          the anonymous class.
     * @param query
     *          The BCA query to use. This is needs to have the proper
     *          relationship to <code>flowUnit</code>. In particular, when the
     *          node being analyzed is inside an instance initializer or field
     *          declaration, or is inside an instance initializer or field
     *          declaration of an anonymous class expression, then this should be
     *          the appropriate sub query object. In cases of highly nested
     *          anonymous classes, this should be the appropriate sub-sub-query.
     */
    public EffectsVisitor(final IRNode flowUnit,
        final BindingContextAnalysis.Query query) {
      super(false, true, flowUnit);
      thisExprBinder = new EVThisExpressionBinder(binder);
      INSTANCE_REGION = RegionModel.getInstanceRegion(flowUnit);    
      context = Context.forNormalMethod(query, flowUnit);
    }

    
    
    public Set<Effect> getTheEffects() {
      return context.theEffects;
    }
    
    
    
    //----------------------------------------------------------------------
    // Helper methods
    //----------------------------------------------------------------------
    
    private final class EVThisExpressionBinder extends AbstractThisExpressionBinder {
      public EVThisExpressionBinder(final IBinder b) {
        super(b);
      }

      @Override
      protected IRNode bindReceiver(IRNode node) {
        return context.theReceiverNode;
      }
      
      @Override
      protected IRNode bindQualifiedReceiver(IRNode outerType, IRNode node) {
        return JavaPromise.getQualifiedReceiverNodeByName(getEnclosingDecl(), outerType);
      }    
    }

    //----------------------------------------------------------------------

    @Override
    protected void handleAsMethodCall(final IRNode call) {
      /* Assumes that the enclosing method/constructor of the call is the
       * method/constructor declaration represented by
       * {@link Context#enclosingMethod enclosing method} of the current
       * {@link #context context.}.
       */
      context.theEffects.addAll(
          Effects.this.getMethodCallEffects(
              context.bcaQuery, thisExprBinder, call, getEnclosingDecl()));
    }
    
    //----------------------------------------------------------------------

    @Override
    protected InstanceInitAction getAnonClassInitAction(
        final IRNode expr, final IRNode classBody) {
      /* First need to determine the class declaration node of the super class
       * of the anonymous class or enum constant class declaration.
       */
      final IRNode superClassDecl;
      if (AnonClassExpression.prototype.includes(expr)) {
        superClassDecl = thisExprBinder.getBinding(AnonClassExpression.getType(expr));
      } else { // Enum
        IRNode current = JJNode.tree.getParentOrNull(expr);
        Operator op = JJNode.tree.getOperator(current);
        while (!EnumDeclaration.prototype.includes(op) && !NestedEnumDeclaration.prototype.includes(op)) {
          current = JJNode.tree.getParentOrNull(current);
          op = JJNode.tree.getOperator(current);
        }
        superClassDecl = current;
      }
      
      /* Need to get the effects of the instance field initializers and the
       * instance initializers of the anonymous class. Effects will come back
       * elaborated. They will then need to be masked (including the special case
       * for constructors, masking out the effects on the receiver of the new
       * object), instantiated based on the current enclosing instances, and then
       * have any "dangling" instance effects turned into any instance effects.
       * 
       * We have to do this because there is no named constructor being called
       * that can be annotated with the effects of the initialization.  Instead
       * this is the one case where we have to infer effects.  
       */
      return new InstanceInitAction() {
        private final Context oldContext = context;
        private Context newContext = null;
        
        @Override
        public void tryBefore() {
          this.newContext = Context.forACE(
              oldContext, expr, getEnclosingDecl(),
              JavaPromise.getReceiverNodeOrNull(getEnclosingDecl()));
          EffectsVisitor.this.context = this.newContext;
        }
        
        @Override
        public void finallyAfter() {
          EffectsVisitor.this.context = oldContext;
        }
        
        @Override
        public void afterVisit() {
          // (1) getEnclosingDecl() refers to the original enclosing method again
          // (2) context and oldContext are identical at this point
          final MethodCallUtils.EnclosingRefs enclosing = 
            MethodCallUtils.getEnclosingInstanceReferences(
                thisExprBinder, expr,
                superClassDecl,
                context.theReceiverNode, getEnclosingDecl());
          for (final Effect e : newContext.theEffects) {
            final Effect maskedEffect = e.mask(thisExprBinder);
            if (maskedEffect != null
                && !maskedEffect.affectsReceiver(newContext.theReceiverNode)) {
              final Target target = maskedEffect.getTarget();
              if (target instanceof InstanceTarget) {
                final IRNode ref = target.getReference();
                
                final IRNode newRef = enclosing.replace(ref);
                if (newRef != null) {
                  final IRNode objectExpr = thisExprBinder.bindThisExpression(newRef);
                  final Target newTarget = new InstanceTarget(
                      objectExpr, target.getRegion(), new AnonClassEvidence(maskedEffect));
                  elaborateInstanceTarget(
                      context.bcaQuery, thisExprBinder, lockModel.get(),
                      expr, maskedEffect.isRead(), newTarget,
                      lockModel.get().getNeededLock(thisExprBinder, newTarget, expr, !maskedEffect.isRead(), objectExpr),
                      context.theEffects);
                } else {
                  /* 2012-08-24: We have to clean the type to make sure it is not a 
                   * type formal.
                   */
                  IJavaType type = thisExprBinder.getJavaType(ref);
                  if (type instanceof IJavaTypeFormal) {
                    type = TypeUtil.typeFormalToDeclaredClass(
                        thisExprBinder.getTypeEnvironment(), (IJavaTypeFormal) type);
                  }
                  lockModel.get().getNeededLock(thisExprBinder, target, expr, !maskedEffect.isRead(), ref);
                  context.theEffects.add(Effect.effect(
                      expr, maskedEffect.isRead(),
                      new AnyInstanceTarget(
                          (IJavaReferenceType) type, target.getRegion(),
                          new UnknownReferenceConversionEvidence(
                              maskedEffect, ref, (IJavaReferenceType) type)),
                              lockModel.get().getNeededLock(thisExprBinder, target, expr, !maskedEffect.isRead(), ref)));
                }
              } else {
                context.theEffects.add(maskedEffect.changeSource(
                expr, new AnonClassEvidence(maskedEffect)));
              }
            }
          }
        }
      };
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitArrayRefExpression(final IRNode expr) {
      final IRNode array = ArrayRefExpression.getArray(expr);
      final IRNode objectExpr = thisExprBinder.bindThisExpression(array);
      final Target target = new InstanceTarget(objectExpr, INSTANCE_REGION, NoEvidence.INSTANCE);
      final boolean isRead = context.isRead();
      elaborateInstanceTarget(
          context.bcaQuery, thisExprBinder, lockModel.get(), expr, isRead,
          target, lockModel.get().getNeededLock(thisExprBinder, target, expr, !isRead, objectExpr),
          context.theEffects);
      doAcceptForChildren(expr);
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitAssignExpression(final IRNode expr) {
      context.isLHS = true;
      this.doAccept(AssignExpression.getOp1(expr));
      this.doAccept(AssignExpression.getOp2(expr));
      return null;
    }
    
    //----------------------------------------------------------------------

    @Override
    protected InstanceInitAction getConstructorCallInitAction(final IRNode ccall) {
      final Context oldContext = context;
      return new InstanceInitAction() {
        @Override
        public void tryBefore() {
          context = Context.forConstructorCall(oldContext, ccall);
        }
        
        @Override
        public void finallyAfter() {
          context = oldContext;
        }
        
        @Override
        public void afterVisit() {
          // do nothing
        }
      };
    }
    
    //----------------------------------------------------------------------

    @Override
    public Void visitFieldRef(final IRNode expr) {
      final boolean isRead = context.isRead();    
      final IRNode id = thisExprBinder.getBinding(expr);
      
      final IRNode object = FieldRef.getObject(expr);
      final IRegion region = RegionModel.getInstance(id);
      if (!TypeUtil.isJSureFinal(id)) {
        if (TypeUtil.isStatic(id)) {
          final Target target = new ClassTarget(region, NoEvidence.INSTANCE);
          context.theEffects.add(Effect.effect(expr, isRead, target,
              lockModel.get().getNeededLock(thisExprBinder, target, expr, !isRead, object)));
        } else {
          final Target initTarget = new InstanceTarget(
              thisExprBinder.bindThisExpression(object),
              region, NoEvidence.INSTANCE);
          final NeededLock initLock =
              lockModel.get().getNeededLock(thisExprBinder, initTarget, expr, !isRead, object);
          elaborateInstanceTarget(
              context.bcaQuery, thisExprBinder, lockModel.get(),
              expr, isRead, initTarget, initLock, context.theEffects);
        }
      } else { // must be a final field
        /* Check for a lock: the final field may be protected by
         * a @GuardedBy(itself) annotation.  N.B. @GuardedBy(itself) is
         * an intrinsic lock always, so we don't need read/write information.
         */
        context.theEffects.add(
            Effect.empty(expr, new EmptyEvidence(Reason.FINAL_FIELD, id),
                lockModel.get().getNeededLock(binder.getJavaType(object), region, expr, !isRead, object)));
      }
      doAcceptForChildren(expr);
      return null;
    }
       
    //----------------------------------------------------------------------

    @Override
    public Void visitOpAssignExpression(final IRNode expr) {
      context.isLHS = true;
      this.doAccept(OpAssignExpression.getOp1(expr));
      this.doAccept(OpAssignExpression.getOp2(expr));
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitPostDecrementExpression(final IRNode expr) {
      context.isLHS = true;
      this.doAccept(PostDecrementExpression.getOp(expr));
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitPostIncrementExpression(final IRNode expr) {
      context.isLHS = true;
      this.doAccept(PostIncrementExpression.getOp(expr));
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitPreDecrementExpression(final IRNode expr) {
      context.isLHS = true;
      this.doAccept(PreDecrementExpression.getOp(expr));
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitPreIncrementExpression(final IRNode expr) {
      context.isLHS = true;
      this.doAccept(PreIncrementExpression.getOp(expr));
      return null;
    }

    //----------------------------------------------------------------------
    
    @Override
    public Void visitQualifiedThisExpression(final IRNode expr) {
      // Here we are directly fixing the ThisExpression to be the receiver node
      final IRNode outerType =
          thisExprBinder.getBinding(QualifiedThisExpression.getType(expr));
      IRNode qr = JavaPromise.getQualifiedReceiverNodeByName(getEnclosingDecl(), outerType);
      context.theEffects.add(Effect.read(expr, new LocalTarget(qr), Effect.NO_LOCKS));
      return null;
    }

    //----------------------------------------------------------------------

    @Override 
    public Void visitSuperExpression(final IRNode expr) {
      // Here we are directly fixing the ThisExpression to be the receiver node
      context.theEffects.add(
          Effect.read(expr, new LocalTarget(context.theReceiverNode), Effect.NO_LOCKS));
      return null;
    }

    //----------------------------------------------------------------------

    @Override 
    public Void visitThisExpression(final IRNode expr) {
      // Here we are directly fixing the ThisExpression to be the receiver node
      context.theEffects.add(
          Effect.read(expr, new LocalTarget(context.theReceiverNode), Effect.NO_LOCKS));
      return null;
    }
    
    //----------------------------------------------------------------------

    @Override
    public Void visitTypeDeclarationStatement(final IRNode expr) {
      // Don't look inside classes/interfaces declared inside a method
      return null;
    }
    
    //----------------------------------------------------------------------

    @Override
    public Void visitVariableUseExpression(final IRNode expr) {
      final boolean isRead = context.isRead();
      final IRNode id = thisExprBinder.getBinding(expr);
      context.theEffects.add(
          Effect.effect(expr, isRead, new LocalTarget(id), Effect.NO_LOCKS));
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    protected void handleFieldInitialization(
        final IRNode varDecl, final boolean isStatic) {
      if (!TypeUtil.isJSureFinal(varDecl)) {
        if (isStatic) {
          final Target target = new ClassTarget(
              RegionModel.getInstance(varDecl), NoEvidence.INSTANCE);
          final NeededLock lock = lockModel.get().getNeededLock(
              thisExprBinder, target, varDecl, true, context.theReceiverNode);
          context.theEffects.add(Effect.write(varDecl, target, lock));
        } else {
          // First we read the receiver . . .
          context.theEffects.add(
              Effect.read(varDecl, new LocalTarget(context.theReceiverNode), Effect.NO_LOCKS));
          /* . . . then we write the field.  This never needs elaborating
           * because it is not a use expression or a field reference expression
           */
          final Target target = new InstanceTarget(
              thisExprBinder.bindThisExpression(context.theReceiverNode),
              RegionModel.getInstance(varDecl), NoEvidence.INSTANCE);
          final NeededLock lock = lockModel.get().getNeededLock(
              thisExprBinder, target, varDecl, true, context.theReceiverNode);
          context.theEffects.add(Effect.write(varDecl, target, lock));
        }
      } else {
        /* N.B. Don't care about @GuardedBy(itself) here because we aren't 
         * accessing the contents of the object referenced by the field here;
         * we are only initializing the field itself.
         */
      }
      doAcceptForChildren(varDecl);
    }

    @Override
    protected void handleLocalVariableDeclaration(final IRNode varDecl) {
      // Don't worry about initialization of final variables
      if (!TypeUtil.isJSureFinal(varDecl)) { // TODO: Really replace with isEffectivelyFinal()
        /* LOCAL VARIABLE: 'varDecl' is already the declaration of the variable,
         * so we don't have to bind it.
         */
        context.theEffects.add(
            Effect.write(varDecl, new LocalTarget(varDecl), Effect.NO_LOCKS));
      }
      doAcceptForChildren(varDecl);
    }
  }

  
  
  private static class TargetElaborator {
    private final IRNode srcExpr;
    private final boolean needsWrite;
    private final BindingContextAnalysis.Query bcaQuery;
    private final ThisExpressionBinder thisExprBinder;
    private final AnalysisLockModel lockModel;
    
    /**
     * The working set of elaborated targets.  Final answer is this set with
     * {@link #elaborated} removed.
     */
    private final Set<Target> targets = new HashSet<Target>();
    
    /**
     * The targets that were elaborated so that we can remove
     * them at the end.
     */
    private final Set<Target> elaborated = new HashSet<Target>();
    
    /**
     * Map from target to the lock that should be used when creating 
     * an effect from the target.
     */
    private final Map<Target, Set<NeededLock>> lockMap = new HashMap<>();
    
    private boolean used = false;
    
    
    public TargetElaborator(
        final IRNode srcExpr, final boolean needsWrite,
        final BindingContextAnalysis.Query bcaQuery,
        final ThisExpressionBinder thisExprBinder,
        final AnalysisLockModel lockModel) {
      this.srcExpr = srcExpr;
      this.needsWrite = needsWrite;
      this.bcaQuery = bcaQuery;
      this.thisExprBinder = thisExprBinder;
      this.lockModel = lockModel;
    }
    
    private boolean addResult(final Target target, final NeededLock lock) {
      return addResult(target, ImmutableSet.of(lock));
    }
    
    private boolean addResult(final Target target, final Set<NeededLock> locks) {
      if (targets.add(target)) {
        lockMap.put(target, locks);
        return true;
      } else {
        return false;
      }
    }
    
    public void getEffects(
        final IRNode srcExpr, final boolean isRead,
        final Set<Effect> outEffects) {
      for (final Target t : targets) {
        outEffects.add(Effect.effect(srcExpr, isRead, t, lockMap.get(t)));
      }
    }
    
    public void elaborateTarget(
        final Target initTarget, final NeededLock initLock) {
      if (used) {
        throw new IllegalStateException("Target elaborate has already been used");
      } else {
        used = true;
      }
      
      addResult(initTarget, initLock);
      Set<Target> newTargets = new HashSet<Target>(targets);

      // Loop until fixed-point is reached
      while (!newTargets.isEmpty()) {
        final Set<Target> newestTargets = new HashSet<Target>();
        for (final Target t : newTargets) {
          elaborationWorker(t, newestTargets);
        }
        newTargets = newestTargets;
      }
      targets.removeAll(elaborated);
    }
    
    private void elaborationWorker(
        final Target target, final Set<Target> newTargets) {
      if (target instanceof InstanceTarget) {
        final IRNode expr = target.getReference();
        
        /* If the expression is of an @Immutable type, we can ignore the effect.
         */
        final IJavaType jType = thisExprBinder.getJavaType(expr);
        if (jType instanceof IJavaDeclaredType
            && LockRules.isImmutableType(((IJavaDeclaredType) jType).getDeclaration())) {
          /* Ignore the target by simply marking it as elaborated and replacing
           * it with a an empty target.
           */
          addResult(
              new EmptyTarget(new EmptyEvidence(
                  EmptyEvidence.Reason.RECEIVER_IS_IMMUTABLE, target, expr)),
              new NeedsNoLock(srcExpr));
          elaborated.add(target);
        } else {
          // Process the expression further
          final Operator op = JJNode.tree.getOperator(expr);
          /*
           * EffectsVisitor does not generate InstanceTargets whose reference is a
           * ThisExpression, SuperExpression, or QualifiedThisExpression. They have
           * already been canonicalized to ReceiverDeclaration and
           * QualifiedReceiverDeclarations: there is no need to have BCA do it for
           * us.
           */
          if (VariableUseExpression.prototype.includes(op)) {
            elaborateUseExpression(expr, target, newTargets);
          } else if (FieldRef.prototype.includes(op)) {
            elaborateFieldRef(expr, target, newTargets);
          } else if (OpUtil.isNullExpression(expr)) {
            /* Public bug 37: if the actual argument is "null" then we ignore 
             * the effect because there is no object. 
             */
            addResult(
                new EmptyTarget(
                    new EmptyEvidence(Reason.NULL_REFERENCE, target, expr)),
                new NeedsNoLock(srcExpr));
            elaborated.add(target);
          }
        }
      }
    }

    private void elaborateUseExpression(
        final IRNode expr, final Target target, final Set<Target> newTargets) {
      final IRegion region = target.getRegion();
      for (final IRNode n : bcaQuery.getResultFor(expr)) {
        /* Check for externally declared variables.  We might have one of these
         * if we are inside a local or anonymous class.  These variables are
         * final variables or final parameters declared outside of the scope
         * of the local/anonymous class.  We turn them into any-instance 
         * targets based on the declared type of the external variable.
         */
        final Target newTarget;
        if (BindingContext.isExternalVar(n)) {
          /* 2012-08-24: We have to clean the type to make sure it is not a 
           * type formal.
           */
          IJavaType type = thisExprBinder.getJavaType(expr);
          if (type instanceof IJavaTypeFormal) {
            type = TypeUtil.typeFormalToDeclaredClass(
                thisExprBinder.getTypeEnvironment(), (IJavaTypeFormal) type);
          }
          newTarget = new AnyInstanceTarget(
              (IJavaReferenceType) type, region, NoEvidence.INSTANCE);
        } else {
          // BCA already binds receivers to ReceiverDeclaration and QualifiedReceiverDeclaration nodes
          final BCAEvidence evidence = new BCAEvidence(target, expr, n);        
          newTarget = new InstanceTarget(
              thisExprBinder.bindThisExpression(n), region, evidence);
        }

        // NB. BCA doesn't ever change the lock we need to hold
        if (addResult(newTarget, lockMap.get(target))) {
          elaborated.add(target);
          newTargets.add(newTarget);          
        }
      }
    }

    private void elaborateFieldRef(
        final IRNode expr, final Target target, final Set<Target> newTargets) {
      final IRNode fieldID = thisExprBinder.getBinding(expr);

      // If the field is unique we can exploit uniqueness aggregation.
      final Map<IRegion, IRegion> aggregationMap = 
        UniquenessUtils.constructRegionMapping(fieldID);
      if (aggregationMap != null) {
        /* We have the target "x.f.R".  We need to find the region "Q" of the
         * object referenced by 'x' that "R" maps into.  We elaborate to the
         * target "x.Q".  This is straightforward, but we also need to determine
         * what locks we should hold, and that is more complicated because 
         * "R" may have subregions.  (This occurs when "x.f" is used as an
         * actual parameter to a method call and  the target"x.f.R" comes back in an
         * effect of that method call.)  If R doesn't have subregions, then
         * we use the lock needed for "x.Q".  But for each subregion "S" of "R",
         * we need to add the lock needed for "x.T", where "S" maps to "T".  
         */
        final IRegion R = target.getRegion();
        final IRegion Q = UniquenessUtils.getMappedRegion(R.getModel(), aggregationMap);
        final AggregationEvidence evidence = new AggregationEvidence(target, aggregationMap, Q);
        final Target newTarget;
        if (Q.isStatic()) {
          newTarget = new ClassTarget(Q, evidence);
        } else {
          final IRNode newObject = FieldRef.getObject(expr);
          newTarget = new InstanceTarget(
              thisExprBinder.bindThisExpression(newObject), Q, evidence);
        }        

        // Before we add the target to the elaboration list, we need find the locks we need
        final ImmutableSet.Builder<NeededLock> builder = ImmutableSet.<NeededLock>builder();
        boolean addedSubRegion = false;
        for (final Map.Entry<IRegion, IRegion> mapping : aggregationMap.entrySet()) {
          if (R.ancestorOf(mapping.getKey())) {
            final IRegion T = mapping.getValue();
            final Target t;
            if (T.isStatic()) {
              t = new ClassTarget(T, NoEvidence.INSTANCE);
            } else {
              t = new InstanceTarget(
                  thisExprBinder.bindThisExpression(FieldRef.getObject(expr)),
                  T, NoEvidence.INSTANCE); 
            }
            final LockGenerator lockGen = lockModel.getLockGenerator(thisExprBinder, t);
            builder.add(lockGen.getLock(srcExpr, needsWrite, FieldRef.getObject(expr)));
            addedSubRegion = true;
          }
        }
        if (!addedSubRegion) {
          final LockGenerator lockGen = lockModel.getLockGenerator(thisExprBinder, newTarget);
          builder.add(lockGen.getLock(srcExpr, needsWrite, FieldRef.getObject(expr)));
        }
        
        // Elaborate
        if (addResult(newTarget, builder.build())) {
          elaborated.add(target);
          newTargets.add(newTarget);          
        }
      } 
    }
  }

  
  
  public final class Query implements AnalysisQuery<Set<Effect>> {
    private final IRNode flowUnit;
    private final BindingContextAnalysis.Query bcaQuery;
    
    /**
     * Construct a new effects query to be used on a particular flow unit.
     * 
     * @param flowUnit
     *          The method or constructor declaration that encloses the nodes that
     *          we will ultimately visit. This <em>must</em> be a
     *          MethodDeclaration, ConstructorDeclaration, InitDeclaration, or
     *          ClassInitDeclaration node. If the nodes we are going to visit are
     *          inside an instance initializer block or instance field declaration
     *          of a non-anonymous class, then this should be the
     *          ConstructorDeclaration node of the constructor on whose behalf
     *          they are being analyzed. If the nodes we are going to visit are
     *          inside the instance initializer or field declaration of an
     *          anonymous class expression, this should be the InitDeclaration of
     *          the anonymous class.
     * @param query
     *          The BCA query to use. This is needs to have the proper
     *          relationship to <code>flowUnit</code>. In particular, when the
     *          node being analyzed is inside an instance initializer or field
     *          declaration, or is inside an instance initializer or field
     *          declaration of an anonymous class expression, then this should be
     *          the appropriate sub query object. In cases of highly nested
     *          anonymous classes, this should be the appropriate sub-sub-query.
     */
    private Query(final IRNode flowUnit, final BindingContextAnalysis.Query query) {
      this.flowUnit = flowUnit;
      this.bcaQuery = query;
    }
    
    @Override
    public Set<Effect> getResultFor(final IRNode expr) {
      final EffectsVisitor visitor =
          new EffectsVisitor(flowUnit, bcaQuery);
      visitor.doAccept(expr);
      return Collections.unmodifiableSet(visitor.getTheEffects());
    }
  }
}
