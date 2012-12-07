/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/Era.java,v 1.17
 * 2003/10/31 15:07:38 chance Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.tree.DepthFirstSearch;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.IdentityHashSet;
import edu.cmu.cs.fluid.util.UniqueID;

/**
 * A set of contiguous versions off of a single root. An Era is used in
 * persistence, each delta identifies its era. An era can be defined by a <em>root</em>
 * version (exclusive) and a <em>fringe</em>, a set of descendant versions
 * (inclusive). A version is in the era if it is a descendant of the root and
 * equal to a member of the fringe or an ancestor of a fringe member. In other
 * words: \exists f \in F: r > v >= f. Each era has a unique identifier used
 * for identification and persistence.
 * <p>
 * The "initial" era is a special case. This era has root = null and the
 * initial version as its only fringe. This era is needed to give a place for
 * the initial version.
 * </p>
 * <p>
 * An era (other than the initial era) refers to a version in its parent era.
 * As versions aren't set up to have undefined parents, we currently require
 * the parent era to be loaded before we can refer to the child era. Perhaps in
 * a more sophisticated system, loading on demand could be handled. In the
 * meantime, I don't imagine the cost will be excessive because eras are
 * relatively small.
 * </p>
 * <p>
 * An era has a <em>shadow region</em> associated it which holds the shadow
 * nodes in the version tree for the versions in the era. This shadow region
 * has a special type, so we can persist it correctly. It might be easier to
 * have make versions IR nodes and eras regions, to avoid the need to shadow,
 * but that would be confusing.
 * </p>
 * <p>
 * With every era we associate persistent entities. Entities can be associated
 * with an era while it is new until it is stored or written as a complete
 * persistent object. Clients can ask whether a particular entity is associated
 * with an era. The answer can be yes, no or maybe.
 * </p>
 */
public class Era extends IRPersistent implements PossibleEra {
  /**
	 * Log4j logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("IR.version");

  private static final int ERAmagic = 0x45524100; // ERA\0
  private Version root;
  private Vector<Version> fringe;
  private Vector<Version> members; /* root plus every member of era */
  //private FileLocator floc;
  private Collection<IRState> changed;
  private boolean changedFrozen = false;

  //private static final Hashtable present = new Hashtable();

  /** Create the initial era. */
  private Era() {
    super(ERAmagic, UniqueID.parseUniqueID("initial"));
    Version initial = Version.getInitialVersion();
    if (initial == null) {
      throw new FluidError("initial version is null");
    }
    root = null;
    fringe = new Vector<Version>(1);
    members = new Vector<Version>(2);
    fringe.addElement(initial);
    members.addElement(null); // not actually a member
    members.addElement(initial);
    try {
      initial.setEra(this, 1);
    } catch (OverlappingEraException ex) {
      throw new FluidError("Initial era created multiply!");
    }
    define();
    forceComplete();
    changedFrozen = true;
  }

  private static Era initialEra;

  /** Return era for initial version. */
  public static Era getInitialEra() {
    if (initialEra == null)
      initialEra = new Era();
    return initialEra;
  }

  /**
	 * Create an incomplete era with the given root.
	 * 
	 * @see #addVersion
	 * @exception DisconnectEraException
	 *              if r does not have an era.
	 */
  public Era(Version r) throws DisconnectedEraException {
    super(ERAmagic, true);
    getInitialEra();
    if (r.getEra() == null) {
      throw new DisconnectedEraException(
        "cannot start era off unassigned version " + r);
    }
    root = r;
    fringe = new Vector<Version>();
    members = new Vector<Version>();
    members.addElement(root); // not actually a member
  }

  /**
	 * Create an era with given root and fringe.
	 * 
	 * @exception OverlappingEraException
	 *              if overlaps with existing era.
	 * @exception DisconnectEraException
	 *              if r does not have an era.
	 */
  public Era(Version r, Version[] f)
    throws OverlappingEraException, DisconnectedEraException {
    super(ERAmagic, true);
    getInitialEra();
    if (r.getEra() == null) {
      throw new DisconnectedEraException(
        "cannot start era off unassigned version " + r);
    }
    root = r;
    fringe = new Vector<Version>(f.length);
    for (int i = 0; i < f.length; ++i)
      fringe.addElement(f[i]);
    members = new Vector<Version>();
    members.addElement(root);
    int index = 0;
    
    final boolean debug = LOG.isLoggable(Level.FINE);
    if (debug) {
      LOG.fine("Creating Era " + this);
    }
    for (Iterator vs = new EraIterator(this); vs.hasNext();) {
      Version v = (Version) vs.next();
      members.addElement(v);
      v.setEra(this, ++index);
      if (debug) {
        LOG.fine("Added " + v + " to " + this);
      }
    }
    complete();
  }

