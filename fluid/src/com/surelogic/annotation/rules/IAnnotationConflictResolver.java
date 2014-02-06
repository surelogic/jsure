package com.surelogic.annotation.rules;

import java.util.Collection;

import com.surelogic.aast.IAASTRootNode;

import edu.cmu.cs.fluid.ir.IRNode;

public interface IAnnotationConflictResolver {
	void resolve(Context context);

	interface Context {
		Iterable<Class<? extends IAASTRootNode>> getAASTTypes();
		<T extends IAASTRootNode> Collection<T> getAASTs(Class<T> cls);
		void remove(IAASTRootNode aast);
		IRNode getNode();
	}	
}
