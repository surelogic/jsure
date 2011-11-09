/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/CallInitializerLabel.java,v 1.8 2005/05/25 18:08:17 chance Exp $ */
package edu.cmu.cs.fluid.java.control;

import edu.cmu.cs.fluid.control.ControlLabel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;

/** The label for a particular call to a class instance initializer.
 * The label is used to handle multiple uses of
 * the initializer in a class.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.JavaTransfer
 */
public class CallInitializerLabel implements ControlLabel {
  public final IRNode callNode;
  public CallInitializerLabel(IRNode node) {
    callNode = node;
  }

  @Override
  public boolean equals(Object x) {
    if (x instanceof CallInitializerLabel) {
      return callNode.equals(((CallInitializerLabel)x).callNode);
    }
    return false;
  }
  
  @Override
  public int hashCode() {
  	return callNode.hashCode();
  }

  @Override
  public String toString() {
    return DebugUnparser.toString(callNode);
  }
}
