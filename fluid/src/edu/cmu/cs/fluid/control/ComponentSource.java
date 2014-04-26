/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/ComponentSource.java,v 1.3 2005/05/25 03:28:36 boyland Exp $ */
package edu.cmu.cs.fluid.control;

/** A control-node source with component specific meaning.
 * ComponentSource nodes are used to represent (for example)
 * method entry nodes.
 * @author John Tang Boyland
 * @see ComponentFlow
 * @see ComponentSink
 */

public class ComponentSource extends Source implements ComponentNode, MutableComponentNode {
  Component comp;
  Object value;
  public ComponentSource(Component c, Object v) {
    comp = c;
    value = v;
    c.registerComponentNode(this);
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