  private Era(UniqueID id) throws IOException {
    super(ERAmagic, id);
    getInitialEra();
  }

  public Version getRoot() {
    return root;
  }
  public Iterator fringe() {
    return fringe.iterator();
  }

  public Era getParentEra() {
    if (root == null)
      return null;
    else
      return root.getEra();
  }

  /**
	 * Add another version to an incomplete era.
	 * 
	 * @throws OverlappingEraException
	 *           if the version already is assigned to an era, or if its parent
	 *           is of a different era, or if the version passed is the root
	 *           version (which cannot be assigned an era) or if the era is
	 *           complete.
	 */
  public void addVersion(Version v) throws OverlappingEraException {
    if (!isNew() || isComplete())
      throw new OverlappingEraException("era cannot receive new versions");
    Version p = v.parent();
    if (p == null)
      throw new OverlappingEraException("root version cannot be in an era");
    if (p != root && p.getEra() != this)
      throw new OverlappingEraException("unrelated version cannot be added");
    members.addElement(v);
    // if parent was in fringe, remove it, and
    // in any case, add new Version on fringe.
    int i = fringe.indexOf(p);
    if (i == -1) {
      fringe.addElement(v);
    } else {
      fringe.setElementAt(v, i);
    }
    // Delay until memebers and fringe set because
    // setEra calls observers.
    v.setEra(this, members.size() - 1);
  }

  /* Return the maximum version index (currently) for this era. */
  public int maxVersionOffset() {
    return members.size() - 1;
  }

  public Version getVersion(int index) {
    try {
      return members.elementAt(index);
    } catch (RuntimeException e) {
      LOG.severe("failed " + this + ".getVersion(" + index + ")");
      throw e;
    }
  }

  /** Return true if version is in era. */
  public boolean contains(Version v) {
    // System.out.println(this + " contains? " + v);
    Era era = v.getEra();
    if (era == this)
      return true;
    if (era != null)
      return false;
    if (isComplete())
      return false; // force early termination if root == null
    /* otherwise, we compute it */
    if (v.equals(root))
      return false;
    if (!v.comesFrom(root))
      return false;
    for (int i = 0; i < fringe.size(); ++i) {
      if (fringe.elementAt(i).comesFrom(v)) {
        return true;
      }
    }
    return false;
  }

  /** Return true if on fringe of complete version. */
  public boolean isFringe(Version v) {
    if (root != null && root.equals(v))
      return false;
    return fringe.contains(v);
  }

  /**
	 * Return the versions in the eras starting from the top (excluding the
	 * root).
	 */
  public Iterator<Version> elements() {
    Iterator<Version> enm = members.iterator();
    enm.next(); // drop root
    return enm;
  }

  /**
	 * Add the given state to the list of states changed for this era. This
	 * function is idempotent. The era must be new and not yet stored or written
	 * as a complete entity. This condition should not be checked for by the
	 * client, nor should the exception be trapped as a matter of course. It is a
	 * serious error that shows that there is a bug in Fluid code. If an entity
	 * needs to be recorded as changed and cannot be, versioned slot consistency is
	 * imperiled.
   * <p>
   * As a special case, it is legal to record a change when the era is old and/or
   * frozen as long as the change is already recorded.
	 * 
	 * @throws FluidError
	 *           if the era is not new or stored or written complete.
	 * @throws NullPointerException
	 *           if p is null.
	 */
  public synchronized void noteChanged(IRState p) {
    if (!isNew() || changedFrozen) {
      int alreadyChanged = isChanged(p);
      if (alreadyChanged >= 0) return;
      throw new FluidError("change list for era is fixed.");
    }
    if (p == null) {
      throw new NullPointerException("cannot change null");
    }
    if (changed == null) changed = new IdentityHashSet<IRState>();
    changed.add(p);
  }

