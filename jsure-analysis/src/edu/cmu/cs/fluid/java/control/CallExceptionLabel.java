/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/control/CallExceptionLabel.java,v 1.2 2003/07/02 20:19:39 thallora Exp $ */
package edu.cmu.cs.fluid.java.control;

import edu.cmu.cs.fluid.ir.IRNode;

/** The exception label for an exception not caught in a called
 * method that is rebroadcast in the callee method.  The call
 * node can be examined to see, for example, if a particular exception
 * could possibly arise from there.
 */
public class CallExceptionLabel extends ExceptionLabel {
  public final IRNode callNode;
  public CallExceptionLabel(IRNode node) {
    callNode = node;
  }
}
