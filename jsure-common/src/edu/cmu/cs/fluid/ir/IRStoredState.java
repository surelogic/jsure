/*
 * Created on Sep 24, 2004
 *
 */
package edu.cmu.cs.fluid.ir;


/**
 * @author Edwin
 *
 */
public interface IRStoredState<T> extends IRState {
  /**
   * Set the parent associated with this state.
   * This can only be done once, unless the same value is set.
   * This method does not need to be synchronized because it will
   * always be set to the same value, and ``it is an error'' if
   * it is set to different values in different threads.
   * @param p parent for this state.  Ignored if null.
   */
  void setParent(IRState p);
  
  /**
   * Same as above, but creates an IRState if needed
   */
  void setParent(SlotInfo<T> si, IRNode n);
}
