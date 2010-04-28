package com.surelogic.analysis.effects;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.bca.uwm.BindingContext;
import com.surelogic.analysis.bca.uwm.BindingContextAnalysis;
import com.surelogic.analysis.effects.targets.DefaultTargetFactory;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.effects.targets.ThisBindingTargetFactory;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.AnalysisQuery;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.CastExpression;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.ParenExpression;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ModuleModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Interface to the region effects analysis.
 */
public final class Effects implements IBinderClient {
  public final class Query implements AnalysisQuery<Set<Effect>> {
    private final IRNode flowUnit;
    
    public Query(final IRNode fu) {
      flowUnit = fu;
    }
    
    public Set<Effect> getResultFor(final IRNode expr) {
      final EffectsVisitor visitor = new EffectsVisitor(binder, bca, flowUnit);
      visitor.doAccept(expr);
      return Collections.unmodifiableSet(visitor.getTheEffects());
    }
  }
  
  
  
  private final IBinder binder;
  private final BindingContextAnalysis bca;
  
  
  
  public Effects(final IBinder binder, final BindingContextAnalysis bca) {
    this.binder = binder;
    this.bca = bca;
  }

  
  
  //----------------------------------------------------------------------
  // -- Utility methods
  //----------------------------------------------------------------------

  private static Effect getWritesAnything(final IRNode effectSrc) {
    final Target anything =
      DefaultTargetFactory.PROTOTYPE.createClassTarget(
          RegionModel.getInstance(RegionModel.ALL));
    return Effect.newWrite(effectSrc, anything);
  }

  
  
  //----------------------------------------------------------------------
  // -- Get the effects of an expression
  //----------------------------------------------------------------------

