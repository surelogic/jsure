/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/SharedVersionedChunk.java,v 1.8 2008/06/24 19:13:12 thallora Exp $
 * 
 * Created on Jun 1, 2004
 */
package edu.cmu.cs.fluid.version;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import com.surelogic.common.Pair;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidError;
import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.FileLocator;


/**
 * A versioned chunk defined on a shared versioned region.
 * This structure should only be used to store.  It
 * is not used afterwards.
 * @author boyland
 * @see SharedVersionedRegion
 */
public class SharedVersionedChunk extends VersionedChunk {
  /**
   * Logger for this class
   */
  private static final Logger LOG = SLLogger.getLogger("IR.version");

  private static HashMap<Pair<SharedVersionedRegion, Bundle>, SharedVersionedChunk> sharedVersionedChunks = new HashMap<Pair<SharedVersionedRegion,Bundle>, SharedVersionedChunk>();
  
  /** Create a new VIC from existing structures in memory. */
  protected SharedVersionedChunk(SharedVersionedRegion svr, Bundle b) {
    super(svr, b);
    sharedVersionedChunks.put(Pair.getInstance(svr,b),this);
  }

  /**
   * Create an SVC for the given VR, era and bundle. This is necessary before
   * storing it persistently. Upon loading, it will be normal chunk.
   */
  public static SharedVersionedChunk get(VersionedRegion vr, Version v, Bundle b) {
    SharedVersionedRegion svr = SharedVersionedRegion.get(vr, v);
    SharedVersionedChunk ch = sharedVersionedChunks.get(Pair.getInstance(svr, b));
    if (ch == null) {
      ch = new SharedVersionedChunk(svr, b);
    }
    return ch;
  }

  /**
   * Create a VIC for the given VC and version. This is necessary before
   * storing it persistently. Upon loading, it will be normal chunk.
   */
  public static SharedVersionedChunk get(VersionedChunk vc, Version v) {
    return get(vc.getVersionedRegion(), v, vc.getBundle());
  }

  /** Return the svr associated with this chunk. */
  public SharedVersionedRegion getSVR() {
    return (SharedVersionedRegion) getVersionedRegion();
  }

  /**
   * Make sure we don't create deltas:
   * @see edu.cmu.cs.fluid.version.VersionedChunk#getDelta(edu.cmu.cs.fluid.version.Era)
   */
  @Override
  public VersionedDelta getDelta(Era era) {
    throw new UnsupportedOperationException("SVCs don't have deltas");
  }

  /**
   * Make sure we don't create snapshots.
   * @see edu.cmu.cs.fluid.version.VersionedChunk#getSnapshot(edu.cmu.cs.fluid.version.Version)
   */
  @Override
  public VersionedSnapshot getSnapshot(Version v) {
    throw new UnsupportedOperationException("SVCs don't have snapshots");
  }
  
  /**
   * Return true when this VIC is actually a proxy for the ``real'' data in
   * core. If so, the VIC does not have any storage of its own. If false, then
   * this chunk is a normal chunk.
   *
  public boolean usesProxies() {
    return getSVR().usesProxies();
  }*/


  public boolean isDefined(Version v) {
    return true;
  }

  public boolean isDefined(Era e) {
    return true;
  }

  public void noteChange(Version v) {
    LOG.severe("Changes should not be performed on SVCs!");
  }

  @Override
  public String toString() {
    return "S" + super.toString();
  }
  
  /**
   * If using proxies, then wrap the attribute to unwrap proxy nodes.
   */
  @Override
  protected PersistentSlotInfo getAttribute(int i) {
    final PersistentSlotInfo psi = super.getAttribute(i);
    /*if (!usesProxies()) {
      return psi;
    }*/
    return new PersistentSlotInfo() {
      public SlotFactory getSlotFactory() {
        return psi.getSlotFactory();
      }
      public IRType getType() {
        return psi.getType();
      }
      public boolean isPredefined() {
        return psi.isPredefined();
      }
      public Object getDefaultValue() {
        return psi.getDefaultValue();
      }

      @Override
      public String toString() {
        return "SVCA(" + psi.toString() + "," + getSVR().getVersion() + ")";
      }

      private IRNode unwrapNode(IRNode n) {
        return SharedVersionedRegion.getOriginal(n);
      }

      // the slot is immutable and unchanging
      public boolean valueChanged(IRNode node) {
        return false;
      }
      public void writeChangedSlot(IRNode node, IROutput out)
        throws IOException {
      }
      public void readChangedSlotValue(IRNode node, IRInput in)
        throws IOException {
        throw new FluidError("assertion failed: should not get here.");
      }

      public void writeSlot(IRNode node, IROutput out) throws IOException {
        psi.writeSlot(unwrapNode(node), out);
      }

      public void readSlotValue(IRNode node, IRInput in) throws IOException {
        psi.readSlotValue(node, in);
      }
      public void undefineSlot(IRNode node) {
        psi.undefineSlot(node);
      }
    };
  }

