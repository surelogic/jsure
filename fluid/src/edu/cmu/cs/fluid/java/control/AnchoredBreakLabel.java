/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/AnchoredBreakLabel.java,v 1.4 2005/05/25 18:03:36 chance Exp $ */
package edu.cmu.cs.fluid.java.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class AnchoredBreakLabel extends BreakLabel {
  public final IRNode stmtNode; // a loop or switch statement
  public AnchoredBreakLabel(IRNode n) {
    stmtNode = n;
  }
  @Override
  public String toString() {
    return "anchored break";
  }
}
