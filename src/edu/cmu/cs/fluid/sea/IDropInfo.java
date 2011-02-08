package edu.cmu.cs.fluid.sea;

import java.util.*;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.xml.*;

public interface IDropInfo {
	String getEntityName();
	<T> T getAdapter(Class<T> type);
	String getType();
	boolean isInstance(Class<?> type);
	String getMessage();
	boolean isValid();
	boolean hasMatchingDeponents(IDropPredicate p);
	void addMatchingDependentsTo(Set<IDropInfo> s, IDropPredicate p);
	
	boolean requestTopLevel();
	int count();
	
	ISrcRef getSrcRef();
	Category getCategory();	
	void setCategory(Category c);
	Collection<ISupportingInformation> getSupportingInformation();
	Collection<? extends IProposedPromiseDropInfo> getProposals();
	void snapshotAttrs(AbstractSeaXmlCreator s);
	Long getHash();
	Long getContextHash();
}
