/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/NamedBreakLabel.java,v 1.6 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.java.control;

import edu.cmu.cs.fluid.ir.IRNode;

/** Break labels for names break statements and for labeled statements.
 */
public class NamedBreakLabel extends BreakLabel {
  public final IRNode breakNode;
  public NamedBreakLabel(IRNode bn) {
    breakNode = bn;
  }
  @Override
  public String toString() {
    return "named break";
  }
}
