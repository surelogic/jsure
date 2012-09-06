package edu.cmu.cs.fluid.sea;

/**
 * Defines a predicate used to filter a set of drops.
 */
public interface DropPredicate {

	/**
	 * Decides if a specific drop should be an element of the resulting set.
	 * 
	 * @param d
	 *            the drop to evaluate.
	 * @return <code>true</code> if the drop should be included,
	 *         <code>false</code> if the drop should be omitted.
	 */
	boolean match(IDropInfo d);
}
