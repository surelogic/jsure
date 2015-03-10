package edu.cmu.cs.fluid.ir;

public class UnsyncdInfoStoredSlotInfo<S,T> extends InfoStoredSlotInfo<S, T> {
	/**
	 * These constructors are just here to check that the underlying
	 * implementations are threadsafe
	 * 
	 * See InfoStoredSlotInfo for more info
	 */
	public UnsyncdInfoStoredSlotInfo(
			String name,
			IRType<T> type,
			SlotStorage<S,T> st,
			Slots<S,T> slots)
	throws SlotAlreadyRegisteredException {
		super(name, type, st, slots);
		checkIfThreadSafe(st, slots);
	}

	public UnsyncdInfoStoredSlotInfo(
			String name,
			IRType<T> type,
			SlotStorage<S,T> st,
			T val,
			Slots<S,T> slots)
	throws SlotAlreadyRegisteredException {
		super(name, type, st, val, slots);
		checkIfThreadSafe(st, slots);
	}

	public UnsyncdInfoStoredSlotInfo(SlotStorage<S,T> st, String label, Slots<S,T> slots) {
		super(st, label, slots);
		checkIfThreadSafe(st, slots);
	}
	
	public UnsyncdInfoStoredSlotInfo(SlotStorage<S,T> st, String label, T val, Slots<S,T> slots) {
		super(st, label, val, slots);
		checkIfThreadSafe(st, slots);
	}
	
	private void checkIfThreadSafe(SlotStorage<S,T> st, Slots<S,T> slots) {
		if (!st.isThreadSafe()) {
			throw new IllegalStateException("Unsafe slot storage");
		}
		if (!slots.isThreadSafe()) {
			throw new IllegalStateException("Unsafe slots");
		}	
	}
	
	@Override
	public boolean valueExists(IRNode node) {
		return valueExists_unsync(node);
	}
	
	@Override
	public T getSlotValue(IRNode node) {
		return getSlotValue_unsync(node);
	}
	
	@Override
	public void setSlotValue(IRNode node, T newValue) {
		setSlotValue_unsync(node, newValue);
	}
}
