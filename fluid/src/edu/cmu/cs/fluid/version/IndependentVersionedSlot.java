/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/IndependentVersionedSlot.java,v 1.15 2008/06/24 19:13:12 thallora Exp $ */
package edu.cmu.cs.fluid.version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;

/** A versioned slot that can handle being in any structure.
 * It carries around its own pointer to versioned structure
 * to be checked and notified on accesses.  It wraps
 * either an immutable slot (that returns a new (unique) slot
 * on a change) or else a unique slot (that it alone changes.)
 */
class IndependentVersionedSlot<T> extends IRAbstractState implements Slot<T> {
  /**
   * Logger for this class
   */
  protected static final Logger LOG =
	  SLLogger.getLogger("IR.version");

  final /* unique OR immutable */ VersionedSlot<T> base;

  /** Wrap an existing versioned slot */
  private IndependentVersionedSlot(IRState c,
				  /*@ unique OR immutable */ VersionedSlot<T> vs) {
    super(c);
    base = vs;
  }

  /** Create a new independent initially undefined versioned slot.
   */
  public IndependentVersionedSlot(IRState c) {
    super(c);
    base = UnassignedVersionedSlot.<T>create();
  }

  /** Create a new independent versioned slot.
   * @param value initial value (at alpha)
   */
  public IndependentVersionedSlot(T value, IRState c) {
    super(c);
    base = UnassignedVersionedSlot.<T>create(value);
  }

  @Override
  public void describe(PrintStream out) {
    base.describe(out);
  }

  public int size() {
    return base.size();
  }

  @Override
  public boolean isValid() {
    return Version.isCurrentlyLoaded(this) && base.isValid();
  }

  /** Set the value of the slot for this version.
   * and inform the current version
   * of the change.
   */
  @Override
  public Slot<T> setValue(T newValue) {
    if (!Version.isCurrentlyLoaded(this)) {
      throw new SlotImmutableException("slot not loaded for current version");
    }
    Slot<T> newb = base.setValue(newValue);
    Version.noteCurrentlyChanged(this);
    if (base != newb) {
      return new IndependentVersionedSlot<T>(getParent(),(VersionedSlot<T>)newb);
    }
    return this;
  }

  @Override
  public T getValue() {
    if (!Version.isCurrentlyLoaded(this)) {
      throw new SlotImmutableException("slot not loaded for current version");
    }
    return base.getValue();
  }

  public Version getLatestChange(Version v) {
    return base.getLatestChange(v);
  }

  private void checkEra(Era e) {
    while (!e.isLoaded(this))
      new SlotUnknownEraException("Slot not loaded at this time", this, e)
        .handle();
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.Slot#isChanged()
   */
  @Override
  public boolean isChanged() {
    return base.isChanged();
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.Slot#readValue(edu.cmu.cs.fluid.ir.IRType, edu.cmu.cs.fluid.ir.IRInput)
   */
  @Override
  public Slot<T> readValue(IRType<T> ty, IRInput in) throws IOException {
    Slot newb = base.readValue(ty,in);
    if (newb != base) {
      return new IndependentVersionedSlot<T>(getParent(),(VersionedSlot<T>)newb);
    }
    return this;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.Slot#writeValue(edu.cmu.cs.fluid.ir.IRType, edu.cmu.cs.fluid.ir.IROutput)
   */
  @Override
  public void writeValue(IRType<T> ty, IROutput out) throws IOException {
    if (getParent() == null) {
      LOG.warning("IndependentVersionedSlot not connected to persistable state! " + this);
    }
    Era e = VersionedSlot.getEra();
    if (e != null) checkEra(e);
    base.writeValue(ty,out);
  }
  
  @Override
  protected SlotFactory getSlotFactory() {
    return VersionedSlotFactory.prototype;
  }
}
