/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/VersionedRegionDelta.java,v 1.9 2008/06/24 19:13:12 thallora Exp $ */
package edu.cmu.cs.fluid.version;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.Bundle;
import edu.cmu.cs.fluid.ir.IRChunk;
import edu.cmu.cs.fluid.ir.IRInput;
import edu.cmu.cs.fluid.ir.IRPersistent;
import edu.cmu.cs.fluid.ir.IRPersistentKind;
import edu.cmu.cs.fluid.ir.IRRegion;

/** A region of nodes added to a VersionedRegion (VR)
 * during a particular Era.  This set is fixed once the era is fixed.
 * @see VersionedRegion
 */
public class VersionedRegionDelta extends IRRegion {
  private static final Logger LOG = SLLogger.getLogger("IR.version");
  private static final int magic = 0x56524400; // "VRD\0"

  private final VersionedRegion base;
  private final Era era;

  /** Create a new versioned region delta for a new era. */
  protected VersionedRegionDelta(VersionedRegion vr, Era e) {
    super(magic, false);
    base = vr;
    era = e;
  }

  /** Create a versioned region delta for an old era.
   */
  protected VersionedRegionDelta(VersionedRegion vr, Era e, boolean ignored) {
    super(magic, null);
    base = vr;
    era = e;
  }
  @Override
  protected void complete(int numNodes) {
    super.complete(numNodes);
  }

  /** Create a versioned region delta for an old era.
   * @param numNodes number of nodes in delta.
   */
  protected VersionedRegionDelta(VersionedRegion vr, Era e, int numNodes) {
    super(magic, null);
    complete(numNodes);
    base = vr;
    era = e;
  }

  public VersionedRegion getBase() {
    return base;
  }

  public Era getEra() {
    return era;
  }

  @Override
  public IRChunk createChunk(Bundle b) {
    if (isNew()) {
      return new VersionedChunk.SubsidiaryChunk(this,b);
    } else {
      return new VersionedChunk.SubsidiaryChunk(this,b,false);
    }
  }
  
  /* persistent kind */

  static final IRPersistentKind kind = new IRPersistentKind() {
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException {
      VersionedRegionDelta vrd = (VersionedRegionDelta) p;
      vrd.getBase().writeReference(out);
      vrd.getEra().writeReference(out);
      // out.writeInt(vrd.getNumNodes());
    }
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException {
      VersionedRegion vr = (VersionedRegion) IRPersistent.readReference(in);
      Era e = (Era) IRPersistent.readReference(in);
      if ((in instanceof IRInput) && ((IRInput) in).getRevision() < 4) {
        int numNodes = in.readInt();
        return vr.getDelta(e, numNodes);
      }
      return vr.getDelta(e);
    }
  };

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  
  @Override
  public void undefine() {
    LOG.info("unloading " + this);
    super.undefine();
  }
  
  @Override
  public String toString() {
    return "VRD(" + getBase() + "," + getEra() + ")";
  }

  public static void ensureLoaded() {
  }
}

/*
class VersionedRegionCopy extends IRRegion {
  
  ** Create a versioned region using current information *
  VersionedRegionCopy(VersionedRegion vr, Version v) {
    super(magic,null);
  }
}
*/
