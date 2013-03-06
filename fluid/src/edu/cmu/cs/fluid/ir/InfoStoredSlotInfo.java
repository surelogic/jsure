/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/InfoStoredSlotInfo.java,v 1.23 2008/10/27 15:26:44 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.ImmutableSet;

/** Slots described by instances of this class are stored outside of the node.
 * @typeparam S type of slot storage
 * @typeparam T typeof values
 */
public class InfoStoredSlotInfo<S,T> extends StoredSlotInfo<S,T> {
  /** The storage for the slots.
   */
  private Slots<S,T> slots;

  /** Register a new stored slot description
   * @param name Name under which to register this description
   * @param sf slot factory used to create slots.
   * @param slots storage for created slots.
   * @precondition nonNull(name) && nonNull(st) && 
   *     nonNull(slots) && unique(slots)
   */
  public InfoStoredSlotInfo(
    String name,
    IRType<T> type,
    SlotStorage<S,T> st,
    Slots<S,T> slots)
    throws SlotAlreadyRegisteredException {
    super(name, type, st);
    this.slots = slots;
  }

  /** Register a new stored slot description
   * @param name Name under which to register this description
   * @param sf slot factory used to create slots.
   * @param val default value for slots.
   * @param slots storage for created slots.
   * @precondition nonNull(name) && nonNull(st) && 
   *     nonNull(slots) && unique(slots)
   */
  public InfoStoredSlotInfo(
    String name,
    IRType<T> type,
    SlotStorage<S,T> st,
    T val,
    Slots<S,T> slots)
    throws SlotAlreadyRegisteredException {
    super(name, type, st, val);
    this.slots = slots;
  }

  /** Create a new anonymous slot description.
   * @param sf slot factory used to create slots.
   * @param slots storage for created slots.
   * @precondition nonNull(st) && nonNull(slots) && unique(slots)
   */
  public InfoStoredSlotInfo(SlotStorage<S,T> st, String label, Slots<S,T> slots) {
    super(st, label);
    this.slots = slots;
  }

  /** Create a new anonymous slot description.
   * @param sf slot factory used to create slots.
   * @param val default value for slots.
   * @param slots storage for created slots.
   * <dl purpose=fluid>
   *   <dt>type<dd> Slots[IRnode,Slot[Value]]
   *   <dt>capabilities<dd> store, read, write
   * </dl>
   * @precondition nonNull(defaultSlot) && nonNull(slots) && unique(slots)
   */
  public InfoStoredSlotInfo(SlotStorage<S,T> st, String label, T val, Slots<S,T> slots) {
    super(st, label, val);
    this.slots = slots;
  }

  /** Get state of a stored slot.
   * @param node the node for which to fetch the slot
   * @return the slot state for the node, or defaultSlotState if none exists.
   * @capabilities store
   */
  @Override
  protected final S getSlot(IRNode node) {
    return slots.getSlot(node,defaultSlotState);
  }

  /** Store a slot.
   * @param node the node for which to store the slot
   * @param slotState the state of the slot for the node
   */
  @Override
  protected void setSlot(IRNode node, S slotState) {
    slots.setSlot(node, slotState, defaultSlotState);
  }

  @Override
  public synchronized ImmutableSet<IRNode> index(T value) {
    if (slots instanceof Map) {
      Map<IRNode, S> table = (Map<IRNode, S>) slots;
      List<IRNode> nodes = new ArrayList<IRNode>();
      Iterator<Map.Entry<IRNode, S>> contents = table.entrySet().iterator();
      boolean normal = true;
      if (predefined
          && (defaultValue == value || (defaultValue != null && defaultValue
              .equals(value)))) {
        normal = false;
      }
      while (contents.hasNext()) {
        Map.Entry<IRNode, S> e = contents.next();
        if (storage.getSlotValue(e.getValue()).equals(value) == normal) {
          nodes.add(e.getKey());
        }
      }
      return new ImmutableHashOrderSet<IRNode>(nodes.toArray(JavaGlobals.noNodes),!normal);
    } else return null;
  }

    /** Destroy all the slots */
  @Override
  public void destroy() {
    slots = null;
    super.destroy();
  }
  
  @Override
  public int size() {
    if (slots == null) {
      return 0;
    }
    return slots.size();
  }
  
  @Override
  public int cleanup() {
    if (slots == null) {
      return 0;
    }
    return slots.cleanup();
  }
  
  @Override
  public void compact() {
    if (slots == null) {
      return;
    }
    slots.compact();
  }
  
  @Override
  public void undefineSlot(IRNode node) {
    slots.undefineSlot(node);
  }
  
  /** 
   * Test code
  
  public void dumpState() {   
    if (slots instanceof Map) {
      Map<IRNode, S> table = (Map<IRNode, S>) slots;
      for(IRNode n : table.keySet()) {
        if (JJNode.tree.isNode(n)) {
          System.out.println(DebugUnparser.toString(n));
        }
      }
    }
  }
  */
}
