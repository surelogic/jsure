/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/RequiresLockNode.java,v 1.9 2007/09/24 21:09:55 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;

/**
 * Represents an AAST node for the @ProhibitsLock annotation
 * @author edwin
 */
public class ProhibitsLockNode extends AbstractLockListNode {
	public static final AbstractAASTNodeFactory factory =
		new AbstractAASTNodeFactory("ProhibitsLock"){
		
		@Override
		public AASTNode create(String _token, int _start, int _stop,
			int _mods, String _id, int _dims, List<AASTNode> _kids){
			List<LockSpecificationNode> locks = makeLockList(_kids);
			return new ProhibitsLockNode (_start, locks);
		}
	};
	
	public ProhibitsLockNode(int offset, List<LockSpecificationNode> locks){
		super(offset, locks);
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
		return unparse(debug, indent, "ProhibitsLock");
	}
	
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new ProhibitsLockNode(getOffset(), cloneLockList(mod));
  }
}
