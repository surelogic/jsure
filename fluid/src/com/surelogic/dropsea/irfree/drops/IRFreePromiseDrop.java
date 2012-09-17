package com.surelogic.dropsea.irfree.drops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.Attributes;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.ir.Category;
import com.surelogic.dropsea.ir.PromiseDrop;

public class IRFreePromiseDrop extends IRFreeProofDrop implements IPromiseDrop {
  /**
   * Only for PromiseDrops
   */
  final List<IAnalysisResultDrop> checkedByResults;

  public void addCheckedByResult(IAnalysisResultDrop info) {
    if (PromiseDrop.useCheckedByResults) {
      checkedByResults.add(info);
    }
  }

  public IRFreePromiseDrop(String name, Attributes a) {
    super(name, a);
    checkedByResults = new ArrayList<IAnalysisResultDrop>(0);

    /*
     * The viewer expects promises to be in a category so we use an
     * "unparented drops" category for any promise that didn't load one from the
     * XML snapshot. This is updating a field in IRFreeDrop.
     */
    if (category == null)
      category = Category.getInstance(149);
  }

  public Collection<? extends IAnalysisResultDrop> getCheckedBy() {
    return checkedByResults;
  }

  @NonNull
  public Set<IPromiseDrop> getDependentPromises() {
    final Set<IPromiseDrop> result = new HashSet<IPromiseDrop>();
    for (IRFreeDrop d : dependents) {
      if (d instanceof IPromiseDrop)
        result.add((IPromiseDrop) d);
    }
    return result;
  }

  @NonNull
  public Set<IPromiseDrop> getDeponentPromises() {
    final Set<IPromiseDrop> result = new HashSet<IPromiseDrop>();
    for (IRFreeDrop d : deponents) {
      if (d instanceof IPromiseDrop)
        result.add((IPromiseDrop) d);
    }
    return result;
  }

  public boolean isAssumed() {
    return "true".equals(getAttribute(PromiseDrop.ASSUMED));
  }

  public boolean isCheckedByAnalysis() {
    return "true".equals(getAttribute(PromiseDrop.CHECKED_BY_ANALYSIS));
  }

  public boolean isIntendedToBeCheckedByAnalysis() {
    return "true".equals(getAttribute(PromiseDrop.TO_BE_CHECKED_BY_ANALYSIS));
  }

  public boolean isVirtual() {
    return "true".equals(getAttribute(PromiseDrop.VIRTUAL));
  }
}
