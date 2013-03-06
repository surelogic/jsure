/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRAbstractState.java,v 1.10 2008/06/27 19:10:35 chance Exp $
 */
package edu.cmu.cs.fluid.ir;

import com.surelogic.*;

import edu.cmu.cs.fluid.FluidError;

/**
 * An IR state that keeps explicit track of the parent.
 * @author boyland
 */
public abstract class IRAbstractState<T> implements IRStoredState<T> {
  /*
  private static ThreadGlobal<IRState> defaultStateParent = new ThreadGlobal<IRState>(null);
  public static void pushDefaultStateParent(IRState p) {
    defaultStateParent.pushValue(p);
  }
  public static void popDefaultStateParent() {
    defaultStateParent.popValue();
  }
  public static IRState getDefaultStateParent() {
    return defaultStateParent.getValue();
  }
  */
  public static IRState getDefaultStateParent() {
    return StoredSlotInfo.defaultStateFactory.create();
  }
  
  /**
   * The parent state of this one.  It may be null if this state
   * does not know its parent yet.  It can be changed from null to
   * the real known parent state, but not changed again.
   * It is protected by this property except for the case that two
   * separate threads attempt to set it at the same time.  This
   * is an error that we do not check for.
   */
  private IRState parent;
  
  /**
   * Create an IR state that has no parent state.
   * 
   * @SingleThreaded
   * @Borrowed this
   */
  public IRAbstractState() {
    this(null);
  }

  /**
   * Create an IR state within the given state.
   * @param p
   * @SingleThreaded
   * @Borrowed this
   */
  @Unique("return")
  public IRAbstractState(IRState p) {
    if (p == null) p = createDefaultStateParent();
    parent = p;
  }

  @Borrowed("this")
  protected IRState createDefaultStateParent() {
	  return getDefaultStateParent();
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRState#getParent()
   */
  @Override
  public IRState getParent() {
    return parent;
  }
  
  /**
   * Set the parent associated with this state.
   * This can only be done once, unless the same value is set.
   * This method does not need to be synchronized because it will
   * always be set to the same value, and ``it is an error'' if
   * it is set to different values in different threads.
   * @param p parent for this state.  Ignored if null.
   */
  @Override
  public void setParent(IRState p) {
    if (p == null) return;
    if (parent != null && !parent.equals(p) && 
        !IRState.Operations.includes(parent,p)) {
      if (IRState.Operations.includes(p,parent)) {
        // we have better information right now
        return;
      }
      throw new FluidError("setParent tries to change parent from " + parent + 
          " to " + p);
    }
    parent = p;
  }
  
  @Override
  public void setParent(SlotInfo<T> si, IRNode n) {
	  setParent(new SlotState<T>(si, n));
  }
  
  /**
   * Return the slotfactory responsible for (<em>most</em> of) this state.
   * This is the slot factory that is informed when the state indicates that it has changed.
   * @return slot factory in charge of this state.
   */
  protected abstract SlotFactory getSlotFactory();
  
  /**
   * A change has happened; notify observers.
   * This simply calls {@link SlotFactory#noteChange(IRState)}
   */
  protected void noteChanged() {
    getSlotFactory().noteChange(this);
  }
  
  // keep track of some things
  { edu.cmu.cs.fluid.util.CountInstances.add(this); }
}
