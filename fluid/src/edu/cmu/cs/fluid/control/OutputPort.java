/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/OutputPort.java,v 1.10 2005/05/23 18:28:50 chance Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.AbstractProxyNode;
import edu.cmu.cs.fluid.ir.IRNode;

/** An output port is a port with input edges coming from the interior
 * of a component.  It includes exit ports of components and
 * entry ports of subcomponents.  It is also a node
 * which is identical to the corresponding (dual) input port.
 * @see BlankOutputPort
 * @see SimpleOutputPort
 * @see DoubleOutputPort
 */
abstract class OutputPort extends AbstractProxyNode implements Port, IOutputPort {
  @Override
  public IRNode getIRNode() {
    return getDual();
  }
  
  @Override 
  public WhichPort which() {
	  return getDual().which();
  }
  
  @Override
  public ControlEdgeIterator getOutputs() {
    Port dual = getDual();
    if (dual == null) {
      return EmptyControlEdgeIterator.prototype;
    } else {
      return dual.getOutputs();
    }
  }
  
  @Override 
  public String toString() {
	  return getClass().getSimpleName() ; // + "(" + getIRNode() + ")";
  }
}
