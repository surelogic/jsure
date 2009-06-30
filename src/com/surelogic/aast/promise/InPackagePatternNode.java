/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/InPackagePatternNode.java,v 1.3 2007/09/25 15:59:10 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.List;
import edu.cmu.cs.fluid.ir.IRNode;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

/**
 * TODO Fill in purpose.
 * 
 * @author ethan
 */
public class InPackagePatternNode extends AASTNode {
	private final InTypePatternNode type;

	public static final AbstractSingleNodeFactory factory = new com.surelogic.parse.AbstractSingleNodeFactory(
			"InPackagePattern") {

		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			InTypePatternNode type = null;
			if (_kids.size() > 0) {
				type = (InTypePatternNode) _kids.get(0);
			}
			return new InPackagePatternNode(_start, type);
		}
	};

	public InPackagePatternNode(int offset, InTypePatternNode type) {
		super(offset);
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.AASTNode#accept(com.surelogic.aast.INodeVisitor)
	 */
	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	/**
	 * 
	 * @return A possibly-null InTypePatternNode
	 */
	public InTypePatternNode getInTypePattern() {
		return type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.AASTNode#unparse(boolean, int)
	 */
	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			if (type != null) {
				indent(sb, indent);
				sb.append("InPackagePattern\n");
				indent(sb, indent + 2);
				sb.append(type.unparse(debug, indent + 2));
			}
		} else {
			if (getInTypePattern() != null) {
				sb.append(" in ");
				sb.append(getInTypePattern().unparse(debug));
			}
		}
		return sb.toString();
	}

	/**
	 * USER SHOULD CALL {@link InPackagePatternNode#combineAASTs(InTypePatternNode)} before calling this
	 * @param irNode
	 * @return True if this tree matches the IRNode-based AST
	 */
	boolean matches(IRNode irNode) {
		if (type != null) {
			return type.matches(irNode);
		}
		// match all
		return true;
	}

	/**
	 * Takes a InTypePatternNode-based AAST that represents the first 'in' statement in a scoped promise (the type), and combines it with this tree (the package).
	 * In essence, it replaces each leaf in this package tree with a copy of the type tree whose leaves are modified to include the package information contained
	 * in this tree's leaves (that are replaced by the modified type tree).
	 * 
	 * An example:
	 * 
	 *                                                 type          package
	 *                                                ________    _______________
	 *                                                |      |    |             |
	 * @ ScopedPromise 'reads Instance' for new() in (a* & b*) in (j.l.* | j.u.*)
	 * 
	 * This forms 2 trees, one for the type, and one for the package:
	 * 
	 * Type                    Package
	 * 
	 *     &                      |
	 *    / \                    / \
	 *  a*   b*              j.l.*  j.u.*
	 *  
	 *  This method creates this tree:
	 *  
	 *                     |
	 *             ________|______   
	 *            /               \
	 *           &                 &
	 *     ______|____          ___|______
	 *    |           |        |          |
	 * j.l.*.a*   j.l.*.b*  j.u.*.a*   j.u.*.b*  
	 * 
	 * @param typeNode
	 * @return InPackagePatternNode with combined information
	 */
	InPackagePatternNode combineAASTs(InTypePatternNode typeNode) {
	  return new InPackagePatternNode(offset, getInTypePattern().combineAASTs(typeNode));	
	}
	
  @Override
  public IAASTNode cloneTree(){
  	if(getInTypePattern() != null){
    	return new InPackagePatternNode(getOffset(), (InTypePatternNode)getInTypePattern().cloneTree());
  	}else{
    	return new InPackagePatternNode(getOffset(), null);
  	}
  }
}
