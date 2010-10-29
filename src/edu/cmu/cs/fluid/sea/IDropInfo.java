package edu.cmu.cs.fluid.sea;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.Category;

public interface IDropInfo {
	<T> T getAdapter(Class<T> type);
	String getType();
	boolean isInstance(Class<?> type);
	String getMessage();
	
	int count();
	Category getCategory();
	IRNode getNode();
	ISrcRef getSrcRef();
	boolean provedConsistent();
	boolean proofUsesRedDot();
	boolean isConsistent();
}
