/*
 * Created on Jun 24, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.IRLocation;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.InsertionPoint;

/**
 * @author chance
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class AbstractImmutableTree {
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#clearParent(fluid.ir.IRNode)
	 */
	public final void clearParent(IRNode node) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#setSubtree(fluid.ir.IRNode, int, fluid.ir.IRNode)
	 */
	public final void setSubtree(IRNode parent, int i, IRNode newChild) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#setSubtree(fluid.ir.IRNode, fluid.ir.IRLocation, fluid.ir.IRNode)
	 */
	public final void setSubtree(IRNode parent, IRLocation loc, IRNode newChild) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#replaceSubtree(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void replaceSubtree(IRNode oldChild, IRNode newChild)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#exchangeSubtree(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void exchangeSubtree(IRNode node1, IRNode node2)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#insertSubtree(fluid.ir.IRNode, fluid.ir.IRNode, fluid.ir.InsertionPoint)
	 */
	public final IRLocation insertSubtree(
		IRNode node,
		IRNode newChild,
		InsertionPoint ip) {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#insertSubtree(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void insertSubtree(IRNode parent, IRNode newChild) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#appendSubtree(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void appendSubtree(IRNode parent, IRNode newChild) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#removeSubtree(fluid.ir.IRNode)
	 */
	public final void removeSubtree(IRNode node) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#insertSubtreeAfter(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void insertSubtreeAfter(IRNode newChild, IRNode oldChild) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableTreeInterface#insertSubtreeBefore(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void insertSubtreeBefore(IRNode newChild, IRNode oldChild) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface#initNode(fluid.ir.IRNode, int, int)
	 */
	public final void initNode(IRNode n, int numParents, int numChildren) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface#setParent(fluid.ir.IRNode, int, fluid.ir.IRNode)
	 */
	public final void setParent(IRNode node, int i, IRNode newParent) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface#setParent(fluid.ir.IRNode, fluid.ir.IRLocation, fluid.ir.IRNode)
	 */
	public final void setParent(IRNode node, IRLocation loc, IRNode newParent) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface#addParent(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void addParent(IRNode node, IRNode newParent)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface#removeParent(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	 
	public final void removeParent(IRNode node, IRNode parent)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface#replaceParent(fluid.ir.IRNode, fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void replaceParent(IRNode node, IRNode oldParent, IRNode newParent)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface#removeParents(fluid.ir.IRNode)
	 */
	public final void removeParents(IRNode node) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableSymmetricDigraphInterface#removeNode(fluid.ir.IRNode)
	 */
	public final void removeNode(IRNode node) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#initNode(fluid.ir.IRNode)
	 */
	public final void initNode(IRNode n) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#initNode(fluid.ir.IRNode, int)
	 */
	public final void initNode(IRNode n, int numChildren) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#setChild(fluid.ir.IRNode, int, fluid.ir.IRNode)
	 */
	public final void setChild(IRNode node, int i, IRNode newChild)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#setChild(fluid.ir.IRNode, fluid.ir.IRLocation, fluid.ir.IRNode)
	 */
	public final void setChild(IRNode node, IRLocation loc, IRNode newChild)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#addChild(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void addChild(IRNode node, IRNode newChild)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#replaceChild(fluid.ir.IRNode, fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void replaceChild(IRNode node, IRNode oldChild, IRNode newChild)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#insertChild(fluid.ir.IRNode, fluid.ir.IRNode, fluid.ir.InsertionPoint)
	 */
	public final IRLocation insertChild(
		IRNode node,
		IRNode newChild,
		InsertionPoint ip)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#insertChild(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void insertChild(IRNode node, IRNode newChild)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#appendChild(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void appendChild(IRNode node, IRNode newChild)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#insertChildAfter(fluid.ir.IRNode, fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void insertChildAfter(IRNode node, IRNode newChild, IRNode oldChild)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#insertChildBefore(fluid.ir.IRNode, fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void insertChildBefore(IRNode node, IRNode newChild, IRNode oldChild)
		throws IllegalChildException {
			throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#removeChild(fluid.ir.IRNode, fluid.ir.IRNode)
	 */
	public final void removeChild(IRNode node, IRNode oldChild)
		throws IllegalChildException {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#removeChild(fluid.ir.IRNode, fluid.ir.IRLocation)
	 */
	public final void removeChild(IRNode node, IRLocation loc) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.fluid.tree.MutableDigraphInterface#removeChildren(fluid.ir.IRNode)
	 */
	public final void removeChildren(IRNode node) {
		throw new UnsupportedOperationException( "Cannot modify a projection." );
	}
}
