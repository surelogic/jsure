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
import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.targets.DefaultTargetFactory;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.effects.targets.ThisBindingTargetFactory;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.sea.drops.effects.RegionEffectsPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.InstanceInitVisitor;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.operator.AnnotationElement;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.NoInitialization;
import edu.cmu.cs.fluid.java.operator.OpAssignExpression;
import edu.cmu.cs.fluid.java.operator.PostDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PostIncrementExpression;
import edu.cmu.cs.fluid.java.operator.PreDecrementExpression;
import edu.cmu.cs.fluid.java.operator.PreIncrementExpression;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.PromiseUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.ModuleModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * EffectsWalker re-written as an IVisitor.
 * 
 * <P>TODO: Say more here!
 * 
 * @author aarong
 * 
 */
public final class EffectsVisitor extends VoidTreeWalkVisitor {
  private final RegionModel ARRAY_ELEMENT;

  /**
   * The binder to use.
   */
  private final IBinder binder;

  /**
   * Binding context analysis, used by target elaboration.
   */
  private final BindingContextAnalysis bca;
  
  /**
   * The set of accumulated effects.  This field has a value only when a 
   * traversal is being performed; otherwise it is <code>null</code>.
   */
  private Set<Effect> theEffects = null;
  
  /**
   * This field is checked on entry to an expression to determine if the effect
   * should be a write effect. It is always immediately restored to
   * <code>false</code> after being checked. This isn't the best way of doing
   * things, but I think it is better than the way I used to do it in
   * EffectsWalker.
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
  private boolean isLHS = false;
  
  /**
   * Helper traversal for capturing the effects of field declarations 
   * and instance initializers so that they are included in the effects
   * of constructors.
   */
  private InstanceInitVisitor<Void> initHelper = null;
  
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
  private IRNode theReceiverNode;
  
  /**
   * The enclosing method a la {@link PromiseUtil#getEnclosingMethod(IRNode)}.
   * We keep this around to get QualifiedReceiverDeclaration nodes 
   * in {@link #fixThisExpression(IRNode)}.  This works correctly for 
   * field initializers and instance initializers because 
   * <ul>
   * <li>{@link #getEffects(IRNode)} Can only invoked on a node that inside
   * a method or constructor body.
   * <li>The instance initialization helper reenters this visitor and
   * this field will therefore still be the constructor we are analyzing. 
   * </ul>
   * <p>Thus, qualified receivers used in the initializers will be 
   * canonicalized appropriate to the constructor being analyzed.
   */
  private IRNode enclosingMethod;
  
  
  
  private final ThisExpressionBinder thisExprBinder;
  private final TargetFactory targetFactory;
  
  
  
  //----------------------------------------------------------------------
  
  /**
   * Construct a new Effects Visitor.
   * 
   * @param b
   *          The Binder to use to look up names.
   */
  public EffectsVisitor(final IBinder b, final BindingContextAnalysis bca) {
    this.binder = b;
    this.bca = bca;
    this.thisExprBinder = new EVThisExpressionBinder(b);
    this.targetFactory = new ThisBindingTargetFactory(thisExprBinder);
    this.ARRAY_ELEMENT = RegionModel.getInstance(PromiseConstants.REGION_ELEMENT_NAME);    
  }
  
  
  
  //----------------------------------------------------------------------
  
  private static Effect getWritesAnything(final IRNode effectSrc) {
    final Target anything =
      DefaultTargetFactory.PROTOTYPE.createClassTarget(
          RegionModel.getInstance(RegionModel.ALL));
    return Effect.newWrite(effectSrc, anything);
  }
  
  

  //----------------------------------------------------------------------

  /* All entrances into this class by a public method need to establish
   * the context so that the receiver can be properly bound, etc.  To avoid
   * problems we have one method that does this that is parameterized by a 
   * Runnable-like interface.
   */
  
  private static interface Body {
    public Set<Effect> run(IRNode node);
  }

