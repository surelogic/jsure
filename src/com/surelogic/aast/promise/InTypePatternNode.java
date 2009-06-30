/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/InTypePatternNode.java,v 1.1 2007/09/17 17:28:58 ethan Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.aast.AASTNode;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Represents a scoped promise's 'in' pattern for the type that a given element
 * is in
 * 
 * @author ethan
 */
public abstract class InTypePatternNode extends AASTNode {
	
	
	/**
	 * Constructor
	 * 
	 * @param offset
	 */
	public InTypePatternNode(int offset){
		super(offset);
	}
	
	/**
	 * Returns <code>true</code> if the target represented by the given IRNode
	 * matches this AAST
	 * 
	 * @param irNode
	 *          The IRNode to match against
	 * @return True if this AAST matches the given IRNode
	 */
	public abstract boolean matches(IRNode irNode);

	/**
	 * Takes a InTypePatternNode-based AAST and combines it with this tree. This
	 * is used to facilitate matching Should only be called to combine a
	 * InTypePatternNode-based tree with a InPackagePatterNode-based tree
	 * therefore it should only be called via
	 * {@link InPackagePatternNode#combineAASTs(InTypePatternNode)}
	 * 
	 * @param typeNode
	 * @return An InTypePatternNode that incorporates both the current tree and the {@link typeNode} tree.
	 */
	abstract InTypePatternNode combineAASTs(InTypePatternNode typeNode);

	/**
	 * Clones this InTypePatternNode-based tree and makes the leaves add the given package.
	 * Used only for modifying the tree for matching. Should only be initiated from a
	 * {@link WildcardTypeQualifierPatternNode#combineAASTs(InTypePatternNode)}
	 * @param packagePattern
	 */
	abstract InTypePatternNode cloneAndModifyTree(String packagePattern);
}
