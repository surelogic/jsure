/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/AnchoredContinueLabel.java,v 1.4 2005/05/25 18:03:36 chance Exp $ */
package edu.cmu.cs.fluid.java.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class AnchoredContinueLabel extends ContinueLabel {
  public final IRNode stmtNode; // a loop node
  public AnchoredContinueLabel(IRNode node) {
    stmtNode = node;
  }
  @Override
  public String toString() {
    return "anchored continue";
  }
}
