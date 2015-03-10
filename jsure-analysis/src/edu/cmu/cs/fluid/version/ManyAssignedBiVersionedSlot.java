/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/ManyAssignedBiVersionedSlot.java,v 1.9 2007/08/21 23:35:57 boyland Exp $
 */
package edu.cmu.cs.fluid.version;

/**
 * A mutable bidirectional derived versioned slot with
 * multiple assignments.
 * @see VersionedSlotFactory.bidirectional
 * @author boyland
 */
public class ManyAssignedBiVersionedSlot<T> extends ManyAssignedVersionedSlot<T> implements VersionedDerivedSlot<T> {

  Version rootVersion;

  /**
   * @param rv root version
   * @param initialValue value everywhere except otherwise stated
   * @param v version where value is different
   * @param value value at this version
   */
  public ManyAssignedBiVersionedSlot(Version rv, T initialValue, Version v,
      T value) {
    super(initialValue, v, value);
    if (rv == null) throw new NullPointerException("root version of many assigned bi slot can't be null");
    if (!v.comesFrom(rv)) {
      LOG.severe("Bidirectional slot needs root version early");
    }
    rootVersion = rv;
  }

  @Override
  protected VersionedSlot<T> setValue(Version v, T newValue) {
    if (rootVersion == null) return super.setValue(v,newValue); // not yet initialized
    return (VersionedSlot<T>) setValue(newValue,v);
  }
  
  @Override
  public synchronized VersionedDerivedSlot<T> setValue(T newValue, Version v) {
    // TODO: avoid redundant set calls.  This is safe for BI slots since they handle contiguous trees.
    if (v == null) throw new NullPointerException("can't set using a null version");
    if (v == rootVersion) {
      initialValue = newValue;
      /*LOG.warning("Changing value at root version of bidirectional slot");
      rootVersion = rootVersion.parent();
      super.setValue(v, newValue);*/
    } else {
      Version lca = Version.latestCommonAncestor(rootVersion, v);
      if (lca == v) {
        Version vc = v.nextToward(rootVersion);
        rootVersion = lca;
        T oldInitialValue = initialValue;
        initialValue = newValue;
        super.setValue(vc, oldInitialValue);
      } else {
        rootVersion = lca;
        super.setValue(v, newValue);
      }
    }
    return this;
  }

  @Override
  public Version getRootVersion() {
    return rootVersion;
  }
  
  public static boolean debugGetValue;
  
  @Override
  public synchronized T getValue(Version v) {
    if (debugGetValue) {
      System.out.println("in getValue: " + debugString());
    }
    // TODO Auto-generated method stub
    return super.getValue(v);
  }
  
  
}
