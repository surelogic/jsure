/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotInfoEvent.java,v 1.3 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

import java.util.EventObject;

/** An event that a mutable attribute of a node has received a new value.
 * @see SlotInfo
 * @see SlotInfoDefinedEvent
 * @see SlotInfoChangedEvent
 */
public abstract class SlotInfoEvent<T> extends EventObject {
  private final SlotInfo<T> attribute;
  private final IRNode node;
  private final T newValue;
  
  public SlotInfoEvent(SlotInfo<T> attr, IRNode n, T newVal) {
    super(attr);
    attribute = attr;
    node = n;
    newValue = newVal;
  }

  public SlotInfo<T> getSlotInfo() {
    return attribute;
  }
  public IRNode getNode() {
    return node;
  }
  public T getNewValue() {
    return newValue;
  }
}
