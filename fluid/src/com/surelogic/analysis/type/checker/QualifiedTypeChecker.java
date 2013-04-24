package com.surelogic.analysis.type.checker;

import java.util.Iterator;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ArrayRefExpression;
import edu.cmu.cs.fluid.java.operator.AssignExpression;
import edu.cmu.cs.fluid.java.operator.BoxExpression;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.CallInterface.NoArgs;
import edu.cmu.cs.fluid.java.operator.DeclStatement;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.OuterObjectSpecifier;
import edu.cmu.cs.fluid.java.operator.ReturnStatement;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.operator.SynchronizedStatement;
import edu.cmu.cs.fluid.java.operator.ThrowStatement;
import edu.cmu.cs.fluid.java.operator.UnboxExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * Visitor that tries to provide method hooks for semantic situations that 
 * might be interesting when verifying type qualifiers.  This situations are 
 * based on those places where the JLS indicates a statement or expression
 * may generate a particular run-time exception.
 */
public abstract class QualifiedTypeChecker<Q> extends AbstractJavaAnalysisDriver<Q> {
  protected final IBinder binder;

  
  
  protected QualifiedTypeChecker(final IBinder b) {
    super();
    binder = b;
  }

  
  
  // ======================================================================
  // == Conversions
  // ======================================================================

  @Override
  public final Void visitBoxExpression(final IRNode e) {
    /*
     * §5.1.7
     * 
     * A boxing conversion may result in an OutOfMemoryError if a new instance
     * of one of the wrapper classes (Boolean, Byte, Character, Short, Integer,
     * Long, Float, or Double) needs to be allocated and insufficient storage is
     * available.
     */
    doAcceptForChildren(e);
    checkBoxExpression(e, BoxExpression.getOp(e));
    return null;
  }
  
  protected void checkBoxExpression(
      final IRNode boxExpr, final IRNode boxedExpr) {
    // Do nothing
  }
  
  
  
  @Override
  public final Void visitUnboxExpression(final IRNode e) {
    /*
     * §5.1.8
     * 
     * At run-time, unboxing conversion proceeds as follows: …
     * 
     * If r is null, unboxing conversion throws a NullPointerException
     */
    doAcceptForChildren(e);
    checkUnboxExpression(e, UnboxExpression.getOp(e));
    return null;
  }
  
  protected void checkUnboxExpression(
      final IRNode unboxExpr, final IRNode unboxedExpr) {
    // Do nothing
  }

  
  
  // ======================================================================
  // == Declarations
  // ======================================================================

  @Override
  public final void handleFieldDeclaration(final IRNode fd) {
    super.handleFieldDeclaration(fd);
    
    /*
     * §8.3.2
     * 
     * If a field declarator contains a variable initializer, then it has the
     * semantics of an assignment (§15.26) to the declared variable.
     */
    final Iterator<IRNode> varDecls = 
        VariableDeclarators.getVarIterator(FieldDeclaration.getVars(fd));
    while (varDecls.hasNext()) {
      final IRNode vd = varDecls.next();
      checkFieldInitialization(fd, vd);
    }
  }
  
  protected void checkFieldInitialization(
      final IRNode fieldDecl, final IRNode varDecl) {
    // do nothing
  }
  
  
  
  // ======================================================================
  // == Statements
  // ======================================================================

  @Override
  public final Void visitDeclStatement(final IRNode s) {
    super.visitDeclStatement(s);
    
    /*
     * Initializers should be handled as assignment
     */
    final Iterator<IRNode> varDecls =
        VariableDeclarators.getVarIterator(DeclStatement.getVars(s));
    while (varDecls.hasNext()) {
      final IRNode vd = varDecls.next();
      checkVariableInitialization(s, vd);
    }
    
    return null;
  }
  
  protected void checkVariableInitialization(final IRNode declStmt, final IRNode vd) {
    // do nothing
  }
  
