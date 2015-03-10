/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRRegion.java,v 1.24 2008/07/11 21:13:16 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Iterator;

import com.surelogic.ThreadSafe;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.util.IntegerTable;
import edu.cmu.cs.fluid.util.UniqueID;

/** A set of persistent nodes in the fluid IR.  Each region
 * "owns" a certain number of IR nodes.
 * Each node has an index within the region.
 * Information about all the owned nodes is stored together. </p>
 * @see IndependentIRNode
 */
@ThreadSafe
public class IRRegion extends IRPersistent {
  /* Node ownership: these tables are used *except* for PlainIRNodes */
  private static final Hashtable<IRNode,Object> nodeOwnerTable = new Hashtable<IRNode,Object>();
  private static final Hashtable<IRNode,Integer> nodeIndexTable = new Hashtable<IRNode,Integer>();
  // NB: the first table holds an array list of observers who want
  // to be informed when the node is assigned to a region.

  /** Retrieve one of three values:
   * <ol>
   * <li> null - the node is not owned
   * <li> list - observers are waiting for it to be defined
   * <li> region - the node has a region.
   * </ol>
   */
  private static Object getOwnerInfo(IRNode node) {
    if (node instanceof PlainIRNode) {
      return ((PlainIRNode)node).getOwnerInfo();
    } else {
      if (node == null) {
    	  throw new NullPointerException();
      }
      // hashtable does the synchronization for us
      return nodeOwnerTable.get(node);
    }
  }

  private static void setOwnerInfo(IRNode node, Object o) {
    if (node instanceof PlainIRNode) {
      ((PlainIRNode)node).setOwnerInfo(o);
    } else {
      // hashtable does the synchronization for us
      nodeOwnerTable.put(node,o);
    }
  }

  /** Return the region associated with this node
   * @throws OwnerUndefinedException if node has no owner
   */
  public static IRRegion getOwner(IRNode node) throws OwnerUndefinedException {
    IRRegion reg = getOwnerOrNull(node);
    if (reg == null) {
      throw new OwnerUndefinedException(node);
    }
    return reg;
  }

  public static IRRegion getOwnerOrNull(IRNode n) {
    Object o = getOwnerInfo(n);
    if (o instanceof ArrayList) o = null;
    return (IRRegion)o;
  }

  /** Return true if node has an owner already. */
  public static boolean hasOwner(IRNode node) {
    return getOwnerOrNull(node) != null;
  }

  public synchronized static
  void whenOwned(IRNode node, IRPersistentObserver o) {
    Object obj = getOwnerInfo(node);
    if (obj == null) {
      obj = new ArrayList<IRPersistentObserver>();
      setOwnerInfo(node,obj);
    } else if (obj instanceof ArrayList) {
      @SuppressWarnings("unchecked") 
      List<IRPersistentObserver> observers = (List)obj;
      if (!observers.contains(o))
        observers.add(o);
    } else {
      o.updatePersistent(getOwner(node),node);
    }
  }
      

  /** Return the region associated with this node
   * @throws OwnerUndefinedException if node has no owner
   */
  public static int getOwnerIndex(IRNode node) throws OwnerUndefinedException {
    if (node instanceof PlainIRNode) {
      return ((PlainIRNode)node).getIndexInRegion();
    } else {
      try {
        return nodeIndexTable.get(node).intValue();
      } catch (NullPointerException ex) {
        throw new OwnerUndefinedException(node);
      }
    }
  }

  /** Set the index for the node. */
  private static void setOwnerIndex(IRNode node, int index) {
    if (node instanceof PlainIRNode) {
      ((PlainIRNode)node).setIndexInRegion(index);
    } else {
      nodeIndexTable.put(node,IntegerTable.newInteger(index));
    }
  }

