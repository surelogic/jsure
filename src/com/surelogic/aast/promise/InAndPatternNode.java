/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/InAndPatternNode.java,v 1.1 2007/09/17 17:28:58 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * TODO Fill in purpose.
 * @author ethan
 */
public class InAndPatternNode extends InTypePatternNode {
	
	private final InTypePatternNode target1;
	private final InTypePatternNode target2;

	public static final AbstractSingleNodeFactory factory = new com.surelogic.parse.AbstractSingleNodeFactory(
			"InAndPattern") {

		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
    	InTypePatternNode target1 = (InTypePatternNode)_kids.get(0);
    	InTypePatternNode target2 = (InTypePatternNode)_kids.get(1);
			return new InAndPatternNode(_start, target1, target2);
		}
	};
	
	public InAndPatternNode(int offset, InTypePatternNode target1, InTypePatternNode target2){
		super(offset);
		this.target1 = target1;
		this.target2 = target2;
	}
	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#accept(com.surelogic.aast.INodeVisitor)
	 */
	@Override
	public <T> T accept(INodeVisitor<T> visitor) {
		return visitor.visit(this);
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.AASTNode#unparse(boolean, int)
	 */
	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
		if(debug){
			indent(sb, indent);
			sb.append("InAndPattern\n");
			sb.append(target1.unparse(debug, indent + 2));
			sb.append(target2.unparse(debug, indent + 2));
		} else {
    		sb.append(getTarget1().unparse(debug));
    		sb.append(" & ");
    		sb.append(getTarget2().unparse(debug));
		}
		return sb.toString();
	}
	/* (non-Javadoc)
	 * @see com.surelogic.aast.promise.InTypePatternNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode irNode) {
		return target1.matches(irNode) && target2.matches(irNode);
	}
	/**
	 * @return
	 */
	public InTypePatternNode getTarget1() {
		return target1;
	}
	
	/**
	 * @return
	 */
	public InTypePatternNode getTarget2() {
		return target2;
	}
	/* (non-Javadoc)
	 * @see com.surelogic.aast.promise.InTypePatternNode#combineAASTs(com.surelogic.aast.promise.InTypePatternNode)
	 */
	@Override
	InTypePatternNode combineAASTs(InTypePatternNode typeNode) {
		InTypePatternNode t1 = getTarget1().combineAASTs(typeNode);
		InTypePatternNode t2 = getTarget2().combineAASTs(typeNode);
	  return new InAndPatternNode(offset, t1, t2);
	}
	
	/* (non-Javadoc)
	 * @see com.surelogic.aast.IAASTNode#cloneTree()
	 */
	@Override
	public IAASTNode cloneTree() {
		InTypePatternNode t1 = (InTypePatternNode) getTarget1().cloneTree();
		InTypePatternNode t2 = (InTypePatternNode) getTarget2().cloneTree();
		
		return new InAndPatternNode(offset, t1, t2);
	}
	
	/* (non-Javadoc)
	 * @see com.surelogic.aast.promise.InTypePatternNode#cloneTree(java.lang.String)
	 */
	@Override
	InTypePatternNode cloneAndModifyTree(String packagePattern) {
		InTypePatternNode t1 = getTarget1().cloneAndModifyTree(packagePattern);
		InTypePatternNode t2 = getTarget2().cloneAndModifyTree(packagePattern);
		
		return new InAndPatternNode(offset, t1, t2);
	}

}
