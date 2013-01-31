/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/project/PlainComponent.java,v 1.14 2008/06/24 19:13:18 thallora Exp $
 */
package edu.cmu.cs.fluid.project;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.ir.Bundle;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.IRNodeType;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.util.FileLocator;
import edu.cmu.cs.fluid.util.UniqueID;
import edu.cmu.cs.fluid.version.Era;
import edu.cmu.cs.fluid.version.SharedVersionedRegion;
import edu.cmu.cs.fluid.version.Version;
import edu.cmu.cs.fluid.version.VersionedChunk;
import edu.cmu.cs.fluid.version.VersionedDelta;
import edu.cmu.cs.fluid.version.VersionedRegion;
import edu.cmu.cs.fluid.version.VersionedSlot;
import edu.cmu.cs.fluid.version.VersionedSlotFactory;


/**
 * A component tailored to work with simple IR documents in a single (versioned) region.
 * We make three simplifying assumptions:
 * <ol>
 * <li> All the nodes in the component live in the same versioned region.
 *      (Other components may also have nodes in this region as well.)
 * <li> The component is rooted in a single IR node.
 * <li> The component is defined solely by a set of bundles for
 *       these nodes.  There is nothing more complex going on.
 * </ol>
 * Some of these assumptions may be broadened in a subclass.
 * <p>
 * The concrete subclass need only define the factory, which should inherit from
 * the nested factory class here.
 * @author boyland
 */
@SuppressWarnings("unchecked")
public abstract class PlainComponent extends Component {
  private Logger LOG = SLLogger.getLogger("FLUID.project");

  private static final VersionedSlot<IRNode> initialRootSlot = (VersionedSlot)VersionedSlotFactory.dependent.predefinedSlot(null);
  VersionedSlot<IRNode> rootSlot = initialRootSlot;
  /*final*/ VersionedRegion region;
  
  /**
   * Constructor for a new rooted component.
   * We create a new region for it.
   */
  public PlainComponent() {
    super(true);
    region = new VersionedRegion();
  }
  protected PlainComponent(int magic) {
    super(magic,true);
    region = new VersionedRegion();
  }
  
  /**
   * We create a new component in an existing region.
   * @param vr
   */
  public PlainComponent(VersionedRegion vr) {
    super(true);
    region = vr;
  }

  /** Constructor for an existing rooted component.
   * @param magic
   * @param id
   */
  protected PlainComponent(UniqueID id, VersionedRegion vr) {
    super(id);
    region = vr;
  }
  protected PlainComponent(int magic, UniqueID id) {
    super(magic,id);
    region = null; // assigned later
  }
  protected PlainComponent(UniqueID id) {
    this(id,null); // region assigned later
  }

  @SuppressWarnings("unchecked")
  protected PlainComponent(PlainComponent from, Version v) {
    super(from,v);
    region = SharedVersionedRegion.get(from.region,v);
    rootSlot = (VersionedSlot) VersionedSlotFactory.dependent.getStorage().newSlot(from.rootSlot.getValue(v));
  }
  
  /** Set the root for this component */
  public void setRoot(IRNode n) {
    rootSlot = (VersionedSlot<IRNode>) rootSlot.setValue(n);
    noteChange();
  }
  
  protected void setRegion(VersionedRegion vr) {
    if (region == null) region = vr;
    else if (region != vr) {
      LOG.severe("Cannot change VR of component " + vr);
    }
  }
  public VersionedRegion getRegion() {
    return region;
  }
  
  /** Return the root of this Component */
  public IRNode getRoot() {
    return rootSlot.getValue();
  }
  
  /** Get the component's root at the version v */
  public IRNode getRoot(Version v) {
    if (rootSlot.isValid()) {
      return rootSlot.getValue(v);
    }
    else return null;
  }
  
  /** is Component changed? */  
  @Override
  public boolean isChanged() {
    return rootSlot.isChanged();
  }

  @Override
  public boolean isShared() {
    return region != null && region.isShared();
  }
  
  @Override
  public void writeContents(IROutput out) throws IOException {
    rootSlot.writeValue(IRNodeType.prototype,out);
  }
  
  @Override
  public void writeChangedContents(IROutput out) 
    throws IOException  {
    if (rootSlot.isChanged()) {
      out.writeBoolean(true);   
      rootSlot.writeValue(IRNodeType.prototype,out);
    } else {
      out.writeBoolean(false);
    }
  }
  
  @Override
  public void readContents(IRInput in) throws IOException {
    rootSlot = (VersionedSlot<IRNode>) rootSlot.readValue(IRNodeType.prototype, in);
  }

  @Override
  public void readChangedContents(IRInput in) throws IOException {
    if (in.readBoolean()) {
      rootSlot = (VersionedSlot<IRNode>) rootSlot.readValue(IRNodeType.prototype, in);
    }
  }
  

