/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotInfoListener.java,v 1.3 2003/07/02 20:19:14 thallora Exp $ */
package edu.cmu.cs.fluid.ir;

import java.util.EventListener;

/** A class of event observers that are notified when a
 * slot for a particular attribute gets a value.
 * @see SlotInfoEvent
 */
public interface SlotInfoListener extends EventListener {
  /** Called when a slot for a particular attribute and node is set. */
  public void handleSlotInfoEvent(SlotInfoEvent e);
}
