/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedSnapshot.java,v 1.10 2008/06/24 19:13:12 thallora Exp $ */
package edu.cmu.cs.fluid.version;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import com.surelogic.common.Pair;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.Bundle;
import edu.cmu.cs.fluid.ir.IRChunk;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IROutput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRPersistentKind;
import edu.cmu.cs.fluid.ir.IRRegion;

/** A snapshot in "time" of the values a chunk */
class VersionedSnapshot extends IRChunk {
  private static final Logger LOG = SLLogger.getLogger("IR.version");
  private static final int magic = 0x56530000; // VCS\0\0
  private final Version version;

  public Version getVersion() {
    return version;
  }

  /** Create a versioned snapshot in preparation for saving. */
  public VersionedSnapshot(IRChunk c, Version v) {
    this(magic, c, v);
  }

  /** Create a chunk version in preparation for saving. */
  protected VersionedSnapshot(int magic, IRChunk c, Version v) {
    super(magic, c.getRegion(), c.getBundle());
    version = v;
  }

  /** Create a versioned snapshot in preparation for loading. */
  public VersionedSnapshot(IRChunk c, Version v, boolean unused) {
    this(magic, c, v, false);
  }

  /** Create a chunk version in preparation for loading. */
  protected VersionedSnapshot(int magic, IRChunk c, Version v, boolean uu) {
    super(magic, c.getRegion(), c.getBundle(), false);
    version = v;
  }

  IRChunk getChunk() {
    return IRChunk.get(getRegion(), getBundle());
  }

  private static HashMap<Pair<IRChunk, Version>, VersionedSnapshot> snapshots = new HashMap<Pair<IRChunk,Version>, VersionedSnapshot>();

  public static VersionedSnapshot find(IRChunk c, Version v) {
    return snapshots.get(Pair.getInstance(c, v));
  }

  protected static void add(VersionedSnapshot vs) {
    snapshots.put(Pair.getInstance(vs.getChunk(), vs.getVersion()), vs);
  }

  public static VersionedSnapshot get(IRChunk c, Version v) {
    VersionedSnapshot vs = find(c, v);
    if (vs == null) {
      if (c instanceof VersionedChunk)
        return ((VersionedChunk) c).getSnapshot(v);
      if (v.getEra() == null || v.getEra().isNew())
        vs = new VersionedSnapshot(c, v);
      else
        vs = new VersionedSnapshot(c, v, false);
      snapshots.put(Pair.getInstance(c, v), vs);
    }
    return vs;
  }

  @Override
  protected String getFileName() {
    return version.getEra().getID().toString()
      + File.separator
      + version.getEraOffset()
      + "-"
      + super.getFileName();
  }

  /* Kind */

  private static IRPersistentKind kind = new IRPersistentKind() {
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException {
      VersionedSnapshot vs = (VersionedSnapshot) p;
      vs.getRegion().writeReference(out);
      vs.getBundle().writeReference(out);
      vs.getVersion().getEra().writeReference(out);
      vs.getVersion().write(out);
    }
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException {
      IRRegion r = (IRRegion) IRPersistent.readReference(in);
      Bundle b = (Bundle) IRPersistent.readReference(in);
      Era e = (Era) IRPersistent.readReference(in);
      IRChunk c = IRChunk.get(r, b);
      return VersionedSnapshot.get(c, Version.read(in, e));
    }
  };
  static {
    IRPersistent.registerPersistentKind(kind, 0x24); // $
  }

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  /* Output */

  // make accessible to VersionedChunk.Snapshot
  @Override
  protected void forceComplete() {
    super.forceComplete();
  }

  /** When writing we set the version to the version of the snapshot
   */
  @Override
  public void writeAttributeValues(IROutput out) throws IOException {
    //VersionedStructure vs = IRChunkVersionedStructure.get(getChunk());
    //VersionedStructureFactory.pushVS(vs);
    Version.saveVersion();
    try {
      Version.setVersion(version);
      super.writeAttributeValues(out);
    } finally {
      Version.restoreVersion();
      //VersionedStructureFactory.popVS();
    }
  }

  /** Read snapshot from file by setting version.
   */
  @Override
  protected void readAttributeValues(IRInput in) throws IOException {
    LOG.finer("reading attribute values at version " + version + ", with VS.era = " + VersionedSlot.getEra());
    //VersionedStructure vs = IRChunkVersionedStructure.get(getChunk());
    //VersionedStructureFactory.pushVS(vs);
    Version.saveVersion();
    version.clamp();
    try {
      Version.setVersion(version);
      super.readAttributeValues(in);
    } finally {
      version.unclamp();
      Version.restoreVersion();
      //VersionedStructureFactory.popVS();
    }
    define();
  }

  @Override
  public String toString() {
    return "VS("
      + getRegion()
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
