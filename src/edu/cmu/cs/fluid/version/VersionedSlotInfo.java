/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedSlotInfo.java,v 1.16
 * 2003/07/02 20:19:24 thallora Exp $
 */
package edu.cmu.cs.fluid.version;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;

/**
 * A specialization of InfoStoredSlotInfo for versioned slots. This class takes
 * into account some of the peculiarities of versioned slots: in particular the
 * way they handle {@link #getSlot}when the node is not new.
 */
// TODO: Use a specialized storage to save space for predefined values.
// It will be VersionedSlot<T> OR Object
public class VersionedSlotInfo<T> extends InfoStoredSlotInfo<Slot<T>,T> {
  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("IR.version");

  public VersionedSlotInfo() {
    super(VersionedSlotFactory.dependent.<T>getStorage(), null, new HashedSlots<Slot<T>,T>());
  }
  public VersionedSlotInfo(T defaultValue) {
    super(VersionedSlotFactory.dependent.<T>getStorage(), null, defaultValue, new HashedSlots<Slot<T>,T>());
  }
  public VersionedSlotInfo(String name, IRType<T> type)
    throws SlotAlreadyRegisteredException {
    super(name, type, VersionedSlotFactory.dependent.<T>getStorage(), new HashedSlots<Slot<T>,T>());
  }
  public VersionedSlotInfo(String name, IRType<T> type, T defaultValue)
    throws SlotAlreadyRegisteredException {
    super(
      name,
      type,
      VersionedSlotFactory.dependent.<T>getStorage(),
      defaultValue,
      new HashedSlots<Slot<T>,T>());
  }

  /**
	 * Return the versioned structure associated with this node if any is
	 * available.
	 */
  IRState getState(IRNode node) {
    IRRegion region = IRRegion.getOwnerOrNull(node);
    Bundle b = getBundle();
    if (region == null || b == null) {
      return new SlotState<T>(this,node);
    }
    return IRChunk.get(region,b);
  }

  /**
	 * Check that the state for the slot with this node is properly
	 * loaded at this version.
	 * 
	 * @return true if it is properly loaded.
	 */
  boolean checkState(IRNode node) {
    IRRegion region = IRRegion.getOwnerOrNull(node);
    Bundle b = getBundle();
    if (region == null || b == null) {
      return true;
    }
    return Version.isCurrentlyLoaded(IRChunk.get(region,b));
  }

  /**
	 * Return the value for this versioned slot of a node. It checks that the
	 * version is available by checking the versioned structure.
	 * 
	 * @exception SlotUndefinedException
	 *              If the slot is not initialized with a value.
	 * @exception SlotUnknownException
	 *              The slot's value may not yet be loaded.
	 */
  @Override
  public T getSlotValue(IRNode node)
    throws SlotUndefinedException, SlotUnknownException {
    while (!checkState(node)) {
      // Either fix up via handler, or raise exception
      Version v = Version.getVersionLocal();
      new SlotUnknownVersionException(
        "slot value not defined for " + v +": debug = " + VersionedChunk.debugIsDefined,
        new SlotInfoSlot<T>(this, node),
        v)
        .handle();
    }
    try {
      T val = super.getSlotValue(node);
      return val;
    } catch (SlotUndefinedException e) {
      LOG.log(Level.FINE, "Slot undefined on node " + node, e);
      throw e;
    }
  }

  @Override
  public boolean valueExists(IRNode node) {
    return checkState(node) && super.valueExists(node);
  }

  /**
	 * Set the value for this versioned slot for the node. It informs the
	 * versioned structure it is in that a change has happened here.
	 */
  @Override
  public void setSlotValue(IRNode node, T newValue) {
    if (!checkState(node)) {
      Version v = Version.getVersionLocal();
      throw new SlotImmutableException("slot value not defined for " + v);
    }
    super.setSlotValue(node, newValue);
    IRState state = getState(node);
    if (newValue instanceof IRStoredState) {
      ((IRStoredState)newValue).setParent(state);
    }
    Version.noteCurrentlyChanged(state);
  }
}
