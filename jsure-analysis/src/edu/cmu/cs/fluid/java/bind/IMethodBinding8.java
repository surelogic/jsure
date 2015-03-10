package edu.cmu.cs.fluid.java.bind;

import edu.cmu.cs.fluid.java.bind.IMethodBinder.InvocationKind;
import edu.cmu.cs.fluid.java.bind.TypeInference8.BoundSet;

public interface IMethodBinding8 extends IBinding {
	InvocationKind getInvocationKind();
	
	/**
	 * Get the bound set used to resolve this binding
	 * 
	 * @return B_2, if computed
	 */
	BoundSet getInitialBoundSet();
}
