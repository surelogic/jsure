package edu.cmu.cs.fluid.dcf.views.coe;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.Category;

public interface IDropInfo {
	boolean isInstance(Class<?> type);
	Category getCategory();
	ISrcRef getSrcRef();
}
