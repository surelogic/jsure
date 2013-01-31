/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/ComponentSink.java,v 1.3 2005/05/25 03:28:36 boyland Exp $ */
package edu.cmu.cs.fluid.control;

/** A control-node sink with component specific meaning.
 * ComponentSink nodes are used to represent (for example)
 * method exit nodes (not returns).
 * @author John Tang Boyland
 * @see ComponentFlow
 * @see ComponentSource
 */

public class ComponentSink extends Sink implements ComponentNode {
  Component comp;
  Object value;
  public ComponentSink(Component c, Object v) {
    comp = c;
    value = v;
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

