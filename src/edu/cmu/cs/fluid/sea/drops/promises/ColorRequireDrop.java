/*
 * Created on Oct 22, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package edu.cmu.cs.fluid.sea.drops.promises;


import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.analysis.CExpr;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.PleaseFolderize;


/**
 * @author dfsuther
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
@Deprecated
public class ColorRequireDrop extends ColorExprDrop implements PleaseFolderize {
  private static final String kind = "colorConstraint";
  
//  public ColorRequireDrop(CExpr expr) {
//    super(kind, expr);
//  }
  
  public ColorRequireDrop(CExpr expr, IRNode locInIR) {
    super(kind, expr, locInIR, false);
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("built colorConstraint " + getRawExpr() + " for " + JJNode.getInfo(locInIR));
    }
    setMessage("colorConstraint " +expr);
  }
  
  public ColorRequireDrop(ColorRequireDrop parentDrop, IRNode locInIR) {
    super(kind, parentDrop.getRawExpr(), locInIR, true);
    parentDrop.addDependent(this);
    if (LOG.isLoggable(Level.FINER)) {
      LOG.finer("built colorRequired " + getRawExpr() + " for " + JJNode.getInfo(locInIR));
    }
    setMessage("(inherited)" +parentDrop.getMessage());
  }
  
  
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.PromiseDrop#isCheckedByAnalysis()
   */
  @Override
  public boolean isCheckedByAnalysis() {
    // All colorRequired annos are always "good" by default.  Their _callers_
    // may not be, the the requirement is.
    return true;
  }
}
