package com.surelogic.dropsea.ir.drops.promises;

import java.util.Collections;
import java.util.Map;

import com.surelogic.aast.promise.UniqueNode;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.dropsea.ir.UiShowAtTopLevel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
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
    setCategory(JavaGlobals.UNIQUENESS_CAT);
    isUniqueReturn = false;

    final IRNode node = getNode();
    if (VariableDeclarator.prototype.includes(node)) {
      setMessage(Messages.UniquenessAnnotation_uniqueDrop1, JavaNames.getFieldDecl(node)); //$NON-NLS-1$
    } else {
      IRNode method = VisitUtil.getEnclosingClassBodyDecl(node);
      if (method == null) {
        // Assume that it is a method
        method = node;
      }
      setMessage(Messages.UniquenessAnnotation_uniqueDrop2, JavaNames.getFieldDecl(node),
          JavaNames.genMethodConstructorName(method)); //$NON-NLS-1$
    }
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