/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/Version.java,v 1.53
 * 2003/10/31 15:08:02 chance Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.Tree;
import edu.cmu.cs.fluid.tree.TreeInterface;
import edu.cmu.cs.fluid.util.ThreadGlobal;

/**
 * The representation of nodes in the global version tree. Also the arbiter of
 * the current version. IRPersistentObservers are informed when the version is
 * an assigned an era.
 */
@SuppressWarnings("deprecation")
public class Version implements Serializable {
  /**
	 * Logger for this class
	 */
  protected static final Logger LOG = SLLogger.getLogger("IR.version");

  private static final Tree versionShadowTree;
  private static final SlotInfo<Version> versionShadowVersionAttribute;
  private /* final */
  IRNode shadowNode;
  static {
    Tree vst = null;
    SlotInfo<Version> vsva = null;
    try {
      vst = new Tree("fluid.version", SimpleSlotFactory.prototype);
      vsva =
        SimpleSlotFactory.prototype.newAttribute("fluid.version.Version", null);
    } catch (SlotAlreadyRegisteredException ex) {
    }
    versionShadowTree = vst;
    versionShadowVersionAttribute = vsva;
  }
  private static void addShadow(Version v) {
    // LOG.info("Creating shadow for version "+v, new Throwable())'
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Creating shadow for version " + v + " as child of " + v.parent);
    }
    if (v.isDestroyed()) {
      LOG.log(Level.SEVERE, "adding a shadow to a destroyed version!", new Error());
    }
    /*
		 * if (v.parent == null) { LOG.warn("parent is null at:", new Throwable()); }
		 */
    IRNode n = new MarkedIRNode(null, "versionShadow " + v);
    v.shadowNode = n;
    n.setSlotValue(versionShadowVersionAttribute, v);
    versionShadowTree.initNode(n, -1); // add after shadow connection made
    Version p = v.parent;
    if (p != null) {
      versionShadowTree.appendChild(p.shadowNode, n);
    }
  }
  public static TreeInterface getShadowTree() {
    return versionShadowTree;
  }
  public IRNode getShadowNode() {
    return shadowNode;
  }
  public static Version getShadowVersion(IRNode node) {
    Version v = node.getSlotValue(versionShadowVersionAttribute);
    ++v.refCount;
    return v;
  }

  private static int totalVersions = 0;
  private static final Version firstVersion = new Version(null);
  private static final ThreadGlobal<Version> current = new ThreadGlobal<Version>(firstVersion);
  private static final ThreadGlobal<Era> defaultEra = new ThreadGlobal<Era>(null);

  private final int id; // Unique Identifier for each version
  private final int depth; // Absolute depth in version tree
  private final Version parent; // Prior version
  private int refCount; // if zero then can modify
  private Vector<Version> children = new Vector<Version>(); // Next versions
  private int childrank; // position in parents children
  // array
  private final Version dynastyFounder; // Groups versions by first
  // descendant Dynasties
  private PossibleEra era = null;
  private int eraOffset = 0;

  private Vector<VersionCursor> cursors; // null or cursors tracking this
  private int clamps; // number of outstanding clamps forbidding changes
  
  // FIXME IRState or IRPersistent?
  private Set changed; // associated state changed in this version

  /**
	 * Creates a version with prior as its parent
	 */
  private Version(Version prior) {
    parent = prior;
    id = totalVersions;
    totalVersions++;
    addShadow(this);

    if (prior == null) {
      dynastyFounder = this;
      depth = 0;
      childrank = 0;
      refCount = 1;
    } else
      synchronized (prior) {
        if (prior.children.size() > 0) {
          dynastyFounder = this;
        } else {
          dynastyFounder = prior.dynastyFounder;
        }
        childrank = prior.children.size();
        prior.children.addElement(this);
        depth = prior.depth + 1;
        if (prior.cursors != null) {
          cursors = prior.cursors;
          prior.cursors = null;
          for (Iterator<VersionCursor> enm = cursors.iterator();
            enm.hasNext();
            ) {
            VersionCursor vc = enm.next();
            vc.moveCursor(this);
          }
        }
        Era def = defaultEra.getValue();
        if (def != null
          && def.isNew()
          && !def.isComplete()
          && (prior == def.getRoot() || prior.era == def)) { // OK to use
																														 // default
          try {
            def.addVersion(this);
          } catch (OverlappingEraException willNotHappen) {
            throw new RuntimeException("assertion failure in Version");
          }
        } else {
          // assign a temporary era
          if (prior.era instanceof TemporaryEra) {
            era = prior.era;
          } else {
            era = new TemporaryEra(prior);
          }
        }
      }
  }

  static Version createVersion(Version parent) {
    Version v = new Version(parent);
    v.freeze();
    return v;
  }

  public Object writeReplace() {
    return new VersionWrapper(this);
  }

  public static void setDefaultEra(Era e) {
    defaultEra.setValue(e);
  }

  public static Era getDefaultEra() {
    return defaultEra.getValue();
  }

  public static void pushDefaultEra(Era e) {
    defaultEra.pushValue(e);
  }

  public static Era popDefaultEra() {
    return (Era) defaultEra.popValue();
  }

  synchronized void setEra(Era e, int offset) throws OverlappingEraException {
    if (era instanceof Era)
      throw new OverlappingEraException(this +" already has era");
    era = e;
    eraOffset = offset;
    e.getShadowRegion().saveNode(shadowNode);
    if (IRRegion.getOwnerIndex(shadowNode) != offset) {
      System.err.println(
        "For "
          + this
          + " era offset="
          + offset
          + ", but index = "
          + IRRegion.getOwnerIndex(shadowNode));
      throw new FluidError("assertion failed: era offset != shadow index");
    }
    if (changed != null) {
      for (Iterator<IRState> it = changed.iterator(); it.hasNext();)
        e.noteChanged(it.next());
      changed = null;
    }
    if (observers != null) {
      for (Iterator<IRPersistentObserver> it = observers.iterator(); it.hasNext();)
         it.next().updatePersistent(e, this);
      observers = null;
    }
  }

  List<IRPersistentObserver> observers = new ArrayList<IRPersistentObserver>();

  public synchronized void addPersistentObserver(IRPersistentObserver o) {
    if (getEra()!= null)
      o.updatePersistent(getEra(), this);
    else if (!observers.contains(o))
      observers.add(o);
  }

  public Era getEra() {
    if (era instanceof Era) return (Era) era;
    if (era == null && this == firstVersion) {
      Era e = Era.getInitialEra();
      era = e;
      return e;
    }
    return null;
  }

  public int getEraOffset() {
    return eraOffset;
  }

  public synchronized void noteChanged(IRState st) {
    if (st == null) {
      throw new NullPointerException("Cannot change null");
    }
    if (getEra() != null) {
      getEra().noteChanged(st);
    } else {
      if (changed == null) changed = new HashSet();
      // TODO: if we want finer change information, we should simply 
      // TODO changed.add(st), rather than this complex set of choices:
      IRPersistent p = IRState.Operations.asPersistent(st);
      if (p == null) {
        changed.add(IRState.Operations.root(st)); // FIXME 
      } else {
        changed.add(p);
      }
    }
  }
  
  public static void noteCurrentlyChanged(IRState st) {
    getVersionLocal().noteChanged(st);
  }
  
  private Set<IRState> loaded = null;
  
  public synchronized boolean isLoaded(IRState st) {
    st = (IRState)IRState.Operations.asPersistent(st);
    VersionedChunk.debugIsDefined = 960;
    if (!(st instanceof IRPersistent)) return true;
    VersionedChunk.debugIsDefined = 961;
    if (loaded != null && loaded.contains(st)) return true;
    VersionedChunk.debugIsDefined = 963;
    if (era.isLoaded(st)) return true;
    VersionedChunk.debugIsDefined += 1000;
    Era e = getEra();
    if (e == null) return false;
    VersionedChunk.debugIsDefined += 2000;
    VersionedState vst = VersionedState.Operations.asVersionedState(st);
    if (vst == null) return false;
    VersionedChunk.debugIsDefined += 4000;
    if (vst.snapshotIsDefined(this)) {
      if (loaded == null) loaded = new HashSet<IRState>();
      loaded.add(st);
      return true;
    }
    VersionedChunk.debugIsDefined += 10000;
    return false;
  }
  
  public static boolean isCurrentlyLoaded(IRState st) {
    return getVersionLocal().isLoaded(st);
  }

  /** Write version to file. Always relative to an era. */
  public void write(DataOutput out) throws IOException {
    out.writeShort(eraOffset);
  }
  /**
	 * Write a marker to the file that when read will be the root version of the
	 * current era.
	 */
  public static void writeRootVersion(DataOutput out) throws IOException {
    out.writeShort(0);
  }

  /** Read version from file. Always relative to an era. */
  public static Version read(DataInput in, Era era) throws IOException {
    int i = in.readShort();
    return era.getVersion(i);
  }

  public void print() {
    System.out.println(id);
  }

  @Override
  public String toString() {
    if (getEra() != null) {
      return getEra().toString() + " v" + getEraOffset();
    } else if (isDestroyed()) {
      return "DESTROYED-V" + id;
    } else {
      return "V" + id;
    }
  }

  public void printNoCr() {
    System.out.print(id);
  }

  static void printCurrent() {
    getVersionLocal().print();
  }

  public static void printVersionTree() {
    printVersionTree(0, firstVersion);
  }

  public static void printVersionTree(int indent, Version toPrint) {
    for (int i = indent; i > 0; i--)
      System.out.print(" ");

    toPrint.print();

    for (int i = 0; i < toPrint.children.size(); i++)
      printVersionTree(indent + 1, (toPrint.children.elementAt(i)));
  }

  /**
	 * Return the parent version of this version. Should be used only to traverse
	 * version tree, not to make some conclusions about the
	 * existence/non-existence of versions in a line of changes.
	 */
  public Version parent() {
    return parent;
  }

  /** Return the first version in this ``dynasty.'' The dynasty system
   * is a partition of the version tree into linear subtrees.  The first 
   * child vresion of a version shares its dynasty but all later versions 
   * start their own dynasties.  The dynasty partition is not persistent:
   * it is computed as versions are created or loaded. This method is provided
   * to make it more efficient to do some version operations.
   * @return root of the linear subtree of the dynasty partition.
   */
  public Version getDynastyFounder() {
    return dynastyFounder;
  }
  
  /** Return the next version is this dynasty. If version has no children,
   * return null. The dynasty system
   * is a partition of the version tree into linear subtrees.  The first 
   * child vresion of a version shares its dynasty but all later versions 
   * start their own dynasties.  The dynasty partition is not persistent:
   * it is computed as versions are created or loaded. This method is provided
   * to make it more efficient to do some version operations.
   * @return child of this (if any) in the linear subtree of the dynasty partition.
   */
  public Version getNextInDynasty() {
    if (children == null || children.isEmpty()) return null;
    return children.get(0);
  }
  
  int depth() {
    return depth;
  }

  void addCursor(VersionCursor vc) {
    if (cursors == null)
      cursors = new Vector<VersionCursor>();
    cursors.addElement(vc);
  }

  void removeCursor(VersionCursor vc) {
    cursors.removeElement(vc);
  }

  public static void printVersionStack() {
    System.out.println(current);
  }

  public static void printDebuggingInfo() {
    VersionedSlot.listing(100);
  }

  public static int getTotalVersions() {
    return totalVersions;
  }

  static Version getVersionLocal() {
    Version v = current.getValue();
    // the interface cannot prevent this from happening,
    if (v.isDestroyed()) {
      LOG.severe("destroyed version was still current");
      v = getInitialVersion();
      setVersion(v);
    }
    return v;
  }

  void freeze() {
    if (clamps > 0 && refCount == 0) {
      throw new FluidRuntimeException("clamped liquid version is being frozen");
    }
    refCount |= 65536; // allow up to 64K mark and releases
  }

  boolean isFrozen() {
    return refCount > 0;
  }

  public static Version getVersion() {
    Version cv = getVersionLocal();
    cv.freeze();
    return cv;
  }

  public boolean isCurrent() {
    return this.equals(getVersionLocal());
  }

  void mark() {
    if (clamps > 0 && refCount < 10)
      System.err.println("Marking clamped version");
    ++refCount;
  }
  /**
	 * Decrement reference count. Safe only in a few situations: for instance
	 * when an enumeration reaches the end. Such a version cannot have any more
	 * than one child, or else someone snuck out a version. This method is not
	 * public because otherwise it would be abused.
	 */
  void release() {
    if (refCount != 65536)
      --refCount; // never clear frozen bit
    if (clamps > 0 && refCount < 10)
      System.err.println("Releasing clamped version");
  }

  public static Version getInitialVersion() {
    return firstVersion;
  }

  public static void setVersion(Version v) {
    current.setValue(v);
  }

  public void executeIn(Runnable thunk) {
    freeze();
    saveVersion(this);
    try {
      thunk.run();
      if (!Version.getVersionLocal().isFrozen()) {
        LOG.warning("Unfrozen version is being discarded in executeIn.");
      }
    } finally {
      restoreVersion();
    }
  }
  
  /**
	 * Save the current version, so that it can be restored after a version
	 * change. A save/restore in client code differs from code which gets and
	 * then sets because it doesn't freeze the current version.
	 * @deprecated use {@link #saveVersion(Version)} 
	 * @see #restoreVersion()
	 */
  public static void saveVersion() {
    current.pushValue(current.getValue());
  }

  /**
   * Save the current version and then set the version to the parameter.
   * The formerly current version can be restored afterwards. 
   * A save/restore in client code differs from code which gets and
   * then sets because it doesn't freeze the current version.
   * 
   * @param v version to switch to.
   * 
   * @see #restoreVersion()
   */
  public static void saveVersion(Version v) {
    current.pushValue(current.getValue());
    current.setValue(v);
  }

  /**
	 * Restore a previously saved version.
	 * 
	 * @see #saveVersion()
	 */
  public static void restoreVersion() {
    current.popValue();
  }

  /**
	 * Forbid modifications to values of the version. Used for debugging.
	 */
  public void clamp() {
    ++clamps;
  }
  /**
	 * Permit modifications to values of the version. Used for debugging.
	 */
  public void unclamp() {
    --clamps;
  }

  public static void clampCurrent() {
    getVersionLocal().clamp();
  }
  public static void unclampCurrent() {
    getVersionLocal().unclamp();
  }

  /**
	 * Notify the versioning system that a change has happened, potentially
	 * requiring a new version. If the current version is not visible, it can be
	 * reused.
	 */
  public static void bumpVersion() {
    Version cv = current.getValue();
    if (!cv.isFrozen()) {
      if (cv.cursors != null) {
        int n = cv.cursors.size();
        for (int i = 0; i < n; ++i) {
          (cv.cursors.elementAt(i)).notifyObservers();
        }
      }
      return;
    }
    /* permit us to debug things that are not supposed to change version */
    if (cv.clamps > 0)
      throw new FluidRuntimeException("no modifications allowed");
    current.setValue(new Version(cv));
  }

  /**
	 * A total order on versions that is consistent with ancestor ordering. A
	 * version is always less than any of its descendants. This ordering is not
	 * preserved peristently.
	 */
  boolean lessThanEq(Version other) {
    return (other.id <= this.id);
  }

  /**
	 * True if the argument is an ancestor or equal to this version.
	 */
  public boolean comesFrom(Version ancestor) {
    //! could be done more efficiently than this
    Version lca = latestCommonAncestor(this, ancestor);
    if (ancestor.equals(lca)) {
      return true;
    }
    return false;
  }

  /**
	 * Compute a slot for a specific version. @precondition nonNull(node) &&
	 * nonNull(si)
	 */
  public <T> T fetchSlotValue(IRNode node, SlotInfo<T> si) {
    Version v = getVersionLocal();
    try {
      setVersion(this);
      return node.getSlotValue(si);
    } finally {
      setVersion(v);
    }
  }

  /**
	 * Find the common ancestor which is deepest in the version tree for two
	 * versions. Ie, find the root of the minimum version subtree containing both
	 * versions.
	 */
  static public Version latestCommonAncestor(Version first, Version second) {
    if (first == null || second == null) {
      LOG.severe("NULL versions!");
    }
    if (first.isDestroyed() || second.isDestroyed()) {
      LOG.warning("lca called on destroyed node");
      return getInitialVersion();
    }
    while (first.dynastyFounder != second.dynastyFounder) {
      if (first.dynastyFounder.depth > second.dynastyFounder.depth) {
        first = first.dynastyFounder.parent;
      } else {
        second = second.dynastyFounder.parent;
      }
    }
    if (first.depth > second.depth) {
      return second;
    } else {
      return first;
    }
  }

  /**
	 * Return true if this version is "between" two other versions, that is
	 * represents changes that potentially cause two versions to differ.
	 * Mathematically, it is between if it is the ancestor of one but not both
	 * versions.
	 */
  public boolean isBetween(Version v1, Version v2) {
    return v1.comesFrom(this) ^ v2.comesFrom(this);
  }

  Version getNextInPreorderNoKids() {
    return dynastyFounder.getNextOlderSibling();
  }

  Version getNextInPreorder() {
    if (children.size() > 0) {
      return children.elementAt(children.size() - 1);
    }

    return getNextInPreorderNoKids();
  }

  private Version getNextOlderSibling() {
    if (childrank < 1) {
      return null;
    }
    return parent.children.elementAt(childrank - 1);
  }

  boolean precedes(Version second) {
    return precedes(this, second);
  }

  /**
	 * Determine which version precedes the over in a preorder of the global
	 * version tree @precondition !first.isDestroyed() && !second.isDestroyed()
	 */
  static boolean precedes(Version first, Version second) {
    if (first.equals(second)) {
      return false;
    }
    if (first.dynastyFounder.equals(second.dynastyFounder)) {
      if (first.depth < second.depth) {
        return true;
      } else {
        return false;
      }
    }
    Version temp1 = first;
    Version trace1 = first;
    Version temp2 = second;
    Version trace2 = second;

    while (temp1.dynastyFounder != temp2.dynastyFounder) {
      if (temp1.dynastyFounder.depth > temp2.dynastyFounder.depth) {
        trace1 = temp1.dynastyFounder;
        temp1 = trace1.parent;
      } else {
        trace2 = temp2.dynastyFounder;
        temp2 = trace2.parent;
      }
    }

    if (temp1.depth < temp2.depth) {
      trace2 = temp2;
      temp2 = temp1;
    }
    if (temp1.depth > temp2.depth) {
      trace1 = temp1;
      temp1 = temp2;
    }
    Version commonAncestor = temp1;

    if (first == commonAncestor) {
      return true;
    }
    if (second == commonAncestor) {
      return false;
    }

    if (trace2.dynastyFounder == commonAncestor.dynastyFounder) {
      return true;
    }
    if (trace1.dynastyFounder == commonAncestor.dynastyFounder) {
      return false;
    }

    if (trace1.childrank > trace2.childrank) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Deterkine the child of this version which is an ancestor of the target
   * @param target
   * @return child version (or null) which is ancestor of target.
   */
  public synchronized Version nextToward(Version target) {
    for (Iterator<Version> it = children.iterator(); it.hasNext(); ) {
      Version vc = it.next();
      if (target.comesFrom(vc)) {
        return vc;
      }
    }
    return null;
  }
  
  /**
	 * Remove a version from memory (if not owned by an era). Such a version
	 * should not be used again. This method does not (in of itself) remove the
	 * version from any versioned slots.
	 */
  public void destroy() {
    Version p = this.parent;
    Vector<Version> ch = this.children;
    IRNode shadow;
    synchronized (this) {
      if (era instanceof Era) {
        LOG.warning("Trying to destroy an owned version: won't work: " + this);
        return;
      }
      if (shadowNode == null)
        return;
      shadow = shadowNode;
      shadowNode = null;
    }
    this.children = null;
    this.cursors = null;
    this.changed = null;
    if (p != null && p.children != null)
      p.children.remove(this);
    if (ch != null) {
      for (Iterator<Version> it = ch.iterator(); it.hasNext();) {
        Version chv = it.next();
        chv.destroy();
      }
      ch.clear(); // hint to gc
    }
    this.childrank = -1;
    versionShadowTree.removeNode(shadow);
    shadowNode.destroy();
  }

  /**
	 * Is this node destroyed? Not synchronized because to avoid race conditions,
	 * caller must take necessary precautions.
	 */
  public boolean isDestroyed() {
    return childrank < 0;
  }

  public static void ensureLoaded() {
    IRVersionType.ensureLoaded();
  }
}

class VersionWrapper implements Serializable {
  private transient Version version;
  VersionWrapper(Version v) {
    version = v;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    if (version == Version.getInitialVersion()) {
      out.writeInt(0);
    } else {
      Era era = version.getEra();
      if (era == null)
        throw new NotSerializableException("versions must have eras to be serialized");
      out.writeInt(version.getEraOffset());
      era.writeReference(out);
    }
  }

  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
    int offset = in.readInt();
    if (offset == 0)
      version = Version.getInitialVersion();
    else {
      Era era = (Era) IRPersistent.readReference(in);
      version = era.getVersion(offset);
    }
  }

  public Object readResolve() {
    return version;
  }
}
