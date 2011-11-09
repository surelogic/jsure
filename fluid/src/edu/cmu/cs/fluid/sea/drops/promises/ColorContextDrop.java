/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;


import com.surelogic.analysis.threadroles.TRExpr;
import com.surelogic.analysis.threadroles.TRolesFirstPass;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.drops.threadroles.TRoleSummaryDrop;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
@Deprecated
public class ColorContextDrop extends ColorExprDrop {
  private static final String kind = "colorContext";
//  public ColorContextDrop(TRExpr expr) {
//    super(kind, expr);
//  }
  
  public ColorContextDrop(TRExpr expr, IRNode locInIR) {
    super(kind, expr, locInIR, false);
    setMessage("colorContext " +expr);
  }
  
  public ColorContextDrop(ColorContextDrop parentDrop, IRNode locInIR) {
    super(kind, parentDrop.getRawExpr(), locInIR, true);
    parentDrop.addDependent(this);
    setMessage("(inherited) " + parentDrop.getMessage());
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction()
   */
  @Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
    if (!(invalidDeponent instanceof TRoleSummaryDrop)) {
      TRolesFirstPass.trackCUchanges(this);
      super.deponentInvalidAction(invalidDeponent);
    }
  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.PromiseDrop#isCheckedByAnalysis()
   */
  @Override
  public boolean isCheckedByAnalysis() {
    return true;
  }
}
