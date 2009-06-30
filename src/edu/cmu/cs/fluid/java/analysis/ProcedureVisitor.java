/*
 * $header$
 * Created on Jan 11, 2005
 */
package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.SuperExpression;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.parse.JJNode;

/**
 * Perform a tree-walk analysis for one procedure.
 * To perform the analysis, override the various visitOp
 * routines needed in a subclass, make a subclass and then call
 * the {@link #doAccept(IRNode)} method on the procedure node.
 * @see CachedProceduralAnalysis
 * @see UnassignedVariables
 * @author boyland
 */
public abstract class ProcedureVisitor<T> extends Visitor<T> {

  /** visit children by default */
  @Override
  public T visit(IRNode node) {
    doAcceptForChildren(node);
    return null;
  }

  private class InstanceInitializerHelper extends Visitor<T> {
    @Override
    public T visitClassInitializer(IRNode node) {
      if (!JavaNode.getModifier(node,JavaNode.STATIC)) {
        ProcedureVisitor.this.visit(node);
      }
      return null;
    }
    @Override
    public T visitFieldDeclaration(IRNode node) {
      if (!JavaNode.getModifier(node,JavaNode.STATIC)) {
        ProcedureVisitor.this.visit(node);
      }
      return null;
    }
  }
  private final InstanceInitializerHelper helper = new InstanceInitializerHelper();
  
  // don't visit these things : they are in different modules
  @Override
  public T visitMethodDeclaration(IRNode node) { return null; }
  public T visitConstructureDeclaration(IRNode node) { return null; }
  @Override
  public T visitClassDeclaration(IRNode node) { return null; }
  @Override
  public T visitInterfaceDeclaration(IRNode node) { return null; }

  // these are in the same module if they are static
  @Override
  public T visitClassInitializer(IRNode node) { 
    if (JavaNode.getModifier(node,JavaNode.STATIC)) {
      visit(node);
    }
    return null;
  }
  @Override
  public T visitFieldDeclaration(IRNode node) { 
    if (JavaNode.getModifier(node,JavaNode.STATIC)) {
      visit(node);
    }
    return null;
  }

  @Override
  public T visitConstructorCall(IRNode node) {
    // after processing the arguments, we do the initializations
    super.visitConstructorCall(node);
    if (JJNode.tree.getOperator(ConstructorCall.getObject(node))
        instanceof SuperExpression) {
      IRNode constrDecl = JJNode.tree.getParent(JJNode.tree.getParent(node));
      assert JJNode.tree.getOperator(constrDecl) instanceof ConstructorDeclaration;
      IRNode classBody = JJNode.tree.getParent(constrDecl);
      helper.doAcceptForChildren(classBody);
    }
    return null;
  }

}
