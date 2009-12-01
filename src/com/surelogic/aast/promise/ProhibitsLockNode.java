/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/RequiresLockNode.java,v 1.9 2007/09/24 21:09:55 ethan Exp $*/
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

/**
 * Represents an AAST node for the @ProhibitsLock annotation
 * @author edwin
 */
public class ProhibitsLockNode extends AASTRootNode {
	public static final AbstractSingleNodeFactory factory =
		new AbstractSingleNodeFactory("ProhibitsLock"){
		
		@Override
		@SuppressWarnings("unchecked")
		public AASTNode create(String _token, int _start, int _stop,
			int _mods, String _id, int _dims, List<AASTNode> _kids){
			List<LockSpecificationNode> locks = new ArrayList<LockSpecificationNode>(_kids.size());
			for (AASTNode lockNameNode : _kids) {
				locks.add((LockSpecificationNode)lockNameNode);
			}
			
			return new ProhibitsLockNode (_start, locks);
		}
		
	};
	
	private List<LockSpecificationNode> locks;
	
	public ProhibitsLockNode(int offset, List<LockSpecificationNode> locks){
		super(offset);
		if(locks == null){
			throw new IllegalArgumentException("lock is null");
		}
    for(LockSpecificationNode lock : locks) {
		  lock.setParent(this);
    }
		this.locks = locks;
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
		if (debug) {
		  indent(sb, indent);
		  sb.append("ProhibitsLockNode\n");
		  for(LockSpecificationNode lock : locks) {
		    sb.append(lock.unparse(debug, indent+2));
		    sb.append('\n');
		  }
		} else {
      sb.append("ProhibitsLock ");
      boolean first = true;
      for(LockSpecificationNode lock : locks) {
        if (first) {
          first = false;
        } else {
          sb.append(", ");
        }
        sb.append(lock.unparse(false));
      }
		}
		return sb.toString();
	}

	/**
	 * Returns the non-null LockNameNode associated with this node
	 * @return LockNameNode
	 */
	public List<LockSpecificationNode> getLockList() {
		return locks;
	}
	
  @Override
  public IAASTNode cloneTree(){
  	List<LockSpecificationNode> locksCopy = new ArrayList<LockSpecificationNode>(locks.size());
  	for (LockSpecificationNode lockSpecificationNode : locks) {
			locksCopy.add((LockSpecificationNode)lockSpecificationNode.cloneTree());
		}
  	return new ProhibitsLockNode(getOffset(), locksCopy);
  }
}
