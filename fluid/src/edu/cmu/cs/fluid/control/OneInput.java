/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/OneInput.java,v 1.4 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.control;

/** Interface for control-flow nodes with one input edge
 * @author John Tang Boyland
 * @see Sink
 * @see Flow
 * @see Split
 */
 
public interface OneInput {
  ControlEdge getInput();
  void setInput(ControlEdge input);
}
