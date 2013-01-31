/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/RootedBiVersionedSlot.java,v 1.3 2007/04/09 06:50:27 boyland Exp $
 */
package edu.cmu.cs.fluid.version;

import java.util.logging.Level;

import edu.cmu.cs.fluid.ir.SlotUndefinedException;
import edu.cmu.cs.fluid.ir.SlotUnknownException;


/**
 * A bidirectional derived versioned slot with just the assignment at the root version.
 * @see VersionedSlotFactory#bidirectional
 * @author boyland
 */
class RootedBiVersionedSlot<T> extends UnassignedVersionedSlot<T> implements VersionedDerivedSlot<T> {

  final Version rootVersion;
  
  /**
   * Create a rooted bidriectional slot around the given root version,
   * with a default undefined value.
   * @param root root version (must not be null)
   */
  public RootedBiVersionedSlot(Version root) {
    super();
    if (root == null) throw new NullPointerException("root cannot be null version");
    rootVersion = root;
  }
  
  /*
   * Create an immutable bidirectional versioned slot
   * with an initial value.  The ``root version'' is the current version.
   * @param value default/initial value
   *
  public RootedBiVersionedSlot(T value) {
    this(Version.getVersionLocal(),value);
  }*/

  /**
   * Create an immutable bidirectional versioned slot
   * with an initial value.  
   * @param root The ``root version''
   * @param value default/initial value
   */
  public RootedBiVersionedSlot(Version root, T value) {
    super(value);
    if (root == null) throw new NullPointerException("root cannot be null version");
    rootVersion = root;
  }

  @Override
  protected VersionedSlot<T> setValue(Version v, T newValue) {
    return (VersionedSlot<T>) setValue(newValue, v);
  }
  
  @Override
  public VersionedDerivedSlot<T> setValue(T newValue, Version newVersion) {
    if (rootVersion == newVersion) {
      if (newValue == initialValue) { // stupid collections:  || newValue.equals(initialValue)) {
        return this; // no change.
      }
      initialValue = newValue;
      return this;
    } else if (newValue == initialValue) { // || newValue.equals(initialValue)) {
      return this; // no change needed
    }
    if (debugSetValue || LOG.isLoggable(Level.FINEST)) {
      LOG.info("Assigning Rooted(" + rootVersion + "->" + 
          initialValue + ") for " + newVersion + "->" + newValue);
    }
    Version lca = Version.latestCommonAncestor(newVersion,rootVersion);
    if (lca == newVersion) {
      Version vc = newVersion.nextToward(rootVersion);
      return new OnceAssignedBiVersionedSlot<T>(lca,newValue,vc,initialValue);
    } else {
      return new OnceAssignedBiVersionedSlot<T>(lca,initialValue,newVersion,newValue);
    }
  }

  @Override
  public Version getRootVersion() {
    return rootVersion;
  }
  
  private boolean debugSetValue = false;
  
  @Override
  public T getValue() throws SlotUndefinedException, SlotUnknownException {
    if (ManyAssignedBiVersionedSlot.debugGetValue) {
      System.out.println("  " + rootVersion + "->" + initialValue);
      debugSetValue = true;
    }
    return super.getValue();
  }
  
  
}
