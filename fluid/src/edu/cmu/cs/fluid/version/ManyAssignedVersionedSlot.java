/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/ManyAssignedVersionedSlot.java,v
 * 1.6 2003/07/08 02:39:39 boyland Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRType;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;

/**
 * A many assigned versioned slot is one with multiple values at multiple
 * versions. It handles the full generality of versioned slots unlike special
 * cases {@link UnassignedVersionedSlot}and {@link OnceAssignedVersionedSlot}.
 */
class ManyAssignedVersionedSlot<T> extends DependentVersionedSlot<T> {
  // Two parallel list to avoid creation of objects.
  // They are always kept the same size
  private List<T> valuesLog = new ArrayList<T>();
  private List<Version> versionsLog = new ArrayList<Version>();

  /** Create a versioned slot with no values assigned (yet). */
  public ManyAssignedVersionedSlot() {
    super();
  }

  /** Create a versioned slot with a single version/value pair. */
  public ManyAssignedVersionedSlot(
    T initialValue,
    Version v,
    T value) {
    super(initialValue);
    setValue(v, value);
  }

  /**
	 * Create a versioned slot from an existing one. Not public because this is
	 * only legal when the old one is no longer used.
	 */
  ManyAssignedVersionedSlot(List<Version> versions, List<T> values) {
    super();
    versionsLog = versions;
    valuesLog = values;
  }

  @Override
  public synchronized void describe(PrintStream out) {
    super.describe(out);
    int length = versionsLog.size();
    for (int i = 0; i < length; i++) {
      out.println("  " + versionsLog.get(i) + ": " + valuesLog.get(i));
    }
  }

  @Override
  public int size() {
    return versionsLog.size();
  }

  /**
	 * Return the index into the lists for this version. It returns
   * the index of the version that is closest to the version argument.
   * AT the same time, it removes destroyed versions.
   * @return -1 if not found, 0..size-1 if found.
	 */
  private int search(Version v) {
    int i=0; 
    int n=versionsLog.size();
    int bestIndex = -1;
    Version bestVersion = Version.getInitialVersion();
    while (i<n) {
      Version v2 = versionsLog.get(i);
      if (v2.isDestroyed()) {
        versionsLog.remove(i);
        valuesLog.remove(i);
        --n;
        continue;
      }
      if (v.comesFrom(v2) && v2.depth() > bestVersion.depth()) {
        bestVersion = v2;
        bestIndex = i;
      }
      ++i;
    }
    return bestIndex;
  }
  

  // cache the last effects of getValueLocal
  private Version lastGetVersion = null;
  private T lastGetValue = null;

  /**
	 * Get the value of this slot (possibly "undefinedValue") at the given
	 * version. Caches last call for speed. The lock on this slot must be held.
	 */
  private T getValueLocal(Version v) {
    if (v == lastGetVersion)
      return lastGetValue;

    int index = search(v);

    T value;

    if (index < 0) { // not found
      value = initialValue;
    } else { // found
      value = valuesLog.get(index); 
    }

    lastGetVersion = v;
    lastGetValue = value;

    return value;
  }

  @Override
  public boolean isValid(Version v) {
    // this is reasonable because the value will be cached.
    return (getValueLocal(v) != undefinedValue);
  }

  /**
	 * We overwrite the value at this version to be the value given. This
	 * algorithm works even when the version already has children, as can happen
	 * when reading in version-value pairs from a file.
	 * <p>
	 * "Redundant" version-value pairs are not safe to ignore since we may load
	 * an intermediate delta or snapshot. As a result, every snapshot requires a
	 * lot of space in-core as well as in the persistent file.
	 */
  @Override
  protected synchronized VersionedSlot<T> setValue(Version v, T newValue) {
    if (lastGetVersion == v)
      lastGetValue = newValue;

    int index = search(v);

    if (index >= 0 && versionsLog.get(index) == v) {
      valuesLog.set(index, newValue);
    } else  {
      versionsLog.add(v);
      valuesLog.add(newValue);
    }
    return this;
  }

  /**
	 * Return the version of this slot for a particular version. This code
	 * assumes that it is protected against accessing data of unloaded
	 * information.
	 * 
	 * @throws SlotUndefinedException
	 *           if slot explicitly undefined at this version.
	 * @see IndependentVersionedSlot
	 */
  @Override
  public synchronized T getValue(Version v) {
    T value = getValueLocal(v);

    if (value == undefinedValue)
      throw new SlotUndefinedException("undefined for " + v);

    return value;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.version.VersionedSlot#getLatestChange(edu.cmu.cs.fluid.version.Version)
   */
  @Override
  public synchronized Version getLatestChange(Version v) {
    int index = search(v);
    if (index < 0)
      return Version.getInitialVersion();
    
    Version possibleChange;
    T value = valuesLog.get(index);

    // NB: equals() is a no-no.
    
    for (;;) {
      possibleChange = versionsLog.get(index);
      Version p = possibleChange.parent();
      if (p == null) break;
      index = search(p);
      if (index < 0) {
        if (initialValue != value) break;
        return Version.getInitialVersion();
      }
      T val2 = valuesLog.get(index);
      if (val2 != value) break;
    }
    return possibleChange;
  }

  /* persistence */

  /**
	 * Return true if this version slot has a recorded change for the era
	 * specified. <strong>Warning</strong>: this does not test whether the
	 * versioned slot was changed in the era.
	 */
  @Override
  public synchronized boolean isChanged(Era era) {
    //!! simple for now
    //!! use a more complicate binary search if this turns out to be costly.
    for (int i = 0; i < versionsLog.size(); ++i) {
      if (isDelta(era, i))
        return true;
    }
    return false;
  }

  /**
	 * Compute whether the entry in the log at position i reflects an assignment
	 * for this era. The version must be in the era, and the value assigned must
	 * not simply be a duplicate of the value for a parent.
	 */
  private synchronized boolean isDelta(Era era, int i) {
    Version v = versionsLog.get(i);
    if (!era.contains(v))
      return false;
    T value = valuesLog.get(i);
    // an undefined value never reflects a change
    if (value == undefinedValue)
      return false;
    T oldValue = getValueLocal(v.parent());
    /*
		 * now compare old and new values: we don't use equals because this
     * has teh wrong semantics for IR sequences.
		 */
    return value != oldValue;
  }

  /*
	 * Write the values for this slot out for the given era. <p><strong>
	 * Warning: </strong> This method will write incorrect values if called
	 * without ensuring it has all the information loaded in. <p> This method
	 * changes the current version.
	 */
  @Override
  public synchronized void writeValues(IRType<T> ty, IROutput out, Era era)
    throws IOException {
    //!! Simple for now.
    //!! If too slow, use a binary search
    for (int i = 0; i < versionsLog.size(); ++i) {
      if (isDelta(era, i)) {
        Version v = versionsLog.get(i);
        T val = valuesLog.get(i);
        writeVersionValue(ty, v, val, out);
      }
    }
  }
  
  public String debugString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < versionsLog.size(); ++i) {
      Version v = versionsLog.get(i);
      T val = valuesLog.get(i);
      sb.append(v);
      sb.append("->");
      sb.append(val);
      sb.append(' ');
    }
    return sb.toString();
  }
}
