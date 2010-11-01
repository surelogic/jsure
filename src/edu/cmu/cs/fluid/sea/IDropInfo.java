package edu.cmu.cs.fluid.sea;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.Category;

public interface IDropInfo {
	<T> T getAdapter(Class<T> type);
	String getType();
	boolean isInstance(Class<?> type);
	String getMessage();
	boolean isValid();
	
	boolean requestTopLevel();
	int count();
	Category getCategory();	
	void setCategory(Category c);
	ISrcRef getSrcRef();
	
	boolean hasMatchingDeponents(IDropPredicate p);
}
