/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedRegion.java,v 1.17
 * 2003/08/05 12:45:22 chance Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.NotImplemented;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.*;

/**
 * A versioned region is essentially a region that grows with the version
 * space. In every era, more nodes may be added. (Currently, we never lose
 * nodes.) Since the storage system would not be able to handle regions
 * growning and shrinking and also since we do not want nodes from alternate
 * eras to be confused, a version region does not actually hold any nodes,
 * except temporarily. All the nodes are distributed between regions, one for
 * each era for this versioned region. Each of these regions is of private type
 * VersionedRegionDelta.
 * <p>
 * A new versioned region must be created in a new version (in a new era). and
 * all the nodes in a VR must similarly be created in new versions (eras) so we
 * know where to put them. If necessary the version is bumped before creating a
 * VR or a node in a VR. A special kind of versioned region, which starts in
 * the initial version/era, is called a <em>shared versioned
 * region</em> {@link SharedVersionedRegion}.
 * These can only be created through the persistence mechanism because the
 * initial version is never new.
 * </p>
 * <p>
 * The versioned region only holds nodes, it does not hold attribute values.
 * The values are in chunks.
 * </p>
 * 
 * @see IRPersistent
 * @see IRRegion
 * @see VersionedChunk
 */
public class VersionedRegion extends IRRegion implements IRState, IRPersistentObserver {
  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("IR.version");

  private static final int magic = 0x56520a00; // "VR\n\0"

  private Version initialVersion;
  private Era initialEra;

  /**
	 * Create a VersionedRegion: a region of nodes that increases in each era. If
	 * the current version is not new, the version is bumped, because a new VR
	 * must be created in a new version.
	 * <p>
	 * A versioned region is implemented as a set of VRD: VersionedRegionDeltas.
	 * Each VRD must be requested before chunks using the nodes in the VRD are
	 * saved.
	 * </p>
	 */
  public VersionedRegion() {
    this(magic);
  }

  protected VersionedRegion(int magic) {
    super(magic);
    complete(0);
    Version v = Version.getVersionLocal();
    if (v.getEra() != null && !v.getEra().isNew()) {
      Version.bumpVersion();
      v = Version.getVersionLocal();
    }
    initialVersion = v;
  }

  VersionedRegion(UniqueID id, Version iv) {
    this(magic, id, iv);
  }

  protected VersionedRegion(int magic, UniqueID id, Version iv) {
    super(magic, id);
    initialVersion = iv;
  }

  public static VersionedRegion loadVersionedRegion(
    UniqueID id,
    FileLocator floc)
    throws IOException {
    VersionedRegion vr = (VersionedRegion) find(id);
    if (vr == null) {
      vr = new VersionedRegion(id,null);
    }
    vr.load(floc);
    return vr;
  }

  /** Return the first era in which this region was defined. */
  public Era getInitialEra() {
    if (initialEra == null) {
      if (initialVersion == null) {
        while (initialEra == null) {
          new SlotUnknownException(getID() + ": initial version",null).handle();
        }
      } else {
        initialEra = initialVersion.getEra();
      }
    }
    return initialEra;
  }

  /**
   * Return true is this is a version independent region.
   * It might seem that one can tell if a VR region is shared
   * by seeing if it is a S{@link haredVersionedRegion}, but
   * {@link SharedVersionedRegion} is just a wrapper used to <em>store</em>
   * shared persistent data, and an SVR is never loaded.  Instead,
   * one gets a VR whose initial era is the initial version's era. 
   * @return true if this versioned region has state for the initial version.
   * @throws SlotUnknownException if this versioned region is undefined.
   */
  public boolean isShared() throws SlotUnknownException {
    return getInitialEra() == Version.getInitialVersion().getEra();
  }
  
  /* deltas */

