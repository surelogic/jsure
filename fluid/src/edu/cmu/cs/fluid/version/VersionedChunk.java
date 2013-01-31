/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedChunk.java,v
 * 1.15 2003/07/19 03:15:17 boyland Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.FileLocator;

/**
 * A chunk of IR including some versioned attributes. The versioned chunk
 * contains a set of attributes as does a chunk, but the set of nodes is a
 * versioned region rather than a normal region. The versioned chunk includes a
 * tree of versions of itself. Each versioned chunk delta (VCD, for short) is
 * an separately storable entity. It is also possible to store a versioned
 * chunk snapshot (VCS) at any particular version.
 * </p>
 */
public class VersionedChunk extends IRChunk implements VersionedState {
  private static final int magic = 0x56430a00; // VC\n\0

  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("IR.version");

  /**
	 * Create a new versioned chunk.
	 */
  protected VersionedChunk(VersionedRegion vr, Bundle b) {
    super(magic, vr, b, false);
  }

  /*
  private Hashtable versionedStructures = new Hashtable();

  /**
	 * A versioned structure is used here temporarily until the nodes are
	 * assigned to VRDs.
	 *
  protected class Structure extends VersionedStructureProxy {
    final Version version;
    Structure(Version v) {
      version = v;
    }

    protected VersionedStructure computeReplacement() {
      if (version.getEra() == null)
        return null;
      VersionedRegion vr = getVersionedRegion();
      IRChunk ch = IRChunk.get(vr.getDelta(version.getEra()), getBundle());
      versionedStructures.remove(version);
      return IRChunkVersionedStructure.get(ch);
    }
  };

  VersionedStructure getVersionedStructure(IRNode n) {
    IRRegion reg = IRRegion.getOwnerOrNull(n);
    if (reg != null) {
      IRChunk ch = IRChunk.get(reg, getBundle());
      return IRChunkVersionedStructure.get(ch);
    }
    VersionedRegion vr = getVersionedRegion();
    Version v = vr.getVersion(n);
    VersionedStructure vs = (VersionedStructure) versionedStructures.get(v);
    if (vs == null) {
      Structure s = new Structure(v);
      vs = s;
      versionedStructures.put(v, vs);
      v.addPersistentObserver(s);
    }
    return vs;
  }
  */

  /**
	 * Find or create a versioned chunk for the given versioned region and
	 * bundle.
	 * 
	 * @deprecated use @{link #get(VersionedRegion,Bundle)}
	 */
  @Deprecated
  public static VersionedChunk getVersionedChunk(
    VersionedRegion vr,
    Bundle b) {
    return get(vr, b);
  }
  /**
	 * Find or create a versioned chunk for the given versioned region and
	 * bundle.
	 */
  public static VersionedChunk get(VersionedRegion vr, Bundle b) {
    VersionedChunk vc = (VersionedChunk) find(vr, b);
    if (vc == null) {
      vc = new VersionedChunk(vr, b);
      if (LOG.isLoggable(Level.FINER)) {
        LOG.finer("Creating new " + vc);
      }
      add(vc);
    }
    return vc;
  }

  protected SubsidiaryChunk getSubsidiary(VersionedRegionDelta r, Bundle b) {
    return SubsidiaryChunk.get(r,b);
  }
  
  /* accessor methods */

  public VersionedRegion getVersionedRegion() {
    return (VersionedRegion) super.getRegion();
  }

  public Era getInitialEra() {
    return getVersionedRegion().getInitialEra();
  }

  /** Return delta for era. */
  public VersionedDelta getDelta(Era era) {
    return Delta.get(this, era);
  }

  /** Return snapshot for version. */
  public VersionedSnapshot getSnapshot(Version v) {
    return Snapshot.get(this, v);
  }