  private Set<Effect> setUpContextAndRun(final IRNode node, final Body body) {
    try {
      theEffects = new HashSet<Effect>();
      enclosingMethod = PromiseUtil.getEnclosingMethod(node);
      if (enclosingMethod != null) {
        theReceiverNode = JavaPromise.getReceiverNodeOrNull(enclosingMethod);
      } else {
        theReceiverNode = null;
      }
      return body.run(node);
    } finally {
      theEffects = null;
      enclosingMethod = null;
      theReceiverNode = null;
    }
  }

  
  
  //----------------------------------------------------------------------
  // -- Public entrances into this class
  //----------------------------------------------------------------------

  /**
   * Clients should call this method to get the effects of an expression.
   * 
   * @param node
   *          The root node of the expression whose effects should be obtained.
   *          This node should a ClassBodyDeclaration node, or a descendant 
   *          of a ClassBodyDeclarationNode.
   * @return An unmodifiable set of effects.
   */
  public Set<Effect> getEffects(final IRNode node) {
    return setUpContextAndRun(node, new Body() {
      public Set<Effect> run(final IRNode node) {
        doAccept(node);
        final Set<Effect> returnValue = Collections.unmodifiableSet(theEffects);
        return returnValue;
      }
    });
  }

  /**
   * Clients should call this method to get the effects of an expression that
   * appears on the LHS of an assignment.
   * 
   * @param node
   *          The root node of the expression whose effects should be obtained.
   * @return An unmodifiable set of effects.
   */
  public Set<Effect> getLHSEffects(final IRNode node) {
    return setUpContextAndRun(node, new Body() {
      public Set<Effect> run(final IRNode node) {
        getLvalueEffects(node);
        final Set<Effect> returnValue = Collections.unmodifiableSet(theEffects);
        return returnValue;
      }
    });
  }

  /**
   * Clients should call this method to get the effects of executing
   * a specific method/constructor call.
   */
  public Set<Effect> getMethodCallEffects(final IRNode call) {
    return setUpContextAndRun(call, new Body() {
      public Set<Effect> run(final IRNode call) {
        return getMethodCallEffectsInternal(call, enclosingMethod);
      }
    });
  }

  /**
   * Clients should call this method to get the raw effects of executing
   * a specific method/constructor call.
   */
  public Set<Effect> getRawMethodCallEffects(final IRNode call) {
    return setUpContextAndRun(call, new Body() {
      public Set<Effect> run(final IRNode call) {
        return getRawMethodCallEffectsInternal(call, enclosingMethod);
      }
    });
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
      return theReceiverNode;
    }
    
