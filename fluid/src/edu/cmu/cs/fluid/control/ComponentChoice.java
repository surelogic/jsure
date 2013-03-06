/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/ComponentChoice.java,v 1.7 2005/05/25 15:52:04 chance Exp $ */
package edu.cmu.cs.fluid.control;

import edu.cmu.cs.fluid.ir.IRNode;

/** Control-flow decision points in the graph that depend
 * on language-specific conditions.  The component is identified
 * as well as an arbitrary value that may be used to distinguish
 * multiple control points within a component.
 */
public class ComponentChoice extends Choice implements ComponentNode {
  Component comp;
  public ComponentChoice(Component c, Object v) {
    super(v);
    comp = c;
  }
  @Override
  public Component getComponent() {
    return comp;
  }
  @Override
  public IRNode getSyntax() {
    return comp.getSyntax();
  }
}