  @Override
  public boolean deltaIsDefined(Era e, Version lastV) {
    VersionedDelta d = VersionedDelta.find(this,e);
    return d != null && d.isDefined(lastV);
  }
  @Override
  public boolean snapshotIsDefined(Version v) {
    VersionedSnapshot s = VersionedSnapshot.find(this,v);
    return s != null && s.isDefined();
  }
  @Override
  public boolean isShared() {
    IRRegion region = getRegion();
    return region instanceof VersionedRegion && ((VersionedRegion)region).isShared();
  }
  
  public static double debugIsDefined;

  @Override
  protected String getFileName() {
    return null; // do not save this entity
  }

  @Override
  public String toString() {
    return "V" + super.toString();
  }

  @Override
  public void describe(PrintStream out) {
    super.describe(out);
  }

  /* Kind */

  private static IRPersistentKind kind = new IRPersistentKind() {
    @Override
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException {
      IRChunk chunk = (IRChunk) p;
      chunk.getRegion().writeReference(out);
      chunk.getBundle().writeReference(out);
    }
    @Override
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException {
      VersionedRegion vr = (VersionedRegion) IRPersistent.readReference(in);
      Bundle b = (Bundle) IRPersistent.readReference(in);
      return get(vr, b);
    }
  };
  static {
    IRPersistent.registerPersistentKind(kind, 0x76); // 'v'
  }

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  public static void ensureLoaded() {
    SubsidiaryChunk.ensureLoaded();
    Delta.ensureLoaded();
    Snapshot.ensureLoaded();
  }


  /**
   * The individual chunks making up a versioned chunk, one for each
   * VRD of the versioned region. We need to have our own class so that the
   * parent state is set correctly.
   * @author boyland
   */
  public static class SubsidiaryChunk extends IRChunk {
    /** Create a SC for a new VRD
     * @param r a VRD for a versioned chunk
     * @param b the bundle
     */
    public SubsidiaryChunk(VersionedRegionDelta r, Bundle b) {
      super(r, b);
      setParent(r,b);
    }

    /**
     * Create an SC for an old VRD
     * @param r a VRD for a versioned chunk
     * @param b the bundle
     * @param unused doesn't matter
     */
    public SubsidiaryChunk(VersionedRegionDelta r, Bundle b, boolean unused) {
      super(magic, r, b, unused);
      setParent(r,b);
    }
    
    private void setParent(VersionedRegionDelta vrd, Bundle b) {
      super.setParent(VersionedChunk.get(vrd.getBase(),b));
    }
    
    public static SubsidiaryChunk get(VersionedRegionDelta vrd, Bundle b) {
      return (SubsidiaryChunk)IRChunk.get(vrd,b);
    }
    
    @Override
    public String toString() {
      return "s" + super.toString();
    }
    
    static final IRPersistentKind kind = new IRPersistentKind() {
      @Override
      public void writePersistentReference(IRPersistent p, DataOutput out) throws IOException {
        // TODO Auto-generated method stub
        SubsidiaryChunk sc = (SubsidiaryChunk)p;
        sc.getRegion().writeReference(out);
        sc.getBundle().writeReference(out);
      }

      @Override
      public IRPersistent readPersistentReference(DataInput in) throws IOException {
        // TODO Auto-generated method stub
        VersionedRegionDelta vrd = (VersionedRegionDelta)IRPersistent.readReference(in);
        Bundle b = (Bundle)IRPersistent.readReference(in);
        return SubsidiaryChunk.get(vrd,b);
      }
    };
    
    static {
      IRPersistent.registerPersistentKind(kind,'s');
    }
    
    public static void ensureLoaded() {}
  }

  /**
	 * A versioned chunk delta (VCD) stores information about a versioned chunk
	 * relative to a single era. It is a combination of two things:
	 * <ol>
	 * <li>a normal chunk of values of attributes of new nodes for the versioned
	 * region for the era.
	 * <li>a set of delta values for attributes of all nodes for the versioned
	 * region for the era.
	 */
  protected static class Delta extends VersionedDelta {
    static final int magic = 0x56434400; // VCD\0

    Delta(VersionedChunk vc, Era e) {
      super(magic, vc, e);
    }

    Delta(VersionedChunk chunk, Era e, boolean unused) {
      super(magic, chunk, e, false);
    }

