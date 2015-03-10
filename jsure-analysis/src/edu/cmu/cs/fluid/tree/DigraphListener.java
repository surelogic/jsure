/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/tree/DigraphListener.java,v 1.2 2003/07/02 20:19:09 thallora Exp $ */
package edu.cmu.cs.fluid.tree;

import java.util.EventListener;

/** A class of event observers that are notified when 
 * something is changed in a directed graph.
 * @see DigraphEvent
 */
public interface DigraphListener extends EventListener {
  /** Called when structure is added or changed. */
  public void handleDigraphEvent(DigraphEvent e);
}
