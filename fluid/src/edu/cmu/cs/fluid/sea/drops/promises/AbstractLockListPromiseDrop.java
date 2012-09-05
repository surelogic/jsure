/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.*;

public abstract class AbstractLockListPromiseDrop<N extends AbstractLockListNode> 
extends PromiseDrop<N> {
	AbstractLockListPromiseDrop(N node) {
		super(node);
	}
	
	private IRNode lockNode; // used for early lock declaration

	/**
	 * @return Returns the lockNode.
	 */
	@Deprecated
	public final IRNode getLockNode() {
		return lockNode;
	}

	/**
	 * @param lockNode The lockNode to set.
	 */
	@Deprecated
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
	
	  
	@Override
	protected void computeBasedOnAST() {
		String lock = getAAST() != null ? getAAST().toString() : DebugUnparser.toString(lockNode);
		setResultMessage(Messages.LockAnnotation_requiresLockDrop, 
				lock, JavaNames.genMethodConstructorName(getNode()));
	}
}
