/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/DependentVersionedSlot.java,v
 * 1.5 2003/07/02 20:19:23 thallora Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.PrintStream;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.SlotFactory;
import edu.cmu.cs.fluid.ir.SlotImmutableException;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.ir.SlotUnknownException;

/**
 * A dependent versioned slot is a slot that relies on external context to
 * ensure the versioned structure has been consulted for the operation. It
 * assumes that it has all information about a slot. It serves as an abstract
 * class with a few predefined operations, in particular it includes an initial
 * value.
 */
abstract class DependentVersionedSlot<T> extends VersionedSlot<T> {
  /**
	 * Logger for this class
	 */
  protected static final Logger LOG = SLLogger.getLogger("IR.version");

  /*
   * This may look type-unsafe but works because we never return the undefined value from here.
   */
  @SuppressWarnings("unchecked")
  protected T initialValue = (T) undefinedValue;

  /** Create a versioned slot with no initial value */
  public DependentVersionedSlot() {
    super();
  }

  /** Create a versioned slot with an initial value */
  public DependentVersionedSlot(T initial) {
    super();
    // LOG.debug("Creating DependentVersionedSlot with initial = "+initial);
    initialValue = initial;
  }

  @Override
  public void describe(PrintStream out) {
    super.describe(out);
    if (initialValue == undefinedValue) {
      out.println("  initially undefined");
    } else {
      out.println("  initial: " + initialValue);
    }
  }

  /**
	 * The number of version-value pairs in this versioned slot. Used primarily
	 * for debugging and statistics.
	 */
  @Override
  public int size() {
    return 0;
  }

  /**
	 * Return the value of this slot at a particular version.
	 * 
	 * @throws SlotUndefinedException
	 *           if the slot does not have a value.
	 */
  @Override
  public T getValue(Version v)
    throws SlotUndefinedException, SlotUnknownException {
    if (initialValue == undefinedValue)
      throw new SlotUndefinedException("no initial value for versioned slot");
    return initialValue;
  }

  @Override
  protected VersionedSlot<T> setValue(Version v, T val) {
    if (v == Version.getInitialVersion()) {
      initialValue = val;
      return this;
    } else {
      throw new SlotImmutableException();
    }
  }

  /**
	 * Does the slot have a value at the given version?
	 */
  @Override
  public boolean isValid(Version v) {
    return initialValue != undefinedValue;
  }
  
  public SlotFactory getSlotFactory() {
    if (this instanceof VersionedDerivedSlot) {
      LOG.warning("dangerous request to get factory of bidirectional slot.");
      return VersionedSlotFactory.bidirectional(((VersionedDerivedSlot<T>)this).getRootVersion());
    } else
      return VersionedSlotFactory.dependent;
  }
}
