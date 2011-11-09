/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotInfoDefinedEvent.java,v 1.3 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

public class SlotInfoDefinedEvent<T> extends SlotInfoEvent<T> {
  public SlotInfoDefinedEvent(SlotInfo<T> attr, IRNode n, T newVal) {
    super(attr,n,newVal);
  }
}
