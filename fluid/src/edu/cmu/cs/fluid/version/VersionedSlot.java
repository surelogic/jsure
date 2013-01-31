/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedSlot.java,v
 * 1.32 2003/08/22 04:01:51 boyland Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.IntegerTable;
import edu.cmu.cs.fluid.util.ThreadGlobal;

/**
 * A versioned slot is a slot that may be assigned once every version. In fact
 * assigning a versioned slot normally increments the version.
 */
/*
 * $Log: VersionedSlot.java,v $
 * Revision 1.44  2008/06/24 19:13:12  thallora
 * Deleted FluidLogger, now using SLLogger
 *
 * Revision 1.43  2008/01/24 15:38:33  chance
 * Made undefinedValue into a constant
 *
 * Revision 1.42  2007/05/25 02:12:41  boyland
 * Unversioned Incrementality, IRList and persistence bug fixes.
 *
 * Revision 1.41  2007/05/24 22:42:08  thallora
 * Defined a "FluidLogger" that passes out java.util.Loggers that we (better) control the format and the output.  The default format is much nicer that what we get from the SDK (it came from FleetBaron)
 *
 * Revision 1.40  2007/02/28 06:46:57  boyland
 * fixed bug in Plain components being loaded -- need to check bundles toofixed bug in way Component used VersionedSlot.pushEra (didn't pop).changed PlaiNCOmponent to use Java 5 features for Bundles.Regularized logging in fluid.versionAdded tracing to canon (again).
 *
 * Revision 1.39  2006/03/30 17:00:33  chance
 * Removed unnecessary casts
 *
 * Revision 1.38  2006/03/28 20:58:45  chance
 * Updated for Java 5 generics
 *
 * Revision 1.37  2006/03/27 21:35:50  boyland
 * Added type parameters to Slot SlotInfo and IRSequence
 * refactored out slot storage into new SLotStorage class
 *
 * Revision 1.36  2005/07/01 17:15:00  chance
 * Eliminated Java 5/generics warnings
 *
 * Revision 1.35  2005/05/25 19:17:51  chance
 * Updated for Java 5
 *
 * Revision 1.34  2004/08/04 15:33:37  boyland
 * The Bi slots are derived so we don't want version bumping.
 *
 * Revision 1.33  2003/10/31 17:35:47  thallora
 * Change from Log4j to java.util.Logging
 * Revision 1.32 2003/08/22 04:01:51 boyland
 * Implemented compounds in VICs. Removed some suspect code for read compounds
 * from VersionedSlot
 * 
 * Revision 1.31 2003/07/02 20:19:24 thallora Initial changes to allow fluid
 * core analysis code to work in the edu.cmu.cs. prefix
 * 
 * Revision 1.30 2003/03/12 21:24:58 tien Add "public" for the class and for
 * the pushEra() method.
 * 
 * Revision 1.29 2003/01/31 23:32:47 boyland Changed Slot and IRSequence to
 * take a describe method. Updated some Makefiles to prevent fluid/test from
 * being visited (gives me errors constantly). Some DOS Gnumakefiles repaired.
 * 
 * Revision 1.28 2003/01/23 23:21:22 chance Added debugging
 * 
 * Revision 1.27 2002/08/03 01:16:27 thallora Log4j conversion of some files
 * 
 * Revision 1.26 2002/03/08 18:27:39 thallora JavaDoc fixes
 * 
 * Revision 1.25 2001/11/19 23:43:39 aarong Fixed javadoc
 * 
 * Revision 1.24 2001/09/18 18:34:38 boyland Made an abstract superclass.
 * Factored out some writing methods. persistence v1.4 some synchronization
 * added.
 * 
 * Revision 1.23 2000/07/17 22:40:53 boyland debugging code accidentally left
 * enabled.
 * 
 * Revision 1.22 2000/07/17 21:13:32 boyland by default: no initial value. an
 * initial value can be supplied from persistence. New constructors to be used
 * when setting initial values and for InitializedVersionedSlot. More debugging
 * help.
 * 
 * Revision 1.21 2000/02/21 22:31:03 boyland *** empty log message ***
 * 
 * Revision 1.20 2000/02/21 22:27:01 boyland Added ability to detect missing
 * deltas.
 *  
 */
