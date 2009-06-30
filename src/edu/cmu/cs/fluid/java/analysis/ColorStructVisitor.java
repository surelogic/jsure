/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/ColorStructVisitor.java,v 1.3 2007/07/10 22:16:29 aarong Exp $*/
package edu.cmu.cs.fluid.java.analysis;

@Deprecated
public abstract class ColorStructVisitor implements IColorVisitor {

  // two useful methods
  public void doAccept(ColorStaticStructure node) {
    node.accept(this);
  }

  public void doAcceptForChildren(ColorStaticWithChildren node) {
    for (ColorStaticStructure cssNode : node.getChildren()) {
      cssNode.accept(this);
    }
  }

  // method called for any operator without a visit method overridden.
  public abstract void visit(ColorStaticStructure node);
  
  public void visitBlock(ColorStaticBlock node) {
    visit(node);
  }

  public void visitCU(ColorStaticCU node) {
    visit(node);
  }

  public void visitCall(ColorStaticCall node) {
    visit(node);
  }

  public void visitClass(ColorStaticClass node) {
    visit(node);
  }

  public void visitMeth(ColorStaticMeth node) {
    visit(node);
  }

  public void visitReference(ColorStaticRef node) {
    visit(node);
  }

}
