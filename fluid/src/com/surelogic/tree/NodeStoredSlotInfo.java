/*$Header$*/
package com.surelogic.tree;

import edu.cmu.cs.fluid.ir.*;

/**
 * Info is stored in SyntaxTreeNode
 * 
 * @author Edwin
 */
abstract class NodeStoredSlotInfo<T> extends StoredSlotInfo<T,T> {  
  protected final Adapter<T,T> adapter;
	
  protected final T undefinedValue;
  private final StoredSlotInfo<T,T> backupSI;
	
  NodeStoredSlotInfo(String label, String name, IRType<T> type, 
		             SyntaxTreeSlotFactory.Storage<T> st, T defaultVal,
		             StoredSlotInfo<T,T> backup) 
  throws SlotAlreadyRegisteredException 
  {
    super(name, type, st, defaultVal);
    undefinedValue = st.getUndefinedValue();
    adapter  = getStorageAdapter(undefinedValue);
    backupSI = backup;
  }
  
  @Override
  public T getSlotValue(IRNode node) throws SlotUndefinedException {
	  synchronized (node) {
		  return getSlotValue_unsync(node);
	  }
  }
  
  @Override
  public boolean valueExists(IRNode node) {
	  synchronized (node) {
		  return valueExists_unsync(node);
	  }
  }
  
  @Override
  public void setSlotValue(IRNode node, T newValue) {
	  synchronized (node) {
		  setSlotValue_unsync(node, newValue);
	  }
  }
  
  @Override
  protected final void setSlot(IRNode node, T slotState) {
	if (node instanceof SyntaxTreeNode) {
		SyntaxTreeNode n = (SyntaxTreeNode) node;
		setSlot(n, slotState);
	} else {		
		adapter.setSlot(backupSI, node, slotState);
	}
  }

  @Override
  public final void undefineSlot(IRNode node) {
	if (node instanceof SyntaxTreeNode) {
		SyntaxTreeNode n = (SyntaxTreeNode) node;
		setSlot(n, predefined ? defaultValue : undefinedValue);		
	} else {
		// What to do?
	}
  }
  
  protected abstract void setSlot(SyntaxTreeNode n, T value);
  
  @Override
  protected T getSlot(IRNode node) {
	T slot;
	try {
		SyntaxTreeNode n = (SyntaxTreeNode) node;
		slot = getSlot(n);
	} catch (ClassCastException e) {	
		slot = adapter.getSlot(backupSI, node);
	}
	if (slot == undefinedValue) {
		return storage.newSlot();
	}
	return slot;
  }
  
  protected abstract T getSlot(SyntaxTreeNode n);
}
