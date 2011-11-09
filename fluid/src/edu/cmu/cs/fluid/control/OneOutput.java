/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/OneOutput.java,v 1.4 2003/07/02 20:19:22 thallora Exp $ */
package edu.cmu.cs.fluid.control;

/** Interface for control-flow nodes with one output edge
 * @author John Tang Boyland
 * @see Source
 * @see Flow
 * @see Join
 */
 
public interface OneOutput {
  ControlEdge getOutput();
  void setOutput(ControlEdge output);
}

