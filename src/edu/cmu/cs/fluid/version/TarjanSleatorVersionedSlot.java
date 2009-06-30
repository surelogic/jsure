/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/ManyAssignedVersionedSlot.java,v
 * 1.6 2003/07/08 02:39:39 boyland Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Level;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRType;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;

/**
 * A many assigned versioned slot is one with multiple values at multiple
 * versions. It handles the full generality of versioned slots unlike special
 * cases {@link UnassignedVersionedSlot}and {@link OnceAssignedVersionedSlot}.
 * This class uses the technique outlined by Tarjan and Sleator for "fat nodes."
 */
class TarjanSleatorVersionedSlot<T> extends DependentVersionedSlot<T> {
  // Two parallel list to avoid creation of objects.
  // They are always kept the same size
  // The versions are in pre-order order (using Version#precedes)
  private List<T> valuesLog = new ArrayList<T>();
  private List<Version> versionsLog = new ArrayList<Version>();

  /** Create a versioned slot with no values assigned (yet). */
  public TarjanSleatorVersionedSlot() {
    super();
  }

  /** Create a versioned slot with a single version/value pair. */
  public TarjanSleatorVersionedSlot(
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
  TarjanSleatorVersionedSlot(List<Version> versions, List<T> values) {
    super();
    versionsLog = versions;
    valuesLog = values;
  }

  @Override
  public synchronized void describe(PrintStream out) {
    super.describe(out);
    int length = versionsLog.size();
    for (int i = 0; i < length; i++) {
      System.out.println("  " + versionsLog.get(i) + ": " + valuesLog.get(i));
    }
  }

  @Override
  public int size() {
    return versionsLog.size();
  }

  /**
	 * Return the index into the lists for this version. If the version is found,
	 * we return -(index + 1). Otherwise we return the index. where the version
	 * should be inserted: 0 being at the beginning and size() at the end. If
	 * destroyed versions are found, they are removed before an index is
	 * returned. The lock on this slot must be held before doing this action.
	 */
  private int search(Version v) {
    return search(v, 0, versionsLog.size());
  }
  // special case with bounds restricted
  private int search(Version v, int min, int max) {
    normalCase : {
      int index;

      while (min < max) {
        index = (min + max) / 2;
        Version v2 = versionsLog.get(index);
        if (v2.isDestroyed())
          break normalCase;
        if (v.precedes(v2)) {
          max = index;
        } else {
          if (v.equals(v2)) {
            return -index - 1;
          }
          min = index + 1;
        }
      }
      return max;
    }

    // otherwise we have some destroyed versions.
    Iterator it1 = versionsLog.iterator();
    Iterator it2 = valuesLog.iterator();
    int removed = 0;
    while (it1.hasNext()) {
      Version vold = (Version) it1.next();
      it2.next(); // for side-effect
      if (vold.isDestroyed()) {
        it1.remove();
        it2.remove();
        ++removed;
      }
    }
    if (removed == 0) {
      throw new FluidError("assertion failure: found no destroyed versions");
    }
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("removed " + removed + " entries from versioned slot " + this);
    }
    return search(v); // try again (without bounds)
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

    if (index < 0) { // the version was found
      value = valuesLog.get(-index - 1);
    } else if (index == 0) { // before the first version:
      value = initialValue;
    } else { // inbetween two entries
      value = valuesLog.get(index - 1); // get entry before this one
    }

    lastGetVersion = v;
    lastGetValue = value;

    return value;
  }

  @Override
  public boolean isValid(Version v) {
    //? need something better than this?
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

    if (index < 0) {
      valuesLog.set(-index - 1, newValue);
      return this;
    }

    /*
		 * Now do a little check (for debugging purposes) as to whether this change
		 * is really necessary.
		 */
    if (LOG.isLoggable(Level.FINE)) {
      T former_value = index > 0 ? valuesLog.get(index - 1) : initialValue;
      if (newValue == former_value) {
        LOG.finer(
          "Inserting redundant " + v + " : " + newValue + " into " + this);
      }
    }

    Version nextInOrder = v.getNextInPreorderNoKids();
    Version nextInLog;
    if (index >= versionsLog.size()) {
      nextInLog = null;
    } else {
      nextInLog = versionsLog.get(index);
    }

    /**
		 * We need to insert another value if the next version after v in the order
		 * is not already in the log. This is accomplished by checking the
		 * following version in the log. If this is the later version (in the
		 * order) or if it is a descendant of the current version (in which case we
		 * assume the check has already been carried out), then no entry need be
		 * inserted.
		 */
    if (nextInOrder != null
      && !nextInOrder.equals(nextInLog)
      && (nextInLog == null || !nextInLog.comesFrom(v))) {
      T former_value = index > 0 ? valuesLog.get(index - 1) : initialValue;
      valuesLog.add(index, former_value);
      versionsLog.add(index, nextInOrder);
    }

    valuesLog.add(index, newValue);
    versionsLog.add(index, v);

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

  @Override
  public synchronized Version getLatestChange(Version v) {
    int index = search(v);
    if (index == 0)
      return Version.getInitialVersion();
    if (index < 0)
      index = -index;
    --index; // move to where entry is.
    T currentValue = valuesLog.get(index);

    Version possibleChange;
    T value;

    do {
      possibleChange = versionsLog.get(index);
      index = search(possibleChange.parent(), 0, index);
      if (index < 0)
        index = -index;
      --index;
      if (index < 0) {
        value = initialValue;
      } else {
        value = valuesLog.get(index);
      }
    } while (index >= 0 && currentValue == value);

    if (index < 0 && currentValue == value)
      return Version.getInitialVersion();
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
		 * now compare old and new values: we don't bother using equals because the
		 * only time a duplicate is put in place by the system it just uses the
		 * same pointer.
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
