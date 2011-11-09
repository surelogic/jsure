/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/ThrownExceptionLabel.java,v 1.3 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.java.control;

import edu.cmu.cs.fluid.ir.IRNode;

public class ThrownExceptionLabel extends ExceptionLabel {
  public final IRNode throwNode;
  public ThrownExceptionLabel(IRNode t) {
    throwNode = t;
  }
  @Override
  public String toString() {
    return "thrown exception";
  }
}
