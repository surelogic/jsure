/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/IRNode.java,v 1.13 2006/03/27 21:35:50 boyland Exp $ */
package edu.cmu.cs.fluid.ir;

import com.surelogic.ThreadSafe;

/** The intermediate representation node for the FLUID project
 * prototype software development environment.  Each node
 * has an arbitrary and dynamic number of named slots. 
 * Some of these may be stored in the node, others may be
 * stored in external tables, or computed on demand. 
 * </p>
 *
 * @see SlotInfo
 * @see PlainIRNode
 */
@ThreadSafe
public interface IRNode {
  public static final int DESTROYED_HASH = 0;
	
  @ThreadSafe
  public static class DestroyedNode {
	  DestroyedNode() {
		// To keep it from being created
	  } 
	  @Override
	  public boolean equals(Object other) {
		  return other == this || (other != null && other.equals(this));
	  }
	  @Override
	  public int hashCode() {
		  return DESTROYED_HASH;
	  }
  }
  
  public static final Object destroyedNode = new DestroyedNode();

  /** The "identity" of the node.  For plain IRNode's,
   * the identity is itself.  Used for equality testing.
   */
  public Object identity();

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
  public <T> T getSlotValue(SlotInfo<T> si) throws SlotUndefinedException;

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
    throws SlotImmutableException;

  // shortcuts:
  public int getIntSlotValue(SlotInfo<Integer> si) throws SlotUndefinedException;
  public void setSlotValue(SlotInfo<Integer> si, int newValue)
    throws SlotImmutableException;

  /** Check if a value is defined for a particular node.
   * @typeparam Value
   * @param si Description of slot to be accessed.
   * <dl purpose=fluid>
   *   <dt>type<dd> SlotInfo[Value]
   * </dl>
   * @precondition nonNull(si)
   * @return true if getSlotValue would return a value, false otherwise
   */
  public <T> boolean valueExists(SlotInfo<T> si);

  /** Remove this IR node.
   * This reference should have identity == destroyedNode
   * and should equal it.
   */
  public void destroy();
}