  public Query getEffectsQuery(final IRNode flowUnit) {
    return new Query(flowUnit);
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
  public static Set<Effect> getDeclaredMethodEffects(
      final IRNode mDecl, final IRNode callSite) {
    // Use the default target factory because we bind the receivers ourselves
    final TargetFactory tf = DefaultTargetFactory.PROTOTYPE;
    
    // Get the effects from the promises
    final RegionEffectsPromiseDrop promisedEffects = MethodEffectsRules.getRegionEffectsDrop(mDecl);
    if (promisedEffects == null) { // No promises, return null
      return null;
    } else {
      final Set<Effect> result = new HashSet<Effect>();
      
      // Convert IRNode representation of effects in Effect objects
      for(final EffectsSpecificationNode effList : promisedEffects.getEffects()) {
        for(final EffectSpecificationNode peff : effList.getEffectList()) {
          final RegionModel region = peff.getRegion().resolveBinding().getModel();
          final boolean isRead = !peff.getIsWrite();
          final ExpressionNode pContext = peff.getContext();
          
          final Target targ;
          if (pContext instanceof ImplicitQualifierNode) {
            if (region.isStatic()) { // Static region -> class target
              targ = tf.createClassTarget(region);
            } else { // Instance region -> qualify with receiver
              // We bind the receiver ourselves, so this is safe
              targ = tf.createInstanceTarget(JavaPromise.getReceiverNode(mDecl), region);
            }
          } else if (pContext instanceof AnyInstanceExpressionNode) {
            final IJavaType type = 
              ((AnyInstanceExpressionNode) pContext).getType().resolveType().getJavaType();
            targ = tf.createAnyInstanceTarget((IJavaReferenceType) type, region);
          } else if (pContext instanceof QualifiedThisExpressionNode) {
            final QualifiedThisExpressionNode qthis =
              (QualifiedThisExpressionNode) pContext;
            final IRNode canonicalReceiver =
              JavaPromise.getQualifiedReceiverNodeByName(mDecl, qthis.resolveType().getNode());
            // We just bound the receiver ourselves, so this is safe
            targ = tf.createInstanceTarget(canonicalReceiver, region);
          } else if (pContext instanceof TypeExpressionNode) {
            targ = tf.createClassTarget(region);
          } else if (pContext instanceof ThisExpressionNode) {
            // We bind the receiver ourselves, so this is safe
            targ = tf.createInstanceTarget(JavaPromise.getReceiverNode(mDecl), region);
          } else if (pContext instanceof VariableUseExpressionNode) {
            // The object expression cannot be a receiver, so this is safe
            targ = tf.createInstanceTarget(((VariableUseExpressionNode) pContext).resolveBinding().getNode(), region);
          } else {
            // Shouldn't happen, but we need to ensure that blank final targ is initialized
            targ = null;
          }
          final Effect eff = Effect.newEffect(callSite, isRead, targ);
          result.add(eff);
        }
      }
      if (result.isEmpty()) {
        result.add(Effect.newEmpty(callSite));
      }
      return Collections.unmodifiableSet(result);
    }
  }
  
  /** Get the declared effects for a method invoked from a particular call-site.
   * @param mCall The IRNode for the call site
   * @param mDecl The IRNode that is the MethodDecl
   * @return Declared effects for cross-module or TheWorld calls, or null for
   * same-non-world-module calls.
   */
  public static Set<Effect> getDeclaredEffectsWM(
      final IRNode mCall, final IRNode mDecl) {
    //if call-site and callee are in different modules, or if either is part of
    // TheWorld we can only depend on the declared effects!
    
    if (!ModuleModel.sameNonWorldModule(mCall, mDecl)) {
      // it's declared effects, or WritesAll!
      return getMethodEffects(mDecl, mCall);
    } else {
      // this module does not apply!
      return null;
    }
  }

  /**
   * Get the declared effects for a method/constructor or
   * <code>writes(All)</code> if no effects are declared.
   * 
   * <P>
   * XXX: This method compensates for unannotated methods.  We still
   * need a system-wide approach to dealing with this.
   * 
   * @param mDecl
   *          a MethodDeclaration or ConstructorDeclaration
   * @param callSite
   *          Node for the effect source. Should be one of MethodCall,
   *          NewExpression, ConstructorCall, MethodDeclaration, or
   *          ConstructorDeclaration.
   */
  public static Set<Effect> getMethodEffects(
      final IRNode mDecl, final IRNode callSite) {
    Set<Effect> effects = getDeclaredMethodEffects(mDecl, callSite);
    if (effects == null) {
      effects = Collections.singleton(getWritesAnything(callSite));
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
  public Set<Effect> getMethodCallEffects(final IRNode call,
      final IRNode caller, final boolean returnRaw) {
	  //createdTEBs++;
	  /*
	   * Changed to lazily compute things, since bindReceiver doesn't get called very often
	   */
    final ThisExpressionBinder teb = new AbstractThisExpressionBinder(binder) {
      private /*final*/ IRNode receiver;// = JavaPromise.getReceiverNodeOrNull(caller);
      private boolean gotReceiver = false;
      
      @Override
      protected IRNode bindReceiver(final IRNode node) {
    	if (!gotReceiver) {
    		gotReceiver = true;
    		receiver = JavaPromise.getReceiverNodeOrNull(caller);
    		//bindReceiver++;
    	}
        return receiver;
      }
      
      @Override
      protected IRNode bindQualifiedReceiver(final IRNode outerType, final IRNode node) {
        return JavaPromise.getQualifiedReceiverNodeByName(caller, outerType);
      }
    };
    return getMethodCallEffects(bca.getExpressionObjectsQuery(caller),
        new ThisBindingTargetFactory(teb), binder, call, caller, returnRaw);
  }
  /*
  static int createdTEBs = 0;
  static int bindReceiver = 0;
  */
  public static void outputStats() {
	  /*
	  System.out.println("Created TEBs   = "+createdTEBs);
	  System.out.println("Bound receiver = "+bindReceiver);
	  */
  }  
  
  /**
   * Get the effects of a specific method/constructor call.  The effects are
   * fully integrated into the context of the caller, that is, region aggregation
   * is taken into account, and BindingContextAnalysis is used to replace
   * uses of local variables in instance targets.  Technically speaking, the
   * effects are properly elaborated.
   * 
   * @param call
   *          The node representing the method/constructor call
   * @param targetFactory
   *          The target factory must insure that ThisExpression or 
   *          QualifiedThisExpression IRNodes that are passed to 
   *          {@link TargetFactory#createInstanceTarget(IRNode, IRegion)} are
   *          property bound.  Currently this means that the targetFactory
   *          had better be an instance of {@link ThisBindingTargetFactory}.
   */
  public static Set<Effect> getMethodCallEffects(
      final BindingContextAnalysis.Query bcaQuery, final TargetFactory targetFactory,
      final IBinder binder, final IRNode call, final IRNode callingMethodDecl) {
    return getMethodCallEffects(
        bcaQuery, targetFactory, binder, call, callingMethodDecl, false);
  }

  /**
   * Get the raw effects of a specific method/constructor call. The effects are
   * <em>not</em> fully integrated into the context of the caller, that is,
   * region aggregation is <em>not</em> taken into account, and uses of local
   * variables in instance targets are kept. Technically speaking, the effects
   * are <em>not</em> elaborated.
   * 
   * @param call
   *          The node representing the method/constructor call
   */
  /* XXX: This method is kept around so that UniqueTransfer.transferCall()
   * works correctly.  I don't know at the moment whether this is really the
   * right thing to do.  If it is, then this method needs to be better
   * integrated with getMethodCallEffects to remove duplicate code.
   */
  public static Set<Effect> getRawMethodCallEffects(
      final TargetFactory targetFactory, final IBinder binder,
      final IRNode call, final IRNode callingMethodDecl) {
    return getMethodCallEffects(
        null, targetFactory, binder, call, callingMethodDecl, true);
  }
  
  /* The bcaQuery needs to be focused to the flow unit represented by callingMethodDecl.
   * It is up to the caller to make sure these values are consistent.  Although
   * we could instead take the bca and force the query to be consistent here,
   * we do not because not doing so allows the query to be cached by the callers
   * and thus not created over and over again for each use.
   */
  // BCA is unused if returnRaw == true
  static Set<Effect> getMethodCallEffects(
      final BindingContextAnalysis.Query bcaQuery, final TargetFactory targetFactory,
      final IBinder binder, final IRNode call, final IRNode callingMethodDecl, 
      final boolean returnRaw) {    
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
          /* Public bug 37: if the actual argument is "null" then we ignore 
           * the effect because there is no object. 
           */
          if (!isNullExpression(val)) {
            final Target newTarg =
              targetFactory.createInstanceTarget(val, t.getRegion());
            if (returnRaw) {
              methodEffects.add(Effect.newEffect(call, eff.isRead(), newTarg));
            } else {
              elaborateInstanceTargetEffects(
                  bcaQuery, targetFactory, binder, call, eff.isRead(),
                  newTarg, methodEffects);
            }
          }
        } else { // See if ref is a QualifiedReceiverDeclaration
          if (QualifiedReceiverDeclaration.prototype.includes(JJNode.tree.getOperator(ref))) {
            final IRNode type = QualifiedReceiverDeclaration.getType(binder, ref);
            final Target newTarg = targetFactory.createAnyInstanceTarget(
                JavaTypeFactory.getMyThisType(type), t.getRegion()); 
            methodEffects.add(Effect.newEffect(call, eff.isRead(), newTarg));
          } else {
            // something went wrong          
            throw new IllegalStateException("Unmappable instance target: " + t);
          }
        }
      } else { // It's an effect on static state, or any instance
        methodEffects.add(eff.setSource(call));
      }
    }
  
    return Collections.unmodifiableSet(methodEffects);
  }

