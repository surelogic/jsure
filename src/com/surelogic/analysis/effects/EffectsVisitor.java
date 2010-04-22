package com.surelogic.analysis.effects;

import java.util.HashSet;
import java.util.Set;

import com.surelogic.analysis.AbstractThisExpressionBinder;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.MethodCallUtils;
import com.surelogic.analysis.ThisExpressionBinder;
import com.surelogic.analysis.bca.uwm.BindingContextAnalysis;
import com.surelogic.analysis.effects.targets.InstanceTarget;
import com.surelogic.analysis.effects.targets.Target;
import com.surelogic.analysis.effects.targets.TargetFactory;
import com.surelogic.analysis.effects.targets.ThisBindingTargetFactory;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.analysis.InstanceInitializationVisitor;
import edu.cmu.cs.fluid.java.analysis.InstanceInitializationVisitor.Action;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
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
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * IVisitor that computes the region effects for an expression.
 */
// XXX: Make this a JavaSemanticsVisitor
final class EffectsVisitor extends VoidTreeWalkVisitor 
implements IBinderClient {
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
     * The method that encloses the current node.  This is either a
     * ConstructorDeclaration, MethodDeclaration, ClassInitDeclaration, or
     * in the case of an anonymous class expression, an InitDeclation.  In 
     * most cases nodes that are part of an instance initializer block or 
     * an instance field declaration are considered to be enclosed by a 
     * particular constructor.  This constructor is specified by the
     * <code>constructorContext</code> argument to the public entry methods
     * of this class.
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
    private /* final */ BindingContextAnalysis.Query bcaQuery;
    
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

    
    
    private Context(final BindingContextAnalysis.Query query,
        final IRNode enclosingMethod) {
      this.theEffects = new HashSet<Effect>();
      this.enclosingMethod = enclosingMethod;
      this.theReceiverNode = JavaPromise.getReceiverNodeOrNull(enclosingMethod);
      this.bcaQuery = query;
      this.isLHS = false;
    }
    
    private Context(final Context oldContext, final IRNode ccall) {
      this.theEffects = oldContext.theEffects; // Purposely alias
      this.enclosingMethod = oldContext.enclosingMethod;
      this.theReceiverNode = oldContext.theReceiverNode;
      this.bcaQuery = oldContext.bcaQuery.getSubAnalysisQuery(ccall);
      this.isLHS = oldContext.isLHS;
    }

    public static Context forNormalMethod(
        final BindingContextAnalysis bca, final IRNode enclosingMethod) {
      return new Context(bca.getExpressionObjectsQuery(enclosingMethod), enclosingMethod);
    }
    
    public static Context forACE(final Context oldContext, final IRNode anonClassExpr) {
      final IRNode enclosingMethod = JavaPromise.getInitMethodOrNull(anonClassExpr);
      return new Context(oldContext.bcaQuery.getSubAnalysisQuery(anonClassExpr), enclosingMethod);
    }
    
    public static Context forConstructorCall(final Context oldContext, final IRNode ccall) {
      return new Context(oldContext, ccall);
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
  private final BindingContextAnalysis bca;
  
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
   */
  public EffectsVisitor(
      final IBinder b, final BindingContextAnalysis bca, final IRNode flowUnit) {
    this.binder = b;
    this.bca = bca;
    this.thisExprBinder = new EVThisExpressionBinder(b);
    this.targetFactory = new ThisBindingTargetFactory(thisExprBinder);
    this.ARRAY_ELEMENT = RegionModel.getInstance(PromiseConstants.REGION_ELEMENT_NAME);    
    this.context = Context.forNormalMethod(bca, flowUnit);
  }
  
  public Set<Effect> getTheEffects() {
    return context.theEffects;
  }
  
  public void clearCaches() {
	  bca.clear();
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
      return JavaPromise.getQualifiedReceiverNodeByName(context.enclosingMethod, outerType);
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
   * Assumes that the enclosing method/constructor of the call is the
   * method/constructor declaration represented by
   * {@link Context#enclosingMethod enclosing method} of the current
   * {@link #context context.}.
   */
  private Set<Effect> getMethodCallEffects(final IRNode call) {
    return Effects.getMethodCallEffects(context.bcaQuery,
        targetFactory, binder, call, context.enclosingMethod, false);
  }


    
  @Override
  public Void visitAnonClassExpression(final IRNode expr) {
    // Get the effects of the evaluating the arguments
    doAccept(AnonClassExpression.getArgs(expr));
    // Get the effects of the super-class constructor
    context.addEffects(getMethodCallEffects(expr));

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

    /* Get the effects of initialization.  Reset the context to the anonymous
     * class.
     */
    final Context oldContext = context;
    final Context newContext = Context.forACE(oldContext, expr);
    InstanceInitializationVisitor.processAnonClassExpression(expr, this,
        new Action() {
          public void tryBefore() {
            context = newContext;
          }
          
          public void finallyAfter() {
            context = oldContext;
          }
        });
        
    final MethodCallUtils.EnclosingRefs enclosing = 
      MethodCallUtils.getEnclosingInstanceReferences(
          binder, thisExprBinder, expr, oldContext.theReceiverNode, oldContext.enclosingMethod);
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
              context.bcaQuery, targetFactory, binder, initEffect.getSource(), initEffect.isRead(),
              newTarget, context.theEffects);
        } else {
          context.addEffect(initEffect);
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

  @Override
  public Void visitClassDeclaration(final IRNode expr) {
    // Class declaration has no effects, do not look inside of it
    return null;
  }
  
  //----------------------------------------------------------------------

  // NEW
  @Override
  public Void visitConstructorCall(final IRNode expr) {
    context.addEffects(getMethodCallEffects(expr));
    doAcceptForChildren(expr);
    
    // Deal with instance initialization if needed
    final Context oldContext = context;
    InstanceInitializationVisitor.processConstructorCall(expr, this,
        new Action() {
          public void tryBefore() {
            context = Context.forConstructorCall(oldContext, expr);
          }
          
          public void finallyAfter() {
            context = oldContext;
          }
        });
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
    final boolean isRead = context.isRead();    
    final IRNode id = binder.getBinding(expr);
    if (!TypeUtil.isFinal(id)) {
      if (TypeUtil.isStatic(id)) {
        context.addEffect(
            Effect.newEffect(expr, isRead, targetFactory.createClassTarget(id)));
      } else {
        final IRNode obj = FieldRef.getObject(expr);
        final Target initTarget = 
          targetFactory.createInstanceTarget(obj, RegionModel.getInstance(id));
        Effects.elaborateInstanceTargetEffects(
            context.bcaQuery, targetFactory, binder, expr, isRead, initTarget, context.theEffects);
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
    context.addEffects(getMethodCallEffects(expr));
    doAcceptForChildren(expr);
    return null;
  }

  //----------------------------------------------------------------------

  @Override
  public Void visitNewExpression(final IRNode expr) {
    context.addEffects(getMethodCallEffects(expr));
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
    context.addEffect(
        Effect.newRead(expr, targetFactory.createLocalTarget(
            JavaPromise.getQualifiedReceiverNodeByName(context.enclosingMethod, outerType))));
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
  public Void visitVariableDeclarator(final IRNode expr) {
    final IRNode init = VariableDeclarator.getInit(expr);
    if (!NoInitialization.prototype.includes(getOperator(init))) {
      // Don't worry about initialization of final variables/fields
      if (!TypeUtil.isFinal(expr)) {
        if (DeclStatement.prototype.includes(getOperator(getParent(getParent(expr))))) {
          /* LOCAL VARIABLE: 'expr' is already the declaration of the variable,
           * so we don't have to bind it.
           */
          context.addEffect(Effect.newWrite(expr, targetFactory.createLocalTarget(expr)));
        } else { // FieldDeclaration
          if (TypeUtil.isStatic(expr)) {
            context.addEffect(Effect.newWrite(expr, targetFactory.createClassTarget(expr)));
          } else {
            context.addEffect(Effect.newRead(expr, targetFactory.createLocalTarget(context.theReceiverNode)));
            // This never needs elaborating because it is not a use expression or a field reference expression
            final Target t = targetFactory.createInstanceTarget(context.theReceiverNode, RegionModel.getInstance(expr));
            context.addEffect(Effect.newWrite(expr, t));
          }
        }
      }
    }
    doAccept(init);
    return null;
  }
}
