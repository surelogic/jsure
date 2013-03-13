/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/version/SharedVersionedRegion.java,v
 * 1.2 2003/09/20 02:09:19 thallora Exp $
 */
package edu.cmu.cs.fluid.version;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.util.*;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.*;

/**
 * A shared versioned region is a region whose definition at the start of time 
 * (\alpha) is the contents of some VR at a particular version.
 * An SVR is created when
 * exporting a chunk of IR to be shared in different version spaces.
 * It is only written out.  It is not read in, and does not exist elsewhere.
 * An SVR is thus metaphorically similar to PIC (position independent code) that
 * can live at any virtual address, except that it can be updated in multiple
 * version spaces.
 * 
 * @see SharedVersionedChunk
 */
public class SharedVersionedRegion extends VersionedRegion {
  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("IR.version");

  // private static final int magic = 0x53565200; // "SVR\0"

  private static Map<UniqueID,SharedVersionedRegion> sharedVersionedRegions = 
    new HashMap<UniqueID,SharedVersionedRegion>();
  
  private VersionedRegion region;
  private UniqueID exportedID;
  private Version version;
  private Map<IRNode, SharedVersionedRegion.Node> proxies;

  private static boolean checkVRV(VersionedRegion vr, Version v) {
    Era e = v.getEra();
    if (e == null || !e.isComplete() || !vr.getDelta(e).isComplete()) {
      LOG.severe("Cannot create SVR with incomplete era and VR");
      return false;
    }
    return true;
  }

  private static UniqueID computeID(VersionedRegion vr, Version v) {
    return vr.getID().combine(v.getEra().getID()).combine(v.getEraOffset());
  }

  /**
	 * Create a SharedVersionedRegion from an existing VersionedRegion for a
	 * given version. The version's era must be complete, and the VRD of the VR
	 * for this era must be complete. This is the constructor used to create a
	 * VIR before storing it.
	 */
  protected SharedVersionedRegion(VersionedRegion vr, Version v) {
    super(null, Version.getInitialVersion());
    checkVRV(vr,v);
    region = vr;
    version = v;
    exportedID = computeID(vr, v);
    proxies = new HashMap<IRNode,SharedVersionedRegion.Node>();
    VersionedRegionDelta vrd = new VersionedRegionDelta(this,Era.getInitialEra(),false);
    vrd.define();
    int count = 0;
    for (Iterator nodes = vr.allNodes(v); nodes.hasNext();) {
      IRNode n = (IRNode) nodes.next();
      new Node(this, vrd, n);
      ++count;
    }
    vrd.complete(count);
    sharedVersionedRegions.put(exportedID,this);
  }

  /*
   * Create a VIR for a particular ID: we don't know the VR or Version thi s
   * comes from.
   *
  protected SharedVersionedRegion(UniqueID id) {
    super(magic, id, Version.getInitialVersion());
    region = null;
    version = null;
  }*/

  /**
	 * Get a version independent region for the given versioned region at the
	 * particular era passed. Create a new SVR if one does not yet exist for this
	 * combination.
	 * 
	 * @param vr
	 *          versioned region to export (VRDs must be complete.)
	 * @param v
	 *          point at which to export it. (Must be complete.)
	 */
  public static SharedVersionedRegion get(VersionedRegion vr, Version v) {
    SharedVersionedRegion svr = sharedVersionedRegions.get(computeID(vr,v));
    if (svr == null) {
      svr = new SharedVersionedRegion(vr, v);
      svr.define();
    }
    return svr;
  }

   /*
   * Get a VIR for a particular ID. Create a new one if it does not yet ex ist.
   * The VIR will have no nodes until we find somewhere that imported it w ith
   * nodes.
   *
  protected static SharedVersionedRegion get(UniqueID id) {
    IRPersistent p = IRPersistent.find(id);
    SharedVersionedRegion vir;
    try {
      vir = (SharedVersionedRegion) p;
    } catch (ClassCastException ex) {
      LOG.severe("Uniqueness error: VIR /= " + p);
      vir = null;
    }
    if (vir == null) {
      vir = new SharedVersionedRegion(id);
    }
    return vir;
  }
  */
  
  /** Return the unique ID of the versioned region that can be loaded.
   * @return ID of the versioned region this stores as.
   */ 
  public UniqueID getExportedID() {
    return exportedID;
  }
  
  /**
	 * Return the Version that exported this VIR.
	 */
  public Version getVersion() {
    return version;
  }

  /*
   * Return true if the nodes in this region aren't regular nodes with regular
   * slots, but actually just proxies for the versioned regions. NB: If proxies
   * are being used then slot access is very inefficient.
   *
  public boolean usesProxies() {
    return proxies != null;
  }*/

  private class Node extends PlainIRNode {
    IRNode represents;
    Node(SharedVersionedRegion vir, VersionedRegionDelta vrd, IRNode n) {
      super(vrd);
      represents = n;
      proxies.put(n, this);
    }

    private SharedVersionedRegion getVIR() {
      return SharedVersionedRegion.this;
    }

    private void saveVersion() {
      Version.saveVersion(getVIR().getVersion());
    }

    // all get's are sent to the proxy
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getSlotValue(SlotInfo<T> si) throws SlotUndefinedException {
      saveVersion();
      try {
        return (T) getVIR().wrap(represents.getSlotValue(si));
      } finally {
        Version.restoreVersion();
      }
    }

    @Override
    public void setSlotValue(SlotInfo si, Object newValue)
      throws SlotImmutableException {
      throw new SlotImmutableException("SVR proxy nodes cannot have slot updates");
    }

    @Override
    public <T> boolean valueExists(SlotInfo<T> si) {
      saveVersion();
      try {
        return represents.valueExists(si);
      } finally {
        Version.restoreVersion();
      }
    }
  }