    VersionedChunk getVersionedChunk() {
      return (VersionedChunk) getChunk();
    }

    VersionedRegion getVersionedRegion() {
      return getVersionedChunk().getVersionedRegion();
    }

    public static Delta get(VersionedChunk ch, Era e) {
      Delta d = (Delta) find(ch, e);
      if (d == null) {
        if (e.isNew())
          d = new Delta(ch, e);
        else
          d = new Delta(ch, e, false);
        add(d);
      }
      return d;
    }

    /* Kind */

    private static final IRPersistentKind kind = new IRPersistentKind() {
      @Override
      public void writePersistentReference(IRPersistent p, DataOutput out)
        throws IOException {
        VersionedChunk.Delta vcd = (VersionedChunk.Delta) p;
        vcd.getVersionedRegion().writeReference(out);
        vcd.getChunk().getBundle().writeReference(out);
        vcd.getEra().writeReference(out);
      }
      @Override
      public IRPersistent readPersistentReference(DataInput in)
        throws IOException {
        VersionedRegion vr = (VersionedRegion) IRPersistent.readReference(in);
        Bundle b = (Bundle) IRPersistent.readReference(in);
        Era e = (Era) IRPersistent.readReference(in);
        VersionedChunk vc = VersionedChunk.get(vr, b);
        return get(vc, e);
      }
    };
    static {
      IRPersistent.registerPersistentKind(kind, 0x03); // Control-C
    }

    @Override
    public IRPersistentKind getKind() {
      return kind;
    }

    /* Output */

    /**
		 * When writing, we write the pieces that make up this thing. It is nothing
		 * more than a hollow shell.
		 */
    @Override
    public void write(IROutput out) throws IOException {
      VersionedRegion vr = getVersionedRegion();
      Version alpha = Version.getInitialVersion();
      Era era = getEra();
      if (era.isComplete()) {
        // LOG.info(this + " being made complete");
        forceComplete();
      }
      Bundle bundle = getChunk().getBundle();
      IRChunk ch0 = getVersionedChunk().getSubsidiary(vr.getDelta(era), bundle);
      writeImports(out);
      VersionedSnapshot vs = VersionedSnapshot.get(ch0, alpha);
      out.debugBegin("subchunk name=" + vs);
      vs.writeAttributeValues(out);
      out.debugEnd("subchunk");
      for (Era e = era; e != null; e = e.getParentEra()) {
        IRChunk ch = getVersionedChunk().getSubsidiary(vr.getDelta(e), bundle);
        if (era.isChanged(ch) == 1) {
          e.writeReference(out);
          VersionedDelta vd = VersionedDelta.get(ch, era);
          out.debugBegin("subdelta name=" + vd);
          vd.writeChangedAttributes(out);
          out.debugEnd("subdelta");
        }
      }
      out.debugMark("sentinel");
      // Now we write a sentinel.
      // Currently, we write out a reference to the initial era,
      // which (before SharedVersionedRegion) was never needed
      // for a subdelta, but this should be changed in 
      // IR persistence 1.5
      Era.getInitialEra().writeReference(out);
    }

    /* Input */

    /**
		 * Before a load, we make sure that the versioned chunk and version region
		 * or loaded and that either the parent is loaded, or else we have a
		 * snapshot for the root version.
		 * 
		 * @see IRPersistent#load(FileLocator floc)
		 */
    @Override
    public void load(FileLocator floc) throws IOException {
      if (isNew() || isComplete())
        return;
      
      final boolean debug = LOG.isLoggable(Level.FINE);
      VersionedChunk ch = getVersionedChunk();
      Era era = getEra();
      Version v = era.getRoot();
      if (!v.isLoaded(ch)) {
        //! Should be changed to use SlotUnknown catchers!
        try {
          VersionedSnapshot snapshot = ch.getSnapshot(v);
          if (debug) {
            LOG.finer("For " + this + ", loading supporting shapshot " + snapshot);
          }
          snapshot.load(floc);
        } catch (IOException ex) {
          VersionedDelta delta = ch.getDelta(v.getEra());
          if (debug) {
            LOG.finer("For " + this + ", loading supporting delta " + delta);
          }
          delta.load(floc);
        }
      } else {
        if (debug) {
          LOG.finer("Delta " + this + " already supported at " + v);
        }
        // LOG.info("debugIsDefined = " + debugIsDefined);
      }
      LOG.fine("Loading " + this);
      super.load(floc);
      if (!era.isLoaded(ch)) {
        LOG.severe(this + " loaded, but era doesn't recognize the loading!\n" +
            "debugIsDefined = " + debugIsDefined);
        describe(System.out);
      }
    }