  /** Set the owner and index for this node:
   * these values are always set together.
   */
  private static
  void setOwner(IRNode node, IRRegion region, int index) {
    Object obj;
    synchronized (node) {
      obj = getOwnerInfo(node);
      setOwnerInfo(node,region);
      setOwnerIndex(node,index);
    }
    if (obj instanceof List) {
      @SuppressWarnings("unchecked") List<IRPersistentObserver> observers = (List)obj;
      if (observers != null) {
        for (IRPersistentObserver o : observers) {
          o.updatePersistent(region,node);
        }
      }
    }
  }


  /* A region never imports other nodes.
     (for simplicity)
   */
  @Override
  public void importRegion(IRRegion other) {
    throw new FluidRuntimeException("cannot import one region into another");
  }


  /* ownership methods */

  private int numNodes = 0;

  /** Return number of nodes owned.
   * The result is valid if the region is complete.
   * Otherwise if it is ``new'' (created here),
   * it gives the number so far.
   * Otherwise the result is undefined.
   **/
  public int getNumNodes() {
    return numNodes;
  }

  private List<IRNode> nodes;

  @Override
  public boolean define() { // NB called from super constructor
    return define(10);
  }

  /** Make the region defined, with
   * a hint to the number of nodes that will be added.
   */
  public boolean define(int numNodes) {
    if (super.define()) {
      nodes = new ArrayList<IRNode>(numNodes);
      return true;
    }
    return false;
  }

  /** Ensure that there are at least min nodes in the
   * region, creating new nodes if necessary.
   */
  protected void ensureNodes(int min) {
    define(min);
    // nodes.ensureCapacity(min);
    for (int i=nodes.size(); i < min; ++i) {
      IRNode n = newNode();
      setOwner(n,this,i+1);
      nodes.add(n);
    }
    if (numNodes < min) numNodes = min;
  }

  protected IRNode newNode() {
    return new PlainIRNode(null);
  }

  /** Return node given index (> 0). */
  @Override
  public IRNode getNode(int index) throws IOException {
    if (index == 0) return null;
    if (index < 0 || index > numNodes) {
      if (isComplete() || isNew()) { throw new IOException("index " + index
          + " out of bounds (0," + numNodes + "] for " + this); }
      // Incomplete, old regions with valid index.
      // We assume that the index is legal, and create a node.
      // This is needed for serialization, or whenever
      // persisting incomplete regions.
      ensureNodes(index);
    }
    return nodes.get(index - 1);
  }

  /** Return index within region
   * @throws IOException if not owned by this region.
   */
  @Override
  public int getIndex(IRNode node) throws IOException {
    if (getOwner(node) == this) {
      return getOwnerIndex(node); // should not raise exception
    } else {
      throw new IOException("not owned");
    }
  }


  /* defining the number of nodes */

  /** Indicate that the storage has exactly this many nodes. */
  protected void complete(int numNodes) {
    if (isComplete() || this.numNodes > numNodes) {
      if (this.numNodes != numNodes)
	throw new FluidRuntimeException("Incompatible numbers of nodes for " +
					toString() + ": " +
					numNodes + " != " + this.numNodes);
    } else {
      ensureNodes(numNodes);
      forceComplete();
    }
  }

  /** Define the nodes in this storage. */
  protected void complete(IRNode[] nodeArray) {
    if (isComplete() || this.numNodes > 0) {
      throw new FluidRuntimeException("Incompatible completion");
    } else {
      define(numNodes);
      numNodes = nodeArray.length;
      for (int i=0; i < numNodes; ++i) {
	IRNode n = nodeArray[i];
	setOwner(n,this,i+1);
	nodes.add(n);
      }
      forceComplete();
    }
  }


  public static final int magic = 0x49525200; // "IRR\0"


  /* Constructors */

  /** Start defining a new region.  The state is defined, but incomplete,
   * and is being written.
   */
  public IRRegion() {
    this(magic);
  }

  /** Start defining a new region.  The state is defined, but incomplete,
   * and is being written.
   */
  protected IRRegion(int magic) {
    this(magic,true);
  }

