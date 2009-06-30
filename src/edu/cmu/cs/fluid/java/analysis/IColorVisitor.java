/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/IColorVisitor.java,v 1.2 2007/07/09 14:08:28 chance Exp $*/
package edu.cmu.cs.fluid.java.analysis;



@Deprecated
public interface IColorVisitor {

  public void visitCU(final ColorStaticCU node);
  public void visitClass(final ColorStaticClass node);
  public void visitMeth(final ColorStaticMeth node);
  public void visitBlock(final ColorStaticBlock node);
  public void visitCall(final ColorStaticCall node);
  public void visitReference(final ColorStaticRef node);
  
}
