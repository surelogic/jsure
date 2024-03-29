package com.surelogic.dropsea.ir.drops.uniqueness;

import java.util.Collections;
import java.util.Map;

import com.surelogic.aast.promise.UniqueNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.UiShowAtTopLevel;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
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
	  return computeAlternateDeclForUnparse(getNode());
  }

  /**
   * @return Returns the isUniqueReturn.
   */
  public boolean isUniqueReturn() {
    return isUniqueReturn;
  }

  @Override
  public UniquePromiseDrop getDrop() {
    return this;
  }

  @Override
  public Map<IRegion, IRegion> getAggregationMap(final IRNode fieldDecl) {
    /*
     * Aggregates Instance into the field if the field is non-final. Aggregates
     * Instance into Instance if the field is final and non-static.  Aggregates
     * Instance into Static if the field is final and static.
     */
    final RegionModel instanceRegion = RegionModel.getInstanceRegion(fieldDecl);
    if (TypeUtil.isJSureFinal(fieldDecl)) {
      if (TypeUtil.isStatic(fieldDecl)) {
        return Collections.<IRegion, IRegion> singletonMap(
            instanceRegion,
            RegionModel.getStaticRegionForClass(
                VisitUtil.getEnclosingType(fieldDecl)));
      } else {
        return Collections.<IRegion, IRegion> singletonMap(
            instanceRegion, instanceRegion);
      }
    } else {
      return Collections.<IRegion, IRegion> singletonMap(
          instanceRegion, RegionModel.getInstance(fieldDecl));
    }
  }
}