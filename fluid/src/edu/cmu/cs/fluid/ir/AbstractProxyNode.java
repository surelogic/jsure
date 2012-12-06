/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/AbstractProxyNode.java,v 1.10 2006/08/23 08:56:04 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

import com.surelogic.ThreadSafe;

/** Placeholders for other IRNodes.
 * These nodes do not have identity in themselves, but instead defer
 * to another node for slot storage.
 * @see ProxyNode
 */
@ThreadSafe
public abstract class AbstractProxyNode implements IRNode {
  /** return the IRNode that this node is the proxy for. */
  protected abstract IRNode getIRNode();
  
  public Object identity() {
    return getIRNode().identity();
  }

  @Override
  public boolean equals(Object other) {
    return getIRNode().equals(other);
  }

  @Override
  public final int hashCode() {
    return getIRNode().hashCode();
  }

  public <T> T getSlotValue(SlotInfo<T> si) throws SlotUndefinedException {
    return getIRNode().getSlotValue(si);
  }

  public <T> void setSlotValue(SlotInfo<T> si, T newValue) 
      throws SlotImmutableException
  {
    getIRNode().<T>setSlotValue(si,newValue);
  }

  public int getIntSlotValue(SlotInfo<Integer> si) throws SlotUndefinedException {
    return getIRNode().getIntSlotValue(si);
  }

  public void setSlotValue(SlotInfo<Integer> si, int newValue) 
       throws SlotImmutableException
  {
    getIRNode().<Integer>setSlotValue(si,(Integer)newValue);
  }
  
  public <T> boolean valueExists(SlotInfo<T> si) {
    return getIRNode().valueExists(si);
  }

  public void destroy() {
    getIRNode().destroy(); // or just remove this one ?
  }

  @Override
  public String toString() {
    return getClass() + "(" + getIRNode() + ")";
  }
}