    @Override
    protected IRNode bindQualifiedReceiver(IRNode outerType, IRNode node) {
      return JavaPromise.getQualifiedReceiverNodeByName(enclosingMethod, outerType);
    }    
  }
  
  /**
   * If the given expression is a ThisExpression, then return the
   * ReceiverDeclaration node that represents the receiver in the given context.
   * Similarly for QualifiedThisExpressions. Returns the given node otherwise.
   * 
   * <p>This version is superior {@link PromiseUtil#fixThisExpression} because
   * it avoids crawling back up the parse tree because this class already keeps
   * track of the receiver and enclosing method.
   * 
   * <p>
   * We used to rely on the use of the BindingContextAnalysis during
   * effect/target elaboration to turn ThisExpressions into ReceiverDeclaration
   * nodes. But that is too late: we don't have the correct context information
   * to know that a use of "this" inside of an instance initializer block that
   * is being analyzed on behalf of a constructor declaration (via the
   * InstanceInitVisitor) should be mapped to the ReceiverDeclaration node of
   * the constructor. If we rely on the BindingContextAnalysis during
   * elaboration, the "this" would be instead be converted to the receiver node
   * for initialization, and then not be compatible with the effects gathered
   * from the constructor declaration itself. In particular, this method needs
   * to be used when examining FieldRefs.
   */
  private IRNode fixThisExpression(final IRNode expr) {
    final Operator op = getOperator(expr);
    if (ThisExpression.prototype.includes(op)
        || SuperExpression.prototype.includes(op)) {
      return theReceiverNode;
    } else if (QualifiedThisExpression.prototype.includes(op)) {
      final IRNode outerType =
        binder.getBinding(QualifiedThisExpression.getType(expr));
      return JavaPromise.getQualifiedReceiverNodeByName(enclosingMethod, outerType);
    } else {
      return expr;
    }
  }
  
  private IRNode getBinding(final IRNode node) {
    return this.binder.getBinding(node);
  }
  
  private IRNode getParent(final IRNode node) {
    return JJNode.tree.getParentOrNull(node);
  }

  private Operator getOperator(final IRNode node) {
    return JJNode.tree.getOperator(node);
  }
  
  /**
   * Get the effects of an expression as if it were on the left hand side 
   * of an assignment.
   * 
   * @param lvalue
   *          The node representing the lhs of the assignment expression
   * @exception IllegalArgumentException
   *              Thrown if <code>lvalue</code> is not an ArrayRefExpression,
   *              FieldRef, or VariableUseExpression
   */
  private void getLvalueEffects(final IRNode lvalue) {
    final Operator op = getOperator(lvalue);
    if (ArrayRefExpression.prototype.includes(op)
        || FieldRef.prototype.includes(op)
        || VariableUseExpression.prototype.includes(op)) {
      this.isLHS = true;
      this.doAccept(lvalue);
    } else {
      throw new IllegalArgumentException("Operator " + op.name()
          + " appears on the lhs of an assignment");
    }
  }

  /**
   * Get the declared effects for a method/constructor or <code>null</code> if
   * no effects are declared.
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
    final Set<Effect> result = new HashSet<Effect>();

    if (promisedEffects == null) { // No promises, return null
      return null;
    } else {
      // Convert IRNode representation of effects in Effect objects
      for(final EffectsSpecificationNode effList : promisedEffects.getEffects()) {
        for(final EffectSpecificationNode peff : effList.getEffectList()) {
          final RegionModel region = peff.getRegion().resolveBinding().getModel();
          final boolean isRead = !peff.getIsWrite();
          final ExpressionNode pContext = peff.getContext();
          
          final Target targ;
          if (pContext instanceof AnyInstanceExpressionNode) {
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
    }
    return Collections.unmodifiableSet(result);
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
      final BindingContextAnalysis bca, final TargetFactory targetFactory,
      final IBinder binder, final IRNode call, final IRNode callingMethodDecl) {
    return getMethodCallEffects(
        bca, targetFactory, binder, call, callingMethodDecl, false);
  }

  // BCA is null if isRaw == true
  private static Set<Effect> getMethodCallEffects(
      final BindingContextAnalysis bca, final TargetFactory targetFactory,
      final IBinder binder, final IRNode call, final IRNode callingMethodDecl, 
      final boolean isRaw) {    
    // Get the node of the method/constructor declaration
    final IRNode mdecl = binder.getBinding(call);
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
      if (t.getKind() == Target.Kind.INSTANCE_TARGET) {
        final IRNode ref = t.getReference();
        final IRNode val = table.get(ref);
        if (val != null) {
          final Target newTarg =
            targetFactory.createInstanceTarget(val, t.getRegion());
          if (isRaw) {
            methodEffects.add(Effect.newEffect(call, eff.isReadEffect(), newTarg));
          } else {
            elaborateInstanceTargetEffects(
                bca, targetFactory, binder, call, eff.isReadEffect(),
                newTarg, methodEffects);
          }
        } else { // See if ref is a QualifiedReceiverDeclaration
          if (QualifiedReceiverDeclaration.prototype.includes(JJNode.tree.getOperator(ref))) {
            final IRNode type = QualifiedReceiverDeclaration.getType(binder, ref);
            final Target newTarg = targetFactory.createAnyInstanceTarget(
                JavaTypeFactory.getMyThisType(type), t.getRegion()); 
            methodEffects.add(Effect.newEffect(call, eff.isReadEffect(), newTarg));
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

  private Set<Effect> getMethodCallEffectsInternal(
      final IRNode call, final IRNode callingMethodDecl) {
    return getMethodCallEffects(bca, targetFactory, binder, call, callingMethodDecl);
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

  private Set<Effect> getRawMethodCallEffectsInternal(
      final IRNode call, final IRNode callingMethodDecl) {
    return getRawMethodCallEffects(
        targetFactory, binder, call, callingMethodDecl);
  }


  
  // ----------------------------------------------------------------------
  // Target elaboration methods
  // ----------------------------------------------------------------------
  
  private static void elaborateInstanceTargetEffects(
      final BindingContextAnalysis bca, final TargetFactory targetFactory,
      final IBinder binder, final IRNode src, final boolean isRead,
      final Target initTarget, final Set<Effect> outEffects) {
    final TargetElaborator te = new TargetElaborator(bca, targetFactory, binder);
    for (final Target t : te.elaborateTarget(initTarget)) {
      outEffects.add(Effect.newEffect(src, isRead, t));
    }
  }

  private void elaborateInstanceTargetEffects(
      final IRNode src, final boolean isRead,
      final Target initTarget, final Set<Effect> outEffects) {
    elaborateInstanceTargetEffects(
        bca, targetFactory, binder, src, isRead, initTarget, outEffects);
  }
  
  private static class TargetElaborator {
    private final BindingContextAnalysis bca;
    private final TargetFactory targetFactory;
    private final IBinder binder;
    /**
     * Keep track of those targets that were elaborated so that we can remove
     * them at the end
     */
    private final Set<Target> elaborated = new HashSet<Target>();
    
    public TargetElaborator(final BindingContextAnalysis bca,
        final TargetFactory targetFactory, final IBinder binder) {
      this.bca = bca;
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
      if (target.getKind() == Target.Kind.INSTANCE_TARGET) {
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
      for (final IRNode n : bca.expressionObjects(expr)) {
        // BCA already binds receivers to ReceiverDeclaration and QualifiedReceiverDeclaration nodes
        final Target newTarget = targetFactory.createInstanceTarget(n, region);
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
          final IRNode newObject = FieldRef.getObject(expr);
          final IRegion newRegion = AggregationUtils.getMappedRegion(region.getModel(), aggregationMap);
          final Target newTarget;
          if (newRegion.isStatic()) {
            newTarget = targetFactory.createClassTarget(newRegion);
          } else {
            // FIX for bug 1284: Need to bind the receiver here!
            newTarget = targetFactory.createInstanceTarget(newObject, newRegion);
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
  // Traversal/visitor methods
  // ----------------------------------------------------------------------
  
  @Override
  public Void visitAnonClassExpression(final IRNode expr) {
    // Get the effects of the evaluating the arguments
    doAccept(AnonClassExpression.getArgs(expr));
    // Get the effects of the super-class constructor
    theEffects.addAll(
        getMethodCallEffectsInternal(expr, enclosingMethod));

    /* Need to get the effects of the instance field initializers and the
     * instance initializers of the anonymous class. Effects will come back
     * elaborated. They will then need to be masked (including the special case
     * for constructors, masking out the effects on the receiver of the new
     * object), instantiated based on the current enclosing instances, and then
     * have any "dangling" instance effects turned into any instance effects.
     */

    final IRNode anonClassInitMethod = JavaPromise.getInitMethodOrNull(expr);
    final IRNode anonClassReceiver = JavaPromise.getReceiverNodeOrNull(anonClassInitMethod);
    
    /* Get the effects of initialization.  First we have to make "theRecieverNode"
     * refer to the receiver of the anonymous class, and not that of the current
     * class.
     */
    final IRNode oldReceiver = theReceiverNode;
    final IRNode oldEnclosingMethod = enclosingMethod;
    final Set<Effect> oldEffects = theEffects;
    final Set<Effect> initEffects;
    try {
      enclosingMethod = anonClassInitMethod;
      theReceiverNode = anonClassReceiver;
      theEffects = new HashSet<Effect>();
      final InstanceInitVisitor<Void> initVisitor = new InstanceInitVisitor<Void>(this);
      initVisitor.doAccept(AnonClassExpression.getBody(expr));
      initEffects = theEffects;
    } finally {
      enclosingMethod = oldEnclosingMethod;
      theReceiverNode = oldReceiver;
      theEffects = oldEffects;
    }
    
    final MethodCallUtils.EnclosingRefs enclosing = 
      MethodCallUtils.getEnclosingInstanceReferences(
          binder, thisExprBinder, expr, theReceiverNode, enclosingMethod);
    for (final Effect initEffect : initEffects) {
      if (!(initEffect.isMaskable(binder) || 
          initEffect.affectsReceiver(anonClassReceiver))) {
        final Target target = initEffect.getTarget();
        if (target.getKind() == Target.Kind.INSTANCE_TARGET) {
          final IRNode ref = target.getReference();
          final Target newTarget;
          
          final IRNode newRef = enclosing.replace(ref);
          if (newRef != null) {
            newTarget = targetFactory.createInstanceTarget(
                newRef, target.getRegion());
          } else {
            final IJavaType type = binder.getJavaType(ref);
            newTarget = targetFactory.createAnyInstanceTarget(
                (IJavaReferenceType) type, target.getRegion());
          }
          elaborateInstanceTargetEffects(
              bca, targetFactory, binder, expr, initEffect.isReadEffect(),
              newTarget, theEffects);
        } else {
          theEffects.add(initEffect.setSource(expr));
        }
      }
    }

    // Don't analyze the body of the anonymous class!
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  public Void visitArrayRefExpression(final IRNode expr) {
    final IRNode array = ArrayRefExpression.getArray(expr);
    final boolean isRead = !this.isLHS;
    this.isLHS = false;
    elaborateInstanceTargetEffects(expr, isRead,
        targetFactory.createInstanceTarget(array, ARRAY_ELEMENT), theEffects);
    doAcceptForChildren(expr);
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  public Void visitAssignExpression(final IRNode expr) {
    this.isLHS = true;
    this.doAccept(AssignExpression.getOp1(expr));
    this.doAccept(AssignExpression.getOp2(expr));
    return null;
  }
  
  //----------------------------------------------------------------------

  @Override
  public Void visitClassDeclaration(final IRNode expr) {
    // Class declaration has no effects, do not look inside of it
    return null;
  }
  
  //----------------------------------------------------------------------

  @Override
  public Void visitConstructorCall(final IRNode expr) {
    if (initHelper != null) initHelper.doVisitInstanceInits(expr);
    theEffects.addAll(getMethodCallEffectsInternal(expr, enclosingMethod));
    doAcceptForChildren(expr);
    return null;
  }
  
  //----------------------------------------------------------------------

  @Override
  public Void visitConstructorDeclaration(final IRNode expr) {
    final InstanceInitVisitor<Void> saveInitHelper = initHelper;
    try {
      initHelper = new InstanceInitVisitor<Void>(this);
      // Get the effects from field declarations and initializers
      initHelper.doVisitInstanceInits(expr);
      // Get the rest of the effects
      doAcceptForChildren(expr);
    } finally {
      initHelper = saveInitHelper;
    }
    return null;
  }
  
  //----------------------------------------------------------------------

  @Override
  public Void visitEnumDeclaration(final IRNode expr) {
    // Enumeration declaration has no effects, do not look inside of it
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  public Void visitFieldRef(final IRNode expr) {
    final boolean isRead = !this.isLHS;
    this.isLHS = false;
    
    final IRNode id = binder.getBinding(expr);
    if (!TypeUtil.isFinal(id)) {
      if (TypeUtil.isStatic(id)) {
        theEffects.add(Effect.newEffect(expr, isRead, targetFactory.createClassTarget(id)));
      } else {
        final IRNode obj = FieldRef.getObject(expr);
        final Target initTarget = 
          targetFactory.createInstanceTarget(obj, RegionModel.getInstance(id));
        elaborateInstanceTargetEffects(expr, isRead, initTarget, theEffects);
      }
    }
    doAcceptForChildren(expr);
    return null;
  }
  
  //----------------------------------------------------------------------

  @Override
  public Void visitInterfaceDeclaration(final IRNode expr) {
    // Interface declaration has no effects, do not look inside of it
    return null;
  }

  //----------------------------------------------------------------------

  @Override 
  public Void visitMethodCall(final IRNode expr) {
    theEffects.addAll(getMethodCallEffectsInternal(expr, enclosingMethod));
    doAcceptForChildren(expr);
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  public Void visitNewExpression(final IRNode expr) {
    theEffects.addAll(getMethodCallEffectsInternal(expr, enclosingMethod));
    doAcceptForChildren(expr);
    return null;
  }
 
  //----------------------------------------------------------------------

  @Override
  public Void visitOpAssignExpression(final IRNode expr) {
    this.isLHS = true;
    this.doAccept(OpAssignExpression.getOp1(expr));
    this.doAccept(OpAssignExpression.getOp2(expr));
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  public Void visitPostDecrementExpression(final IRNode expr) {
    this.isLHS = true;
    this.doAccept(PostDecrementExpression.getOp(expr));
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  public Void visitPostIncrementExpression(final IRNode expr) {
    this.isLHS = true;
    this.doAccept(PostIncrementExpression.getOp(expr));
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  public Void visitPreDecrementExpression(final IRNode expr) {
    this.isLHS = true;
    this.doAccept(PreDecrementExpression.getOp(expr));
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  public Void visitPreIncrementExpression(final IRNode expr) {
    this.isLHS = true;
    this.doAccept(PreIncrementExpression.getOp(expr));
    return null;
  }

  //----------------------------------------------------------------------
  
  @Override
  public Void visitQualifiedThisExpression(final IRNode expr) {
    // Here we are directly fixing the ThisExpression to be the receiver node
    final IRNode outerType =
      binder.getBinding(QualifiedThisExpression.getType(expr));
    theEffects.add(Effect.newRead(expr, targetFactory.createLocalTarget(
        JavaPromise.getQualifiedReceiverNodeByName(enclosingMethod, outerType))));
    return null;
  }

  //----------------------------------------------------------------------

  @Override 
  public Void visitSuperExpression(final IRNode expr) {
    // Here we are directly fixing the ThisExpression to be the receiver node
    theEffects.add(Effect.newRead(expr, targetFactory.createLocalTarget(theReceiverNode)));
    return null;
  }

  //----------------------------------------------------------------------

  @Override 
  public Void visitThisExpression(final IRNode expr) {
    // Here we are directly fixing the ThisExpression to be the receiver node
    theEffects.add(Effect.newRead(expr, targetFactory.createLocalTarget(theReceiverNode)));
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
    final boolean isRead = !this.isLHS;
    this.isLHS = false;
    
    final IRNode id = getBinding(expr);
    theEffects.add(Effect.newEffect(expr, isRead, targetFactory.createLocalTarget(id)));
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  public Void visitVariableDeclarator(final IRNode expr) {
    final IRNode init = VariableDeclarator.getInit(expr);
    if (!NoInitialization.prototype.includes(getOperator(init))) {
      // Don't worry about initialization of final variables/fields
      if (!TypeUtil.isFinal(expr)) {
        if (DeclStatement.prototype.includes(getOperator(getParent(getParent(expr))))) {
          /* LOCAL VARIABLE: 'expr' is already the declaration of the variable,
           * so we don't have to bind it.
           */
          theEffects.add(Effect.newWrite(expr, targetFactory.createLocalTarget(expr)));
        } else { // FieldDeclaration
          if (TypeUtil.isStatic(expr)) {
            theEffects.add(Effect.newWrite(expr, targetFactory.createClassTarget(expr)));
          } else {
            theEffects.add(Effect.newRead(expr, targetFactory.createLocalTarget(theReceiverNode)));
            // This never needs elaborating because it is not a use expression or a field reference expression
            final Target t = targetFactory.createInstanceTarget(theReceiverNode, RegionModel.getInstance(expr));
            theEffects.add(Effect.newWrite(expr, t));
          }
        }
      }
    }
    doAccept(init);
    return null;
  }
}
