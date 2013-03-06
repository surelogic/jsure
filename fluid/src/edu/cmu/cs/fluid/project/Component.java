package edu.cmu.cs.fluid.project;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.logging.Logger;

import com.surelogic.common.Pair;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRPersistentKind;
import edu.cmu.cs.fluid.ir.IRState;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.UniqueID;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.SharedVersionedRegion;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedRegion;
import edu.cmu.cs.fluid.version.VersionedSlot;
import edu.cmu.cs.fluid.version.VersionedSlot.IORunnable;
import edu.cmu.cs.fluid.version.VersionedState;

/**
 * @author Tien
 * A Component: a piece of versioned state that can be saved.
 * A component lives within a version
 * space defined by a {@link Project}.
 * @see Project.
 */

public abstract class Component extends IRPersistent implements VersionedState
  /*implements VersionedStructure */ {
  
  private static Logger LOG = SLLogger.getLogger("FLUID.project");
  
  private static final int    magic = 0x434F4D50; // 'COMP'
      
  // Constructor for a new component (if hasID = true)
  protected Component(int magic, boolean hasID) {
    super(magic,hasID);
  }
  protected Component(boolean hasID) {
    this(magic,hasID);
  }
  // Constructor for an existing component
  protected Component(int magic, UniqueID id) {
    super(magic,id);
  }
  protected Component(UniqueID id) {
    this(magic,id);
  }
  
  /**
   * Create a Component that is initialized in state \alpha
   * to the current state.  In other words, create an exported/shared
   * component for the current version.  This constructor remains protected, 
   * because client should work through {@link #share(Version)} which
   * ensures that we don't ask for the same thing more than once.
   */
  protected Component(Component from, Version v) {
    super(from.getMagic(),makeSharedID(from,v));
    IRNode oldProjectNode = from.getProjectNode();
    if (projectNode != null) {
      VersionedRegion oldVR = VersionedRegion.getVersionedRegion(oldProjectNode);
      projectNode = SharedVersionedRegion.get(oldVR,v).getProxy(oldProjectNode);
    }
  }
  
  /**
   * Create a new component that shares this one at the current version.
   * This method is protected so that clients go through the ``share'' method.
   * This method probably should return <tt>new XXXComponent(this,v)</tt>
   * @param v version at which to make copy
   * @return new Component with shared pieces.
   */
  protected abstract Component makeShared(Version v);
  
  protected static UniqueID makeSharedID(IRPersistent p, Version v) {
    return p.getID().combine(v.getEra().getID()).combine(v.getEraOffset());
  }
  
  /**
   * Create a shared copy of a component for a particular version.
   * @param v version to create shared structure for.
   * @return possibly new component, but always a different component.
   */
  public Component share(Version v) {
      UniqueID sharedID = makeSharedID(this,v);
      IRPersistent p = IRPersistent.find(sharedID);
      if (p != null) {
        if (p instanceof Component) return (Component)p;
        LOG.severe("Non-unique shared ID " + sharedID);
      }
      return makeShared(v);
  }
  
  IRNode projectNode;
  void setProjectNode(IRNode node) {
    if (projectNode != null && projectNode != node) {
      LOG.severe("Changing the project for component " + this);
    }
    projectNode = node;
  }
  public IRNode getProjectNode() {
    return projectNode;
  }
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRState#getParent()
   */
  @Override
  public IRState getParent() {
    return null;
  }
  
  /** Return the factory of this component */
  public abstract ComponentFactory getFactory();

  /** is Component changed? */  
  public boolean isChanged() {
    return false;
  }
  
  /** save/loadDelta and save/loadSnapshot for VersionedChunkDelta */
  public void saveDelta(Era era,edu.cmu.cs.fluid.util.FileLocator floc) 
    throws IOException {
    ensureStored(floc);
    getDelta(era).ensureStored(floc);
  }

  public void loadDelta(Era era, edu.cmu.cs.fluid.util.FileLocator floc) 
    throws IOException {
    load(floc);
    getDelta(era).load(floc);
  }

  public void saveSnapshot(Version v, edu.cmu.cs.fluid.util.FileLocator floc) 
    throws IOException {
    ensureStored(floc);
    getSnapshot(v).ensureStored(floc);
  }

  public void loadSnapshot(Version v, edu.cmu.cs.fluid.util.FileLocator floc) 
    throws IOException {
    load(floc);
    getSnapshot(v).load(floc);
  }
    
  /** write/readContents and write/readCC */
  public void writeContents(IROutput out) throws IOException { }
  public void writeChangedContents(IROutput out) throws IOException { }
  public void readContents(IRInput in)     throws IOException { }
  public void readChangedContents(IRInput in) throws IOException { }
  
  @Override
  protected void write(IROutput out) throws IOException {
    out.writeNode(projectNode);
  }
  
  @Override
  protected void read(IRInput in) throws IOException {
    setProjectNode(in.readNode());
  }

  // Utilities
  
  public Delta getDelta(Era era) {
    return Delta.get(this,era);
  }
  
  public Snapshot getSnapshot(Version v) {
    return Snapshot.get(this,v);
  }
  
  @Override
  public boolean deltaIsDefined(Era e, Version lastV) {
    Delta d = Delta.find(this,e);
    return d != null && d.isDefined(lastV);
  }
  @Override
  public boolean snapshotIsDefined(Version v) {
    Snapshot s = Snapshot.find(this,v);
    return s != null && s.isDefined();
  }
  
  /* persistent kind */
  private static final IRPersistentKind kind = new IRPersistentKind() {
    @Override
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException
    {
      p.getID().write(out);
      // Write out the factory's name of the component
      Component comp = (Component) p;
      ComponentFactory factory = comp.getFactory();
      out.writeUTF(factory.getName());
    }
    @Override
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException
    {
      UniqueID id = UniqueID.read(in);      
      ComponentFactory factory = ComponentFactory.getComponentFactory(in.readUTF());
      Component comp = (Component)IRPersistent.find(id);
      if (comp == null) comp = factory.create(id);
      return comp;
    }
  };
  static {
    IRPersistent.registerPersistentKind(kind,0x63); // 'c'
  }

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  @Override
  public void undefine() {
    // XXX: actually this clears for everyone, but I think that they can be created cheaply
    Delta.clear();
    Snapshot.clear();
  }
  
  public static void ensureLoaded() {
  }

  @Override
  public void describe(PrintStream out) {
    super.describe(out);
  }

  @Override
  public String getFileName() {
    return getID() + ".cmp";
  }
  
  public boolean isDefined(Version v) {
    return v.isLoaded(this);
  }

  public boolean isDefined(Era e) {
    return e.isLoaded(this);
  }
  
  /**
   * Record that this component changed for this version.
   * This method (or {@link #noteCanged()}) must be called
   * when changes happen or else demand loading will not work
   * correctly.
   * @param v Version to check
   */
  public void noteChange(Version v) {
    v.noteChanged(this);
  }
  
  /**
   * Record that this component changed for the current version.
   * This method (or {@link #noteCanged(Version)}) must be called
   * when changes happen or else demand loading will not work
   * correctly.
   */
  public void noteChange() {
    Version.noteCurrentlyChanged(this);
  }
  
  /** Make sure the component is valid for the given version,
   * loading information as necessary.  If it cannot be loaded, then
   * an exception is thrown.
   * @param v version for which we want the component available.
   * @param floc filelocator to use
   */
  public void ensureLoaded(Version v, FileLocator floc) {
    if (v.isLoaded(this)) return;
    Version v1 = v.parent();
    Era e = v.getEra();
    if (e == null) {
      for (; e == null; v1 = v1.parent()) {
        e = v1.getEra();
      }
    }
    ensureLoaded(e.getRoot(),floc);
    // It might seem strange to check this condition again, but it may have changed
    // now that the parent era is loaded if this era has no changes.  Without
    // this second check, it will try to load a non-existent delta.
    // without the first check, we recurse back into NULL.
    if (v.isLoaded(this)) return;
    try {
      loadDelta(e,floc);
    } catch (IOException e1) {
      LOG.warning("cannot load component delta, trying snapshot instead");
      if (v.getEra() == null) {
        v = v1; // this is enough
      }
      try {
        loadSnapshot(v,floc);
      } catch (IOException e2) {
        LOG.severe("cannot load component: " + e2);
        throw new FluidRuntimeException("cannot load information");        
      }
    }
  }
  
  /** Private class Delta */
  
  public static class Delta extends IRPersistent {
    static final int magic = 0x43504400; // 'CPD\0'

    private final Era era;
    private final Component comp;

    private static HashMap<Pair<Component, Era>, Delta> deltas = new HashMap<Pair<Component,Era>, Component.Delta>();
    
    /** new one */
    Delta(Component c, Era e) {
      super(magic,false);
      era = e;
      if (!e.isNew()) 
        throw new FluidError("Creating new delta from old eras");
      comp = c;
    }
  
    /** Existing one */
    Delta(Component c, Era e, boolean unused) {
      super(magic,null);
      era = e;
      comp = c;
    }

    public Era getEra() { return era;}
    
    public Component getComponent() { return comp;}
    
    public synchronized static Delta find(Component c, Era e) {
      return deltas.get(Pair.getInstance(c,e));
    }
    
    private synchronized static void add(Delta d) {
      deltas.put(Pair.getInstance(d.getComponent(),d.getEra()),d);
      LOG.fine("Adding " + d);
    }

    public synchronized static Delta get(Component c, Era e) {
      Delta d = find(c, e);
      if (d == null) {
        if (e.isNew())
          d = new Delta(c, e);
        else
          d = new Delta(c, e, false);
        add(d);
      }
      return d;
    }

    @Override
    public String getFileName() {
      return era.getID().toString()
            + File.separator
            + comp.getID()
            + ".cpd";
    }

    @Override
    public IRPersistentKind getKind() { return null;}
    
    public boolean isDefined(Version v) {
      if (!isDefined()) return false;
      if (v.getEra() != era) return false;
      if (isComplete() || isNew()) return true;
      return (v.getEraOffset() <= era.maxVersionOffset());
    }

    // WRITE/READ
    
    @Override
    public void write(final IROutput out) throws IOException {
      if (era.isComplete()) {
        // LOG.info(this + " being made complete");
        forceComplete();
      }
      VersionedSlot.runInEra(era, new IORunnable() {
        @Override
        public void run() throws IOException {
          comp.writeChangedContents(out);
        }
      });     
    }

    @Override
    protected void read(final IRInput in) throws IOException {
      VersionedSlot.runInEra(era, new IORunnable() {
        @Override
        public void run() throws IOException {
          comp.readChangedContents(in);
        }
      });
    }

    public static void clear() {
      deltas.clear();
    }
    
    @Override
    public String toString() {
      return "CPD("
        + getComponent().getID()
        + ","
        + getEra()
        + ")";
    }

    public static void ensureLoaded() {
    }
  }

  // SNAPSHOT

  public static class Snapshot extends IRPersistent {
    static final int magic = 0x43505300; // 'CPS\0'

    private final Version version;
    private final Component comp;

    private static HashMap<Pair<Component, Version>, Snapshot> snapshots = new HashMap<Pair<Component,Version>, Component.Snapshot>();

    /** new one */
    Snapshot(Component c, Version v) {
      super(magic,false);
      version = v;
      comp = c;
      forceComplete();
    }
    
    /** existing one */
    Snapshot(Component c, Version v, boolean unused) {
      super(magic,null);
      version = v;
      comp = c;
      forceComplete();
    }
    
    public Version getVersion() { return version;}
    
    public Component getComponent() { return comp;}
    
    public static Snapshot find(Component c, Version v) {
      return snapshots.get(Pair.getInstance(c,v));
    }
    
    private static void add(Snapshot s) {
      snapshots.put(Pair.getInstance(s.getComponent(),s.getVersion()),s);
    }

    /** Find or create a snapshot */
    public static Snapshot get(Component c, Version v) {
      Snapshot s = find(c, v);
      if (s == null) {
        if (v.getEra() == null || v.getEra().isNew())
          s = new Snapshot(c,v);
        else s = new Snapshot(c,v,false);
        add(s);
      }
      return s;
    }

    @Override
    public void write(IROutput out) throws IOException {
      comp.writeContents(out);
    }

    @Override
    public void read(IRInput in) throws IOException {
      comp.readContents(in);
    }

    public static void clear() {
      snapshots.clear();
    }
    
    @Override
    public String toString() {
      Version version = getVersion();
      return "CPS("
        + getComponent().getID()
        + ","
        + version.getEra()
        + ","
        + version.getEraOffset()
        + ")";
    }

    @Override
    public IRPersistentKind getKind() { return null;}

    public static void ensureLoaded() {
    }
  }
}
