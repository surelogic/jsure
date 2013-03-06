package edu.cmu.cs.fluid.mvc;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * @deprecated
 */
@Deprecated
public abstract class SimpleCallback implements AttributeChangedCallback {
  static void debug(String message) {
    System.out.println(message);
  }

  private final AttributeChangedCallback next;
  private final String[] attrs;

  public SimpleCallback(final AttributeChangedCallback n, final String[] a) {
    next = n;
    attrs = a;
  }

  @Override
  public void attributeChanged(
    final String attr,
    final IRNode node,
    final Object value) {
    for (int i = 0; i < attrs.length; i++) {
      if (attr == attrs[i]) {
        ModelEvent e =
          new AttributeValuesChangedEvent(getModel(), node, attr, value);

        // debug("Firing "+e+" with "+node+", "+attr+", "+value);
        getModelCore().fireModelEvent(e);
        break;
      }
    }
    next.attributeChanged(attr, node, value);
  }

  public abstract Model getModel();
  public abstract ModelCore getModelCore();
}