  /**
	 * Determine whether the given state is changed during this era. We may not know
	 * if the era is not stored completely, or if it
	 * was loaded without this information.
	 * 
	 * @return <dl>
	 *         <dt>1</dt>
	 *         <dd>If the entity is changed.</dd>
	 *         <dt>-1</dt>
	 *         <dd>If the entity is not changed.</dd>
	 *         <dt>0</dt>
	 *         <dd>If we do not know.</dd>
	 *         </dl>
	 */
  public synchronized int isChanged(IRState p) {
    boolean possible = false;
    if (changed != null) {
      for (Iterator it = changed.iterator(); it.hasNext();) {
        IRState st = (IRState)it.next();
        if (IRState.Operations.includes(p,st)) return 1;
        if (!possible && IRState.Operations.includes(st,p)) possible = true;
      }
    }
    // if we're the initial era, and the state is shared, then isChanged is true:
    if (getRoot() == null) {
      IRPersistent p2 = IRState.Operations.asPersistent(p);
      if (p2 instanceof VersionedState && ((VersionedState)p2).isShared()) return 1;
    }
    /* otherwise, if we have all information,
     * and nothing included this state
     * and this state is represented in persistence, then
     * we know that it isn't changed.
     */
    if (changedFrozen && 
        !possible && 
        IRState.Operations.root(p) instanceof IRPersistent) {
      return -1;
    }
    /* Othewise, there may be a change */
    return 0;
  }

  /**
   * Make change information coarser by replacing each with persistent state
   */
  protected synchronized void makeChangedPersistable() {
    Set<IRState> remaining = null;
    for (Iterator it=changed.iterator(); it.hasNext();) {
      IRState st = (IRState)it.next();
      IRState r = (IRState)IRState.Operations.asPersistent(st);
      if (st != r && r != null) {
        it.remove();
        if (remaining == null) remaining = new HashSet<IRState>();
        remaining.add(r);
      }
    }
    if (remaining != null) changed.addAll(remaining);
  }
  
  private final Set<IRState> loaded = new HashSet<IRState>();
  
  /** Return whether the following state is loaded in the system.
   * If the state is new (not under anything persistent), then it is OK.
   * Otherwise, we check to see if the delta for this era is loaded,
   * (if its associated) or if its loaded for the parent era (otherwise).
   * This information is cached.
   * @see edu.cmu.cs.fluid.version.PossibleEra#isLoaded(edu.cmu.cs.fluid.ir.IRState)
   */
  public synchronized boolean isLoaded(IRState st) {
    st = (IRState) IRState.Operations.asPersistent(st);
    VersionedChunk.debugIsDefined = 540;
    if (!(st instanceof IRPersistent)) return true;
    VersionedChunk.debugIsDefined = 541;
    if (loaded.contains(st)) return true;
    VersionedChunk.debugIsDefined = 542;
    VersionedState vst = VersionedState.Operations.asVersionedState(st);
    if (vst == null) return false; // warning generated
    VersionedChunk.debugIsDefined += 0.4;
    if (isChanged(st) >= 0 && !isNew()) {
      VersionedChunk.debugIsDefined += 0.1;
      Version maxVersion = getVersion(maxVersionOffset());
      if (((IRPersistent) st).isNew()
          || vst.deltaIsDefined(this, maxVersion)) {
        VersionedChunk.debugIsDefined += 0.2;
        loaded.add(st);
        return true;
      }
    } else {
      Version rv = getRoot();
      if (rv == null || rv.isLoaded(st)) {
        VersionedChunk.debugIsDefined *= 10;
        loaded.add(st);
        return true; // TODO: shared stuff?  need to load it!
      }
      /*
      Era pe = getParentEra();
      if (pe == null) return true;
      if (pe.isLoaded(st)) {
        VersionedChunk.debugIsDefined *= 10;
        loaded.add(st);
        return true;
      }*/
    }
    return false;
  }
  
  /**
	 * Find an era given an ID.
	 * 
	 * @return era or null, if none with this ID
	 * @see #loadEra(UniqueID,FileLocator)
	 */
  public static Era findEra(UniqueID id) {
    return (Era) IRPersistent.find(id);
  }

  /**
	 * Find an era using an ID or load if not present.
	 * 
	 * @see #findEra(UniqueID)
	 */
  public static Era loadEra(UniqueID id, FileLocator floc) throws IOException {
    Era era = findEra(id);
    if (era == null) {
      era = new Era(id);
      era.load(floc);
    }
    return era;
  }

  /** era as a filename */
  @Override
  protected String getFileName() {
    return getID().toString() + ".era";
  }

