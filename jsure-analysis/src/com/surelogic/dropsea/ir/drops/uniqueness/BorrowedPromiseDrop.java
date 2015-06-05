package com.surelogic.dropsea.ir.drops.uniqueness;

import com.surelogic.aast.promise.BorrowedNode;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Promise drop for "borrowed" promises established by the uniqueness analysis.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.UniqueAnalysis
 */
public final class BorrowedPromiseDrop extends BooleanPromiseDrop<BorrowedNode> {

  public BorrowedPromiseDrop(BorrowedNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.UNIQUENESS_CAT);
  }

  @Override
  protected IRNode useAlternateDeclForUnparse() {
	  return VisitUtil.getEnclosingClassBodyDecl(getNode());
  }
}