/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/ReturnLabel.java,v 1.6 2007/07/05 18:15:17 aarong Exp $ */
package edu.cmu.cs.fluid.java.control;

import edu.cmu.cs.fluid.control.ControlLabel;

public class ReturnLabel implements ControlLabel {
  public static final ReturnLabel prototype = new ReturnLabel();
  @Override
  public String toString() { return "return"; }
}
