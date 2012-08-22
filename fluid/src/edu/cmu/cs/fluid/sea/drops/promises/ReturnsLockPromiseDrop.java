package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ReturnsLockNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Promise drop for "returnsLock" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ReturnsLockPromiseDrop extends PromiseDrop<ReturnsLockNode> {

	/**
	 * Constructor to create a drop with an associated ReturnsLockNode
	 * @param node
	 */
	public ReturnsLockPromiseDrop(ReturnsLockNode node) {
		super(node);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
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
  
  @Override
  protected void computeBasedOnAST() {
    if (getAST() != null) {
      IRNode mdecl = VisitUtil.getEnclosingClassBodyDecl(getAST().getPromisedFor());
      setResultMessage(Messages.LockAnnotation_returnsLockDrop,
          getAST().getLock(),
          JavaNames.genMethodConstructorName(mdecl));
    }
  }
}