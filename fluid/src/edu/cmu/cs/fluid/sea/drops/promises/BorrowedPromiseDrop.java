package edu.cmu.cs.fluid.sea.drops.promises;

import java.util.Collections;
import java.util.Map;

import com.surelogic.aast.promise.BorrowedNode;
import com.surelogic.analysis.regions.FieldRegion;
import com.surelogic.analysis.regions.IRegion;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

/**
 * Promise drop for "borrowed" promises established by the
 * uniqueness analysis.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.UniqueAnalysis
 * @see edu.cmu.cs.fluid.java.bind.UniquenessAnnotation
 */
public final class BorrowedPromiseDrop extends BooleanPromiseDrop<BorrowedNode>
implements RegionAggregationDrop {
  public BorrowedPromiseDrop(BorrowedNode a) {
    super(a);
    setCategory(JavaGlobals.UNIQUENESS_CAT);
  }
  @Override
  protected void computeBasedOnAST() {
    final IRNode node = getNode();
    setResultMessage(Messages.UniquenessAnnotation_borrowedDrop, 
               getAST().allowReturn() ? JavaNames.getFieldDecl(node)+", allowReturn=true": JavaNames.getFieldDecl(node), 
               JavaNames.getFullName(VisitUtil.getEnclosingClassBodyDecl(node))); //$NON-NLS-1$
  }
  
  public final boolean allowReturn() {
      return getAST().allowReturn();
  }
  
  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    /* Aggregates Instance into the field if the field is non-final.
     * Aggregates Instance into Instance if the field is final and non-static.
     */
    final RegionModel instanceRegion = RegionModel.getInstanceRegion(fieldDecl);
    if (TypeUtil.isFinal(fieldDecl)) {
      return Collections.<IRegion, IRegion>singletonMap(
          instanceRegion, instanceRegion);
    } else {
      return Collections.<IRegion, IRegion>singletonMap(
          instanceRegion, new FieldRegion(fieldDecl));
    }
  }
}