    /**
		 * Read in changes for all attributes for all nodes in the growing
		 * versioned region. In persistence v1.4, we read in the pieces
		 * individually. In earlier versions we have a more monolithic solution.
		 */
    @Override
    protected void read(IRInput in) throws IOException {
      VersionedRegion vr = getVersionedRegion();
      Version alpha = Version.getInitialVersion();
      Era era = getEra();
      Bundle bundle = getChunk().getBundle();
      IRChunk ch0 = getVersionedChunk().getSubsidiary(vr.getDelta(era), bundle);
      VersionedSnapshot vs = VersionedSnapshot.get(ch0, alpha);
      if (in.getRevision() >= 4) {
        readImports(in);
        in.debugBegin("subchunk name=" + vs);
        vs.readAttributeValues(in);
        in.debugEnd("subchunk");
        Era e = null;
        Era e0 = Era.getInitialEra();
        for (;;) {
          Era olde = e;
          e = (Era) IRPersistent.readReference(in);
          IRChunk ch = getVersionedChunk().getSubsidiary(vr.getDelta(e), bundle);
          // this condition is a little messy because of a poor choice for
          // sentinel mentioned in write.
          if (e == e0 && (era.isChanged(ch) < 0 || olde == e)) break;
          VersionedDelta vd = VersionedDelta.get(ch, era);
          in.debugBegin("subdelta name=" + vd);
          vd.readChangedAttributes(in);
          in.debugEnd("subdelta");
        }
        return;
      }
      // old way:
      readImports(in);
      in.debugBegin("subchunk name=" + vs);
      vs.readAttributeValues(in);
      in.debugEnd("subchunk");
      //VersionedStructureFactory.pushVS(null);
      try {
        Version.saveVersion();
        try {
          // permit readDeltas to set Version
          // (Hence they are inside this 'try')
          if (in.debug()) {
            LOG.fine("Reading deltas for " + this);
          }
          readDeltas(in);
        } finally {
          Version.restoreVersion();
        }
      } finally {
        //VersionedStructureFactory.popVS();
      }
      // force all the deltas implicitly loaded to be defined.
      for (Era e = era; e != Era.getInitialEra(); e = e.getParentEra()) {
        IRChunk ch = getVersionedChunk().getSubsidiary(vr.getDelta(e), bundle);
        VersionedDelta vd = VersionedDelta.get(ch, era);
        vd.forceDefined();
      }
    }

