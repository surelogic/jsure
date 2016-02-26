/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/WriteLockNode.java,v 1.6 2008/01/18 23:09:59 aarong Exp $*/
package com.surelogic.aast.promise;

import java.util.List;
import java.util.Map;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.AbstractAASTNodeFactory;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Represents an AAST node for the .readLock() annotation
 * 
 * @author edwin
 */
public final class WriteLockNode extends JUCLockNode {
	public static final AbstractAASTNodeFactory factory = 
		new AbstractAASTNodeFactory("WriteLock") {
		@Override
		public AASTNode create(String _token, int _start, int _stop, int _mods,
				String _id, int _dims, List<AASTNode> _kids) {
			LockNameNode lock =
				(LockNameNode) _kids.get(0);
			
			return new WriteLockNode(_start, lock);
		}
	};
	
	public WriteLockNode(int offset, LockNameNode lock){
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
    return LockType.WRITE_LOCK;
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
      sb.append("WriteLockNode\n");
      indent(sb, indent+2);
      sb.append(getLock().unparse(debug, indent+2));
    } else {
      sb.append(getLock().unparse(false));
      sb.append(".writeLock()");
    }
		return sb.toString();
    
	}
	
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new WriteLockNode(getOffset(), (LockNameNode)getLock().cloneOrModifyTree(mod));
  }
  
  
  
  @Override
  public boolean namesSameLockAs(
      final JUCLockNode ancestor, final Map<IRNode, Integer> positionMap,
      final How how) {
    return ancestor.namesSameLockAsWriteLock(this, positionMap, how);
  }
  
  @Override
  boolean namesSameLockAsReadLock(
      final ReadLockNode overriding, final Map<IRNode, Integer> positionMap,
      final How how) {
    if (how == How.CONTRAVARIANT) {
      // write lock requirement can be degraded to a read lock requirement
      return overriding.getLock().namesSameLockAs(getLock(), positionMap, how);
    }
    return false;
  }
  
  @Override
  boolean namesSameLockAsWriteLock(
      final WriteLockNode overriding, final Map<IRNode, Integer> positionMap,
      final How how) {
    return overriding.getLock().namesSameLockAs(getLock(), positionMap, how);
  }
}
