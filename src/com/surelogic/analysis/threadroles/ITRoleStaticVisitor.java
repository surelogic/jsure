/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/IColorVisitor.java,v 1.2 2007/07/09 13:39:26 chance Exp $*/
package com.surelogic.analysis.threadroles;




public interface ITRoleStaticVisitor {

  public void visitCU(final TRoleStaticCU node);
  public void visitClass(final TRoleStaticClass node);
  public void visitMeth(final TRoleStaticMeth node);
  public void visitBlock(final TRoleStaticBlock node);
  public void visitCall(final TRoleStaticCall node);
  public void visitReference(final TRoleStaticRef node);
  
}