  @Override
  protected void write(IROutput out) throws IOException {
    out.debugBegin("era name=" + this);
    if (isComplete() && isNew())
      changedFrozen = true;
    out.debugBegin("root");
    IRVersionType.writeVersion(root, out);
    out.debugEnd("root");
    out.debugBegin("versions");
    for (Iterator<Version> enm = elements(); enm.hasNext();) {
      Version v = enm.next();
      out.debugBegin("item");
      if (isFringe(v))
        out.writeByte(2);
      else
        out.writeByte(1);
      Version p = v.parent();
      out.debugMark("parent");
      if (p == root)
        out.writeShort(0);
      else
        out.writeShort(p.getEraOffset());
      out.debugEnd("item");
    }
    out.debugMark("sentinel");
    out.writeByte(0);
    out.debugEnd("versions");
    makeChangedPersistable();
    out.debugBegin("numAssociated");
    out.writeInt(changed == null ? 0 : changed.size());
    out.debugEnd("numAssociated");
    if (changed != null && changed.size() > 0) {
      out.debugBegin("associated");
      for (Iterator i = changed.iterator(); i.hasNext();) {
        IRState state = (IRState)i.next();
        IRPersistent p = IRState.Operations.asPersistent(state);
        if (p != null) {
          out.writePersistentReference(p);
        } else {
          LOG.warning("Cannot record state change for " + state);
          IRState ps = state.getParent();
          LOG.warning("Parent state = " + ps);
          out.writePersistentReference(this); // filler
        }
      }
      out.debugEnd("associated");
    }
    out.debugBegin("frozen");
    out.writeBoolean(changedFrozen);
    out.debugEnd("frozen");
  }

  @Override
  protected void read(IRInput in) throws IOException {
    in.debugBegin("era name=" + this);
    in.debugBegin("root");
    if (in.getRevision() < 4) {
      int i = in.readByte();
      if (i == 0) {
        root = Version.getInitialVersion();
      } else if (i == 1) {
        UniqueID id = UniqueID.read(in);
        Era era = findEra(id);
        if (era == null || !era.isDefined())
          throw new IOException("parent era not defined");
        root = Version.read(in, era);
      } else {
        root = null;
      }
    } else {
      root = IRVersionType.readVersion(in);
    }
    in.debugEnd("root");
    if (in.debug())
      System.out.println("Root of era " + getID() + " is " + root);
    byte b;
    Vector<Version> newMembers = new Vector<Version>();
    Vector<Version> newFringe = new Vector<Version>();
    newMembers.addElement(root);
    try {
      Version.pushDefaultEra(null);
      in.debugBegin("versions");
      in.debugBegin("item");
      while ((b = in.readByte()) != 0) {
        in.debugMark("parent");
        int vpi = in.readShort();
        Version vp = newMembers.elementAt(vpi);
        if (in.debug())
          System.out.println(
            "Parent of "
              + ((b == 2) ? "" : "non-")
              + "fringe version "
              + newMembers.size()
              + " is version "
              + vpi
              + " = "
              + vp);
        Version v;
        if (members == null || members.size() <= newMembers.size()) {
          v = Version.createVersion(vp);
          v.setEra(this, newMembers.size());
        } else {
          v = members.elementAt(newMembers.size());
        }
        newMembers.addElement(v);
        if (b == 2)
          newFringe.addElement(v);
        in.debugEnd("item");
        in.debugBegin("item");
      }
      in.debugEnd("item");
      in.debugEnd("versions");
    } catch (OverlappingEraException ex) {
      // doesn't happen
      throw new FluidError("impossible");
    } finally {
      Version.popDefaultEra();
    }
    if (members == null || newMembers.size() > members.size()) {
      members = newMembers;
      fringe = newFringe;
    }
    if (in.getRevision() >= 4) {
      changedFrozen = false;
      in.debugBegin("numAssociated");
      int n = in.readInt();
      in.debugEnd("numAssociated");
      if (n == 0) {
        // this should be unnecessary, but may be necessary if
        // sneaky editing of IR files is done.
        changed = null;
      } else {
        changed = new HashSet<IRState>(n);
        in.debugBegin("associated");
        while (--n >= 0) {
          IRPersistent p = in.readPersistentReference();
          if (p == this) continue;
          changed.add((IRState)p);
        }
        in.debugEnd("associated");
      }
      in.debugBegin("frozen");
      changedFrozen = in.readBoolean();
      in.debugEnd("frozen");
    }
  }

