/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/RequiresLockNode.java,v 1.9 2007/09/24 21:09:55 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

/**
 * Represents an AAST node for the @RequiresLock annotation
 * @author ethan
 */
public class RequiresLockNode extends AbstractLockListNode {
	public static final AbstractSingleNodeFactory factory =
		new AbstractSingleNodeFactory("RequiresLock"){
		
		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop,
			int _mods, String _id, int _dims, List<AASTNode> _kids){
			List<LockSpecificationNode> locks = makeLockList(_kids);
			return new RequiresLockNode (_start, locks);
		}
		
	};
	
	public RequiresLockNode(int offset, List<LockSpecificationNode> locks){
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
		return unparse(debug, indent, "RequiresLock");
	}
	
  @Override
  public IAASTNode cloneTree(){
  	return new RequiresLockNode(getOffset(), cloneLockList());
  }
}
