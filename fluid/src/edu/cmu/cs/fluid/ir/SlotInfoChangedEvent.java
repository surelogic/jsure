/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotInfoChangedEvent.java,v 1.3 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

public class SlotInfoChangedEvent<T> extends SlotInfoEvent<T> {
  private final T oldValue;
  public SlotInfoChangedEvent(SlotInfo<T> attr, IRNode n, 
  			      T oldVal, T newVal) 
  {
    super(attr,n,newVal);
    oldValue = oldVal;
  }
  
  public T getOldValue() {
    return oldValue;
  }
}