    @SuppressWarnings("unchecked")
    protected void readDeltas(IRInput in) throws IOException {
      VersionedChunk vc = getVersionedChunk();
      Bundle b = vc.getBundle();
      Era era = getEra();
      try {
        VersionedSlot.pushEra(era); // change behavior of versioned slots
        int n = b.getNumAttributes();
        in.debugBegin("body");
        if (in.getRevision() >= 1) {
          in.debugBegin("numAttributes");
          n = in.readInt();
          in.debugEnd("numAttributes");
        }        
        for (int i = 1; i <= n; ++i) {
          SlotInfo attr = b.getAttribute(i);
          PersistentSlotInfo psi = (PersistentSlotInfo) attr;
          in.debugBegin("dl name=" + psi);
          IRNode node;
          while ((node = in.readNode()) != null) {
            //VersionedStructureFactory.setVS(vc.getVersionedStructure(node));
            psi.readSlotValue(node, in); //NB: and version
          }
          IRType t = psi.getType();
          if (t instanceof IRCompoundType) {
            if (in.debug()) {
              LOG.fine("Reading changed contents for " + attr.name());
            }
            in.debugMark("changedcontents");
            while ((node = in.readNode()) != null) {
              //VersionedStructureFactory.setVS(vc.getVersionedStructure(node));
              if (in.debug()) {
                IRRegion r = IRRegion.getOwner(node);
                LOG.fine("  Node " + r + " #" + r.getIndex(node));
              }
              Version v;
              v = Version.getInitialVersion();
              Version.setVersion(v);
              IRCompound c = (IRCompound) node.getSlotValue(attr);
              c.readChangedContents((IRCompoundType) t, in);
            }
          }
          in.debugEnd("dl");
        }
        in.debugEnd("body");
      } finally {
        VersionedSlot.popEra();
      }
    }

    @Override
    protected void forceComplete() {
      VersionedRegion vr = getVersionedRegion();
      Version alpha = Version.getInitialVersion();
      Era era = getEra();
      Bundle bundle = getChunk().getBundle();
      IRChunk ch0 = getVersionedChunk().getSubsidiary(vr.getDelta(era), bundle);
      VersionedSnapshot.get(ch0, alpha).forceComplete();
      Era e0 = Era.getInitialEra();
      for (Era e = era; e != e0; e = e.getParentEra()) {
        IRChunk ch = getVersionedChunk().getSubsidiary(vr.getDelta(e), bundle);
        VersionedDelta.get(ch, era).forceComplete();
      }
      super.forceComplete();
    }

    @Override
    public String toString() {
      return "VCD("
        + getVersionedRegion()
        + ","
        + getChunk().getBundle()
        + ","
        + getEra()
        + ")";
    }

    public static void ensureLoaded() {
    }
  }

  /**
	 * A versioned chunk snapshot (VCS) is snapshot of the state of versioned
	 * chunk at a particular version. It consists of snapshots for all the VRD's
	 * making up a VR.
	 */
  protected static class Snapshot extends VersionedSnapshot {
    private static final int magic = 0x56435300; // VCS\0

    public VersionedRegion getVersionedRegion() {
      return (VersionedRegion) getRegion();
    }

    public VersionedChunk getVersionedChunk() {
      return (VersionedChunk) getChunk();
    }

    /** Create a versioned chunk version in preparation for saving. */
    protected Snapshot(VersionedChunk vc, Version v) {
      super(magic, vc, v);
    }

    /** Create a versioned chunk version in preparation for loading. */
    protected Snapshot(VersionedChunk vc, Version v, boolean unused) {
      super(magic, vc, v, false);
      vc.getVersionedRegion().exportTo(this, v.getEra());
    }

    /** Find or create a snapshot */
    public static Snapshot get(VersionedChunk vc, Version v) {
      Snapshot vcs = (Snapshot) find(vc, v);
      if (vcs == null) {
        if (v.getEra() == null || v.getEra().isNew())
          vcs = new Snapshot(vc, v);
        else
          vcs = new Snapshot(vc, v, false);
        add(vcs);
      }
      return vcs;
    }

    /* Kind */

    private static final IRPersistentKind kind = new IRPersistentKind() {
      @Override
      public void writePersistentReference(IRPersistent p, DataOutput out)
        throws IOException {
        Snapshot vcs = (Snapshot) p;
        vcs.getRegion().writeReference(out);
        vcs.getBundle().writeReference(out);
        vcs.getVersion().getEra().writeReference(out);
        vcs.getVersion().write(out);
      }
      @Override
      public IRPersistent readPersistentReference(DataInput in)
        throws IOException {
        VersionedRegion vr = (VersionedRegion) IRPersistent.readReference(in);
        Bundle b = (Bundle) IRPersistent.readReference(in);
        Era e = (Era) IRPersistent.readReference(in);
        VersionedChunk vc = VersionedChunk.get(vr, b);
        return get(vc, Version.read(in, e));
      }
    };
    static {
      IRPersistent.registerPersistentKind(kind, 0x13); // control-S
    }

