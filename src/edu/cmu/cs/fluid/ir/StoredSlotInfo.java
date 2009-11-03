/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/ir/StoredSlotInfo.java,v 1.42 2008/10/27 15:26:44 chance Exp $ */
package edu.cmu.cs.fluid.ir;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.util.ThreadLocalStack;

/** This abstract class partially specifies the implementation of
 * slots which are stored somewhere and for which there is a default
 * slot.  Subclasses determine where the slots are stored.
 * @typeparam Value
 * @see InfoStoredSlotInfo
 */
public abstract class StoredSlotInfo<S,T>
  extends SlotInfo<T>
  implements PersistentSlotInfo<T> {
  /**
	 * Logger for this class
	 */
  private static final Logger LOG = SLLogger.getLogger("IR");
  
  //public static final Object noSlotState = Slots.noSlotState;
  
  protected final SlotStorage<S,T> storage;
  protected final boolean predefined;
  protected final T defaultValue;
  protected final S defaultSlotState;

  public SlotFactory getSlotFactory() {
    return storage.getSlotFactory();
  }
  public boolean isPredefined() {
    return predefined;
  }
  public T getDefaultValue() {
    return defaultValue;
  }

  /** Register a new stored slot description (default undefined)
   * @param name Name under which to register this description
   * @param sf slot factory used to create slots.
   * @precondition nonNull(name) && nonNull(defaultSlot)
   */
  public StoredSlotInfo(String name, IRType<T> type, SlotStorage<S,T> st)
    throws SlotAlreadyRegisteredException {
    super(name, type);
    storage = st;
    predefined = false;
    defaultValue = null;
    defaultSlotState = st.newSlot();
  }

  /** Register a new stored slot description
   * @param name Name under which to register this description
   * @param sf slot factory used to create slots.
   * @param val default value
   * @precondition nonNull(name) && nonNull(defaultSlot)
   */
  public StoredSlotInfo(String name, IRType<T> type, SlotStorage<S,T> st, T val)
    throws SlotAlreadyRegisteredException {
    super(name, type);
    storage = st;
    predefined = true;
    defaultValue = val;
    defaultSlotState = st.newSlot(val);
  }

  /** Create a new anonymous slot description (default undefined)
   * @precondition nonNull(defaultSlot)
   * @param sf slot factory used to create slots.
   */
  public StoredSlotInfo(SlotStorage<S,T> st, String label) {
    super(label);
    storage = st;
    predefined = false;
    defaultValue = null;
    defaultSlotState = st.newSlot();
  }

  /** Create a new anonymous slot description
   * @precondition nonNull(defaultSlot)
   * @param sf slot factory used to create slots.
   * @param val default value
   */
  public StoredSlotInfo(SlotStorage<S,T> st, String label, T val) {
    super(label);
    storage = st;
    predefined = true;
    defaultValue = val;
    defaultSlotState = st.newSlot(val);
  }

  /** Get a stored slot state (implicit or explicit).
   * @param node the node for which to fetch the slot
   * @return the slotstate for the node, or defaultSlotState if none exists.
   */
  protected abstract S getSlot(IRNode node);

  /** Get a stored slot state (implicit or explicit).
   * If it is not valid, it tries to recover using an IUndefinedSlotHandler
   * 
   * TODO who should use this besides getSlotValue() and valueExists()
   * and valueChanged() and setSlotValue()?
   * 
   * @param node the node for which to fetch the slot
   * @return the slotstate for the node, or defaultSlotState if none exists.
   */
  private S getSlot_safe(IRNode node) {
    S slotState = getSlot(node);
    if (!storage.isValid(slotState)) {
      // Try again -- but only once
      if (IR.handleSlotUndefinedException(this, node)) {
        slotState = getSlot(node);
      }
    }
    return slotState;
  }
  
  /** Store a slot.
   * @param node the node for which to store the slot
   * @param slotState the slot-state for the node
   */
  protected abstract void setSlot(IRNode node, S slotState);

  /* Not correct (for MT)
  private IRNode lastNode = null;
  private S lastSlot = null;
  */
  
  /** Get the value of the stored slot (or of the default
   * slot if none is explicitly stored.)
   * @exception SlotUndefinedException
   * If the slot is not initialized with a value.
   */
  @Override
  public synchronized T getSlotValue(IRNode node) throws SlotUndefinedException {
	  S slotState = getSlot_safe(node);
	  return storage.getSlotValue(slotState);
	  //return getSlotValue_unsync(node);
  }
  
  protected final T getSlotValue_unsync(IRNode node) {	  
    S slotState = getSlot_safe(node);
    /*
    if (lastNode == node) {
      slotState = lastSlot;
    } else {
      slotState = getSlot_safe(node);
      lastSlot = slotState;
      lastNode = node;
    } 
    */   
    return storage.getSlotValue(slotState);
  }

  @Override
  public synchronized boolean valueExists(IRNode node) {
	  return valueExists_unsync(node);
  }
  
  protected boolean valueExists_unsync(IRNode node) {
    S slotState = getSlot_safe(node);
    /*
    if (lastNode == node) {
      slotState = lastSlot;
    } else {
      slotState = getSlot_safe(node);
      lastSlot = slotState;
      lastNode = node;
    }    
    */
    return storage.isValid(slotState);
  }

  public synchronized boolean valueChanged(IRNode node) {
    S slotState = getSlot_safe(node);
    if (slotState == defaultSlotState)
      return false;
    return storage.isChanged(slotState);
  }

  /** Set the value in the stored slot (possible storing a slot
   * in the process).
   * @exception SlotImmutableException
   * If the slot is not mutable.  The slot may be constant.
   * Or its value may be derived from other slots.
   */
  @Override
  public synchronized void setSlotValue(IRNode node, T newValue)
    throws SlotImmutableException {
	  setSlotValue_unsync(node, newValue);
  }
  
  protected void setSlotValue_unsync(IRNode node, T newValue) {
    if (type() != null && !type().isValid(newValue)) {
      throw new FluidRuntimeException(
        "setSlotValue: " + newValue + " not of type " + type());
    }
    //lastNode = null;
    S slotState = getSlot_safe(node);
    T oldValue = null;
    boolean defined = false;
    if (hasListeners() && storage.isValid(slotState)) {
      try {
        oldValue = storage.getSlotValue(slotState);
        defined = true;
      } catch (SlotUndefinedException e) {
    	// Nothing to do
      }
    }
    S newSlotState = storage.setSlotValue(slotState,newValue);
    if (slotState != newSlotState)
      setSlot(node, newSlotState);
    if (hasListeners()) {
      SlotInfoEvent<T> e =
        defined
          ? (SlotInfoEvent<T>) new SlotInfoChangedEvent<T>(this,
            node,
            oldValue,
            newValue)
          : (SlotInfoEvent<T>) new SlotInfoDefinedEvent<T>(this, node, newValue);
      informListeners(e);
    }
    if (defined)
      notifyIRObservers(node);
    else
      notifyDefineObservers(node);
    setStateParent(node,newValue);
  }

  /* persistence methods */

  public void writeSlot(IRNode node, IROutput out) throws IOException {
    S slotState = getSlot(node);
    if (slotState != defaultSlotState && storage.isValid(slotState)) {
      if (out.debug()) {
        IRRegion r = IRRegion.getOwner(node);
        System.out.println(
          "Writing " + name() + " for " + r + " #" + r.getIndex(node));
      }
      out.debugBegin("pair");
      out.writeNode(node);
      storage.writeSlotValue(slotState,type(), out);
      out.debugEnd("pair");
    }
  }

  @SuppressWarnings("unchecked")
  public void writeChangedSlot(IRNode node, IROutput out) throws IOException {
    S slotState = getSlot(node);
    if (slotState == defaultSlotState)
      return;
    boolean changed = storage.isChanged(slotState);
    boolean isCompound = type() instanceof IRCompoundType;
    IRCompound comp =
      (isCompound && storage.isValid(slotState)) ? (IRCompound) storage.getSlotValue(slotState) : null;
    boolean changedContents = comp != null && comp.isChanged();
    if (!changed && !changedContents)
      return; // nothing to do
    out.debugBegin("change");
    out.writeNode(node);
    byte b = '0';
    if (changed)
      b += 1;
    if (changedContents)
      b += 2;
    if (isCompound) {
      out.debugBegin("changeKind");
      out.writeByte(b); // otherwise, assume just changed
      out.debugEnd("changeKind");
    }
    if (changedContents) {
      if (out.debug()) {
        IRRegion r = IRRegion.getOwner(node);
        System.out.println(
          "Writing changed contents of "
            + name()
            + " for "
            + r
            + " #"
            + r.getIndex(node));
      }
      out.debugBegin("changedContents");
      comp.writeChangedContents((IRCompoundType<T>) type(), out);
      out.debugEnd("changedContents");
    }
    if (changed) {
      if (out.debug()) {
        IRRegion r = IRRegion.getOwner(node);
        System.out.println(
          "Writing changed " + name() + " for " + r + " #" + r.getIndex(node));
      }
      storage.writeSlotValue(slotState,type(), out);
    }
    out.debugEnd("change");
  }

  public void readSlotValue(IRNode node, IRInput in) throws IOException {
    if (in.debug()) {
      IRRegion r = IRRegion.getOwner(node);
      System.out.println(
        "Reading " + name() + " for " + r + " #" + r.getIndex(node));
    }
    S slotState = getSlot(node);
    
    S newSlotState;
    // ensure that if the slot's value is an abstract state, its parent is set.
    //IRAbstractState.pushDefaultStateParent(new SlotState<T>(this,node));
    @SuppressWarnings({ "unchecked", "unused" })
    Object stack = slotParentInfo.pushPair(this, node);
    try {
      newSlotState = storage.readSlotValue(slotState,type(), in);
    } finally {
      //IRAbstractState.popDefaultStateParent();
      slotParentInfo.popPair();
    }
    if (slotState != newSlotState)
      setSlot(node, newSlotState);
    // maybe: notifyDefineObservers(node);
    /*?? No event? */
  }

  @SuppressWarnings("unchecked")
  private static final ThreadLocalStack slotParentInfo = new ThreadLocalStack(null);
  
  public static final IRStateFactory defaultStateFactory = new IRStateFactory() {
    @SuppressWarnings("unchecked")
    public SlotState create() {
      IRNode n          = (IRNode) slotParentInfo.peek();
      StoredSlotInfo si = (StoredSlotInfo) slotParentInfo.peek(1);
      if (n == null) {
        return null;
      }
      return new SlotState(si, n);
    }    
  };
  
  @SuppressWarnings("unchecked")
  public void readChangedSlotValue(IRNode node, IRInput in)
    throws IOException {
    if (in.getRevision() < 4) {
      readSlotValue(node, in);
      return;
    }
    boolean isCompound = type() instanceof IRCompoundType;
    boolean changed = false;
    boolean changedContents = false;
    if (isCompound) {
      byte b;
      in.debugBegin("changeKind");
      b = in.readByte();
      if ((b & 01) != 0)
        changed = true;
      if ((b & 02) != 0)
        changedContents = true;
      in.debugEnd("changeKind");
    } else {
      changed = true;
    }
    notifyIRObservers(null); // HACK needed for versioned change info
    if (changedContents) {
      S slotState = getSlot(node);
      T obj;
      if (slotState == defaultSlotState
        || !storage.isValid(slotState)
        || !((obj = storage.getSlotValue(slotState)) instanceof IRCompound)) {
        throw new SlotUndefinedException("Cannot read changed contents");
      }
      if (in.debug()) {
        IRRegion r = IRRegion.getOwner(node);
        System.out.println(
          "Reading changed contents of "
            + name()
            + " for "
            + r
            + " #"
            + r.getIndex(node));
      }
      in.debugBegin("changedContents");
      ((IRCompound) obj).readChangedContents((IRCompoundType) type(), in);
      in.debugEnd("changedContents");
    }
    if (changed) {
      readSlotValue(node, in);
    } 
    if (changed || changedContents) {
      notifyIRObservers(node);
    }
  }

  
  /** Print debugging information for this node. */
  @Override
  public synchronized void describeSlot(IRNode n, PrintStream out) {
    S s = getSlot(n);
    storage.describe(s,out);
  }
  
  /**
   * Set the parent state of a value which is its own state.
   * @param node node for which the slot is bound
   * @param value possible stateful value
   */
  @SuppressWarnings("unchecked")
  protected void setStateParent(IRNode node, Object value) {
    if (value instanceof IRStoredState) {
      ((IRStoredState) value).setParent(this, node);
    } else {
      if (value instanceof IRCompound) {
        LOG.warning("The compound state has no way to be observed");
      }
    }
  }
  
  protected static class Adapter<U,V> {
	  final U defaultValue;
	  
	  Adapter(U val) {
		  defaultValue = val;
	  }
	  
	  public U getSlot(StoredSlotInfo<U,V> si, IRNode node) {
		  U result = si.getSlot(node);
		  if (result == si.defaultSlotState) {
			  return defaultValue;
		  }
		  return result;
	  }
	  public void setSlot(StoredSlotInfo<U,V> si, IRNode node, U slotState) {
		  si.setSlot(node, slotState);
	  }
	  public void undefineSlot(StoredSlotInfo<U,V> si, IRNode node) {
		  // What to do here?
	  }
  }
  
  protected static <U,V> Adapter<U,V> getStorageAdapter(U val) {
	  return new Adapter<U,V>(val);
  }
}