  @Override
  public final Void visitReturnStatement(final IRNode s) {
    /*
     * §14.17
     * 
     * The Expression must denote a variable or value of some type T, or a
     * compile-time error occurs.
     * 
     * The type T must be assignable (§5.2) to the declared result type of the
     * method, or a compile-time error occurs.
     */
    doAcceptForChildren(s);
    checkReturnStatement(s, ReturnStatement.getValue(s));
    return null;
  }
  
  protected void checkReturnStatement(
      final IRNode returnStmt, final IRNode valueExpr) {
    // Do nothing
  }
  
  
  
  @Override
  public final Void visitThrowStatement(final IRNode s) {
    /*
     * §14.18
     * 
     * A throw statement first evaluates the Expression. Then:
     * 
     * If evaluation of the Expression completes normally, producing a null
     * value, then an instance V' of class NullPointerException is created and
     * thrown instead of null. The throw statement then completes abruptly, the
     * reason being a throw with value V'.
     */
    doAcceptForChildren(s);
    checkThrowStatement(s, ThrowStatement.getValue(s));
    return null;
  }
  
  protected void checkThrowStatement(
      final IRNode throwStmt, final IRNode thrownExpr) {
    // Do nothing
  }
  
  
  
  @Override
  public final Void visitSynchronizedStatement(final IRNode s) {
    /*
     * §14.19
     * 
     * A synchronized statement is executed by first evaluating the Expression.
     * Then:
     * 
     * If the value of the Expression is null, a NullPointerException is thrown.
     */
    doAcceptForChildren(s);
    checkSynchronizedStatement(s, SynchronizedStatement.getLock(s));
    return null;
  }
  
  protected void checkSynchronizedStatement(
      final IRNode syncStmt, final IRNode lockExpr) {
    // do nothing
  }

  
  
  // ======================================================================
  // == Expressions
  // ======================================================================
  
  @Override
  public final Void visitOuterObjectSpecifier(final IRNode e) {
    /*
     * §15.9.4
     * 
     * if the class instance creation expression is a qualified class instance
     * creation expression, the qualifying primary expression is evaluated. If
     * the qualifying expression evaluates to null, a NullPointerException is
     * raised, and the class instance creation expression completes abruptly.
     * 
     * §8.8.7.1
     * 
     * If the superclass constructor invocation is qualified, then the Primary
     * expression p immediately preceding ".super" is evaluated.
     * 
     * If p evaluates to null, a NullPointerException is raised, and the
     * superclass constructor invocation completes abruptly.
     */
    doAcceptForChildren(e);
    checkOuterObjectSpecifier(
        e, OuterObjectSpecifier.getObject(e), OuterObjectSpecifier.getCall(e));
    return null;
  }
  
  protected void checkOuterObjectSpecifier(final IRNode e,
      final IRNode object, final IRNode call) {
    // do nothing
  }
  
  @Override
  protected final void handleMethodCall(final IRNode e) {
    super.handleMethodCall(e); // made sure handleAsMethodCall() is called
    
    /*
     * §15.12.4.4
     * 
     * Otherwise, an instance method is to be invoked and there is a target
     * reference. If the target reference is null, a NullPointerException is
     * thrown at this point.
     */
    final IRNode methodDecl = binder.getBinding(e);
    if (!TypeUtil.isStatic(methodDecl)) {
      final IRNode target = MethodCall.getObject(e);
      checkMethodTarget(e, methodDecl, target);
    }
  }
  
  protected void checkMethodTarget(final IRNode call,
      final IRNode methodDecl, final IRNode target) {
    // do nothing
  }

  @Override
  protected final void handleAsMethodCall(final IRNode e) {
    /* Here we deal with formal–actual parameter matching for all
     * types of calls.
     */
    try {
      // Get the actuals
      final Operator exprOp = JJNode.tree.getOperator(e);
      final IRNode actuals = ((CallInterface) exprOp).get_Args(e);
  
      // get the formals
      final IRNode decl = binder.getBinding(e);
      final IRNode formals = SomeFunctionDeclaration.getParams(decl); 
      
      checkActualsVsFormals(e, actuals, formals);
    } catch (final NoArgs ex) {
      // No arguments: nothing to check!
    }
  }
  