  /** Start defining a region.
   * The state is defined but incomplete.
   * @param magic magic number associated with this region
   * (default: magic number of IRRegion)
   * @param hasID whether this region has identity
   * (default: true)
   */
  protected IRRegion(int magic, boolean hasID) {
    super(magic,hasID);
  }

  /** Set up a referred-to (undefined) region.
   */
  protected IRRegion(UniqueID id) {
    super(magic,id);
  }

  /** Set up a referred-to (undefined) region.
   */
  protected IRRegion(int magic, UniqueID id) {
    super(magic,id);
  }

  public static IRRegion getRegion(UniqueID id) {
    IRRegion reg = (IRRegion)find(id);
    if (reg == null) reg = new IRRegion(id);
    return reg;
  }


  /** Ensure this node is owned by this region unless
   * already owned.
   * @return true if node was added.
   */
  public synchronized boolean saveNode(IRNode node) {
    if (isComplete()) return false;
    if (node == null) return false;
    define();
    synchronized (node) {
      if (node.equals(IRNode.destroyedNode)) return false;
      if (!hasOwner(node)) {
        setOwner(node, this, ++numNodes);
        try {
          nodes.add(node);
        } catch (OutOfMemoryError e) {
          System.err.println("Memory failure in IRRegion.saveNode");
          throw e;
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Create a chunk (presumed not existing) for this region and a bundle.
   * @param b bundle of attributes
   * @return new chunk for this region and bundle
   */
  public IRChunk createChunk(Bundle b) {
    if (isNew()) return new IRChunk(this,b);
    else return new IRChunk(this,b,false);
  }
  
  /* output */
  @Override
  protected void write(IROutput out) throws IOException {
    int n = numNodes;
    if (!isComplete()) n = ~n;
    out.writeInt(n);
  }

  /* Input */
  @Override
  protected void read(IRInput in) throws IOException {
    if (in.getRevision() < 4) return;
    int n = in.readInt();
    if (n < 0) {
      n = ~n;
      define(n);
      ensureNodes(n);
    } else {
      complete(n);
    }
  }

  /* Destruction */

  /** Undefining a region destroys all of its nodes.
   */
  @Override
  public void undefine() {
    Iterator<IRNode> ni;
    synchronized (this) {
      super.undefine();
      ni = nodes.iterator();
      nodes = null;
      numNodes = 0;
    }
    while (ni.hasNext()) {
      IRNode n = ni.next();
      setOwnerInfo(n,null);
      n.destroy();
    }
  }

  /** Remove this region from memory.
   * Every node in the region is destroyed as well.
   */
  @Override
  public void destroy() {
    super.destroy();
  }

  /* persistent kind */

  private static final IRPersistentKind kind = new IRPersistentKind() {
    public void writePersistentReference(IRPersistent p, DataOutput out)
      throws IOException
    {
      //IRRegion r = (IRRegion)p;
      p.getID().write(out);
      // version 1.0:
      // out.writeInt(r.getNumNodes());
    }
    public IRPersistent readPersistentReference(DataInput in)
      throws IOException
    {
      UniqueID id = UniqueID.read(in);
      int numNodes = 0;
      if (in instanceof IRInput &&
	  ((IRInput)in).getRevision() < 1)  // backward compatability
	numNodes = in.readInt();
      IRRegion r = (IRRegion)find(id);
      if (r == null) {
	r = new IRRegion(id);
	r.define(numNodes);
	// version 1.0:
	// r.complete(numNodes);
      }
      return r;
    }
  };
  static {
    IRPersistent.registerPersistentKind(kind,0x52); // 'R'
  }

  @Override
  public IRPersistentKind getKind() {
    return kind;
  }

  @Override
  public void describe(PrintStream out) {
    super.describe(out);
    int numNodes = getNumNodes();
    if (numNodes > 0) {
      out.println("Region has " + numNodes +
		  ((numNodes > 1) ? " nodes" : " node"));
    }
  }
}
