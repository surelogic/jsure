package com.surelogic.annotation.scrub;

import edu.cmu.cs.fluid.java.IHasPromisedFor;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Handle to pass information back to the scrubber traversal
 * 
 * @author Edwin
 */
public interface IAnnotationTraversalCallback<A extends IHasPromisedFor> {
	/**
	 * Handles a derived AAST that annotates a subclass of the class we are currently visiting
	 * 
	 * @param clone A derived version of the original AAST referred to by pd
	 * @param pd Assumed to implement ValidatedDropCallback<?>
	 */
	void addDerived(A clone, PromiseDrop<? extends A> pd);
}
