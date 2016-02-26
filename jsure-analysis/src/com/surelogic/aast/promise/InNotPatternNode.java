/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/InNotPatternNode.java,v 1.1 2007/09/17 17:28:58 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Represents a negation of the contained InTypePatternNode
 * @author ethan
 */
public class InNotPatternNode extends InTypePatternNode {
	
	private final InTypePatternNode target;
	
	public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
			"InNotPattern") {

		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			InTypePatternNode target = (InTypePatternNode)_kids.get(0);
			return new InNotPatternNode(_start, target);
		}
	};
	
	public InNotPatternNode(int offset, InTypePatternNode target){
		super(offset);
		this.target = target;
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
			sb.append("InNotPattern\n");
			sb.append(target.unparse(debug, indent + 2));
		} else {
    		sb.append("!(");
    		sb.append(getTarget().unparse(debug));
    		sb.append(")");
		}
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.promise.InTypePatternNode#matches(edu.cmu.cs.fluid.ir.IRNode)
	 */
	@Override
	public boolean matches(IRNode irNode) {
		return !target.matches(irNode);
	}
	
	public InTypePatternNode getTarget(){
		return target;
	}

	/* (non-Javadoc)
	 * @see com.surelogic.aast.IAASTNode#cloneTree()
	 */
	@Override
	protected IAASTNode internalClone(final INodeModifier mod) {
		InTypePatternNode t = (InTypePatternNode) getTarget().cloneOrModifyTree(mod);
		
		return new InNotPatternNode(offset, t);
	}
	
	@Override
	public boolean isFullWildcard() {
		return false;
	}
}
