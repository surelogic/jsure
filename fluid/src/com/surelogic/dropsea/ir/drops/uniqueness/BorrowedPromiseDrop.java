package com.surelogic.dropsea.ir.drops.uniqueness;

import java.util.Collections;
import java.util.Map;

import com.surelogic.aast.promise.BorrowedNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Promise drop for "borrowed" promises established by the uniqueness analysis.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.UniqueAnalysis
 */
public final class BorrowedPromiseDrop extends BooleanPromiseDrop<BorrowedNode> implements RegionAggregationDrop {

  public BorrowedPromiseDrop(BorrowedNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.UNIQUENESS_CAT);
  }

  @Override
  protected IRNode useAlternateDeclForUnparse() {
	  return VisitUtil.getEnclosingClassBodyDecl(getNode());
  }
  
  public final boolean allowReturn() {
    return getAAST().allowReturn();
  }

  @Override
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