/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/IColorVisitor.java,v 1.2 2007/07/09 13:39:26 chance Exp $*/
package com.surelogic.analysis.colors;




public interface IColorVisitor {

  public void visitCU(final ColorStaticCU node);
  public void visitClass(final ColorStaticClass node);
  public void visitMeth(final ColorStaticMeth node);
  public void visitBlock(final ColorStaticBlock node);
  public void visitCall(final ColorStaticCall node);
  public void visitReference(final ColorStaticRef node);
  
}
