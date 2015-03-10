/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/OnceAssignedBiVersionedSlot.java,v 1.10 2007/07/05 18:15:13 aarong Exp $
 */
package edu.cmu.cs.fluid.version;

import java.util.logging.Level;


/**
 * A mutable bidirectional derived versioned slot.
 * @see VersionedSlotFactory.bidirectional
 * @author boyland
 */
class OnceAssignedBiVersionedSlot<T> extends OnceAssignedVersionedSlot<T> implements VersionedDerivedSlot<T> {
  Version rootVersion;
  /**
   * @param rv root version
   * @param initial initial value
   * @param v change version
   * @param newValue newValue at change site
   */
  public OnceAssignedBiVersionedSlot(Version rv, T initial, Version v, T newValue) {
    super(initial, v, newValue);
    if (!v.comesFrom(rv)) {
      LOG.severe("Bidirectional slot needs root version early");
    }
    rootVersion = rv;
  }

  @Override
  protected VersionedSlot<T> generalize() {
    VersionedSlot<T> g;
    if (version.isDestroyed()) {
      g = new RootedBiVersionedSlot<T>(rootVersion,initialValue);
    } else {
      g = new ManyAssignedBiVersionedSlot<T>(rootVersion,initialValue, version, value);
    }
    retire(); // only if new creation worked
    return g;
  }
  
  @Override
  public VersionedSlot<T> setValue(Version v, T newValue) {
    return (VersionedSlot<T>) setValue(newValue,v);
  }
  
  @Override
  public synchronized VersionedDerivedSlot<T> setValue(T newValue, Version v) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine(debugString()+": set " + v + "->" + newValue);
    }
    if (v == version) {
      value = newValue;
      return this;
    } else if (v == rootVersion) {
      initialValue = newValue;
      return this;
    } else if (newValue == value) {
      if (v.comesFrom(version)) return this; // no change needed
      if (version.comesFrom(v) && v.comesFrom(rootVersion)) {
        LOG.warning("Bidirectional slot(" + rootVersion + "->" + initialValue + 
            "," + version + ") assigned for intermediate version " + v + "->" + value);
      }
      // fall through
    } else if (newValue == initialValue) {
      if (!v.comesFrom(version)) {
        // Avoid moving root version:
        // rootVersion = Version.latestCommonAncestor(rootVersion,version);
        return this;
      }
      // fall through
    }
    return (VersionedDerivedSlot<T>)super.setValue(v, newValue);
  }

  @Override
  public Version getRootVersion() {
    return rootVersion;
  }
  
  @Override
  public T getValue(Version v) {
    if (ManyAssignedBiVersionedSlot.debugGetValue) {
      System.out.println(debugString());
    }
    return super.getValue(v);
  }

  /**
   * @return
   */
  public String debugString() {
    return rootVersion + "->" + initialValue + ", " + version + "->" + value;
  }
  
  
}
