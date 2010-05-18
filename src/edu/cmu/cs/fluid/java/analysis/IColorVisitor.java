/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/IColorVisitor.java,v 1.2 2007/07/09 14:08:28 chance Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import com.surelogic.analysis.threadroles.TRoleStaticBlock;
import com.surelogic.analysis.threadroles.TRoleStaticCU;
import com.surelogic.analysis.threadroles.TRoleStaticCall;
import com.surelogic.analysis.threadroles.TRoleStaticClass;
import com.surelogic.analysis.threadroles.TRoleStaticMeth;
import com.surelogic.analysis.threadroles.TRoleStaticRef;



@Deprecated
public interface IColorVisitor {

  public void visitCU(final TRoleStaticCU node);
  public void visitClass(final TRoleStaticClass node);
  public void visitMeth(final TRoleStaticMeth node);
  public void visitBlock(final TRoleStaticBlock node);
  public void visitCall(final TRoleStaticCall node);
  public void visitReference(final TRoleStaticRef node);
  
}
