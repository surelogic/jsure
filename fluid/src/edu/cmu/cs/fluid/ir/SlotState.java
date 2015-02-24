/*
 * $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/SlotState.java,v 1.4 2007/11/05 15:03:26 chance Exp $
 */
package edu.cmu.cs.fluid.ir;


/**
 * A change in a slot: attribute of a node.
 * @author boyland
 */
public class SlotState<T> implements IRState {

  public final SlotInfo<T> attribute;
  public final IRNode node;
  private IRState parent = null; // protected by always being null or the same thing
  
  /**
   * Create a slot delta for given attribute and node.
   */
  public SlotState(SlotInfo<T> si, IRNode n) {
    if (si == null || n == null) {
      throw new IllegalArgumentException();
    }
    attribute = si;
    node = n;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.ir.IRState#getParent()
   */
  public IRState getParent() {
    if (parent != null) return parent;
    IRRegion reg=IRRegion.getOwnerOrNull(node);
    Bundle b = attribute.getBundle();
    if (reg == null || b == null) return null;
    return parent = IRChunk.get(reg,b);
  }

  /**
   * Return if the other is a slot delta for same slot.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SlotState) {
      SlotState sd = (SlotState)obj;
      return sd.attribute.equals(attribute) && sd.node.equals(node);
    }
    return false;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return attribute.hashCode() + node.hashCode();
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "<" + attribute.toString() + "," + node.toString() + ">";
  }
}
