/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/ComponentFlow.java,v 1.8 2005/05/25 03:28:36 boyland Exp $ */
package edu.cmu.cs.fluid.control;

/** A control-node with Component-specific non-flow actions.
 * ComponentFlow are used to represent uses of constants and variables,
 * @author John Tang Boyland
 * @see ComponentChoice
 */

public class ComponentFlow extends Flow implements ComponentNode, MutableComponentNode {
  Component comp;
  Object value;
  public ComponentFlow(Component c, Object v) {
    comp = c;
    value = v;
    if (c != null) c.registerComponentNode(this);
  }
  
  @Override
  public void setComponent(Component c) {
	comp = c;
  }

@Override
  public Component getComponent() {
    return comp;
  }
  public Object getValue() {
    return value;
  }
  public Object getInfo() {
    return value;
  }
}

