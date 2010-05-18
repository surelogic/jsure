/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStructVisitor.java,v 1.3 2007/07/09 14:00:11 chance Exp $*/
package com.surelogic.analysis.threadroles;


public abstract class TRoleStructVisitor implements ITRoleStaticVisitor {

  // two useful methods
  public void doAccept(TRoleStaticStructure node) {
    node.accept(this);
  }

  public void doAcceptForChildren(TRoleStaticWithChildren node) {
    for (TRoleStaticStructure trssNode : node.getChildren()) {
      trssNode.accept(this);
    }
  }

  // method called for any operator without a visit method overridden.
  public abstract void visit(TRoleStaticStructure node);
  
  public void visitBlock(TRoleStaticBlock node) {
    visit(node);
  }

  public void visitCU(TRoleStaticCU node) {
    visit(node);
  }

  public void visitCall(TRoleStaticCall node) {
    visit(node);
  }

  public void visitClass(TRoleStaticClass node) {
    visit(node);
  }

  public void visitMeth(TRoleStaticMeth node) {
    visit(node);
  }

  public void visitReference(TRoleStaticRef node) {
    visit(node);
  }

}
