/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/PlainIRNode.java,v 1.35 2008/09/09 13:56:02 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.surelogic.*;

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
 */
@ThreadSafe
@Region("private State")
@RegionLock("StateLock is this protects State")
public class PlainIRNode extends AbstractIRNode implements Serializable {
  private static final ThreadGlobal<IRRegion> regionVar = new ThreadGlobal<IRRegion>(null);
  
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


  /** Create a new IRNode.  Add it to current region, if any.
   */
  @Unique("return")
  public PlainIRNode() {
    this(getCurrentRegion());
  }

  /** Create a new IRNode.
   * @param region region to add node to.
   */
  @Unique("return")
  public PlainIRNode(IRRegion region) {
    if (region != null) {
      region.saveNode(this);
    } else {
      // (new Throwable()).printStackTrace();
    }
  }

  @InRegion("State")
  private Object ownerInfo = null;

  private volatile int index = 0;

  // used only by IRRegion:
  synchronized final Object getOwnerInfo() { return ownerInfo; }
  synchronized final void setOwnerInfo(Object o) { ownerInfo = o; }
  final int getIndexInRegion() { return index; }
  final void setIndexInRegion(int i) { index = i; }

  /** If the node is not in a region, mark it as destroyed.
   * Otherwise, the whole region needs to be destroyed.
   */
  @Override
  public void destroy() {
    synchronized (this) {
      if (ownerInfo instanceof IRRegion) return;
      index = -1;
      ownerInfo = null;
      super.destroy();
    }
  }

  /** If in a region, then self-identify */
  @Override
  protected String toString_internal() {
    IRRegion reg = IRRegion.getOwnerOrNull(this);
    if (reg != null) {
      return reg + " #" + IRRegion.getOwnerIndex(this);
    } else {
      return super.toString_internal();
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