  /**
   * Test if an expression is a NullLiteral.  Unwraps ParenExpression
   * and CastExpressions.
   */
  // Package visible: Allow EffectsVisitor to call this method
  static boolean isNullExpression(final IRNode expr) {
    final Operator op = JJNode.tree.getOperator(expr);
    if (NullLiteral.prototype.includes(op)) {
      return true;
    } else if (ParenExpression.prototype.includes(op)) {
      return isNullExpression(ParenExpression.getOp(expr));
    } else if (CastExpression.prototype.includes(op)) {
      return isNullExpression(CastExpression.getExpr(expr));
    } else {
      return false;
    }
  }

  
  // ----------------------------------------------------------------------
  // Target elaboration methods
  // ----------------------------------------------------------------------
  
  public static Set<Effect> elaborateEffect(
      final BindingContextAnalysis.Query bcaQuery,
      final TargetFactory targetFactory,
      final IBinder binder, final IRNode src, final boolean isRead,
      final Target target) {
    if (target instanceof InstanceTarget) {
      final Set<Effect> elaboratedEffects = new HashSet<Effect>();
      elaborateInstanceTargetEffects(
          bcaQuery, targetFactory, binder, src, isRead, target, elaboratedEffects);
      return Collections.unmodifiableSet(elaboratedEffects);
    } else {
      return Collections.singleton(Effect.newEffect(src, isRead, target));
    }
  }

  static void elaborateInstanceTargetEffects(
      final BindingContextAnalysis.Query bcaQuery,
      final TargetFactory targetFactory,
      final IBinder binder, final IRNode src, final boolean isRead,
      final Target initTarget, final Set<Effect> outEffects) {
    final TargetElaborator te = new TargetElaborator(bcaQuery, targetFactory, binder);
    for (final Target t : te.elaborateTarget(initTarget)) {
      outEffects.add(Effect.newEffect(src, isRead, t));
    }
  }
  
  private static class TargetElaborator {
    private final BindingContextAnalysis.Query bcaQuery;
    private final TargetFactory targetFactory;
    private final IBinder binder;
    /**
     * Keep track of those targets that were elaborated so that we can remove
     * them at the end
     */
    private final Set<Target> elaborated = new HashSet<Target>();
    
    public TargetElaborator(final BindingContextAnalysis.Query bcaQuery,
        final TargetFactory targetFactory, final IBinder binder) {
      this.bcaQuery = bcaQuery;
      this.targetFactory = targetFactory;
      this.binder = binder;
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
              (IJavaReferenceType) type, region);
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
      final IRegion region = target.getRegion();
      final IRNode fieldID = binder.getBinding(expr);
      final boolean isUnique = UniquenessRules.isUnique(fieldID);

      if (isUnique) {
        // The field is unique, see if we can exploit uniqueness aggregation.
        final Map<RegionModel, IRegion> aggregationMap = 
          AggregationUtils.constructRegionMapping(fieldID);
        if (aggregationMap != null) {
          final IRegion newRegion = AggregationUtils.getMappedRegion(region.getModel(), aggregationMap);
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
        }
      }
    }
  }


  
  // ----------------------------------------------------------------------
  // IBinderClient methods
  // ----------------------------------------------------------------------
  
  public void clearCaches() {
    bca.clear();
  }

  public IBinder getBinder() {
    return binder;
  }
}