  // Output
  
  protected class Delta extends VersionedChunk.Delta {
    /** Create a delta for the initial era.
     */
    protected Delta() {
      super(SharedVersionedChunk.this, Era.getInitialEra());
    }
    
    /** If using proxies, wrap the output stream to wrap the nodes. */
    @Override
    public void write(IROutput out) throws IOException {
      LOG.fine("outputting SVC initial delta using wrapper");
      out = new IROutputWrapper(out) {

        @Override
        public void writeNode(IRNode node) throws IOException {
          super.writeNode(getSVR().getProxy(node));
        }
      };
      // the version protecting is needed only if proxies are being used,
      // but have no effect otherwise.
      Version.saveVersion(getSVR().getVersion());
      VersionedSlot.pushInVIC();
      try {
        super.write(out);
      } finally {
        Version.restoreVersion();
        VersionedSlot.popInVIC();
      }
    }
    
    
    /* We need to use the exported ID
     * @see edu.cmu.cs.fluid.ir.IRPersistent#getFileName()
     */
    @Override
    protected String getFileName() {
      return Era.getInitialEra().getID().toString()
        + File.separator
        + getSVR().getExportedID()
        + "-"
        + getBundle().getID()
        + ".ird";
    }
  }
  
  /** Store an SVC to persistent store.
   * Actually what is stored is the delta for the initial era.
   * @see edu.cmu.cs.fluid.ir.IRPersistent#store(edu.cmu.cs.fluid.util.FileLocator)
   */
  @Override
  public synchronized void store(FileLocator floc) throws IOException {
    this.new Delta().store(floc);
  }
  

  /** SVCs cannot be loaded.
   * @see edu.cmu.cs.fluid.ir.IRPersistent#load(edu.cmu.cs.fluid.util.FileLocator)
   */
  @Override
  public synchronized void load(FileLocator floc) throws IOException {
    throw new UnsupportedOperationException("SVCs cannot be loaded.");
  }
  
 }

class IROutputWrapper implements IROutput {
  private final IROutput out;
  public IROutputWrapper(IROutput o) {
    out = o;
  }

  public void write(byte[] b) throws IOException {
    out.write(b);
  }
  public void write(byte[] b, int off, int len) throws IOException {
    out.write(b, off, len);
  }
  public void write(int b) throws IOException {
    out.write(b);
  }

  public void writeBoolean(boolean v) throws IOException {
    out.writeBoolean(v);
  }
  public void writeByte(int v) throws IOException {
    out.writeByte(v);
  }
  public void writeBytes(String s) throws IOException {
    out.writeBytes(s);
  }
  public void writeChar(int v) throws IOException {
    out.writeChar(v);
  }
  public void writeChars(String s) throws IOException {
    out.writeChars(s);
  }
  public void writeDouble(double v) throws IOException {
    out.writeDouble(v);
  }
  public void writeFloat(float v) throws IOException {
    out.writeFloat(v);
  }
  public void writeInt(int v) throws IOException {
    out.writeInt(v);
  }
  public void writeLong(long v) throws IOException {
    out.writeLong(v);
  }
  public void writeShort(int v) throws IOException {
    out.writeShort(v);
  }
  public void writeUTF(String str) throws IOException {
    out.writeUTF(str);
  }

  public void writeNode(IRNode node) throws IOException {
    out.writeNode(node);
  }

  public boolean writeCachedObject(Object object) throws IOException {
    return out.writeCachedObject(object);
  }

  public void writeIRType(IRType ty) throws IOException {
    out.writeIRType(ty);
  }

  public void writeSlotFactory(SlotFactory sf) throws IOException {
    out.writeSlotFactory(sf);
  }

  public void writePersistentReference(IRPersistent p) throws IOException {
    out.writePersistentReference(p);
  }

  public boolean debug() {
    return out.debug();
  }

  public void debugBegin(String x) {
    out.debugBegin(x);
  }

  public void debugEnd(String x) {
    out.debugEnd(x);
  }

  public void debugMark(String x) {
    out.debugMark(x);
  }
}

