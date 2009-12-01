package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ProhibitsLockNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 * Promise drop for "ProhibitsLock" promises.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ProhibitsLockPromiseDrop extends PromiseDrop<ProhibitsLockNode> {
  public ProhibitsLockPromiseDrop(ProhibitsLockNode n) {
    super(n);
    setCategory(JavaGlobals.LOCK_REQUIRESLOCK_CAT);
  }
  
  private String lockName;

  private IRNode lockNode; // used for early lock declaration
  
  /**
   * @return Returns the lockName.
   */
  @Deprecated
  public final String getLockName() {
    return lockName;
  }

  /**
   * @param lockName The lockName to set.
   */
  @Deprecated
  public final void setLockName(String lockName) {
    this.lockName = lockName;
  }

//  /**
//   * Gets the associated lock model (a promise drop) for this drop.
//   * 
//   * @return the correct associated promise drop for this lock.
//   * 
//   * @see edu.cmu.cs.fluid.sea.drops.promises.LockModel
//   */
//  public LockModel getModel() {
//    return LockModel.getInstance(lockName);
//  }

  /**
   * @return Returns the lockNode.
   */
  public final IRNode getLockNode() {
    return lockNode;
  }

  /**
   * @param lockNode The lockNode to set.
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
  
  @Override
  protected void computeBasedOnAST() {
    String lock = getAST() != null ? getAST().toString() : DebugUnparser.toString(lockNode);
    setMessage(Messages.LockAnnotation_requiresLockDrop, 
               lock, JavaNames.genMethodConstructorName(getNode()));
  }
}