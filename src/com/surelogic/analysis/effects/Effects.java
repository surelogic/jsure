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
import com.surelogic.analysis.JavaProjects;
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.bca.BindingContext;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.targets.DefaultTargetFactory;
import com.surelogic.analysis.effects.targets.EmptyTarget.Reason;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.effects.targets.ThisBindingTargetFactory;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;
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
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.NullLiteral;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.ParenExpression;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.promise.QualifiedReceiverDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ReadOnlyPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Interface to the region effects analysis.
 */
public final class Effects implements IBinderClient {
  public final class Query implements AnalysisQuery<Set<Effect>> {
    private final IRNode flowUnit;
    private final BindingContextAnalysis.Query bcaQuery;
    
    // NOTE: JavaDoc taken from the EffectsVisitor constructor
    /**
     * Construct a new effects query to be used on a particular flow unit.
     * 
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
    public Query(final IRNode flowUnit, final BindingContextAnalysis.Query query) {
      this.flowUnit = flowUnit;
      this.bcaQuery = query;
    }
    
    public Set<Effect> getResultFor(final IRNode expr) {
      final EffectsVisitor visitor =
          new EffectsVisitor(binder, flowUnit, bcaQuery, Effects.NullCallback.INSTANCE);
      visitor.doAccept(expr);
      return Collections.unmodifiableSet(visitor.getTheEffects());
    }
  }
  
  
  
  private final IBinder binder;
  private IIRProject allRegionProj = null;
  private RegionModel allRegion;
  
  public Effects(final IBinder binder) {
    this.binder = binder;
  }

  
  
  //----------------------------------------------------------------------
  // -- Utility methods
  //----------------------------------------------------------------------

  private RegionModel getAllRegion(IRNode context) {
	  final IIRProject p = JavaProjects.getEnclosingProject(context);
	  if (p != allRegionProj) {
		  // Update, since it's the wrong project
		  allRegion = RegionModel.getAllRegion(p);
		  allRegionProj = p;
	  }
	  return allRegion;
  }
  
  private Effect getWritesAnything(final IRNode effectSrc) {	  
    final Target anything =
      DefaultTargetFactory.PROTOTYPE.createClassTarget(getAllRegion(effectSrc));
    return Effect.newWrite(effectSrc, anything);
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

  // NOTE: JavaDoc taken from the EffectsVisitor constructor
  /**
   * Construct a new effects query to be used on a particular flow unit.
   * 
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
      final Effects.ElaborationCallback callback) {
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
        if (!e.isMaskable(binder)) newEffects.add(e);
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
    // Get the effects from the promises
    final RegionEffectsPromiseDrop promisedEffects = MethodEffectsRules.getRegionEffectsDrop(mDecl);
    if (promisedEffects == null) { // No promises, return null
      return null;
    } else {
      final List<Effect> result = new ArrayList<Effect>();
      // Convert IRNode representation of effects in Effect objects
      getEffectsFromSpecificationNode(mDecl, promisedEffects.getEffects(), result, callSite);
      if (result.isEmpty()) {
        result.add(Effect.newEmpty(callSite));
      }
      return Collections.unmodifiableList(result);
    }
  }



  public static void getEffectsFromSpecificationNode(final IRNode mDecl,
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
  public List<Effect> getMethodEffects(
      final IRNode mDecl, final IRNode callSite) {
    List<Effect> effects = getDeclaredMethodEffects(mDecl, callSite);
    if (effects == null) {
      effects = Collections.singletonList(getWritesAnything(callSite));
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
    return getMethodCallEffects(
        bcaQuery, new ThisBindingTargetFactory(teb), binder,
        NullCallback.INSTANCE, call, caller);
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
  public Set<Effect> getMethodCallEffects(
      final BindingContextAnalysis.Query bcaQuery, final TargetFactory targetFactory,
      final IBinder binder, final ElaborationCallback callback,
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
          /* Public bug 37: if the actual argument is "null" then we ignore 
           * the effect because there is no object. 
           */
          if (!isNullExpression(val)) {
            final Target newTarg =
              targetFactory.createInstanceTarget(val, t.getRegion());
            elaborateInstanceTargetEffects(
                bcaQuery, targetFactory, binder, call, callback, eff.isRead(),
                newTarg, methodEffects);
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
  
  public Set<Effect> elaborateEffect(
      final BindingContextAnalysis.Query bcaQuery,
      final TargetFactory targetFactory,
      final IBinder binder, final IRNode src, final boolean isRead,
      final Target target) {
    if (target instanceof InstanceTarget) {
      final Set<Effect> elaboratedEffects = new HashSet<Effect>();
      elaborateInstanceTargetEffects(
          bcaQuery, targetFactory, binder, src, NullCallback.INSTANCE, isRead,
          target, elaboratedEffects);
      return Collections.unmodifiableSet(elaboratedEffects);
    } else {
      return Collections.singleton(Effect.newEffect(src, isRead, target));
    }
  }

  void elaborateInstanceTargetEffects(
      final BindingContextAnalysis.Query bcaQuery,
      final TargetFactory targetFactory,
      final IBinder binder, final IRNode src,
      final ElaborationCallback callback, final boolean isRead,
      final Target initTarget, final Set<Effect> outEffects) {
    final TargetElaborator te =
        new TargetElaborator(bcaQuery, targetFactory, binder, callback, !isRead);
    for (final Target t : te.elaborateTarget(initTarget)) {
      outEffects.add(Effect.newEffect(src, isRead, t));
    }
  }
  
  public interface ElaborationCallback {
    public void writeToBorrowedReadOnly(
        ReadOnlyPromiseDrop pd, IRNode expr, Target t);
  }
  
  public static enum NullCallback implements ElaborationCallback {
    INSTANCE;
    
    public void writeToBorrowedReadOnly(
        final ReadOnlyPromiseDrop pd, final IRNode expr, final Target t) {
      // does nothing
    }    
  }
  
  private class TargetElaborator {
    private final BindingContextAnalysis.Query bcaQuery;
    private final TargetFactory targetFactory;
    private final IBinder binder;
    private final boolean isWrite;
    private final ElaborationCallback callback;
    
    /**
     * Keep track of those targets that were elaborated so that we can remove
     * them at the end
     */
    private final Set<Target> elaborated = new HashSet<Target>();
    
    
    
    public TargetElaborator(final BindingContextAnalysis.Query bcaQuery,
        final TargetFactory targetFactory, final IBinder binder,
        final ElaborationCallback c, final boolean write) {
      this.bcaQuery = bcaQuery;
      this.targetFactory = targetFactory;
      this.binder = binder;
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
                targetFactory.createEmptyTarget(
                    target.getElaborationEvidence(),
                    Reason.RECEIVER_IS_IMMUTABLE));
            elaborated.add(target);
          }
          
          /* If the expr is a read only ref, then we replace the target with 
           * a class target on Object:All.
           */
          if (UniquenessRules.isReadOnly(nodeToTest)) {
            targets.add(
                targetFactory.createClassTarget(getAllRegion(expr)));
            elaborated.add(target);
          }
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
            targetFactory.createEmptyTarget(
                target.getElaborationEvidence(), Reason.RECEIVER_IS_IMMUTABLE));
        elaborated.add(target);
      } else if (UniquenessRules.isReadOnly(fieldID)) {
        /* Field is read only: Replace the target with Object:All. */
        targets.add(
            targetFactory.createClassTarget(getAllRegion(expr)));
        elaborated.add(target);
      }
    }
  }


  
  // ----------------------------------------------------------------------
  // IBinderClient methods
  // ----------------------------------------------------------------------
  
  public void clearCaches() {
    // Do nothing
    // bca.clear();
  }

  public IBinder getBinder() {
    return binder;
  }
}