public abstract class VersionedSlot<T> extends AbstractSlot<T> {
  /**
	 * Logger for this class
	 */
  protected static final Logger LOG = SLLogger.getLogger("IR.version");

  /*
	 * back channel communication: some methods behave differently when this
	 * variable is set to a non-null era.
	 */
  private static final ThreadGlobal<Era> eraVar = new ThreadGlobal<Era>(null);
  
  /*
   * More back channel communication:
   * Used when getEra() returns true:
   * we store read slots in this vector.
   */
  private static final ThreadGlobal<List<Version>> versionChanges = new ThreadGlobal<List<Version>>(null);
  
  static Era getEra() {
    return eraVar.getValue();
  }
  static void setEra(Era era) {
    eraVar.setValue(era);
    versionChanges.getValue().clear();
  }
  static void pushEra(Era era) {
    eraVar.pushValue(era);
    versionChanges.pushValue(new ArrayList<Version>());
  }
  static void popEra() {
    versionChanges.popValue();
    eraVar.popValue();
  }
  static List<Version> getEraChanges() {
    return versionChanges.getValue();
  }
  
  public static interface IORunnable {
    public void run() throws IOException;
  }
  public static void runInEra(Era era, IORunnable code) throws IOException {
    pushEra(era);
    try {
      code.run();
    } finally {
      popEra();
    }
  }

  /*
	 * more back-channel communication: true when writing VICs
	 */
  private static final ThreadGlobal<Boolean> inVIC = new ThreadGlobal<Boolean>(null);
  static boolean isInVIC() {
    return inVIC.getValue() != null;
  }
  static void pushInVIC() {
    inVIC.pushValue(Boolean.TRUE);
  }
  static void popInVIC() {
    inVIC.popValue();
  }

  // a special value used to represent undefined:
  protected static final Object undefinedValue = new Object();

  // Used to keep track of all the 'VersionedSlot's created
  private static Vector<VersionedSlot> slotList = new Vector<VersionedSlot>();
  static boolean verboseDebug = false;

  /**
	 * Remove a slot from the list of all slots because it is being retired, no
	 * use for debugging. (NB This is only used for debugging, and has no effect
	 * otherwise.)
	 */
  protected void retire() {
    slotList.removeElement(this); // just about free when not debugging
  }

  /** Create a versioned slot. */
  public VersionedSlot() {
    super();
    if (verboseDebug)
      slotList.addElement(this);
  }

  /**
	 * The number of version-value pairs in this versioned slot. Used primarily
	 * for debugging and statistics.
	 */
  public abstract int size();

  /**
	 * Turn on debugging. Currently this only enables the stashing of all
	 * versioned slots.
	 * 
	 * @see #listing
	 */
  public static synchronized void debugOn() {
    verboseDebug = true;
  }

  public static synchronized void debugOff() {
    verboseDebug = false;
  }

  public static Object debugTogglePrintMe = new Object() {
    @Override
    public String toString() {
      verboseDebug = !verboseDebug;
      return verboseDebug ? "debugging turned on" : "debugging turned off";
    }
  };

