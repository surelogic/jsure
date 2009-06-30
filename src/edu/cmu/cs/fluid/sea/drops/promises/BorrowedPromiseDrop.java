package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.BorrowedNode;
import com.surelogic.sea.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Promise drop for "borrowed" promises established by the
 * uniqueness analysis.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.UniqueAnalysis
 * @see edu.cmu.cs.fluid.java.bind.UniquenessAnnotation
 */
public final class BorrowedPromiseDrop extends BooleanPromiseDrop<BorrowedNode> {
  public BorrowedPromiseDrop(BorrowedNode a) {
    super(a);
    setCategory(JavaGlobals.UNIQUENESS_CAT);
  }
  @Override
  protected void computeBasedOnAST() {
    final IRNode node = getNode();
    setMessage(Messages.UniquenessAnnotation_borrowedDrop, 
               JavaNames.getFieldDecl(node), 
               JavaNames.genMethodConstructorName(VisitUtil.getEnclosingClassBodyDecl(node))); //$NON-NLS-1$
  }
}