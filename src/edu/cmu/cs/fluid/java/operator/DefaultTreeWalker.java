// $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/operator/DefaultTreeWalker.java,v 1.14 2006/01/17 15:29:21 chance Exp $
package edu.cmu.cs.fluid.java.operator;

import java.util.Iterator;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.tree.Operator;

/** Implementation of <TT>ExtendedTreeWalker</tt> that simply recurses on every
  *  child IRNode at each point using walkTree/Iterator
  * Roughly equivalent to a topDown() enumeration
  * @see TreeWalker
  * @see ExtendedTreeWalker
  * @author Edwin Chan
  */
public class DefaultTreeWalker extends ExtendedTreeWalker {

  /** Create a new DefaultTreeWalker.
    * @param defVal The value to be returned if the operator is unknown,
    * or if the analysis has nothing to do.  This may not necessarily be
    * useful for all analyses.
    */
  public DefaultTreeWalker(final Object defVal) {
    super(defVal);
  }

  //**********************************************************************
  //**********************************************************************
  // Methods to handle each operator type (from TreeWalker)
  //**********************************************************************
  //**********************************************************************

  @Override public Object arguments(final IRNode root, final Iterator<IRNode> args) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object arrayCreationExpression(
    final IRNode root,
    final IRNode alloc,
    final IRNode base,
    final IRNode init,
    final int unalloc) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object arrayDeclaration(
    final IRNode root,
    final IRNode base,
    final int dims) {
    return walkTree(base);
  }

  //----------------------------------------------------------------------

  @Override public Object arrayInitializer(
    final IRNode root,
    final Iterator<IRNode> elements) {
    return walkIterator(elements);
  }
  
  //----------------------------------------------------------------------
  
 @Override public Object arrayLength(
      final IRNode root,
      final IRNode array) {
      return walkTree(array);
    }

  //----------------------------------------------------------------------

  @Override public Object arrayRefExpression(
    final IRNode root,
    final IRNode array,
    final IRNode index) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object assertStatement(final IRNode root, final IRNode assertion) {
    return walkTree(assertion);
  }

  //----------------------------------------------------------------------

  @Override public Object assertMessageStatement(
    final IRNode root,
    final IRNode assertion,
    final IRNode message) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object assignExpression(
    final IRNode root,
    final IRNode lhs,
    final IRNode rhs) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  // Already implemented by ExtendedTreeWalker
//  @Override public Object binOpExpression(
//    final Operator op,
//    final IRNode root,
//    final IRNode op1,
//    final IRNode op2) {
//    return binOpExpression(op, root, op1, op2);
//  }

  //----------------------------------------------------------------------

  @Override public Object blockStatement(final IRNode root, final Iterator<IRNode> stmts) {
    return walkIterator(stmts);
  }

  //----------------------------------------------------------------------

