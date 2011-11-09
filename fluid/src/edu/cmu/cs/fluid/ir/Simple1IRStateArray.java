package edu.cmu.cs.fluid.ir;

import com.surelogic.*;

/**
 * Dynamically creates a instance of SlotState if needed
 * 
 * @author Edwin.Chan
 */
public class Simple1IRStateArray<T> extends Simple1IRArray<T> {
	private /*final*/ SlotInfo<T> attribute;
	private /*final*/ IRNode node;

	/**
	 * Called by the super-constructor
	 * FIX will this work?
	 */
	@Override
	@Borrowed("this")
	protected IRState createDefaultStateParent() {		
		IRState p =  super.createDefaultStateParent();
		if (p instanceof SlotState) {
			// Intercept and store contents here
			mergeSlotState(p);
			return null;
		}
		node = null;
		return p;
	}
	
	@Borrowed("this")
	private void mergeSlotState(IRState p) {
		@SuppressWarnings("unchecked") 
		SlotState<T> s = (SlotState<T>) p;
		this.attribute = s.attribute;
		this.node      = s.node;
	}
	
	@Override
    public void setParent(SlotInfo<T> si, IRNode n) {
		IRState current = super.getParent();
		if (current == null) {
			attribute = si;
			node      = n;
		} else {
			node = null;		
			super.setParent(si, n);
		}
	}
	
	@Override
    public void setParent(IRState p) {
		IRState current = super.getParent();
		if (current == null && p instanceof SlotState) {
			// Intercept and store contents here
			mergeSlotState(p);
			return;
		}
		node = null;
		super.setParent(p);
	}
	
	@Override
	public IRState getParent() {				
		if (node != null) {
			return new SlotState<T>(attribute, node);
		}
		return super.getParent();
	}
}
