package com.surelogic.dropsea.ir.drops.uniqueness;

import java.util.Collections;
import java.util.Map;

import com.surelogic.aast.promise.UniqueNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.common.Pair;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.UiShowAtTopLevel;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;

/**
 * Promise drop for "unique" promises established by the uniqueness analysis.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.UniqueAnalysis
 */
public final class UniquePromiseDrop extends BooleanPromiseDrop<UniqueNode> implements UiShowAtTopLevel, RegionAggregationDrop,
    IUniquePromise {

  private final boolean isUniqueReturn;

  public UniquePromiseDrop(UniqueNode n) {
    super(n);
    setCategorizingMessage(JavaGlobals.UNIQUENESS_CAT);
    isUniqueReturn = false;
  }

  @Override
  protected IRNode useAlternateDeclForUnparse() {
	  final IRNode node = getNode();
	  if (VariableDeclarator.prototype.includes(node)) {
		  return null;
	  }
	  IRNode rv = VisitUtil.getEnclosingClassBodyDecl(getNode());
	  if (rv == null) {
		  return node;
	  }
	  return rv;
  }
  
  @Override
  public boolean isCheckedByAnalysis() {
    if (isUniqueReturn) {
      return super.isCheckedByAnalysis();
    } else {
      return true;
    }
  }

  /**
   * @return Returns the isUniqueReturn.
   */
  public boolean isUniqueReturn() {
    return isUniqueReturn;
  }

  public final boolean allowRead() {
    return getAAST().allowRead();
  }

  public UniquePromiseDrop getDrop() {
    return this;
  }

  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    /*
     * Aggregates Instance into the field if the field is non-final. Aggregates
     * Instance into Instance if the field is final and non-static.
     */
    final RegionModel instanceRegion = RegionModel.getInstanceRegion(fieldDecl);
    if (TypeUtil.isFinal(fieldDecl)) {
      return Collections.<IRegion, IRegion> singletonMap(instanceRegion, instanceRegion);
    } else {
      return Collections.<IRegion, IRegion> singletonMap(instanceRegion, RegionModel.getInstance(fieldDecl));
    }
  }
}