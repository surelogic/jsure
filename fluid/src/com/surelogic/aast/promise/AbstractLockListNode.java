/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.promise;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.aast.*;

public abstract class AbstractLockListNode extends AASTRootNode {	
	protected final List<LockSpecificationNode> locks;
	
	protected AbstractLockListNode(int offset, List<LockSpecificationNode> locks) {
		super(offset);
		
		if(locks == null){
			throw new IllegalArgumentException("locks is null");
		}
		for(LockSpecificationNode lock : locks) {
			lock.setParent(this);
		}
		this.locks = locks;
	}

	protected static List<LockSpecificationNode> makeLockList(List<AASTNode> _kids) {
		List<LockSpecificationNode> locks = new ArrayList<LockSpecificationNode>(_kids.size());
		for (AASTNode lockNameNode : _kids) {
			locks.add((LockSpecificationNode)lockNameNode);
		}
		return locks;
	}
	
	/**
	 * Returns the non-null LockNameNode associated with this node
	 * @return LockNameNode
	 */
	public final List<LockSpecificationNode> getLockList() {
		return locks;
	}
	
	protected final List<LockSpecificationNode> cloneLockList() {
		List<LockSpecificationNode> locksCopy = new ArrayList<LockSpecificationNode>(locks.size());
	  	for (LockSpecificationNode lockSpecificationNode : locks) {
				locksCopy.add((LockSpecificationNode)lockSpecificationNode.cloneTree());
			}
		return locksCopy;
	}
	
	protected final String unparse(boolean debug, int indent, String name) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append(name).append("Node\n");
			for(LockSpecificationNode lock : locks) {
				sb.append(lock.unparse(debug, indent+2));
				sb.append('\n');
			}
		} else {
			sb.append(name).append("(\"");
			boolean first = true;
			for(LockSpecificationNode lock : locks) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(lock.unparse(false));
			}
			sb.append("\")");
		}
		return sb.toString();
	}
	
	@Override
  public String unparseForPromise() {
		return unparse(false);
	}
}
