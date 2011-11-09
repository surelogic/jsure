/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/ExceptionLabel.java,v 1.5 2007/05/30 20:35:18 chance Exp $ */
package edu.cmu.cs.fluid.java.control;

import edu.cmu.cs.fluid.control.ControlLabel;

public class ExceptionLabel implements ControlLabel {
  public static final ExceptionLabel prototype = new ExceptionLabel();
  @Override
  public String toString() { return "exception"; }
}