  protected void checkActualsVsFormals(final IRNode call,
      final IRNode actuals, final IRNode formals) {
    // do nothing
  }
  
  @Override
  public final Void visitArrayCreationExpression(final IRNode e) {
    /*
     * §15.10.1
     * 
     * If there are no dimension expressions, then there must be an array
     * initializer.
     * 
     * A newly allocated array will be initialized with the values provided by
     * the array initializer as described in §10.6.
     * 
     * From §10.6: Each variable initializer must be assignment-compatible
     * (§5.2) with the array's component type, or a compile-time error occurs.
     * 
     * XXX: Not going to worry about checking initalizers yet.  They also
     * come up when checking field declarations and local variable declarations.
     * Checking them won't be necessary until/if we have arrays of @NonNull
     * references.
     *      * 
     * If the value of any DimExpr expression is less than zero, then a
     * NegativeArraySizeException is thrown.
     */
    doAcceptForChildren(e);
    checkArrayCreationExpression(e);
    return null;
  }
  
  protected void checkArrayCreationExpression(final IRNode e) {
    // do nothing
  }
  
  @Override
  public final Void visitFieldRef(final IRNode e) {
    /*
     * §15.11.1
     * 
     * If the field is not static: … If the value of the Primary is null, then a
     * NullPointerException is thrown.
     */
    doAcceptForChildren(e);
    checkFieldRef(e, FieldRef.getObject(e));
    return null;
  }
  
  protected void checkFieldRef(
      final IRNode fieldRefExpr, final IRNode objectExpr) {
    // do nothing
  }
  
  @Override
  public final Void visitArrayRefExpression(final IRNode e) {
    /*
     * §15.13
     * 
     * Otherwise, if the value of the array reference expression is null, then a
     * NullPointerException is thrown.
     * 
     * Otherwise, the value of the array reference expression indeed refers to
     * an array. If the value of the index expression is less than zero, or
     * greater than or equal to the array's length, then an
     * ArrayIndexOutOfBoundsException is thrown.
     */
    doAcceptForChildren(e);
    checkArrayRefExpression(
        e, ArrayRefExpression.getArray(e), ArrayRefExpression.getIndex(e));
    return null;
  }
  
  protected void checkArrayRefExpression(final IRNode arrayRefExpr,
      final IRNode arrayExpr, final IRNode indexExpr) {
    // do nothing
  }
  
  @Override
  public final Void visitDivExpression(final IRNode e) {
    /*
     * §15.17.2
     * 
     * If the value of the divisor in an integer division is 0, then an
     * ArithmeticException is thrown.
     */
    doAcceptForChildren(e);
    checkDivExpression(e);
    return null;
  }
  
  protected void checkDivExpression(final IRNode divExpr) {
    // do nothing
  }
  
  @Override
  public final Void visitRemExpression(final IRNode e) {
    /*
     * §15.17.3
     * 
     * If the value of the divisor for an integer remainder operator is 0, then
     * an ArithmeticException is thrown.
     */
    doAcceptForChildren(e);
    checkRemExpression(e);
    return null;
  }
  
  protected void checkRemExpression(final IRNode remExpr) {
    // do nothing
  }
  
  @Override
  public final Void visitAssignExpression(final IRNode e) {
    /*
     * §15.26.1
     * 
     * A compile-time error occurs if the type of the right-hand operand cannot
     * be converted to the type of the variable by assignment conversion (§5.2).
     * 
     * N.B. Most of this is actually handled by visitFieldRef() and
     * visitArrayRefExpression().
     * 
     * If the left-hand operand is an array access expression (§15.13) then an
     * ArrayStoreException is raised if the run-time type of the object being
     * assigned to the array is a proper supertype of the run-time type of the
     * array element.
     */
    doAcceptForChildren(e);
    checkAssignExpression(
        e, AssignExpression.getOp1(e), AssignExpression.getOp2(e));
    return null;
  }
  
  protected void checkAssignExpression(
      final IRNode assignExpr, final IRNode lhs, final IRNode rhs) {
    // do nothing
  }
}
