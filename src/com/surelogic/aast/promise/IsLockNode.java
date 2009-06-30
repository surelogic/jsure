/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/IsLockNode.java,v 1.5 2007/09/24 21:09:55 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

/**
 * Represents an AAST node for the @IsLock annotation
 * @author ethan
 */
public class IsLockNode extends AASTRootNode {
	private LockNameNode lock;
	
	public static final AbstractSingleNodeFactory factory =
		new AbstractSingleNodeFactory("IsLock"){
		
		@Override
    @SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			return new IsLockNode(_start, (LockNameNode)_kids.get(0));
		}
	};
	
	public IsLockNode(int offset, LockNameNode lock){
		super(offset);
		if(lock == null){
			throw new IllegalArgumentException("lock is null");
		}
		lock.setParent(this);
		this.lock = lock;
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
		indent(sb, indent);
		sb.append("IsLockNode\n");
		indent(sb, indent+2);
		sb.append(getLock().unparse(debug, indent+2));
		return sb.toString();
	}
	
	/**
	 * Returns a non-null LockNameNode
	 * @return
	 */
	public LockNameNode getLock(){
		return lock;
	}
	
  @Override
  public IAASTNode cloneTree(){
  	return new IsLockNode(getOffset(), (LockNameNode)getLock().cloneTree());
  }

}
