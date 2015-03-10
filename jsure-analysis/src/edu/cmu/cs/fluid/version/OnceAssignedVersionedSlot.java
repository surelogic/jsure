/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/OnceAssignedVersionedSlot.java,v 1.12 2007/07/05 18:15:13 aarong Exp $ */
package edu.cmu.cs.fluid.version;

import java.io.IOException;
import java.io.PrintStream;

import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRType;

/** An versioned slot that has only been assigned once.
 * This kind of versioned slot can be more efficiently
 * handled than a full versioned slot because the latter slot
 * requires two vectors for each slot.
 * <p>
 * This slot depends on its context to ensure that it isn't requested
 * for its value for an unloaded version.
 * @see ManyAssignedVersionedSlot
 * @see IndependentVersionedSlot
 */
class OnceAssignedVersionedSlot<T>extends DependentVersionedSlot<T> {
  final protected Version version;
  protected T value;

  public OnceAssignedVersionedSlot(Version v, T newValue) {
    super();
    version = v;
    value = newValue;
  }

  public OnceAssignedVersionedSlot(T i, Version v, T newValue) {
    super(i);
    version = v;
    value = newValue;
  }

  /** Express this in the general form so we can then add pairs. */
  protected VersionedSlot<T> generalize() {
    VersionedSlot<T> g;
    if (version.isDestroyed()) {
      g = UnassignedVersionedSlot.<T>create(initialValue);
    } else {
      g = new ManyAssignedVersionedSlot<T>(initialValue, version, value);
    }
    retire(); // only if new creation worked
    return g;
  }

  @Override
  public void describe(PrintStream out) {
    super.describe(out);
    Object val;
    synchronized (this) {
      val = value;
    }
    out.print("  " + version + ": ");
    out.println(val);
  }

  @Override
  public int size() {
    return 1;
  }

  // slot methods:

  @Override
  public boolean isValid(Version v) {
    if (!version.isDestroyed() && v.comesFrom(version))
      return true;
    return super.isValid(v);
  }

  @Override
  public T getValue(Version v) {
    if (!version.isDestroyed() && v.comesFrom(version)) {
      synchronized (this) {
        return value;
      }
    } else {
      // LOG.fine("Version "+v+" not from version "+version);
      return super.getValue(v);
    }
  }

  @Override
  public Version getLatestChange(Version v) {
    if (v.comesFrom(version)) {
      return version;
    } else {
      return Version.getInitialVersion();
    }
  }

  @Override
  public synchronized VersionedSlot<T> setValue(Version v, T newValue) {
    // System.out.println(this + ".setValue(" + v + "," + newValue + ")");
    if (version.equals(v)) {
      value = newValue;
      return this;
    } else {
      return generalize().setValue(v, newValue);
    }
  }

  /* persistence */

  @Override
  public boolean isChanged(Era e) {
    return version.isDestroyed() || e.contains(version);
  }

  /* Write the change as a version,value sequence.
   * <p>
   * This method changes the current version.
   */
  @Override
  protected void writeValues(IRType<T> ty, IROutput out, Era e)
    throws IOException {
    if (isChanged(e)) {
      writeVersionValue(ty, version, value, out);
    }
  }
}
