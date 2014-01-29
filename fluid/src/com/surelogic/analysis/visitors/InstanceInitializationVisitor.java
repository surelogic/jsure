package com.surelogic.analysis.visitors;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.ThisExpression;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

/**
 * A visitor meant to be used in support of another visitor in order to
 * recursively visit the instance field initializers and instance initializer
 * blocks after a super constructor is called.
 * 
 * <p>
 * This class has a private constructor. Instance of it are created and used via
 * the <code>processAnonClassExpression</code> and
 * <code>processConstructorCall</code> methods. As the names imply, there are
 * two situations in which this class should be used, when a ConstructorCall
 * node is visited and when an AnonClassExpression node is visited. In the first
 * case, to model Java evaluation semantics, the ConstructorCall node and its
 * arguments should be processed by the visitor's
 * {@link Visitor#visitConstructorCall(IRNode)} method <em>before</em>
 * <code>processConstructorCall</code> is called. Similarly, the arguments to
 * the anonymous class expression should be processed by the visitor's
 * {@link Visitor#visitAnonClassExpression(IRNode)} method before calling
 * <code>processAnonClassExpression</code>.
 * 
 * <p>
 * When processing constructor calls, the initializers are only visited if the
 * constructor call represents a call to a super constructor. Calls to another
 * constructor in the same class <em>do not</em> trigger a visitation of the
 * initializers.
 * 
 * <p>
 * When processing anonymous class expressions the initializers are always
 * visited.
 * 
 * <p>
 * In there most general form, the <code>processAnonClassExpression</code> and
 * <code>processConstructorCall</code> methods take a reference to an
 * {@link InstanceInitAction} object. This object specifies actions to be taken immediately
 * before and immediately after the recursive visitation is performed.
 * Furthermore, these actions are taken only if the recursive visitation occurs.
 * In particular, they methods of the {@link Action} object are called from
 * within a <code>try</code>&ndash;<code>finally</code> block as follows
 * 
 * <pre>
 * if (visit) {
 *   try {
 *     action.tryBefore();
 *     // perform recursive visitation
 *   } finally {
 *     action.finallyAfter();
 *   }
 *   action.afterVisit();
 * }
 * </pre>
 * 
 * <p>
 * The action object is provided as means for initializing and resetting
 * context-sensitive state within the parent visitor.
 */
@Deprecated
public final class InstanceInitializationVisitor {
  // Prevent instantiation of this class
  private InstanceInitializationVisitor() {
    // do nothing
  }

  
  
  private static void processClassBody(
      final Visitor<Void> analysisWeAreHelping, final IRNode classBody) {
    for (final IRNode bodyDecl : ClassBody.getDeclIterator(classBody)) {
      final Operator op = JJNode.tree.getOperator(bodyDecl);
      if (FieldDeclaration.prototype.includes(op) ||
          ClassInitializer.prototype.includes(op)) {
        if (!JavaNode.getModifier(bodyDecl, JavaNode.STATIC)) {
          analysisWeAreHelping.doAcceptForChildren(bodyDecl);
        }
      }       
    }
  }


  
  /**
   * Process a constructor call and visit the instance initializers if
   * appropriate. This version expects the caller to provide the class
   * body node that contains the constructor declaration that contains
   * the given constructor call. Most clients will already have this information
   * because they need to keep track of it for other reasons.
   * 
   * <p>If you really want to follow Java evaluation order, this method should
   * be called after the caller has already processed the constructorCall.
   * 
   * @param <X>
   *          The return type of the analysis.
   * @param constructorCall
   *          The constructor call node to process.
   * @param classBody
   *          The ClassBody node that contains the constructor
   *          declaration that contains the given constructor call.
   * @param analysis
   *          The analysis to invoke.
   * @return Whether the initializers where visited or not.
   */
  public static boolean processConstructorCall(final IRNode constructorCall,
      final IRNode classBody, final Visitor<Void> analysis,
      final InstanceInitAction action) {
    final IRNode conObject = ConstructorCall.getObject(constructorCall);
    final Operator conObjectOp = JJNode.tree.getOperator(conObject);
    if (SuperExpression.prototype.includes(conObjectOp)) {
      // Visit the initializers.
      action.tryBefore();
      try {
        processClassBody(analysis, classBody);
      } finally {
        action.finallyAfter();
      }
      action.afterVisit();
      return true;
    } else if (ThisExpression.prototype.includes(conObjectOp)) {
      // Don't do anything
      return false;
    } else { 
      throw new IllegalArgumentException("Unknown constructor call operator " + conObjectOp.name());
    }
  }

  public static boolean processConstructorCall(final IRNode constructorCall,
      final IRNode classBody, final Visitor<Void> analysis) {
    return processConstructorCall(
        constructorCall, classBody, analysis, InstanceInitAction.NULL_ACTION);
  }

  /**
   * Process a constructor call and visit the instance initializers if
   * appropriate.  This version climbs up the tree to find the enclosing class
   * body.
   * 
   * <p>If you really want to follow Java evaluation order, this method should
   * be called after the caller has already processed the constructorCall.
   * 
   * @param <X>
   *          The return type of the analysis.
   * @param constructorCall
   *          The constructor call node to process.
   * @param analysis
   *          The analysis to invoke.
   * @return Whether the initializers where visited or not.
   */
  public static boolean processConstructorCall(
      final IRNode constructorCall, final Visitor<Void> analysis,
      final InstanceInitAction action) {
    IRNode classBody = JJNode.tree.getParentOrNull(constructorCall);
    while (!ClassBody.prototype.includes(classBody)) {
      classBody = JJNode.tree.getParentOrNull(classBody);
    }
    return processConstructorCall(constructorCall, classBody, analysis, action);
  }

  public static boolean processConstructorCall(
      final IRNode constructorCall, final Visitor<Void> analysis) {
    return processConstructorCall(
        constructorCall, analysis, InstanceInitAction.NULL_ACTION);
  }

  /**
   * Process an anonymous class expression and visit all the instance
   * initializers in the class declaration.
   * 
   * @param <X>
   *          The return type of the analysis.
   * @param anonClassExpr
   *          The anonymousClassExpression to process.
   * @param analysis
   *          The analysis to invoke.
   */
  public static void processAnonClassExpression(final IRNode anonClassExpr,
      final Visitor<Void> analysis, final InstanceInitAction action) {
    action.tryBefore();
    try {
      processClassBody(analysis, AnonClassExpression.getBody(anonClassExpr));
    } finally {
      action.finallyAfter();
    }
    action.afterVisit();
  }

  /**
   * Process an enumeration constant class expression and visit all the instance
   * initializers in the class declaration.
   * 
   * @param <X>
   *          The return type of the analysis.
   * @param enumClassDecl
   *          The EnumConstantClassDeclaration to process.
   * @param analysis
   *          The analysis to invoke.
   */
  public static void processEnumConstantClassDeclaration(final IRNode enumClassDecl,
      final Visitor<Void> analysis, final InstanceInitAction action) {
    action.tryBefore();
    try {
      processClassBody(analysis, EnumConstantClassDeclaration.getBody(enumClassDecl));
    } finally {
      action.finallyAfter();
    }
    action.afterVisit();
  }
  
  public static void processAnonClassExpression(
      final IRNode anonClassExpr,  final Visitor<Void> analysis) {
    processAnonClassExpression(
        anonClassExpr, analysis, InstanceInitAction.NULL_ACTION);
  }
}
