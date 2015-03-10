/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/CaughtExceptionLabel.java,v 1.3 2005/05/25 18:30:37 chance Exp $ */
package edu.cmu.cs.fluid.java.control;

import edu.cmu.cs.fluid.ir.IRNode;

/** A label used for reverse control-flow analysis from a catch clause. */
public class CaughtExceptionLabel extends ExceptionLabel {
  public final IRNode catchNode;
  public CaughtExceptionLabel(IRNode c) {
    catchNode = c;
  }
  @Override
  public String toString() {
    return "catch exception";
  }
}
