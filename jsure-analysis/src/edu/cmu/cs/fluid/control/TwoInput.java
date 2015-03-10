/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/TwoInput.java,v 1.5 2006/04/14 19:32:32 boyland Exp $ */
package edu.cmu.cs.fluid.control;

/** Interface for control-flow nodes with two Input edges
 * @author John Tang Boyland
 * @see Join
 */
 
public interface TwoInput {
  ControlEdge getInput1();
  ControlEdge getInput2();
  ControlEdge getInput(boolean secondary);
  void setInput1(ControlEdge input1);
  void setInput2(ControlEdge input2);
}