  /**
	 * Print out a description of slots created while debugging is on. It prints
	 * the total count, the first maxPrint of the slots and then a description of
	 * how many (of all the slots) have the given number of version-value pairs.
	 */
  public static void listing(int maxPrint) {
    int totalSlots = slotList.size();
    System.out.println(totalSlots + " slots in all.");
    Vector<Integer> histogram = new Vector<Integer>();

    for (int i = 0; i < totalSlots; ++i) {
      VersionedSlot s = slotList.elementAt(i);
      if (i < maxPrint) {
        s.describe(System.out);
        System.out.println();
      }
      int n = s.size();
      if (histogram.size() <= n)
        histogram.setSize(n + 1);
      Integer count = histogram.elementAt(n);
      if (count == null) {
        count = IntegerTable.newInteger(1);
      } else {
        count = IntegerTable.newInteger(count.intValue() + 1);
      }
      histogram.setElementAt(count, n);
    }

    System.out.println("Number of slots of the following sizes:");
    int hsize = histogram.size();
    for (int i = 0; i < hsize; ++i) {
      Integer count = histogram.elementAt(i);
      if (count != null) {
        System.out.println("  " + i + ": " + count);
      }
    }
  }

  public static Object debugDumpPrintMe = new Object() {
    @Override
    public String toString() {
      listing(100);
      return "Done.";
    }
  };

  @Override
  public void describe(PrintStream out) {
  }

  /* slot methods */

  /**
	 * Return the value of this slot at the current version.
	 * 
	 * @see Version#getVersion()
	 * @see #getValue(Version)
	 * @throws SlotUndefinedException
	 *           if the slot does not have a value.
	 * @throws SlotUnknownException
	 *           if we do not know whetehr the slot has a value.
	 */
  @Override
  public T getValue()
    throws SlotUndefinedException, SlotUnknownException {
    return getValue(Version.getVersionLocal());
  }

  /**
	 * Return the value of this slot at a particular version.
	 * 
	 * @throws SlotUndefinedException
	 *           if the slot does not have a value.
	 * @throws SlotUnknownException
	 *           if we do not know whetehr the slot has a value.
	 */
  public abstract T getValue(Version v)
    throws SlotUndefinedException, SlotUnknownException;

  /**
	 * Return the version at which this slot was assigned the value given.
   * Warning: this method tries to be helpful by finding the last REAL change of this slot.
   * This can be inefficient.  (O(n^2) for n assignments.)
	 */
  public abstract Version getLatestChange(Version v);

  /**
	 * Set the value of this slot for a new child of the current version.
	 */
  @Override
  public Slot<T> setValue(T newValue) {
    if (!(this instanceof VersionedDerivedSlot)) Version.bumpVersion();
    return setValue(Version.getVersionLocal(), newValue);
  }

  /**
	 * Set the value of this slot for a particular version. This method is
	 * protected because used incorrectly it can upset the property that a
	 * versioned slot never changes for a version.
	 */
  protected abstract VersionedSlot<T> setValue(Version v, T newValue);

  /** Does the slot have a value at the current version? */
  // <b>Special behavior when @{link #getEra()} returns a
  // non-null era.</b>
  @Override
  public boolean isValid() {
    getEra();
    //if (era != null) return isChanged(era);
    return isValid(Version.getVersionLocal());
  }

  /**
	 * Does the slot have a value at the given version?
	 */
  public abstract boolean isValid(Version v);

  /* persistence */

  @Override
  public boolean isChanged() {
    Era era = getEra();
    if (era != null)
      return isChanged(era);
    return true;
  }

  /**
	 * Return true if this version slot has been changed somewhere in the given
	 * version region.
	 */
  public abstract boolean isChanged(Era era);

  @Override
  public void writeValue(IRType<T> ty, IROutput out) throws IOException {
    Era era = getEra();

    if (era != null) {
      out.debugBegin("vl");
      writeValues(ty, out, era);
      out.debugMark("sentinel");
      Version.writeRootVersion(out);
      out.debugEnd("vl");
    } else {
      T val = getValue();
      if (ty instanceof IRCompoundType && !isInVIC()) {
        Version v0 = Version.getInitialVersion();
        Version v = Version.getVersionLocal();
        if (v != v0) {
          v = getLatestChange(Version.getVersionLocal());
          out.debugMark("compound_date");
          IRVersionType.prototype.writeValue(v, out);
        }
      }
      ty.writeValue(val, out);
    }
  }