  /** Hashtable from versions to vectors of nodes. */
  private final Hashtable<Version,Vector<IRNode>> versionNodes = new Hashtable<Version,Vector<IRNode>>();
  /** Hashtable from nodes to versions. */
  private final Hashtable<IRNode,Version> nodeVersion = new Hashtable<IRNode,Version> ();
  /** Hashtable from eras to ir regions. */
  private final Hashtable<Era, VersionedRegionDelta> regionDeltas = new Hashtable<Era, VersionedRegionDelta>();
  /** Hashtable from ir nodes to VR (while holding) */
  private static final IRNodeHashedMap<VersionedRegion> tempRegion = 
    new IRNodeHashedMap<VersionedRegion>();

  /**
	 * Put this node into the correct VRD. We first make sure the current version
	 * is new, by bumping if necessary. Then if the era has been assigned, then
	 * place in a VRD for that era, otherwise hold it until the era is known.
	 * 
	 * @return true if node was added, or held.
	 */
  @Override
  public boolean saveNode(IRNode node) {
    if (!hasOwner(node)) { // NB: we check if the node may be in a VR
      Version v = Version.getVersionLocal();
      Era e = v.getEra();
      if (e != null && !e.isNew()) {
        Version.bumpVersion();
        v = Version.getVersionLocal();
        e = v.getEra();
      }
      v.noteChanged(this);
      if (e != null) {
        VersionedRegionDelta vrd = getDelta(e);
        vrd.saveNode(node);
      } else {
        Vector<IRNode> vec = versionNodes.get(v);
        if (vec == null) {
          vec = new Vector<IRNode>();
          versionNodes.put(v, vec);
        }
        vec.addElement(node);
        nodeVersion.put(node, v);
        tempRegion.put(node, this);
        v.addPersistentObserver(this);
      }
      return true;
    }
    return false;
  }

  /**
	 * Return true if the node has a region or has been assigned to some
	 * VersionedRegion.
	 */
  public static boolean hasOwner(IRNode node) {
    return IRRegion.hasOwner(node) || tempRegion.get(node) != null;
  }

  public static IRRegion getOwner(IRNode node) {
    VersionedRegion vr = tempRegion.get(node);
    if (vr != null)
      return vr;
    return IRRegion.getOwner(node);
  }

  public static IRRegion getOwnerOrNull(IRNode n) {
    VersionedRegion vr = tempRegion.get(n);
    if (vr != null)
      return vr;
    return IRRegion.getOwnerOrNull(n);
  }

  /**
	 * Return the versioned region this node is created for.
	 * 
	 * @return null if not assigned to a VersionedRegion
	 */
  public static VersionedRegion getVersionedRegion(IRNode node) {
    IRRegion r = getOwnerOrNull(node);
    if (r instanceof VersionedRegion) {
      return (VersionedRegion) r;
    } else if (r instanceof VersionedRegionDelta) {
      return ((VersionedRegionDelta) r).getBase();
    } else {
      return null;
    }
  }

  @Override
  public IRChunk createChunk(Bundle b) {
    return VersionedChunk.get(this,b);
  }
  
  /**
	 * Return the version when this node was added, or if already assigned to an
	 * era, return some version in the era.
	 */
  public Version getVersion(IRNode n) {
    Version v = nodeVersion.get(n);
    if (v == null) {
      VersionedRegionDelta vrd = (VersionedRegionDelta) IRRegion.getOwner(n);
      return vrd.getEra().getVersion(1);
    }
    // LOG.fine("Version is " + v);
    if (v.getEra() != null)
      getDelta(v.getEra());
    return v;
  }

