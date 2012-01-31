package com.surelogic.analysis.effects;

import java.util.*;

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
import com.surelogic.analysis.InstanceInitAction;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.analysis.JavaSemanticsVisitor;
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.bca.BindingContext;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.targets.AggregationEvidence;
import com.surelogic.analysis.effects.targets.BCAEvidence;
import com.surelogic.analysis.effects.targets.CallEvidence;
import com.surelogic.analysis.effects.targets.DefaultTargetFactory;
import com.surelogic.analysis.effects.targets.EmptyEvidence;
import com.surelogic.analysis.effects.targets.EmptyEvidence.Reason;
import com.surelogic.analysis.effects.targets.AnonClassEvidence;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.IteratorEvidence;
import com.surelogic.analysis.effects.targets.MappedArgumentEvidence;
import com.surelogic.analysis.effects.targets.NoEvidence;
import com.surelogic.analysis.effects.targets.QualifiedReceiverConversionEvidence;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetEvidence;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.effects.targets.ThisBindingTargetFactory;
import com.surelogic.analysis.effects.targets.UnknownReferenceConversionEvidence;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.AnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.NestedEnumDeclaration;
import edu.cmu.cs.fluid.java.operator.OpAssignExpression;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.PostDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PostIncrementExpression;
import edu.cmu.cs.fluid.java.operator.PreDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PreIncrementExpression;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.OpUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ReadOnlyPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Interface to the region effects analysis.
 */
public final class Effects implements IBinderClient {
  private final IBinder binder;
  private IIRProject allRegionProj = null;
  private RegionModel allRegion;
  
  
  
  public Effects(final IBinder binder) {
    this.binder = binder;
  }


  
  // ----------------------------------------------------------------------
  // IBinderClient methods
  // ----------------------------------------------------------------------
  
  public void clearCaches() {
    // Do nothing
  }

  public IBinder getBinder() {
    return binder;
  }

  
  
  //----------------------------------------------------------------------
  // -- Utility methods
  //----------------------------------------------------------------------

