/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/NamedContinueLabel.java,v 1.5 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.java.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class NamedContinueLabel extends ContinueLabel {
  public final IRNode continueNode;
  public NamedContinueLabel(IRNode node) {
    continueNode = node;
  }
  @Override
  public String toString() {
    return "named continue";
  }
}