  /**
	 * Get a potentially versioned object and wrap it nodes are translated. We
	 * must be able to figure out what sort of object it is. <b>Warning:</b>
	 * This code is very inefficient.
	 */
  @SuppressWarnings("unchecked")
  private Object wrap(Object o) {
    if (o instanceof IRNode) {
      return getProxy((IRNode) o);
    } else if (o instanceof IRSequence) {
      return getProxySequence((IRSequence) o);
    } else if (
      o instanceof Number || o instanceof String || o instanceof Boolean) {
      return o;
    } else {
      LOG.severe("Cannot determine how to wrap this value: " + o);
      return o;
    }
  }

  /**
	 * Return the proxy for this node, If the node belongs to a versioned region,
	 * we get the proxy associated with the VIR associated with its versioned
	 * region and the era of this VIR. (This method creates the appropriate VIR
	 * if necessary.)
	 * 
	 * @return n or a proxy.
	 */
  public IRNode getProxy(IRNode n) {
    if (n == null)
      return n;
    IRRegion reg = VersionedRegion.getVersionedRegion(n);
    if (reg instanceof VersionedRegion) {
      SharedVersionedRegion vir = get((VersionedRegion) reg, version);
      synchronized (vir) {
        IRNode proxy = vir.proxies.get(n);
        if (proxy != null)
          return proxy;
      }
    }
    return n;
  }

  /**
	 * Return the node that this node represents.
	 * 
	 * @return n if not a proxy node
	 */
  public static IRNode getOriginal(IRNode n) {
    if (n instanceof Node) {
      return ((Node) n).represents;
    }
    return n;
  }

  private <T> IRSequence<T> getProxySequence(IRSequence<T> s) {
    return new IRSequenceWrapper<T>(s) {
      private void saveVersion() {
        Version.saveVersion(SharedVersionedRegion.this.getVersion());
      }

      // mutations forbidden:
      @Override
      public void setElementAt(T o, IRLocation loc) {
        throw new SlotImmutableException("ProxySequences cannot be mutated");
      }
      @Override
      public IRLocation insertElementAt(T o, InsertionPoint ip) {
        throw new SlotImmutableException("ProxySequences cannot be mutated");
      }
      @Override
      public void removeElementAt(IRLocation loc) {
        throw new SlotImmutableException("ProxySequences cannot be mutated");
      }

      @Override
      @SuppressWarnings("unchecked")
      public Iteratable elements() {
        saveVersion();
        try {
          return new FilterIterator(super.elements()) {
            @Override
            protected Object select(Object o) {
              return wrap(o);
            }
          };
        } finally {
          Version.restoreVersion();
        }
      }

      @Override
      @SuppressWarnings("unchecked")
      public T elementAt(IRLocation loc) {
        saveVersion();
        try {
          return (T) wrap(super.elementAt(loc));
        } finally {
          Version.restoreVersion();
        }
      }

      @Override
      public int size() {
        saveVersion();
        try {
          return super.size();
        } finally {
          Version.restoreVersion();
        }
      }
      @Override
      public boolean isVariable() {
        saveVersion();
        try {
          return super.isVariable();
        } finally {
          Version.restoreVersion();
        }
      }
      @Override
      public boolean hasElements() {
        saveVersion();
        try {
          return super.hasElements();
        } finally {
          Version.restoreVersion();
        }
      }
      @Override
      public boolean validAt(IRLocation loc) {
        saveVersion();
        try {
          return super.validAt(loc);
        } finally {
          Version.restoreVersion();
        }
      }
      @Override
      public IRLocation location(int i) {
        saveVersion();
        try {
          return super.location(i);
        } finally {
          Version.restoreVersion();
        }
      }
      @Override
      public int locationIndex(IRLocation loc) {
        saveVersion();
        try {
          return super.locationIndex(loc);
        } finally {
          Version.restoreVersion();
        }
      }
      @Override
      public IRLocation firstLocation() {
        saveVersion();
        try {
          return super.firstLocation();
        } finally {
          Version.restoreVersion();
        }
      }
      @Override
      public IRLocation lastLocation() {
        saveVersion();
        try {
          return super.lastLocation();
        } finally {
          Version.restoreVersion();
        }
      }
      @Override
      public IRLocation nextLocation(IRLocation loc) {
        saveVersion();
        try {
          return super.nextLocation(loc);
        } finally {
          Version.restoreVersion();
        }
      }
      @Override
      public IRLocation prevLocation(IRLocation loc) {
        saveVersion();
        try {
          return super.prevLocation(loc);
        } finally {
          Version.restoreVersion();
        }
      }
      @Override
      public int compareLocations(IRLocation loc1, IRLocation loc2) {
        saveVersion();
        try {
          return super.compareLocations(loc1, loc2);
        } finally {
          Version.restoreVersion();
        }
      }
    };
  }

  /* persistent kind */

  /*
  private static IRPersistentKind kind = new IRPersistentKind() {
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException {
      p.getID().write(out);
    }
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException {
      UniqueID id = UniqueID.read(in);
      return SharedVersionedRegion.get(id);
    }
  };
  static {
    IRPersistent.registerPersistentKind(kind, 0x12); // Control-R
  }

  public IRPersistentKind getKind() {
    return kind;
  }
  */
  
  @Override
  protected String getFileName() {
    return exportedID.toString() + ".vr";
  }


  @Override
  public String toString() {
    if (region != null) {
      return "SVR(" + region + "," + version + ")";
    }
    return super.toString();
  }

  @Override
  public void describe(PrintStream out) {
    super.describe(out);
    if (region != null) {
      out.println("  region = " + region + ", version = " + version);
    }
  }

  public static void ensureLoaded() {
  }
}