  private RegionModel getAllRegion(final IRNode context) {
	  final IIRProject p = JavaProjects.getEnclosingProject(context);
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
   * <code>this.getEffectsQuery(flowUnit, bca.getExpressionObjectsQuery(flowUnit)).getResultsFor(flowUnit)</code>,
   * except that it allows the elaboration callback to be specified.
   * 
   * @param decl A MethodDeclaration or ConstructorDeclaration node.
   * @param bac The BCA analysis to use.
   * @return The effects of the method as implemented, not declared.
   */
  public Set<Effect> getImplementationEffects(
      final IRNode flowUnit, final BindingContextAnalysis bca,
      final Effects.ElaborationErrorCallback callback) {
    final EffectsVisitor visitor = new EffectsVisitor(binder, flowUnit,
        bca.getExpressionObjectsQuery(flowUnit), callback);
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
        result.add(Effect.newEmpty(callSite));
      }
      return Collections.unmodifiableList(result);
    }
  }

  private static void getEffectsFromSpecificationNode(final IRNode mDecl,
      final Iterable<EffectsSpecificationNode> promisedEffects,
      final List<Effect> result, final IRNode callSite) {
    // Use the default target factory because we bind the receivers ourselves
    final TargetFactory tf = DefaultTargetFactory.PROTOTYPE;      
    for(final EffectsSpecificationNode effList : promisedEffects) {
      for(final EffectSpecificationNode peff : effList.getEffectList()) {
        final RegionModel region = peff.getRegion().resolveBinding().getModel();
        final boolean isRead = !peff.getIsWrite();
        final ExpressionNode pContext = peff.getContext();
        
        final Target targ;
        if (pContext instanceof ImplicitQualifierNode) {
          if (region.isStatic()) { // Static region -> class target
            targ = tf.createClassTarget(region, NoEvidence.INSTANCE);
          } else { // Instance region -> qualify with receiver
            // We bind the receiver ourselves, so this is safe
            targ = tf.createInstanceTarget(
                JavaPromise.getReceiverNode(mDecl), region, NoEvidence.INSTANCE);
          }
        } else if (pContext instanceof AnyInstanceExpressionNode) {
          final IJavaType type = 
            ((AnyInstanceExpressionNode) pContext).getType().resolveType().getJavaType();
          targ = tf.createAnyInstanceTarget(
              (IJavaReferenceType) type, region, NoEvidence.INSTANCE);
        } else if (pContext instanceof QualifiedThisExpressionNode) {
          final QualifiedThisExpressionNode qthis =
            (QualifiedThisExpressionNode) pContext;
          final IRNode canonicalReceiver =
            JavaPromise.getQualifiedReceiverNodeByName(mDecl, qthis.resolveType().getNode());
          // We just bound the receiver ourselves, so this is safe
          targ = tf.createInstanceTarget(
              canonicalReceiver, region, NoEvidence.INSTANCE);
        } else if (pContext instanceof TypeExpressionNode) {
          targ = tf.createClassTarget(region, NoEvidence.INSTANCE);
        } else if (pContext instanceof ThisExpressionNode) {
          // We bind the receiver ourselves, so this is safe
          targ = tf.createInstanceTarget(
              JavaPromise.getReceiverNode(mDecl), region, NoEvidence.INSTANCE);
        } else if (pContext instanceof VariableUseExpressionNode) {
          // The object expression cannot be a receiver, so this is safe
          targ = tf.createInstanceTarget(
              ((VariableUseExpressionNode) pContext).resolveBinding().getNode(),
              region, NoEvidence.INSTANCE);
        } else {
          // Shouldn't happen, but we need to ensure that blank final targ is initialized
          targ = null;
        }
        final Effect eff = Effect.newEffect(callSite, isRead, targ);
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
      final Target anything = DefaultTargetFactory.PROTOTYPE.createClassTarget(
          getAllRegion(callSite), NoEvidence.INSTANCE);
      effects = Collections.singletonList(Effect.newWrite(callSite, anything));
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
   * @param returnRaw
   *          Whether the raw effects should be return. If so, then effects are
   *          not elaborated. Normally this should be <code>false</code>.
   * @return An unmodifiable set of effects.
   */
  public Set<Effect> getMethodCallEffects(
      final BindingContextAnalysis.Query bcaQuery,
      final IRNode call, final IRNode caller) {
    /* Used to compute the receiver lazily, but now we need it ahead of time
     * because we have to pass the receiver to getMethodCallEffects directly.
     */
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
    return getMethodCallEffects(null,
        bcaQuery, new ThisBindingTargetFactory(teb), binder, rcvr,
        ElaborationErrorCallback.NullCallback.INSTANCE, call, caller);
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
  public Set<Effect> getMethodCallEffects(final IRNode specialCaseIterator,
      final BindingContextAnalysis.Query bcaQuery, final TargetFactory targetFactory,
      final IBinder binder, final IRNode rcvr, final ElaborationErrorCallback callback,
      final IRNode call, final IRNode callingMethodDecl) {
    // Get the node of the method/constructor declaration
    final IRNode mdecl = binder.getBinding(call);
    if (mdecl == null) {
    	return Collections.emptySet();
    }
    final Operator op = JJNode.tree.getOperator(mdecl);
  
    // Don't process pseudo-method calls that make up Java 5 annotations
    if (AnnotationElement.prototype.includes(op)) {
      return Collections.emptySet();
    }
  
    final Map<IRNode, IRNode> table =
      MethodCallUtils.constructFormalToActualMap(binder, call, mdecl, callingMethodDecl);
    
    // === Step 2: Instantiate the declared effects based on the substitution map
    
    // go through list and replace each effect on p with effects on table[p]
    final Set<Effect> methodEffects = new HashSet<Effect>();
    for (final Effect eff : getMethodEffects(mdecl, call)) {
      final Target t = eff.getTarget();
      if (t instanceof InstanceTarget) {
        final IRNode ref = t.getReference();
        final IRNode val = table.get(ref);
        if (val != null) {
          boolean specialCase = false;
          
          if ((specialCaseIterator != null) && eff.isWrite()) {
            if (ReceiverDeclaration.prototype.includes(ref)) {
              if (t.getRegion().equals(RegionModel.getInstanceRegion(callingMethodDecl))) {
                // found "writes this:Instance" --> convert to read
                specialCase = true;
              }
            }
          }
          
          TargetEvidence ev = new MappedArgumentEvidence(mdecl, ref, val);
          if (specialCase) {
            ev = new IteratorEvidence(specialCaseIterator, ev);
          }
          final Target newTarg = 
              targetFactory.createInstanceTarget(val, t.getRegion(), ev);
          elaborateInstanceTargetEffects(
              bcaQuery, targetFactory, binder, rcvr, call, callback, specialCase ? true : eff.isRead(),
              newTarg, methodEffects);
        } else { // See if ref is a QualifiedReceiverDeclaration
          if (QualifiedReceiverDeclaration.prototype.includes(JJNode.tree.getOperator(ref))) {
            final IRNode type = QualifiedReceiverDeclaration.getType(binder, ref);
            final IJavaReferenceType javaType = JavaTypeFactory.getMyThisType(type);
            final Target newTarg = targetFactory.createAnyInstanceTarget(
                javaType, t.getRegion(),
                new QualifiedReceiverConversionEvidence(mdecl, ref, javaType)); 
            methodEffects.add(Effect.newEffect(call, eff.isRead(), newTarg));
          } else {
            // something went wrong          
            throw new IllegalStateException("Unmappable instance target: " + t);
          }
        }
      } else { // It's an effect on static state, or any instance
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
  public Set<Effect> elaborateEffect(
      final BindingContextAnalysis.Query bcaQuery,
      final TargetFactory targetFactory, final IBinder binder, 
      final IRNode rcvr, final IRNode src, final boolean isRead,
      final Target target) {
    if (target instanceof InstanceTarget) {
      final Set<Effect> elaboratedEffects = new HashSet<Effect>();
      elaborateInstanceTargetEffects(
          bcaQuery, targetFactory, binder, rcvr, src,
          ElaborationErrorCallback.NullCallback.INSTANCE, isRead,
          target, elaboratedEffects);
      return Collections.unmodifiableSet(elaboratedEffects);
    } else {
      return Collections.singleton(Effect.newEffect(src, isRead, target));
    }
  }

  private void elaborateInstanceTargetEffects(
      final BindingContextAnalysis.Query bcaQuery,
      final TargetFactory targetFactory,
      final IBinder binder, final IRNode rcvr, final IRNode src,
      final ElaborationErrorCallback callback, final boolean isRead,
      final Target initTarget, final Set<Effect> outEffects) {
    final TargetElaborator te =
        new TargetElaborator(bcaQuery, targetFactory, binder, rcvr, callback, !isRead);
    for (final Target t : te.elaborateTarget(initTarget)) {
      outEffects.add(Effect.newEffect(src, isRead, t));
    }
  }
  
  
  
  // ----------------------------------------------------------------------
  // Nested types
  // ----------------------------------------------------------------------
  
  private final static class EffectsVisitor extends JavaSemanticsVisitor implements IBinderClient {
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
      
      
      
      public void setLHS() {
        isLHS = true;
      }
      
      public boolean isRead() {
        final boolean isRead = !this.isLHS;
        this.isLHS = false;
        return isRead;
      }
      
      public void addEffect(final Effect effect) {
        theEffects.add(effect);
      }
      
      public void addEffects(final Set<Effect> effects) {
        theEffects.addAll(effects);
      }
    }
    
    
    
    private final RegionModel INSTANCE_REGION;

    private final Effects.ElaborationErrorCallback callback;
    
    /**
     * The binder to use.
     */
    private final IBinder binder;

    private final ThisExpressionBinder thisExprBinder;

    private final TargetFactory targetFactory;
    private final Effects effects;
    
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
    public EffectsVisitor(final IBinder b, final IRNode flowUnit,
        final BindingContextAnalysis.Query query, final Effects.ElaborationErrorCallback cb) {
      super(false, flowUnit);
      this.callback = cb;
      this.binder = b;
      this.thisExprBinder = new EVThisExpressionBinder(b);
      this.targetFactory = new ThisBindingTargetFactory(thisExprBinder);
      this.INSTANCE_REGION = RegionModel.getInstanceRegion(flowUnit);    
      this.context = Context.forNormalMethod(query, flowUnit);
      this.effects = new Effects(b);
    }

    
    
    public Set<Effect> getTheEffects() {
      return context.theEffects;
    }
    
    public void clearCaches() {
      // Do nothing
    }
    
    public IBinder getBinder() {
      return binder;
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
      /* If the called method is iterator(), then see if the returned value
       * is ever used to invoke remove().  If not, we can convert the 
       * declared writes("this:Instance") effect to reads("this:Instance").
       */
      boolean convertWritesThisInstanceToRead = false;      
      if (isSpecialIteratorMethod(call)) {
        boolean writtenTo = false;
        final Set<IRNode> calls = getMethodCallsUsingAsReceiver(call);
        for (final IRNode c : calls) {
          if (isMutatingIteratorMethod(c)) {
            writtenTo = true;
          }
        }
        convertWritesThisInstanceToRead = !writtenTo;
      }
      
      /* Assumes that the enclosing method/constructor of the call is the
       * method/constructor declaration represented by
       * {@link Context#enclosingMethod enclosing method} of the current
       * {@link #context context.}.
       */
      context.addEffects(
          effects.getMethodCallEffects(
              convertWritesThisInstanceToRead ? call : null,
              context.bcaQuery, targetFactory, binder, context.theReceiverNode, 
              callback, call, getEnclosingDecl()));
    }

    private boolean isSpecialIteratorMethod(final IRNode call) {
      return isCallOfMethod(call, "java.lang.Iterable", "iterator");
    }
    
    private boolean isMutatingIteratorMethod(final IRNode call) {
      return isCallOfMethod(call, "java.util.Iterator", "remove");
//      final String name = MethodDeclaration.getId(binder.getBinding(call));
//      return name.equals("remove");
    }
    
    // Assumption: b is the binding for a method declaration
    private boolean isDeclarationInType(final IBinding b, final String cName) {
      final IJavaType erased =
          binder.getTypeEnvironment().computeErasure(b.getContextType());
      return erased.getName().equals(cName);
    }
    
    private boolean isCallOfMethod(
        final IRNode mcall, final String cName, final String mName) {
      final IBinding ib = binder.getIBinding(mcall);
      if (MethodDeclaration.prototype.includes(ib.getNode())
          && MethodCall.getMethod(mcall).equals(mName)) {
        // First check the declaration of the method being called...
        if (isDeclarationInType(ib, cName)) {
          return true;
        } else {
          //...then check the declarations of any overridden declarations
          for (final IBinding ancestor : binder.findOverriddenMethods(ib.getNode())) {
            if (isDeclarationInType(ancestor, cName)) {
              return true;
            }
          }
        }
      }
      return false;
    }
    
    private Set<IRNode> getMethodCallsUsingAsReceiver(final IRNode call) {
      class Finder extends JavaSemanticsVisitor {
        private final Set<IRNode> calls = new HashSet<IRNode>();
        
        public Finder() {
          super(false, context.enclosingMethod);
        }
        
        public Set<IRNode> getCalls() {
          return calls;
        }
        
        @Override
        public Void visitVariableUseExpression(final IRNode use) {
          final IRNode parent = JJNode.tree.getParent(use);
          if (MethodCall.prototype.includes(parent)
              && MethodCall.getObject(parent).equals(use)) {
            for (final IRNode origin : context.bcaQuery.getResultFor(use)) {
              if (origin.equals(call)) {
                calls.add(parent);
                break;
              }
            }
          }
          return null;
        }
      }
      
      final Finder f = new Finder();
      f.doAccept(context.enclosingMethod);
      return f.getCalls();
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
        superClassDecl = binder.getBinding(AnonClassExpression.getType(expr));
      } else {
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
        
        public void tryBefore() {
          this.newContext = Context.forACE(oldContext, expr,
              getEnclosingDecl(),
              JavaPromise.getReceiverNodeOrNull(getEnclosingDecl()));
          EffectsVisitor.this.context = this.newContext;
        }
        
        public void finallyAfter() {
          EffectsVisitor.this.context = oldContext;
        }
        
        public void afterVisit() {
          // (1) getEnclosingDecl() refers to the original enclosing method again
          // (2) context and oldContext are identical at this point
          final MethodCallUtils.EnclosingRefs enclosing = 
            MethodCallUtils.getEnclosingInstanceReferences(
                binder, thisExprBinder, expr,
                superClassDecl,
                context.theReceiverNode, getEnclosingDecl());
          for (final Effect e : newContext.theEffects) {
            final Effect maskedEffect = e.mask(binder);
            if (maskedEffect != null
                && !maskedEffect.affectsReceiver(newContext.theReceiverNode)) {
              final Target target = maskedEffect.getTarget();
              if (target instanceof InstanceTarget) {
                final IRNode ref = target.getReference();
                
                final IRNode newRef = enclosing.replace(ref);
                if (newRef != null) {
                  effects.elaborateInstanceTargetEffects(
                      context.bcaQuery, targetFactory, binder, 
                      newContext.theReceiverNode, expr, 
                      callback, maskedEffect.isRead(), 
                      targetFactory.createInstanceTarget(
                          newRef, target.getRegion(), 
                          new AnonClassEvidence(maskedEffect)),
                      context.theEffects);
                } else {
                  final IJavaType type = binder.getJavaType(ref);
                  context.addEffect(Effect.newEffect(expr, maskedEffect.isRead(),
                      targetFactory.createAnyInstanceTarget(
                          (IJavaReferenceType) type, target.getRegion(), 
                          new UnknownReferenceConversionEvidence(maskedEffect, ref, (IJavaReferenceType) type))));
                }
              } else {
                context.addEffect(
                    maskedEffect.changeSource(
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
      final boolean isRead = context.isRead();
      effects.elaborateInstanceTargetEffects(
          context.bcaQuery, targetFactory, binder, context.theReceiverNode,
          expr, callback, isRead, targetFactory.createInstanceTarget(
              array, INSTANCE_REGION, NoEvidence.INSTANCE),              
          context.theEffects);
      doAcceptForChildren(expr);
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitAssignExpression(final IRNode expr) {
      context.setLHS();
      this.doAccept(AssignExpression.getOp1(expr));
      this.doAccept(AssignExpression.getOp2(expr));
      return null;
    }
    
    //----------------------------------------------------------------------

    @Override
    protected InstanceInitAction getConstructorCallInitAction(final IRNode ccall) {
      final Context oldContext = context;
      return new InstanceInitAction() {
        public void tryBefore() {
          context = Context.forConstructorCall(oldContext, ccall);
        }
        
        public void finallyAfter() {
          context = oldContext;
        }
        
        public void afterVisit() {
          // do nothing
        }
      };
    }
    
    //----------------------------------------------------------------------

    @Override
    public Void visitFieldRef(final IRNode expr) {
      final boolean isRead = context.isRead();    
      final IRNode id = binder.getBinding(expr);
      if (!TypeUtil.isFinal(id)) {
        if (TypeUtil.isStatic(id)) {
          context.addEffect(Effect.newEffect(expr, isRead,
              targetFactory.createClassTarget(RegionModel.getInstance(id), NoEvidence.INSTANCE)));
        } else {
          final IRNode obj = FieldRef.getObject(expr);
          final Target initTarget = targetFactory.createInstanceTarget(
              obj, RegionModel.getInstance(id), NoEvidence.INSTANCE);
          effects.elaborateInstanceTargetEffects(
              context.bcaQuery, targetFactory, binder, context.theReceiverNode,
              expr, callback, isRead, initTarget, context.theEffects);
        }
      } else {
        context.addEffect(
            Effect.newEffect(expr, isRead, 
                targetFactory.createEmptyTarget(new EmptyEvidence(
                    EmptyEvidence.Reason.FINAL_FIELD, null, id))));
      }
      doAcceptForChildren(expr);
      return null;
    }
    
    //----------------------------------------------------------------------

    @Override
    public Void visitOpAssignExpression(final IRNode expr) {
      context.setLHS();
      this.doAccept(OpAssignExpression.getOp1(expr));
      this.doAccept(OpAssignExpression.getOp2(expr));
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitPostDecrementExpression(final IRNode expr) {
      context.setLHS();
      this.doAccept(PostDecrementExpression.getOp(expr));
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitPostIncrementExpression(final IRNode expr) {
      context.setLHS();
      this.doAccept(PostIncrementExpression.getOp(expr));
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitPreDecrementExpression(final IRNode expr) {
      context.setLHS();
      this.doAccept(PreDecrementExpression.getOp(expr));
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    public Void visitPreIncrementExpression(final IRNode expr) {
      context.setLHS();
      this.doAccept(PreIncrementExpression.getOp(expr));
      return null;
    }

    //----------------------------------------------------------------------
    
    @Override
    public Void visitQualifiedThisExpression(final IRNode expr) {
      // Here we are directly fixing the ThisExpression to be the receiver node
      final IRNode outerType =
        binder.getBinding(QualifiedThisExpression.getType(expr));
      IRNode qr = JavaPromise.getQualifiedReceiverNodeByName(getEnclosingDecl(), outerType);
      if (qr == null) {
        JavaPromise.getQualifiedReceiverNodeByName(getEnclosingDecl(), outerType);
      }
      context.addEffect(Effect.newRead(expr, targetFactory.createLocalTarget(qr)));
      return null;
    }

    //----------------------------------------------------------------------

    @Override 
    public Void visitSuperExpression(final IRNode expr) {
      // Here we are directly fixing the ThisExpression to be the receiver node
      context.addEffect(Effect.newRead(expr, targetFactory.createLocalTarget(context.theReceiverNode)));
      return null;
    }

    //----------------------------------------------------------------------

    @Override 
    public Void visitThisExpression(final IRNode expr) {
      // Here we are directly fixing the ThisExpression to be the receiver node
      context.addEffect(Effect.newRead(expr, targetFactory.createLocalTarget(context.theReceiverNode)));
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
      final IRNode id = binder.getBinding(expr);
      context.addEffect(Effect.newEffect(expr, isRead, targetFactory.createLocalTarget(id)));
      return null;
    }

    //----------------------------------------------------------------------

    @Override
    protected void handleFieldInitialization(
        final IRNode varDecl, final boolean isStatic) {
      if (!TypeUtil.isFinal(varDecl)) {
        if (isStatic) {
          context.addEffect(Effect.newWrite(varDecl, 
              targetFactory.createClassTarget(
                  RegionModel.getInstance(varDecl), NoEvidence.INSTANCE)));
        } else {
          context.addEffect(Effect.newRead(varDecl, targetFactory.createLocalTarget(context.theReceiverNode)));
          // This never needs elaborating because it is not a use expression or a field reference expression
          final Target t = targetFactory.createInstanceTarget(context.theReceiverNode, RegionModel.getInstance(varDecl), NoEvidence.INSTANCE);
          context.addEffect(Effect.newWrite(varDecl, t));
        }
      }
      doAcceptForChildren(varDecl);
    }

    @Override
    protected void handleLocalVariableDeclaration(final IRNode varDecl) {
      // Don't worry about initialization of final variables
      if (!TypeUtil.isFinal(varDecl)) {
        /* LOCAL VARIABLE: 'varDecl' is already the declaration of the variable,
         * so we don't have to bind it.
         */
        context.addEffect(Effect.newWrite(varDecl, targetFactory.createLocalTarget(varDecl)));
      }
      doAcceptForChildren(varDecl);
    }
  }

  
  
  public interface ElaborationErrorCallback {
    public static enum NullCallback implements ElaborationErrorCallback {
      INSTANCE;
      
      public void writeToBorrowedReadOnly(
          final ReadOnlyPromiseDrop pd, final IRNode expr, final Target t) {
        // does nothing
      }    
    }

    public void writeToBorrowedReadOnly(
        ReadOnlyPromiseDrop pd, IRNode expr, Target t);
  }
  
  
  
  private class TargetElaborator {
    private final BindingContextAnalysis.Query bcaQuery;
    private final TargetFactory targetFactory;
    private final IBinder binder;
    private final IRNode theReceiver;
    private final boolean isWrite;
    private final ElaborationErrorCallback callback;
    
    /**
     * Keep track of those targets that were elaborated so that we can remove
     * them at the end
     */
    private final Set<Target> elaborated = new HashSet<Target>();
    
    
    
    public TargetElaborator(final BindingContextAnalysis.Query bcaQuery,
        final TargetFactory targetFactory, final IBinder binder,
        final IRNode rcvr, final ElaborationErrorCallback c,
        final boolean write) {
      this.bcaQuery = bcaQuery;
      this.targetFactory = targetFactory;
      this.binder = binder;
      this.theReceiver = rcvr;
      this.callback = c;
      this.isWrite = write;
    }
    
    public Set<Target> elaborateTarget(final Target initTarget) {
      final Set<Target> targets = new HashSet<Target>();
      targets.add(initTarget);
      Set<Target> newTargets = new HashSet<Target>(targets);

      // Loop until fixed-point is reached
      while (!newTargets.isEmpty()) {
        final Set<Target> newestTargets = new HashSet<Target>();
        for (final Target t : newTargets) {
          elaborationWorker(t, targets, newestTargets);
        }
        newTargets = newestTargets;
      }
      targets.removeAll(elaborated);
      return targets;
    }
    
    private void elaborationWorker(final Target target,
        final Set<Target> targets, final Set<Target> newTargets) {
      if (target instanceof InstanceTarget) {
        final IRNode expr = target.getReference();
        final Operator op = JJNode.tree.getOperator(expr);
        /*
         * EffectsVisitor does not generate InstanceTargets whose reference is a
         * ThisExpression, SuperExpression, or QualifiedThisExpression. They have
         * already been canonicalized to ReceiverDeclaration and
         * QualifiedReceiverDeclarations: there is no need to have BCA do it for
         * us.
         */
        if (VariableUseExpression.prototype.includes(op)) {
          elaborateUseExpression(expr, target, targets, newTargets);
        } else if (FieldRef.prototype.includes(op)) {
          elaborateFieldRef(expr, target, targets, newTargets);
        } else if (QualifiedReceiverDeclaration.prototype.includes(op)) {
          elaborateQualifiedRcvrRef(expr, target, targets, newTargets);
        } else if (ParameterDeclaration.prototype.includes(op) ||
            ReceiverDeclaration.prototype.includes(op) ||
            MethodCall.prototype.includes(op)) {
          final IRNode nodeToTest = MethodCall.prototype.includes(op) ?
              JavaPromise.getReturnNodeOrNull(binder.getBinding(expr)) : expr;
              
          /* If the expr is an immutable ref, then we ignore the target by
           * simply marking it as elaborated and replacing it with a 
           * an empty target.
           */
          if (LockRules.isImmutableRef(nodeToTest)) {
            targets.add(
                targetFactory.createEmptyTarget(new EmptyEvidence(
                    EmptyEvidence.Reason.RECEIVER_IS_IMMUTABLE, target, nodeToTest)));
            elaborated.add(target);
          }
          
          /* If the expr is a read only ref, then we replace the target with 
           * a class target on Object:All.
           */
          if (UniquenessRules.isReadOnly(nodeToTest)) {
            targets.add(
                targetFactory.createClassTarget(getAllRegion(expr), NoEvidence.INSTANCE));
            elaborated.add(target);
          }
        } else if (OpUtil.isNullExpression(expr)) {
          /* Public bug 37: if the actual argument is "null" then we ignore 
           * the effect because there is no object. 
           */
          targets.add(targetFactory.createEmptyTarget(
                  new EmptyEvidence(Reason.NULL_REFERENCE, target, expr)));
          elaborated.add(target);
        }
      }
    }

    private void elaborateUseExpression(
        final IRNode expr, final Target target, final Set<Target> targets,
        final Set<Target> newTargets) {
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
          final IJavaType type = binder.getJavaType(expr);
          newTarget = targetFactory.createAnyInstanceTarget(
              (IJavaReferenceType) type, region, NoEvidence.INSTANCE);
        } else {
          // BCA already binds receivers to ReceiverDeclaration and QualifiedReceiverDeclaration nodes
          final BCAEvidence evidence = new BCAEvidence(target, expr, n);        
          newTarget = targetFactory.createInstanceTarget(n, region, evidence);
        }
        if (targets.add(newTarget)) {
          elaborated.add(target);
          newTargets.add(newTarget);          
        }
      }
    }

    private void elaborateFieldRef(
        final IRNode expr, final Target target, final Set<Target> targets,
        final Set<Target> newTargets) {
      final IRNode fieldID = binder.getBinding(expr);

      // If the field is unique or borrowed we can exploit uniqueness aggregation.
      final Map<IRegion, IRegion> aggregationMap = 
        UniquenessUtils.constructRegionMapping(fieldID);
      if (aggregationMap != null) {
        /* If the field is also @ReadOnly, then we have a @Borrowed @ReadOnly
         * field.  We knows it's not @Unique @ReadOnly because that combination
         * is rejected by sanity checking.
         */
        final ReadOnlyPromiseDrop readOnlyPD = UniquenessRules.getReadOnly(fieldID);
        if (readOnlyPD != null && isWrite) {
          /* ILLEGAL: Cannot be checked for in the uniqueness flow analysis
           * because there is no way to model borrowed & read-only.
           */
          callback.writeToBorrowedReadOnly(readOnlyPD, expr, target);
        }
        // Aggregate the state
        final IRegion region = target.getRegion();
        final IRegion newRegion = UniquenessUtils.getMappedRegion(region.getModel(), aggregationMap);
        final AggregationEvidence evidence =
          new AggregationEvidence(target, aggregationMap, newRegion);
        final Target newTarget;
        if (newRegion.isStatic()) {
          newTarget = targetFactory.createClassTarget(newRegion, evidence);
        } else {
          final IRNode newObject = FieldRef.getObject(expr);
          // FIX for bug 1284: Need to bind the receiver here!
          newTarget = targetFactory.createInstanceTarget(newObject, newRegion, evidence);
        }        
        if (targets.add(newTarget)) {
          elaborated.add(target);
          newTargets.add(newTarget);
        }
      } else if (LockRules.isImmutableRef(fieldID)) {
        /* Field is immutable: We ignore the effect by marking the target as
         * elaborated so it is removed from the final set of targets, and
         * replace it with a new empty target.
         */
        targets.add(
            targetFactory.createEmptyTarget(new EmptyEvidence(
                EmptyEvidence.Reason.RECEIVER_IS_IMMUTABLE, target, fieldID)));
        elaborated.add(target);
      } else if (UniquenessRules.isReadOnly(fieldID)) {
        /* Field is read only: Replace the target with Object:All. */
        targets.add(
            targetFactory.createClassTarget(getAllRegion(expr), NoEvidence.INSTANCE));
        elaborated.add(target);
      }
    }

    private void elaborateQualifiedRcvrRef(
        final IRNode expr, final Target target, final Set<Target> targets,
        final Set<Target> newTargets) {
      /* target is "C.this:R" 
       * 
       * expr is the QualifiedReceiverDeclaration "C.this" (IFQR), which is 
       * really a pseudo field of the form this.$QualifiedReceiver, so we 
       * aggregate into "this".
       */

      /* If the IFQR is borrowed we can exploit aggregation.
       */
      final Map<IRegion, IRegion> aggregationMap = 
          UniquenessUtils.constructRegionMapping(expr);
      if (aggregationMap != null) {
        // Aggregate the state
        final IRegion region = target.getRegion();
        final IRegion newRegion = UniquenessUtils.getMappedRegion(region.getModel(), aggregationMap);
        final AggregationEvidence evidence =
          new AggregationEvidence(target, aggregationMap, newRegion);
        // Use Default target factory because the receiver is already bound
        final Target newTarget =
            DefaultTargetFactory.PROTOTYPE.createInstanceTarget(
                theReceiver, newRegion, evidence);
        if (targets.add(newTarget)) {
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
    
    public Set<Effect> getResultFor(final IRNode expr) {
      final EffectsVisitor visitor =
          new EffectsVisitor(binder, flowUnit, bcaQuery,
              ElaborationErrorCallback.NullCallback.INSTANCE);
      visitor.doAccept(expr);
      return Collections.unmodifiableSet(visitor.getTheEffects());
    }
  }
}
