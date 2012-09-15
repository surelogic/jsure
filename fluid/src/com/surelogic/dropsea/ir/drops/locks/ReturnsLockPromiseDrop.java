package com.surelogic.dropsea.ir.drops.locks;

import com.surelogic.aast.promise.ReturnsLockNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Promise drop for "returnsLock" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ReturnsLockPromiseDrop extends PromiseDrop<ReturnsLockNode> {

  /**
   * Constructor to create a drop with an associated ReturnsLockNode
   * 
   * @param node
   */
  public ReturnsLockPromiseDrop(ReturnsLockNode node) {
    super(node);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    IRNode mdecl = VisitUtil.getEnclosingClassBodyDecl(getAAST().getPromisedFor());
    setMessage(Messages.LockAnnotation_returnsLockDrop, getAAST().getLock(), JavaNames.genMethodConstructorName(mdecl));
  }
}