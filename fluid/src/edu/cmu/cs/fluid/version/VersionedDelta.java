/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedDelta.java,v 1.12 2008/06/24 19:13:12 thallora Exp $ */
package edu.cmu.cs.fluid.version;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import com.surelogic.common.Pair;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.FileLocator;

/** Changes to a chunk during a particular era. */
public class VersionedDelta extends IRChunkDelta {
  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("IR.version");

  private static final int magic = 0x56440000; // VD\0\0
  private final Era era;
  private int eraMaxOffset; // used for incomplete eras

  public Era getEra() {
    return era;
  }

  /** Create a versioned delta in preparation for saving.
   * The era must be new.
   */
  protected VersionedDelta(IRChunk chunk, Era e) {
    this(magic, chunk, e);
  }

  /** Create a versioned delta with a specific magic number
   * in preparation for saving.
   * The era must be new.
   */
  protected VersionedDelta(int magic, IRChunk chunk, Era e) {
    super(magic, chunk, false);
    era = e;
    /*
    if (!e.isNew()) {
      throw new FluidError("Creating new deltas from old eras.");
    }
    */
  }

  /** Create a versioned delta in preparation for loading. */
  protected VersionedDelta(IRChunk chunk, Era e, boolean unused) {
    this(magic, chunk, e, false);
  }

  /** Create a versioned delta with a specific magic number
   * in preparation for loading.
   */
  protected VersionedDelta(int magic, IRChunk chunk, Era e, boolean unused) {
    super(magic, chunk, null);
    era = e;
  }

  private static HashMap<Pair<IRChunk, Era>, VersionedDelta> deltas = new HashMap<Pair<IRChunk,Era>, VersionedDelta>();

  public static VersionedDelta find(IRChunk c, Era e) {
    return deltas.get(Pair.getInstance(c, e));
  }

  protected static void add(VersionedDelta vd) {
    deltas.put(Pair.getInstance(vd.getChunk(), vd.getEra()), vd);
  }

  /** Force this delta to be defined.
   * We need to do this for deltas that are empty,
   * and thus not stored explicitly in a chunk file.
   * @see VersionedChunk.Delta#read
   */
  protected void forceDefined() {
    super.define();
  }

  public static VersionedDelta get(IRChunk c, Era e) {
    VersionedDelta vd = find(c, e);
    if (vd == null) {
      if (c instanceof VersionedChunk)
        return ((VersionedChunk) c).getDelta(e);
      if (e.isNew())
        vd = new VersionedDelta(c, e);
      else
        vd = new VersionedDelta(c, e, false);
      add(vd);
    }
    return vd;
  }

  /** Return whether this delta is defined at this version.
   * @see VersionedChunk#isDefined(Version)
   */
  public boolean isDefined(Version v) {
    VersionedChunk.debugIsDefined = 1000;
    if (!isDefined())
      return false;
    VersionedChunk.debugIsDefined = 1001;
    if (v.getEra() != era)
      return false;
    VersionedChunk.debugIsDefined = 1002;
    if (isComplete() || isNew())
      return true;
    VersionedChunk.debugIsDefined = 1004 + eraMaxOffset/10.0;
    return (v.getEraOffset() <= eraMaxOffset);
  }

  @Override
  protected String getFileName() {
    IRChunk chunk = getChunk();
    if (chunk.getRegion().getID() == null) {
      LOG.severe("Region " + chunk.getRegion() + " has no ID!");
    }
    return era.getID().toString()
      + File.separator
      + chunk.getRegion().getID()
      + "-"
      + chunk.getBundle().getID()
      + ".ird";
  }

  /* Kind */

  private static IRPersistentKind kind = new IRPersistentKind() {
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException {
      VersionedDelta vd = (VersionedDelta) p;
      vd.getChunk().writeReference(out);
      vd.getEra().writeReference(out);
    }
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException {
      IRChunk chunk = (IRChunk) IRPersistent.readReference(in);
      Era era = (Era) IRPersistent.readReference(in);
      return get(chunk, era);
    }
  };
  static {
    IRPersistent.registerPersistentKind(kind, 0x04); // control-D
  }

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  /* Output */

  // make accessible to VersionedChunk.Delta
  @Override
  protected void forceComplete() {
    super.forceComplete();
  }

  /** Write the attribute values that have changed during the era.
   */
  @Override
  protected void writeChangedAttributes(IROutput out) throws IOException {
    if (isNew())
      eraMaxOffset = era.maxVersionOffset();
    out.debugBegin("eraMaxOffset");
    out.writeShort(eraMaxOffset);
    out.debugEnd("eraMaxOffset");
    //VersionedStructureFactory.pushVS(IRChunkVersionedStructure.get(getChunk()));
    try {
      VersionedSlot.pushEra(era); // change behavior of versioned slots
      try {
        Version.saveVersion();
        try {
          // we set the version to the era root
          // for attributes not changed in the current era
          // that refer to compounds that *are* changed.
          // This version is used to get the current value of the
          // compound before reading changed contents.
          // When the attribute itself has changed, we use a
          // different technique to save the changes.
          Version.setVersion(era.getRoot());
          super.writeChangedAttributes(out);
        } finally {
          Version.restoreVersion();
        }
      } finally {
        VersionedSlot.popEra();
      }
    } finally {
      //VersionedStructureFactory.popVS();
    }
  }

  /* Input */

  /** Before a load, we make sure that the system is defined for
   * the previous version.  Otherwise, we throw SlotUnknownException.
   */
  @Override
  public void load(FileLocator floc) throws IOException {
    if (isNew() || isComplete())
      return;
    IRChunk chunk = getChunk();
    if (!era.getRoot().isLoaded(chunk)) {
      try {
        VersionedSnapshot.get(chunk, era.getRoot()).load(floc);
      } catch (IOException ex) {
        VersionedDelta.get(chunk, era.getParentEra()).load(floc);
      }
    }
    LOG.fine("Loading " + this +  " while current version is " + Version.getVersionLocal());
    super.load(floc);
  }

  /** Read the attribute values that have changed during the era.
   */
  @Override
  protected void readChangedAttributes(IRInput in) throws IOException {
    in.debugBegin("eraMaxOffset");
    eraMaxOffset = in.readShort();
    in.debugEnd("eraMaxOffset");
    //VersionedStructureFactory.pushVS(IRChunkVersionedStructure.get(getChunk()));
    try {
      VersionedSlot.pushEra(era); // change behavior of versioned slots
      try {
        Version.saveVersion(); // for convenience: version can be changed
        try {
          Version.setVersion(era.getRoot());
          super.readChangedAttributes(in);
        } finally {
          Version.restoreVersion();
        }
      } finally {
        VersionedSlot.popEra();
      }
    } finally {
      //VersionedStructureFactory.popVS();
    }
    define();
  }

  @Override
  public String toString() {
    return "VD(" + getChunk() + "," + getEra() + ")";
  }
}
