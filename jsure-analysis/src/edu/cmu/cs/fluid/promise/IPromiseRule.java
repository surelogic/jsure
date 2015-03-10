/*
 * Created on Oct 24, 2003
 *
 */
package edu.cmu.cs.fluid.promise;

import edu.cmu.cs.fluid.tree.Operator;

/**
 * Represents the various kinds of parse / bind / check rules.
 * 
 * @author chance
 */
public interface IPromiseRule {
	/**
	 * Find out what operators this rule applies to.
	 * 
	 * @param type The aspect of this rule being queried (e.g. IPromiseParseRule, IPromiseBindRule)
	 * @return an array of the operators this rule applies to
	 */
	Operator[] getOps(Class type);
}