  /**
	 * Write all values of this slot for the given era, in the form
	 * version,value,version,value etc. The version will never be null.
	 */
  protected abstract void writeValues(IRType<T> ty, IROutput out, Era e)
    throws IOException;

  /**
	 * Write out one version,value pair. Special case work is done for compounds.
	 */
  @SuppressWarnings("unchecked")
  protected void writeVersionValue(
    IRType<T> ty,
    Version v,
    T val,
    IROutput out)
    throws IOException {
    out.debugBegin("vv");
    if (out.debug()) {
      System.out.println("  " + v + " : " + val);
    }
    out.debugBegin("v");
    v.write(out);
    out.debugEnd("v");
    if (ty instanceof IRCompoundType && val != null) {
      // special case!
      // (We don't need to handle null here because
      // if it is null, nothing is done differently
      Version.saveVersion();
      pushEra(null);
      try {
        Version.setVersion(Version.getInitialVersion());
        ty.writeValue(val, out);
      } finally {
        popEra();
        Version.restoreVersion();
      }
      if (val != null) {
        out.debugBegin("changed");
        ((IRCompound<T>) val).writeChangedContents((IRCompoundType<T>) ty, out);
        out.debugEnd("changed");
      }
    } else {
      ty.writeValue(val, out);
    }
    out.debugEnd("vv");
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized Slot<T> readValue(IRType<T> ty, IRInput in)
    throws IOException {
    Era era = getEra();
    if (era == null) {
      Version v = Version.getVersionLocal();
      Version.saveVersion(v);
      try {
        T val;
        if (in.getRevision() >= 4 && ty instanceof IRCompoundType) {
          if (!isInVIC() && v != Version.getInitialVersion()) {
            in.debugMark("compound_date");
            v = IRVersionType.prototype.readValue(in);
          }
        }
        if (in.getRevision() < 4 && ty instanceof IRCompoundType)
          v = Version.getInitialVersion(); // used to be constant
        if (ty instanceof IRCompoundType && isValid(v) && getValue(v) != null) {
          if (in.debug()) {
            System.out.println("Reusing " + getValue(v));
          }
          val = ((IRCompoundType<T>)ty).readValue(in,getValue(v));
        } else {
          val = ty.readValue(in);
        }
        if (in.debug()) {
          System.out.println("  " + v + " : " + val);
        }
        return setValue(v, val);
      } finally {
        Version.restoreVersion();
      }      
    }
    in.debugBegin("vl");
    Version root = era.getRoot();
    Version v;
    VersionedSlot<T> s = this;
    in.debugBegin("vv");
    in.debugBegin("v");
    while (!(v = Version.read(in, era)).equals(root)) {
      in.debugEnd("v");
      T val;
      if (in.getRevision() >= 4 && ty instanceof IRCompoundType) {
        Version.saveVersion();
        pushEra(null);
        try {
          Version.setVersion(era.getRoot());
          if (isValid(v) && getValue(v) != null) {
            if (in.debug()) {
              System.out.println("Reusing " + getValue(v));
            }
            val = ((IRCompoundType<T>)ty).readValue(in,getValue(v));
          } else {
            val = ty.readValue(in);
          }
        } finally {
          popEra();
          Version.restoreVersion();
        }
        if (val != null) {
          in.debugBegin("changed");
          ((IRCompound<T>) val).readChangedContents((IRCompoundType<T>) ty, in);
          in.debugEnd("changed");
        }
      } else {
        val = ty.readValue(in);
      }
      if (in.debug()) {
        System.out.println("  " + v + " : " + val);
      }
      in.debugEnd("vv");
      VersionedSlot.getEraChanges().add(v);
      s = s.setValue(v, val);
      in.debugBegin("vv");
      in.debugBegin("v");
    }
    in.debugEnd("v");
    in.debugEnd("vv");
    in.debugEnd("vl");
    return s;
  }
}
