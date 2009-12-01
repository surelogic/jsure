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
}
