/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/UnassignedBiVersionedSlot.java,v 1.9 2007/07/10 22:16:33 aarong Exp $
 */
package edu.cmu.cs.fluid.version;

import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.ir.SlotUnknownException;


/**
 * A bidirectional derived versioned slot with either no assignments
 * at all, or simply the first assignment.  This slot is immutable.
 * @see VersionedSlotFactory#bidirectional
 * @author boyland
 */
class UnassignedBiVersionedSlot<T> extends UnassignedVersionedSlot<T> implements VersionedDerivedSlot<T> {
  
  private final Version rootVersion;
  
  /**
   * Create an immutable bidrectional versioned slot with
   * no value yet.  When it is assigned, a new slot will be created.
   */
  public UnassignedBiVersionedSlot(Version v) {
    super();
    rootVersion = v;
  }

  /**
   * Create an immutable bidirectional versioned slot
   * with an initial value.
   * @param value default/initial value
   */
  public UnassignedBiVersionedSlot(Version v, T value) {
    super(value);
    rootVersion = v;
  }

  @Override
  protected VersionedSlot<T> setValue(Version v, T newValue) {
    return (VersionedSlot<T>) setValue(newValue, v);
  }
  
  @Override
  public VersionedDerivedSlot<T> setValue(T newValue, Version newVersion) {
    // easiest to give the work to RootedBiVersionedSlot
    RootedBiVersionedSlot<T> mutable = new RootedBiVersionedSlot<T>(rootVersion,initialValue);
    return mutable.setValue(newValue,newVersion);
  }
  
  @Override
  public Version getRootVersion() {
    return rootVersion;
  }

  @Override
  public T getValue() throws SlotUndefinedException, SlotUnknownException {
    if (ManyAssignedBiVersionedSlot.debugGetValue) {
      System.out.println("  " + rootVersion + "->" + initialValue);
    }
    return super.getValue();
  }
  
  
}
