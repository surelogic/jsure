/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/ControlNode.java,v 1.7 2005/05/20 15:48:04 chance Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

/** Control nodes for Java programs.
 * @author John Tang Boyland
 * @see Source
 * @see Sink
 * @see Flow
 * @see Join
 * @see Split
 */
public interface ControlNode extends IRNode {
  abstract public ControlEdgeIterator getInputs();
  abstract public ControlEdgeIterator getOutputs();
}