    @Override
    public IRPersistentKind getKind() {
      return kind;
    }

    /* Output */

    // the following two methods are needed only for reading old IR:

    @Override
    protected int getNumAttributedNodes() {
      int n = 0;
      VersionedRegion vr = getVersionedRegion();
      for (Era e = getVersion().getEra(); e != null; e = e.getParentEra()) {
        n += vr.getDelta(e).getNumNodes();
      }
      return n;
    }

    @Override
    protected IRNode getAttributedNode(int i) throws IOException {
      IRNode n = getImportedNode(i);
      return n;
    }

    /**
		 * When writing, we jam together VS's for each VRD
		 */
    @Override
    public void write(IROutput out) throws IOException {
      writeImports(out);
      VersionedRegion vr = getVersionedRegion();
      Bundle b = getBundle();
      Version version = getVersion();
      Era e = version.getEra();
      Era e0 = vr.getInitialEra();
      for (;;) {
        IRChunk ch = getVersionedChunk().getSubsidiary(vr.getDelta(e), b);
        VersionedSnapshot vs = VersionedSnapshot.get(ch, version);
        out.debugBegin("subchunk name=" + vs);
        vs.writeAttributeValues(out);
        out.debugEnd("subchunk");
        if (e == e0)
          break; // stop loop
        e = e.getParentEra();
      }
    }

    /* (non-Javadoc)
     * @see edu.cmu.cs.fluid.ir.IRPersistent#load(edu.cmu.cs.fluid.util.FileLocator)
     */
    @Override
    public void load(FileLocator floc) throws IOException {
      super.load(floc);
      if (!getVersion().isLoaded(getVersionedChunk())) {
        LOG.severe(this + " loaded, but version does not recognize loaded.\n" +
            "debugIsDefined = " + debugIsDefined);
      }
    }
    
    /**
		 * Read snapshot from file. For new IR, read in little VS's. For old IR,
		 * more monolithic.
		 */
    @Override
    public void read(IRInput in) throws IOException {
      VersionedRegion vr = getVersionedRegion();
      Bundle b = getBundle();
      Version version = getVersion();
      Era e0 = vr.getInitialEra();
      if (in.getRevision() >= 4) {
        readImports(in);
        for (Era e = version.getEra();; e = e.getParentEra()) {
          IRChunk ch = getVersionedChunk().getSubsidiary(vr.getDelta(e), b);
          VersionedSnapshot vs = VersionedSnapshot.get(ch, version);
          in.debugBegin("subchunk name=" + vs);
          vs.readAttributeValues(in);
          in.debugEnd("subchunk");
          // NB: even in 1.4, we read in a chunk for e0.
          if (e == e0)
            break; // stop loop
        }
        return;
      }
      // old
      //VersionedStructureFactory.pushVS(null);
      Version.saveVersion();
      version.clamp();
      try {
        Version.setVersion(version);
        super.read(in);
      } finally {
        version.unclamp();
        Version.restoreVersion();
        //VersionedStructureFactory.popVS();
      }
    }

    @Override
    protected void forceComplete() {
      VersionedRegion vr = getVersionedRegion();
      Bundle b = getBundle();
      Version version = getVersion();
      Era e0 = vr.getInitialEra(); // Now force complete on little VSs
      for (Era e = version.getEra();; e = e.getParentEra()) {
        IRChunk ch = getVersionedChunk().getSubsidiary(vr.getDelta(e), b);
        VersionedSnapshot.get(ch, version).forceComplete();
        if (e == e0)
          break;
      }
      super.forceComplete();
    }

    @Override
    public String toString() {
      Version version = getVersion();
      return "VCS("
        + getVersionedRegion()
        + ","
        + getBundle()
        + ","
        + version.getEra()
        + ","
        + version.getEraOffset()
        + ")";
    }

    public static void ensureLoaded() {
    }
  }
}
