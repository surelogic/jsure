package com.surelogic.analysis.effects;

import java.util.HashSet;
import java.util.Set;

import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.InstanceInitAction;
import com.surelogic.analysis.JavaSemanticsVisitor;
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.bca.uwm.BindingContextAnalysis;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.effects.targets.ThisBindingTargetFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
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
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * IVisitor that computes the region effects for an expression.
 */
final class EffectsVisitor extends JavaSemanticsVisitor implements IBinderClient {
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

    
    
    private Context(final Set<Effect> effects, final IRNode rcvr,
        final BindingContextAnalysis.Query query, final boolean lhs) {
      this.theEffects = effects;
      this.theReceiverNode = rcvr;
      this.bcaQuery = query;
      this.isLHS = lhs;
    }

    public static Context forNormalMethod(
        final BindingContextAnalysis.Query query, final IRNode enclosingMethod) {
      return new Context(new HashSet<Effect>(),
          JavaPromise.getReceiverNodeOrNull(enclosingMethod),
          query, false);
    }
    
    public static Context forACE(final Context oldContext, final IRNode anonClassExpr, final IRNode rcvr) {
      return new Context(new HashSet<Effect>(), rcvr,
          oldContext.bcaQuery.getSubAnalysisQuery(anonClassExpr), false);
    }
    
    public static Context forConstructorCall(final Context oldContext, final IRNode ccall) {
      // Purposely alias the effects set
      return new Context(oldContext.theEffects, oldContext.theReceiverNode,
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
  
  
  
  private final RegionModel ARRAY_ELEMENT;

  /**
   * The binder to use.
   */
  private final IBinder binder;

  /**
   * Binding context analysis, used by target elaboration.
   */
//  private final BindingContextAnalysis bca;
  
  private final ThisExpressionBinder thisExprBinder;

  private final TargetFactory targetFactory;
  
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
      final BindingContextAnalysis.Query query) {
    super(false, flowUnit);
    this.binder = b;
    this.thisExprBinder = new EVThisExpressionBinder(b);
    this.targetFactory = new ThisBindingTargetFactory(thisExprBinder);
    this.ARRAY_ELEMENT = RegionModel.getArrayElementRegion();    
    this.context = Context.forNormalMethod(query, flowUnit);
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
  
  private IRNode getBinding(final IRNode node) {
    return this.binder.getBinding(node);
  }

  /**
   * Assumes that the enclosing method/constructor of the call is the
   * method/constructor declaration represented by
   * {@link Context#enclosingMethod enclosing method} of the current
   * {@link #context context.}.
   */
  private Set<Effect> getMethodCallEffects(final IRNode call) {
    return Effects.getMethodCallEffects(context.bcaQuery,
        targetFactory, binder, call, getEnclosingDecl(), false);
  }
  
  @Override
  protected void handleAsMethodCall(final IRNode call) {
    context.addEffects(getMethodCallEffects(call));
  }

  //----------------------------------------------------------------------

//  @Override
//  protected void handleAnonClassExpression(final IRNode expr) {
//    // Get the effects of the evaluating the arguments
//    doAccept(AnonClassExpression.getArgs(expr));
//    // Get the effects of the super-class constructor
//    context.addEffects(getMethodCallEffects(expr));
//  }

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
//              binder.getBinding(AnonClassExpression.getType(expr)),
              context.theReceiverNode, getEnclosingDecl());
        for (final Effect initEffect : newContext.theEffects) {
          if (!(initEffect.isMaskable(binder) || 
              initEffect.affectsReceiver(newContext.theReceiverNode))) {
            final Target target = initEffect.getTarget();
            if (target instanceof InstanceTarget) {
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
              Effects.elaborateInstanceTargetEffects(
                  context.bcaQuery, targetFactory, binder, expr, initEffect.isRead(),
                  newTarget, context.theEffects);
            } else {
              context.addEffect(initEffect.setSource(expr));
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
    Effects.elaborateInstanceTargetEffects(
        context.bcaQuery, targetFactory, binder, expr, isRead,
        targetFactory.createInstanceTarget(array, ARRAY_ELEMENT), context.theEffects);
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

//  @Override
//  protected void handleConstructorCall(final IRNode ccall) {
//    context.addEffects(getMethodCallEffects(ccall));
//    doAcceptForChildren(ccall);
//  }
  
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
        context.addEffect(
            Effect.newEffect(expr, isRead, targetFactory.createClassTarget(id)));
      } else {
        final IRNode obj = FieldRef.getObject(expr);
        // Public bug 37: Skip effects on "null" references
        if (!Effects.isNullExpression(obj)) {
          final Target initTarget = 
            targetFactory.createInstanceTarget(obj, RegionModel.getInstance(id));
          Effects.elaborateInstanceTargetEffects(
              context.bcaQuery, targetFactory, binder, expr, isRead, initTarget, context.theEffects);
        }
      }
    }
    doAcceptForChildren(expr);
    return null;
  }
  
  //----------------------------------------------------------------------

//  @Override 
//  public Void visitMethodCall(final IRNode expr) {
//    context.addEffects(getMethodCallEffects(expr));
//    doAcceptForChildren(expr);
//    return null;
//  }

  //----------------------------------------------------------------------

//  @Override
//  public Void visitNewExpression(final IRNode expr) {
//    context.addEffects(getMethodCallEffects(expr));
//    doAcceptForChildren(expr);
//    return null;
//  }
 
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
    context.addEffect(
        Effect.newRead(expr, targetFactory.createLocalTarget(
            JavaPromise.getQualifiedReceiverNodeByName(getEnclosingDecl(), outerType))));
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
    final IRNode id = getBinding(expr);
    context.addEffect(Effect.newEffect(expr, isRead, targetFactory.createLocalTarget(id)));
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  protected void handleFieldInitialization(
      final IRNode varDecl, final boolean isStatic) {
    if (!TypeUtil.isFinal(varDecl)) {
      if (isStatic) {
        context.addEffect(Effect.newWrite(varDecl, targetFactory.createClassTarget(varDecl)));
      } else {
        context.addEffect(Effect.newRead(varDecl, targetFactory.createLocalTarget(context.theReceiverNode)));
        // This never needs elaborating because it is not a use expression or a field reference expression
        final Target t = targetFactory.createInstanceTarget(context.theReceiverNode, RegionModel.getInstance(varDecl));
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
  
//  @Override
//  protected void handleSimpleEnumConstantDeclaration(final IRNode decl) {
//    // treat as new expression
//    context.addEffects(getMethodCallEffects(decl));
//    // no children
//  }
  
//  @Override
//  protected void handleNormalEnumConstantDeclaration(final IRNode decl) {
//    // treat as new expression
//    context.addEffects(getMethodCallEffects(decl));
//    // Handle the arguments
//    doAcceptForChildren(decl);
//  }

  
//  @Override
//  protected void handleEnumConstantClassDeclaration(final IRNode decl) {
//    // treat as new expression
//    context.addEffects(getMethodCallEffects(decl));
//    // Handle the arguments
//    doAcceptForChildren(EnumConstantClassDeclaration.getArgs(decl));
//  }
}
