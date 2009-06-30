/*
 * Created on Oct 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;


import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.analysis.CExpr;
import edu.cmu.cs.fluid.java.analysis.ColorFirstPass;
import edu.cmu.cs.fluid.sea.Drop;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
@Deprecated
public class ColorContextDrop extends ColorExprDrop {
  private static final String kind = "colorContext";
//  public ColorContextDrop(CExpr expr) {
//    super(kind, expr);
//  }
  
  public ColorContextDrop(CExpr expr, IRNode locInIR) {
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
    if (!(invalidDeponent instanceof ColorSummaryDrop)) {
      ColorFirstPass.trackCUchanges(this);
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