  @Override
  protected void write(IROutput out) throws IOException {
    super.write(out);
    region.writeReference(out);
  }
  
  @Override
  protected void read(IRInput in) throws IOException {
    super.read(in);
    setRegion((VersionedRegion)IRPersistent.readReference(in));
  }
  
  /**
   * Return the bundles for which we must load to get things up-to-date
   * 
   * @return iterator of bundles (all distinct)
   */
  protected abstract Collection<Bundle> getBundles();
  
  protected void ensureRegionLoaded(FileLocator floc) {
    if (region == null || !region.isDefined()) {
      try {
        if (region == null) load(floc);
        region.load(floc);
      } catch (IOException e) {
        LOG.log(Level.SEVERE,"Cannot find region for plain component",e);
        throw new FluidRuntimeException("Cannot load component");
      }
    }
  }

  /* XXX
   * The following routines are not clear on who is responsible for what.
   * ensureLoaded figures out what to load, but sometimes it calls these ones
   * indirectly through teh superclass.  (JTB 2007/2/28)
   */
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.project.Component#saveDelta(edu.cmu.cs.fluid.version.Era, edu.cmu.cs.fluid.util.FileLocator)
   */
  @Override
  public void saveDelta(Era era, FileLocator floc) throws IOException {
    super.saveDelta(era,floc);
    region.ensureStored(floc);
    for (Bundle b : getBundles()) {
      VersionedChunk vc = VersionedChunk.get(region,b);
      if (era.isChanged(vc) >= 0) {
        vc.getDelta(era).ensureStored(floc);
      }
    }
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.project.Component#loadDelta(edu.cmu.cs.fluid.version.Era, edu.cmu.cs.fluid.util.FileLocator)
   */
  @Override
  public void loadDelta(Era era, FileLocator floc) throws IOException {
    super.loadDelta(era,floc);
    ensureRegionLoaded(floc);
    for (Bundle b : getBundles()) {
      VersionedChunk vc = VersionedChunk.get(region,b);
      VersionedDelta delta = vc.getDelta(era);
      if (era.isChanged(vc) >= 0) {
        delta.load(floc);
      } else {
        LOG.fine("Skipping load of " + delta);
      }
      if (!era.isLoaded(vc)) {
        LOG.severe("loadDelta didn't work!" + this + " for " + era);
      }
    }
  }
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.project.Component#saveSnapshot(edu.cmu.cs.fluid.version.Version, edu.cmu.cs.fluid.util.FileLocator)
   */
  @Override
  public void saveSnapshot(Version v, FileLocator floc) throws IOException {
    super.saveSnapshot(v,floc);
    region.ensureStored(floc);
    for (Bundle b : getBundles()) {
      VersionedChunk vc = VersionedChunk.get(region,b);
      IRPersistent snap = vc.getSnapshot(v);
      snap.store(floc);
    }
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.project.Component#loadSnapshot(edu.cmu.cs.fluid.version.Version, edu.cmu.cs.fluid.util.FileLocator)
   */
  @Override
  public void loadSnapshot(Version v, FileLocator floc) throws IOException {
    super.loadSnapshot(v,floc);
    for (Bundle b : getBundles()) {
      VersionedChunk vc = VersionedChunk.get(region,b);
      IRPersistent snapshot = vc.getSnapshot(v);
      snapshot.load(floc);
    }
  } 
  
  @Override
  public void ensureLoaded(Version v, FileLocator floc) {
    super.ensureLoaded(v, floc);
    Era era = v.getEra();
    if (era != null) {
      if (!era.isDefined()) {
        LOG.info("Need to load era!");
        try {
          era.load(floc);
        } catch (IOException e) {
          LOG.log(Level.SEVERE,"Cannot load Era",e);
          throw new FluidRuntimeException("Cannot load as requested.");
        }
      }
    }
    ensureRegionLoaded(floc);
    for (Bundle b : getBundles()) {
      VersionedChunk vc = VersionedChunk.get(region, b);
      if (v.isLoaded(vc)) continue;
      try {
        if (era != null) {
          IRPersistent delta = vc.getDelta(era);
          delta.load(floc);
        }
        continue;
      } catch (IOException e) {
        LOG.info("Unable to load delta, trying snapshot");
      }
      try {
        IRPersistent snapshot = vc.getSnapshot(v);
        snapshot.load(floc);
        continue;
      } catch (IOException e) {
        LOG.severe("unable to find snapshot or delta");
      }
      throw new FluidRuntimeException("unable to load requested IR");
    }
  }
  
  @Override
  protected Component makeShared(Version v) {
    // TODO Auto-generated method stub
    return null;
  }
  @Override
  public synchronized void undefine() {
    region.unload();
    rootSlot = initialRootSlot;
    super.undefine();
  }
}
