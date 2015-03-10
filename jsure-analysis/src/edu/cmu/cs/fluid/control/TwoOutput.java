/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/TwoOutput.java,v 1.4 2006/04/14 19:32:32 boyland Exp $ */
package edu.cmu.cs.fluid.control;

/** Interface for control-flow nodes with two output edges
 * @author John Tang Boyland
 * @see Split
 */
 
public interface TwoOutput {
  ControlEdge getOutput1();
  ControlEdge getOutput2();
  ControlEdge getOutput(boolean secondary);
  void setOutput1(ControlEdge output1);
  void setOutput2(ControlEdge output2);
}

