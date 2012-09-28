/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.promise;

import com.surelogic.aast.*;

public abstract class AbstractSingleLockNode extends AASTRootNode {
	protected final LockNameNode lock;
	
	protected AbstractSingleLockNode(int offset, LockNameNode lock) {
		super(offset);
		
		if(lock == null){
			throw new IllegalArgumentException("lock is null");
		}
		lock.setParent(this);
		this.lock = lock;
	}
	
	/**
	 * Returns a non-null LockNameNode
	 * @return
	 */
	public final LockNameNode getLock(){
		return lock;
	}
	
	public final String unparseForPromise() {
		return unparse(false);
	}
	
	protected final String unparse(boolean debug, int indent, String name) {
		StringBuilder sb = new StringBuilder();
		if (debug) {
			indent(sb, indent);
			sb.append(name).append("Node\n");
			indent(sb, indent+2);
			sb.append(getLock().unparse(debug, indent+2));
		} else {
			sb.append(name).append("(\"");
			sb.append(getLock().unparse(debug, indent));
			sb.append("\")");
		}
		return sb.toString();
	}
}