  @Override public Object breakStatement(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object castExpression(
    final IRNode root,
    final IRNode expr,
    final IRNode type) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object catchClause(
    final IRNode root,
    final IRNode body,
    final IRNode param) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object catchClauses(final IRNode root, final Iterator<IRNode> clauses) {
    return walkIterator(clauses);
  }

  //----------------------------------------------------------------------

  @Override public Object charLiteral(final IRNode root, final Object token) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object classBody(final IRNode root, final Iterator<IRNode> decls) {
    return walkIterator(decls);
  }

  //----------------------------------------------------------------------

  @Override public Object classDeclaration(
    final IRNode root,
    final IRNode body,
    final IRNode extension,
    final Object id,
    final IRNode impls,
    final int mods) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object classExpression(final IRNode root, final IRNode type) {
    return walkTree(type);
  }

  //----------------------------------------------------------------------

  @Override public Object classInitializer(
    final IRNode root,
    final IRNode block,
    final int mods) {
    return walkTree(block);
  }

  //----------------------------------------------------------------------

  @Override public Object compilationUnit(
    final IRNode root,
    final IRNode decls,
    final IRNode imps,
    final IRNode pkg) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object complementExpression(final IRNode root, final IRNode expr) {
    return walkTree(expr);
  }

  //----------------------------------------------------------------------

  @Override public Object conditionalExpression(
    final IRNode root,
    final IRNode cond,
    final IRNode ifTrue,
    final IRNode ifFalse) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object constantLabel(final IRNode root, final IRNode op) {
    return walkTree(op);
  }

  //----------------------------------------------------------------------

  @Override public Object constructorCall(
    final IRNode root,
    final IRNode object,
    final IRNode args) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object constructorDeclaration(
    final IRNode root,
    final IRNode block,
    final IRNode exceptions,
    final int mods,
    final Object name,
    final IRNode params) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object continueStatement(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object declStatement(
    final IRNode root,
    final int mods,
    final IRNode type,
    final IRNode vars) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object defaultLabel(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object demandName(final IRNode root, final String pkg) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object dimExprs(final IRNode root, final Iterator<IRNode> dims) {
    return walkIterator(dims);
  }

  //----------------------------------------------------------------------

  public Object dims(final IRNode root, final int dims) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object doStatement(
    final IRNode root,
    final IRNode cond,
    final IRNode loop) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object emptyStatement(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object exprStatement(final IRNode root, final IRNode expr) {
    return walkTree(expr);
  }

  //----------------------------------------------------------------------

  @Override public Object extensions(final IRNode root, final Iterator<IRNode> classes) {
    return walkIterator(classes);
  }

  //----------------------------------------------------------------------

  @Override public Object falseExpression(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  // special
  @Override public Object fieldDeclaration(
    final IRNode root,
    final int mods,
    final IRNode type,
    final IRNode vars) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object fieldRef(
    final IRNode root,
    final IRNode object,
    final Object id) {
    return walkTree(object);
  }

  //----------------------------------------------------------------------

  @Override public Object finallyClause(final IRNode root, final IRNode body) {
    return walkTree(body);
  }

  //----------------------------------------------------------------------

  @Override public Object floatLiteral(final IRNode root, final Object token) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object forStatement(
    final IRNode root,
    final IRNode init,
    final IRNode cond,
    final IRNode update,
    final IRNode loop) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object ifElseStatement(
    final IRNode root,
    final IRNode cond,
    final IRNode thenPart,
    final IRNode elsePart) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object ifStatement(
    final IRNode root,
    final IRNode cond,
    final IRNode thenPart) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object implementsList(
    final IRNode root,
    final Iterator<IRNode> interfaces) {
    return walkIterator(interfaces);
  }

  //----------------------------------------------------------------------

  @Override public Object importDeclaration(final IRNode root, final IRNode item) {
    return walkTree(item);
  }

  //----------------------------------------------------------------------

  @Override public Object importDeclarations(
    final IRNode root,
    final Iterator<IRNode> imports) {
    return walkIterator(imports);
  }

  //----------------------------------------------------------------------

  @Override public Object initialization(final IRNode root, final IRNode value) {
    return walkTree(value);
  }

  //----------------------------------------------------------------------

  @Override public Object instanceOfExpression(
    final IRNode root,
    final IRNode val,
    final IRNode type) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object intLiteral(final IRNode root, final Object token) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object interfaceDeclaration(
    final IRNode root,
    final IRNode body,
    final IRNode extensions,
    final Object id,
    final int mods) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object labeledBreakStatement(final IRNode root, final Object id) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object labeledContinueStatement(final IRNode root, final Object id) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object labeledStatement(
    final IRNode root,
    final Object label,
    final IRNode stmt) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object methodBody(final IRNode root, final IRNode block) {
    return walkTree(block);
  }

  //----------------------------------------------------------------------

  @Override public Object methodCall(
    final IRNode root,
    final IRNode args,
    final Object method,
    final IRNode obj) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object methodDeclaration(
    final IRNode root,
    final IRNode body,
    final int dims,
    final IRNode exceptions,
    final Object id,
    final int mods,
    final IRNode params,
    final IRNode returnType) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object minusExpression(final IRNode root, final IRNode op) {
    return walkTree(op);
  }

  //----------------------------------------------------------------------

  @Override public Object name(final IRNode root, String names) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object nestedClassDeclaration(
    final IRNode root,
    final IRNode body,
    final IRNode extension,
    final Object id,
    final IRNode impls,
    int mods) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object nestedInterfaceDeclaration(
    final IRNode root,
    final IRNode body,
    final IRNode extensions,
    final Object id,
    int mods) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object newExpression(
    final IRNode root,
    final IRNode args,
    final IRNode type) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object noArrayInitializer(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object noClassBody(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object noFinally(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object noInitialization(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object noMethodBody(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object notExpression(final IRNode root, final IRNode op) {
    return walkTree(op);
  }

  //----------------------------------------------------------------------

  @Override public Object nullLiteral(final IRNode root) {
    return defaultValue;
  }
  
  //----------------------------------------------------------------------

  @Override public Object omittedMethodBody(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object opAssignExpression(
    final IRNode root,
    final IRNode lhs,
    final IRNode rhs,
    final Operator op) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object parameterDeclaration(
    final IRNode root,
    final Object id,
    final int mods,
    final IRNode type) {
    return walkTree(type);
  }

  //----------------------------------------------------------------------

  @Override public Object parameters(final IRNode root, final Iterator<IRNode> params) {
    return walkIterator(params);
  }

  //----------------------------------------------------------------------

  @Override public Object plusExpression(final IRNode root, final IRNode expr) {
    return walkTree(expr);
  }

  //----------------------------------------------------------------------

  @Override public Object postDecrementExpression(final IRNode root, final IRNode expr) {
    return walkTree(expr);
  }

  //----------------------------------------------------------------------

  @Override public Object postIncrementExpression(final IRNode root, final IRNode expr) {
    return walkTree(expr);
  }

  //----------------------------------------------------------------------

  @Override public Object preDecrementExpression(final IRNode root, final IRNode expr) {
    return walkTree(expr);
  }

  //----------------------------------------------------------------------

  @Override public Object preIncrementExpression(final IRNode root, final IRNode expr) {
    return walkTree(expr);
  }

  //----------------------------------------------------------------------

  @Override public Object qualifiedAllocationExpression(
    final IRNode root,
    final IRNode alloc,
    final IRNode type) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  public Object qualifiedName(
    final IRNode root,
    final Object id,
    final IRNode pkg) {
    return walkTree(pkg);
  }

  //----------------------------------------------------------------------

  @Override public Object qualifiedThisExpression(final IRNode root, final IRNode type) {
    return walkTree(type);
  }

  //----------------------------------------------------------------------

  @Override public Object returnStatement(final IRNode root, final IRNode val) {
    return walkTree(val);
  }

  //----------------------------------------------------------------------

  public Object simpleName(final IRNode root, final Object id) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object statementExpressionList(
    final IRNode root,
    final Iterator<IRNode> stmts) {
    return walkIterator(stmts);
  }

  //----------------------------------------------------------------------

  @Override public Object stringLiteral(final IRNode root, final Object token) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object superExpression(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object switchBlock(final IRNode root, final Iterator<IRNode> elements) {
    return walkIterator(elements);
  }

  //----------------------------------------------------------------------

  @Override public Object switchElement(
    final IRNode root,
    final IRNode label,
    final IRNode stmts) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object switchStatement(
    final IRNode root,
    final IRNode block,
    final IRNode expr) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object switchStatements(final IRNode root, final Iterator<IRNode> stmts) {
    return walkIterator(stmts);
  }

  //----------------------------------------------------------------------

  @Override public Object synchronizedStatement(
    final IRNode root,
    final IRNode block,
    final IRNode lock) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object thisExpression(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object throwStatement(final IRNode root, final IRNode val) {
    return walkTree(val);
  }

  //----------------------------------------------------------------------

  @Override public Object throwsList(final IRNode root, final Iterator<IRNode> types) {
    return walkIterator(types);
  }

  //----------------------------------------------------------------------

  @Override public Object trueExpression(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object tryStatement(
    final IRNode root,
    final IRNode block,
    final IRNode catchPart,
    final IRNode finallyPart) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object typeDeclarations(final IRNode root, final Iterator<IRNode> types) {
    return walkIterator(types);
  }

  //----------------------------------------------------------------------

  @Override public Object typeExpression(final IRNode root, final IRNode type) {
    return walkTree(type);
  }

  //----------------------------------------------------------------------

  @Override public Object unnamedPackageDeclaration(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object useExpression(final IRNode root, final Object id) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object variableDeclarator(
    final IRNode root,
    final Object id,
    final int dims,
    final IRNode init) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------

  @Override public Object variableDeclarators(
    final IRNode root,
    final Iterator<IRNode> vars) {
    return walkIterator(vars);
  }

  //----------------------------------------------------------------------

  @Override public Object voidReturnStatement(final IRNode root) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object whileStatement(
    final IRNode root,
    final IRNode cond,
    final IRNode loop) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------
  //----------------------------------------------------------------------
  //----------------------------------------------------------------------

  @Override public Object compiledMethodBody(final IRNode root, final Object code) {
    return defaultValue;
  }

  //----------------------------------------------------------------------

  @Override public Object anonClassExpression(
    final IRNode root,
    final IRNode args,
    final IRNode body,
    final IRNode type) {
    return walkIterator(tree.children(root));
  }

  //----------------------------------------------------------------------
  //----Stuff from ExtendedTreeWalker
  //----------------------------------------------------------------------

  @Override public Object addExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object divExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object remExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object mulExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object subExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object conditionalAndExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object conditionalOrExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object andExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object orExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object xorExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object greaterThanEqualExpression(
    IRNode root,
    IRNode op1,
    IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object greaterThanExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object lessThanEqualExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object LessThanExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object eqExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object notEqExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object leftShiftExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object rightShiftExpression(IRNode root, IRNode op1, IRNode op2) {
    return walkIterator(tree.children(root));
  }
  @Override public Object unsignedRightShiftExpression(
    IRNode root,
    IRNode op1,
    IRNode op2) {
    return walkIterator(tree.children(root));
  }
}
