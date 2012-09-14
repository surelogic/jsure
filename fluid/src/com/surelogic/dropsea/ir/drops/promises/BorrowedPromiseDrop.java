package com.surelogic.dropsea.ir.drops.promises;

import java.util.Collections;
import java.util.Map;

import com.surelogic.aast.promise.BorrowedNode;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Promise drop for "borrowed" promises established by the uniqueness analysis.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.UniqueAnalysis
 */
public final class BorrowedPromiseDrop extends BooleanPromiseDrop<BorrowedNode> implements RegionAggregationDrop {

  public BorrowedPromiseDrop(BorrowedNode a) {
    super(a);
    setCategory(JavaGlobals.UNIQUENESS_CAT);
    final IRNode node = getNode();
    setMessage(Messages.UniquenessAnnotation_borrowedDrop, getAAST().allowReturn() ? JavaNames.getFieldDecl(node)
        + ", allowReturn=true" : JavaNames.getFieldDecl(node), JavaNames.getFullName(VisitUtil.getEnclosingClassBodyDecl(node))); //$NON-NLS-1$
  }

  public final boolean allowReturn() {
    return getAAST().allowReturn();
  }

  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    /*
     * Borrowed fields must be final and non-static. Also applies to the
     * QualifiedReceiverDeclaration on types, which is implicitly final. There
     * is no point in testing for final here, like we do in UniquePromiseDrop.
     * Aggregates Instance into Instance.
     */
    final RegionModel instanceRegion = RegionModel.getInstanceRegion(fieldDecl);
    return Collections.<IRegion, IRegion> singletonMap(instanceRegion, instanceRegion);
  }
}