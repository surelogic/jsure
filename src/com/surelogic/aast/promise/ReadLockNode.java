/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ReadLockNode.java,v 1.6 2008/01/18 23:09:59 aarong Exp $*/
package com.surelogic.aast.promise;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.AbstractAASTNodeFactory;

/**
 * Represents an AAST node for the .readLock() annotation
 * 
 * @author edwin
 */
public final class ReadLockNode extends JUCLockNode {
	public static final AbstractAASTNodeFactory factory = 
		new AbstractAASTNodeFactory("ReadLock") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			LockNameNode lock =
				(LockNameNode) _kids.get(0);
			
			return new ReadLockNode(_start, lock);
		}
	};
	
	public ReadLockNode(int offset, LockNameNode lock){
		super(offset, lock);
	}
	
	@Override
  public boolean bindingExists() {
	  // We bind the inner lock specification
	  return getLock().bindingExists();
  }

	@Override
  public ILockBinding resolveBinding() {
    // We bind the inner lock specification
	  return getLock().resolveBinding();
  }

  @Override
  public LockType getType() {
    return LockType.READ_LOCK;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.surelogic.aast.AASTNode#unparse(boolean, int)
	 */
	@Override
	public String unparse(boolean debug, int indent) {
		StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent);
      sb.append("ReadLockNode\n");
      indent(sb, indent+2);
      sb.append(getLock().unparse(debug, indent+2));
    } else {
      sb.append(getLock().unparse(false));
      sb.append(".readLock()");
    }
		return sb.toString();
    
	}
	
  @Override
  public IAASTNode cloneTree(){
  	return new ReadLockNode(getOffset(), (LockNameNode)getLock().cloneTree());
  }
	
}
