/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotInfoWrapper.java,v 1.8 2007/07/05 18:15:15 aarong Exp $ */
package edu.cmu.cs.fluid.ir;

import edu.cmu.cs.fluid.util.ImmutableSet;

/**
 * A class that delegates all requests for attribute values
 * to another slot info.  It can be used as the base
 * for implementing various kinds of wrapped attributes.
 * <p>Known Bugs:
 * <ol>
 *   <li> Observers/listeners will notice nothing.
 * </ol>
 */
public class SlotInfoWrapper<T> extends SlotInfo<T> {
  protected final SlotInfo<T> wrapped;
  /** Construct a wrapper around the given attribute. */
  public SlotInfoWrapper(SlotInfo<T> attr) {
    super();
    wrapped = attr;
  }

  @Override
  public int size() {
    return wrapped.size();
  }
  
  @Override
  public IRType<T> type() {
    return wrapped.type();
  }

  @Override
  protected boolean valueExists(IRNode node) {
    return node.valueExists(wrapped);
  }

  @Override
  protected T getSlotValue(IRNode node)
      throws SlotUndefinedException
  {
    return node.getSlotValue(wrapped);
  }

  @Override
  protected void setSlotValue(IRNode node, T newValue)
      throws SlotImmutableException
  {
    node.setSlotValue(wrapped,newValue);
  }

  @Override
  public ImmutableSet<IRNode> index(T value) {
    return wrapped.index(value);
  }
}