  /**
	 * Check to see if all nodes are assigned or can be assigned. If nodes can be
	 * assigned to VRDs, then assign them. Return true if all nodes assigned.
	 */
  public boolean finishNodes() {
    Set<Era> eraset = new HashSet<Era>();
    boolean interrupted = false;
    try {
      for (Iterator<Version> keys = versionNodes.keySet().iterator(); keys.hasNext();) {
        Version v = keys.next();
        Era e = v.getEra();
        if (e != null) {
          eraset.add(e);
        }
      }
    } catch (ConcurrentModificationException ex) {
      interrupted = true;
    }

    // we have to wait until after we have finished the iterator
    // before creating any deltas, because creating deltas modifies
    // the hash table
    for (Iterator<Era> eras = eraset.iterator(); eras.hasNext();) {
      getDelta(eras.next());
    }

    // if we were interrupted, then try again
    if (interrupted)
      return finishNodes();

    // otherwise done if there are no more nodes waiting to be assigned
    return versionNodes.isEmpty();
  }

  /**
	 * Add to a versioned region delta the nodes created in versions of the era.
	 */
  protected synchronized void fillDelta(VersionedRegionDelta vrd, Era era) {
    Iterator<Version> versions = era.elements();
    while (versions.hasNext()) {
      Version v = versions.next();
      fillDelta(vrd, v);
    }
  }

