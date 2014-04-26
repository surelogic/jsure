/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/InputPort.java,v 1.9 2005/05/20 15:48:04 chance Exp $ */
package edu.cmu.cs.fluid.control;


/** An input port is a port with output edges leading into the interior
 * of a component.  It includes entry ports to components and
 * exit ports of subcomponents.
 * @see BlankInputPort
 * @see SimpleInputPort
 * @see DoubleInputPort
 */
abstract class InputPort extends Entity implements Port, IInputPort {
  @Override
  public ControlEdgeIterator getInputs() {
    Port dual = getDual();
    if (dual == null) {
      return EmptyControlEdgeIterator.prototype;
    } else {
      return dual.getInputs();
    }
  }
}
