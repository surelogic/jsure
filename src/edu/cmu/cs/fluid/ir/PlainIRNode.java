/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/PlainIRNode.java,v 1.35 2008/09/09 13:56:02 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import edu.cmu.cs.fluid.*;
import edu.cmu.cs.fluid.util.CountInstances;
import edu.cmu.cs.fluid.util.ThreadGlobal;

/** A default implementation of the intermediate representation node 
 * interface.  There is a notion of a current region;
 * if it is non-null, the node is added to that region.
 * <p>
 * This class optimizes space for the case that the node
 * will probably have an owning region.
 * @see SlotInfo
 * @see IRRegion
 * 
 * @region private State
 * @lock StateLock is this protects State
 */
public class PlainIRNode implements IRNode, Serializable {
  private static final ThreadGlobal<IRRegion> regionVar = new ThreadGlobal<IRRegion>(null);
  private static final AtomicInteger destroyedNodes = new AtomicInteger();
  
  public static boolean checkIfNumDestroyed(int num) {
	  int current = destroyedNodes.get();
	  if (current > num) {
		  return destroyedNodes.compareAndSet(current, 0);
	  }
	  return false;
  }
  
  /** Set the current region.
   * By default, newly created nodes are in this region.
   */
  public static void setCurrentRegion(IRRegion r) {
    regionVar.setValue(r);
  }
  public static IRRegion getCurrentRegion() {
    return regionVar.getValue();
  }
  public static void pushCurrentRegion(IRRegion r) {
    regionVar.pushValue(r);
  }
  public static void popCurrentRegion() {
    regionVar.popValue();
  }

  public Object identity() {
    if (destroyed()) return destroyedNode;
    return this;
  }

  /**
   * @mapInto State
   */
  private int hash = IRNodeUtils.hash(super.hashCode());
  
  /**
   * Modified to allow this hash to be directly used
   * by a hashtable like JDK 1.4's HashMap
   */
  @Override
  public synchronized int hashCode() {
    /*
    if (destroyed()) return DESTROYED_HASH;
    return super.hashCode();
    */
    return hash;
  }

  @Override
  public boolean equals(Object other) {
    if (destroyed()) {
      return other == destroyedNode || (other != null && other.equals(destroyedNode));
    }
    // not destroyed
    if (other instanceof IRNode) {
      return this == ((IRNode) other).identity();
    } else {
      return false;
    }
  }

  private static int nodesCreated = 0;

  public static int getTotalNodesCreated() {
    return nodesCreated;
  }

  /** Create a new IRNode.  Add it to current region, if any.
   */
  public PlainIRNode() {
    this(getCurrentRegion());
  }

  /** Create a new IRNode.
   * @param region region to add node to.
   */
  public PlainIRNode(IRRegion region) {
    nodesCreated++;
    if (region != null) {
      region.saveNode(this);
    } else {
      // (new Throwable()).printStackTrace();
    }
  }

  /**
   * @mapInto State
   */
  private Object ownerInfo = null;
  /**
   * @mapInto State
   */
  private volatile int index = 0;

  // used only by IRRegion:
  synchronized final Object getOwnerInfo() { return ownerInfo; }
  synchronized final void setOwnerInfo(Object o) { ownerInfo = o; }
  final int getIndexInRegion() { return index; }
  final void setIndexInRegion(int i) { index = i; }

  /** Get the slot's value for a particular node.
   * @typeparam Value
   * @param si Description of slot to be accessed.
   * <dl purpose=fluid>
   *   <dt>type<dd> SlotInfo[Value]
   * </dl>
   * @precondition nonNull(si)
   * @return the value of the slot associated with this node.
   * <dl purpose=fluid>
   *   <dt>type<dd> Value
   * </dl>
   * @exception SlotUndefinedException
   * If the slot is not initialized with a value.
   */
  public <T> T getSlotValue(SlotInfo<T> si) throws SlotUndefinedException {
    if (si == null) {
      throw new NullPointerException();
    }
    try { 
      return si.getSlotValue(this);
    }
    catch (SlotUndefinedException e) {
      if (this.identity() == destroyedNode) {
        throw new FluidError("Trying to access a destroyed node");
      }
      throw e;
    }
  }

  /** Change the value stored in the slot.
   * @typeparam Value
   * @param si Description of slot to be accessed.
   * <dl purpose=fluid>
   *   <dt>type<dd> SlotInfo[Value]
   * </dl>
   * @param newValue value to store in the slot.
   * <dl purpose=fluid>
   *   <dt>type<dd> Value
   *   <dt>capabilities<dd> store
   * </dl>
   */
  public <T> void setSlotValue(SlotInfo<T> si, T newValue)
    throws SlotImmutableException {
    if (si == null) {
      throw new NullPointerException();
    }
    si.setSlotValue(this, newValue);
  }

  // for convenience:
  public int getIntSlotValue(SlotInfo<Integer> si) {
    return (this.<Integer>getSlotValue(si)).intValue();
  }
  // for convenience
  public void setSlotValue(SlotInfo<Integer> si, int newValue) {
    setSlotValue(si, (Integer)(newValue));
  }

  /** Check if a value is defined for a particular node.
   * @typeparam Value
   * @param si Description of slot to be accessed.
   * <dl purpose=fluid>
   *   <dt>type<dd> SlotInfo[Value]
   * </dl>
   * @precondition nonNull(si)
   * @return true if getSlotValue would return a value, false otherwise
   */
  public <T> boolean valueExists(SlotInfo<T> si) {
    return si.valueExists(this);
  }

  /** If the node is not in a region, mark it as destroyed.
   * Otherwise, the whole region needs to be destroyed.
   */
  public void destroy() {
    synchronized (this) {
      if (ownerInfo instanceof IRRegion) return;
      index = -1;
      ownerInfo = null;
      --nodesCreated; // I suppose
      
      hash = DESTROYED_HASH;
      destroyedNodes.incrementAndGet();
    }
  }
  
  /** True for a node that has been destroyed. */
  private final boolean destroyed() {
    return getIndexInRegion() == -1;
  }

  /**
   * True if the node has been destroyed, works for any {@link IRNode}.
   * @param n node to test
   * @return true if this node has been destoryed.
   */
  public static boolean isDestroyed(IRNode n) {
    return n.identity() == IRNode.destroyedNode;
  }


  /** If in a region, then self-identify */
  @Override
  public String toString() {
    if (destroyed()) {
      return "Destroyed" + super.toString();
    }
    IRRegion reg = IRRegion.getOwnerOrNull(this);
    if (reg != null) {
      return reg + " #" + IRRegion.getOwnerIndex(this);
    } else {
      return super.toString();
    }
  }

  /** Return a wrapped IRNode for serialization.
   * Requires JDK 1.2
   */
  public Object writeReplace() {
    if (destroyed()) return null;
    return new PlainIRNodeWrapper(this);
  }
  
  { CountInstances.add(this); }
}

class PlainIRNodeWrapper implements Serializable {
  private transient IRNode node;

  PlainIRNodeWrapper(IRNode n) {
    node = n;
  }
  private void writeObject(ObjectOutputStream out) throws IOException {
    final IRRegion reg = IRRegion.getOwner(node);
    final int index = IRRegion.getOwnerIndex(node);
    out.writeInt(index);
    reg.writeReference(out);
  }
  private void readObject(ObjectInputStream in)
    throws IOException, ClassNotFoundException {
    int i = in.readInt();
    IRRegion reg = (IRRegion) IRPersistent.readReference(in);
    node = reg.getNode(i);
  }
  public Object readResolve() {
    return node;
  }
}