  /**
	 * Add to a versioned region delta the nodes created for this version.
	 * <p>
	 * <B>NB:</B> the version <em>must</em> be in the era.
	 */
  protected synchronized void fillDelta(VersionedRegionDelta vrd, Version v) {
    if (v.getEra() != vrd.getEra()) {
      LOG.severe("fillDelta(Version) called illegally: " + vrd + " with " + v);
      return;
    }
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Filling delta " + vrd + " for " + v);
    }
    Vector<IRNode> vec = versionNodes.get(v);
    if (vec != null) {
      Iterator<IRNode> nodes = vec.iterator();
      while (nodes.hasNext()) {
        IRNode node = nodes.next();
        vrd.saveNode(node);
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("Saving " + node);
        }
        tempRegion.remove(node);
        nodeVersion.remove(node);
      }
      versionNodes.remove(v);
    }
  }

  /**
	 * Notified that the version has been assigned an era.
	 */
  public void updatePersistent(IRPersistent p, Object o) {
    if (p instanceof Era && o instanceof Version) {
      fillDelta(getDelta((Era) p), (Version) o);
    }
  }

  /**
	 * Create or find the specified delta. If the era is not new and the delta
	 * does not already exist, it creates an undefined delta placeholder.
	 * 
	 * @param era
	 *          era for delta.
	 */
  public VersionedRegionDelta getDelta(Era era) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.finer("In " + this +".getDelta(" + era + ")");
    }
    VersionedRegionDelta vrd = regionDeltas.get(era);
    if (vrd == null) {
      if (era.isNew()) {
        vrd = new VersionedRegionDelta(this, era);
        if (LOG.isLoggable(Level.FINE)) {
          LOG.finer("Adding " + vrd + " to " + this);
        }
        fillDelta(vrd, era);
      } else {
        if (era.isChanged(this) < 0) {
          vrd = new VersionedRegionDelta(this, era, 0);
          LOG.finer("Era " + era + " has no record for " + vrd);
        } else {
          vrd = new VersionedRegionDelta(this, era, false);
          LOG.fine("Era " + era + " may have unknown changes for " + vrd);
        }
      }
      regionDeltas.put(era, vrd);
    }
    if (era.isNew() && era.isComplete())
      vrd.complete();
    return vrd;
  }

  /**
	 * Create or find the specified delta.
	 * 
	 * @param era
	 *          era for delta.
	 * @param numNodes
	 *          the number of nodes in this delta.
	 * @throws FluidRuntimeException
	 *           if number of nodes wrong.
	 */
  protected VersionedRegionDelta getDelta(Era era, int numNodes) {
    VersionedRegionDelta vrd = regionDeltas.get(era);
    if (vrd == null) {
      vrd = new VersionedRegionDelta(this, era, numNodes);
      regionDeltas.put(era, vrd);
    }
    vrd.complete(numNodes);
    if (vrd.getNumNodes() != numNodes)
      throw new FluidRuntimeException("delta has different number of nodes");
    return vrd;
  }

  /**
   * Define the VRD for a particular era.
   * @param era
   * @param vrd
   * @throws FluidError if delta already defined
   */
  protected void setDelta(Era era, VersionedRegionDelta vrd) {
    VersionedRegionDelta old = regionDeltas.get(era);
    if (old != null && old != vrd) {
      throw new FluidError("Cannot change VRD for VR");
    }
    regionDeltas.put(era,vrd);
  }
  
  public void exportTo(IRPersistent other) {
    Era era = Version.getVersion().getEra();
    exportTo(other, era);
  }

  public void exportTo(IRPersistent p, Era era) {
    if (era != null) {
      if (!era.equals(initialEra)) {
        exportTo(p, era.getParentEra());
      }
      p.importRegion(getDelta(era));
    }
  }

  /**
	 * Return an iterator of all region deltas starting from the current version.
	 * The VersionedRegion itself is returned as a delta for nodes currently
	 * awaiting eras.
	 */
  public Iterator allRegions(final Version startingVersion) {
    return new AbstractRemovelessIterator<IRRegion>() {
      private Version v = startingVersion;
      private Era e = startingVersion.getEra();
      public boolean hasNext() {
        return v != null && e != null;
      }
      public IRRegion next() throws NoSuchElementException {
        if (e == null) {
          if (v == null)
            throw new NoSuchElementException("no more regions");
          do {
            v = v.parent();
          } while (v.getEra() == null);
          return VersionedRegion.this;
        } else {
          IRRegion reg = getDelta(e);
          e = e.getParentEra();
          if (e == null)
            v = null;
          return reg;
        }
      }
    };
  }

  /**
	 * Return an enumeration of all nodes in the region from the initial time to
	 * the era of the version given. If the version is not yet in an era, we use
	 * the nodes in it and parent versions back until one *is* in an era.
	 */
  public Iterator<IRNode> allNodes(final Version v) {
    if (v.getEra() != null)
      return allNodes(v.getEra()); // shortcut
    return new AbstractRemovelessIterator<IRNode>() {
      private Iterator<IRNode> rest = null;
      private Iterator<IRNode> here = null;
      private Version version = v;

      {
        initialize();
      }

      public boolean hasNext() {
        for (;;) {
          if (rest != null)
            return rest.hasNext();
          if (here.hasNext())
            return true;
          moveVersion();
        }
      }
      public IRNode next() throws NoSuchElementException {
        for (;;) {
          if (rest != null)
            return rest.next();
          if (here.hasNext())
            return here.next();
          moveVersion();
        }
      }

      private void moveVersion() {
        version = version.parent();
        initialize();
      }
      private void initialize() {
        if (version == null) {
          rest = new EmptyIterator<IRNode>();
        } else if (version.getEra() == null) {
          Vector<IRNode> vc = versionNodes.get(version);
          if (vc == null)
            moveVersion();
          else
            here = vc.iterator();
        } else {
          rest = allNodes(version.getEra());
        }
      }
    };
  }

  /**
	 * Return an enumeration of all nodes in the region from initial time to the
	 * era given. Order not guaranteed.
	 */
  public Iterator<IRNode> allNodes(final Era e) {
    return new AbstractRemovelessIterator<IRNode>() {
      private Era era = e;
      private int i = 0;
      private VersionedRegionDelta vrd = getDelta(e);
      private int n = vrd.getNumNodes();
      public boolean hasNext() {
        moveEra();
        return i < n;
      }
      private void moveEra() {
        while (i >= n && era != null) {
          if (era.equals(getInitialEra())) {
            era = null;
          } else {
            era = era.getRoot().getEra();
            if (era == null)
              throw new NullPointerException("era is null");
            vrd = getDelta(era);
            i = 0;
            n = vrd.getNumNodes();
          }
        }
      }
      public IRNode next() throws NoSuchElementException {
        moveEra();
        if (i < n) {
          try {
            return vrd.getNode(++i);
          } catch (IOException ex) {
            // won't happen:
            throw new FluidError("VRD getNode died!");
          }
        } else {
          throw new NoSuchElementException("no more nodes in this era");
        }
      }
    };
  }

  // shared versioned regions

  Hashtable<Version,IRRegion> copies = null;

  /**
	 * Return a region that copies this versioned region at a particular version.
	 * The region is not extensible.
	 */
  public synchronized IRRegion getCopy(Version v) {
    IRRegion reg = null;
    if (copies == null)
      copies = new Hashtable<Version,IRRegion>();
    else
      reg = copies.get(v);
    if (reg == null) {
      //reg = new VersionedRegionCopy(this,v);
      //copies.put(v,reg);
      throw new NotImplemented("VersionedRegion#getCopy(Version)");
    }
    return reg;
  }

  /* persistent kind */

  private static IRPersistentKind kind = new IRPersistentKind() {
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException {
      VersionedRegion vr = (VersionedRegion) p;
      vr.getID().write(out);
    }
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException {
      UniqueID id = UniqueID.read(in);
      VersionedRegion vr = (VersionedRegion) find(id);
      if (vr == null) {
        vr = new VersionedRegion(id,null);
      }
      return vr;
    }
  };
  static {
    IRPersistent.registerPersistentKind(kind, 0x56); // 'V'
    IRPersistent.registerPersistentKind(VersionedRegionDelta.kind, 0x16);
    // '^V'
  }

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  @Override
  protected String getFileName() {
    return getID().toString() + ".vr";
  }

  @Override
  protected void write(IROutput out) throws IOException {
    if (getInitialEra() == null) {
      throw new IOException(
        "VersionedRegion "
          + this
          + " created in version "
          + initialVersion
          + " which has not yet been assigned an era.");
    }
    getInitialEra().writeReference(out);
  }

  @Override
  protected void read(IRInput in) throws IOException {
    initialEra = (Era) IRPersistent.readReference(in);
    if (in.debug())
      LOG.fine("Read initial era = " + initialEra);
  }

  /* A versioned region is changed in the eras that it gets new nodes.
   * @see edu.cmu.cs.fluid.ir.IRState#getParent()
   */
  public IRState getParent() {
    return null;
  }
  
  /** Destroy all VRDs associated with this VR.
   * Remove all links to other classes.  Other classes may <em>still</em>
   * have links to this class, although not to its VRDs.
   * @see edu.cmu.cs.fluid.ir.IRPersistent#undefine()
   */
  @Override
  public void undefine() {
    for (Enumeration<VersionedRegionDelta> deltas = regionDeltas.elements(); deltas.hasMoreElements();) {
      VersionedRegionDelta vrd = deltas.nextElement();
      vrd.destroy();
    }
    regionDeltas.clear();
    for (Enumeration<Vector<IRNode>> nodes = versionNodes.elements(); nodes.hasMoreElements();) {
      for (IRNode n : nodes.nextElement()) {
        n.destroy();
      }
    }
    versionNodes.clear();
    nodeVersion.clear();
    tempRegion.cleanup();
  }
  
  @Override
  public void describe(PrintStream out) {
    super.describe(out);
    out.println(
      "  initial era = "
        + (initialEra == null ? "<null>" : initialEra.toString()));
    for (Iterator<Era> eras = regionDeltas.keySet().iterator(); eras.hasNext();) {
      Era era = eras.next();
      VersionedRegionDelta vrd = regionDeltas.get(era);
      out.println("  " + era + " -> " + vrd.getNumNodes());
    }
    for (Iterator<Version> vs = versionNodes.keySet().iterator(); vs.hasNext();) {
      Version v = vs.next();
      out.println("  " + v + " => " + ((Vector) versionNodes.get(v)).size());
    }
  }

  public static void ensureLoaded() {
    VersionedRegionDelta.ensureLoaded();
  }
}