  /* Persistent kind */

  public static final IRPersistentKind kind = new IRPersistentKind() {
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException {
      ((Era) p).getID().write(out);
    }
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException {
      UniqueID id = UniqueID.read(in);
      IRPersistent p = IRPersistent.find(id);
      FileLocator floc = IRPersistent.currentLoadFileLocator();
      if (floc == null) floc = IRPersistent.fluidFileLocator;
      if (p == null) {
        p = new Era(id);
        try {
          p.load(floc);
        } catch (IOException e) {
          LOG.warning("cannot demand load parent era " + p);
        }
      }
      return p;
    }
  };
  static {
    IRPersistent.registerPersistentKind(kind, 0x45); // 'E'
    IRPersistent.registerPersistentKind(EraShadowRegion.kind, 0x05); // '^e'
  }

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  @Override
  public void describe(PrintStream out) {
    super.describe(out);
    Version v = members.elementAt(0);
    if (v != null) {
      out.println("Root: " + v.getEra().getID() + " v" + v.getEraOffset());
    }
    for (int i = 1; i < members.size(); ++i) {
      v = members.elementAt(i);
      Version p = v.parent();
      int po = (p != null && p.getEra() == this) ? p.getEraOffset() : 0;
      out.println("Version v" + v.getEraOffset() + " under v" + po);
    }
    out.println("Associations:");
    if (changed != null)
      for (Iterator it = changed.iterator(); it.hasNext();) {
        IRState state = (IRState)it.next();
        out.print("  " + state);
        for (IRState p = state.getParent(); p != null; p = p.getParent()) {
          out.print(" < " + p);
        }
        out.println();
      }
    if (!changedFrozen)
      out.println("  ...");
  }

  public static void ensureLoaded() {
    EraShadowRegion.ensureLoaded();
    Version.ensureLoaded();
    //System.out.println("Era loaded");
  }

  private final IRRegion shadowRegion = new EraShadowRegion(this);

  /**
	 * Return the shadow region of the era: the region holding the version shadow
	 * nodes.
	 */
  public IRRegion getShadowRegion() {
    return shadowRegion;
  }
}

/**
 * Iterator versions in era. We reuse tree enumerations on the shadow tree
 * to do most of the work for us.
 */
class EraIterator implements Iterator<Version> {
  private final Era era;
  private final DepthFirstSearch dfs;

  EraIterator(Era e) {
    dfs = new DepthFirstSearch(Version.getShadowTree(), e.getRoot().getShadowNode()) {
      /** Return true if in era (or if root). */
      @Override
      protected boolean mark(IRNode node) {
        /* we need to permit the root to get things going. */
        if (Version.getShadowVersion(node).equals(era.getRoot()))
          return true;
        return era.contains(Version.getShadowVersion(node));
      }
    };
    era = e;
    dfs.next(); // suck up root (not in era)
  }

  /**
	 * Return next version. Overrides method in superclass to fetch version from
	 * shadow node.
	 */
  public Version next() throws NoSuchElementException {
    IRNode next = dfs.next();
    return Version.getShadowVersion(next);
  }

  public boolean hasNext() {
    return dfs.hasNext();
  }

  public void remove() {
    dfs.remove();
  }
}

class EraShadowRegion extends IRRegion {
  private static final int magic = 0x45535200; // "ESR\0"

  private final Era era;

  /** The era's shadow region. */
  EraShadowRegion(Era e) {
    super(magic, null);
    era = e;
  }

  // defer all these to the era:

  @Override
  public UniqueID getID() {
    return era.getID();
  }
  @Override
  public boolean isNew() {
    return era.isNew();
  }
  @Override
  public boolean isDefined() {
    return era.isDefined();
  }
  @Override
  public boolean isComplete() {
    return era.isComplete();
  }
  @Override
  public boolean isStored() {
    return era.isStored();
  }

  /* persistent kind */

  static final IRPersistentKind kind = new IRPersistentKind() {
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException {
      EraShadowRegion esr = (EraShadowRegion) p;
      esr.era.writeReference(out);
    }
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException {
      Era e = (Era) IRPersistent.readReference(in);
      return e.getShadowRegion();
    }
  };

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  @Override
  public String toString() {
    return "ESR(" + era + ")";
  }

  public static void ensureLoaded() {
  }
}
