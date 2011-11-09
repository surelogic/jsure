/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/sea/drops/promises/IsLockPromiseDrop.java,v 1.3 2007/07/05 18:15:18 aarong Exp $*/
package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.IsLockNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * A promise drop for the IsLock promise
 * 
 * @author ethan
 */
public class IsLockPromiseDrop extends PromiseDrop<IsLockNode> {

	// The name of the lock that this IsLock promises on
	private String lockName;

	private IRNode lockNode; //

	public IsLockPromiseDrop(IsLockNode a){
		super(a);
	}
	/**
	 * @return Returns the lockName.
	 */
	public final String getLockName() {
		return lockName;
	}

	/**
	 * @param lockName
	 *            The lockName to set.
	 */
	public final void setLockName(String lockName) {
		this.lockName = lockName;
	}

	/**
	 * @return Returns the lockNode.
	 */
	public final IRNode getLockNode() {
		return lockNode;
	}

	/**
	 * @param lockNode
	 *            The lockNode to set.
	 */
	public final void setLockNode(IRNode lockNode) {
		this.lockNode = lockNode;
	}

	/**
	 * Need to clean up lock models.
	 * 
	 * @see edu.cmu.cs.fluid.sea.Drop#deponentInvalidAction(Drop)
	 */
	@Override
  protected void deponentInvalidAction(Drop invalidDeponent) {
		super.deponentInvalidAction(invalidDeponent);
		LockModel.purgeUnusedLocks();
	}

	/**
	 * @see edu.cmu.cs.fluid.sea.PromiseDrop#isCheckedByAnalysis()
	 */
	@Override
  public boolean isCheckedByAnalysis() {
		return true;
	}

}
