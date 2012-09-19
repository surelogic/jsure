package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.ASSUMED;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CHECKED_BY_ANALYSIS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TO_BE_CHECKED_BY_ANALYSIS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.VIRTUAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.ir.Category;

public class IRFreePromiseDrop extends IRFreeProofDrop implements IPromiseDrop {

  final List<IRFreePromiseDrop> dependentPromises = new ArrayList<IRFreePromiseDrop>(0);
  final List<IRFreePromiseDrop> deponentPromises = new ArrayList<IRFreePromiseDrop>(0);
  final List<IRFreeAnalysisResultDrop> checkedByResults = new ArrayList<IRFreeAnalysisResultDrop>(0);

  public void addCheckedByResult(IRFreeAnalysisResultDrop info) {
    checkedByResults.add(info);
  }

  public void addDependentPromise(IRFreePromiseDrop p) {
    dependentPromises.add(p);
  }

  public void addDeponentPromise(IRFreePromiseDrop p) {
    deponentPromises.add(p);
  }

  public IRFreePromiseDrop(Entity e) {
    super(e);

    /*
     * The viewer expects promises to be in a category so we use an
     * "unparented drops" category for any promise that didn't load one from the
     * XML snapshot. This is updating a field in IRFreeDrop.
     */
    if (f_category == null)
      f_category = Category.getInstance(149);
  }

  public Collection<? extends IAnalysisResultDrop> getCheckedBy() {
    return checkedByResults;
  }

  @NonNull
  public Collection<? extends IPromiseDrop> getDependentPromises() {
    return dependentPromises;
  }

  @NonNull
  public Collection<? extends IPromiseDrop> getDeponentPromises() {
    return deponentPromises;
  }

  public boolean isAssumed() {
    return "true".equals(getEntity().getAttribute(ASSUMED));
  }

  public boolean isCheckedByAnalysis() {
    return "true".equals(getEntity().getAttribute(CHECKED_BY_ANALYSIS));
  }

  public boolean isIntendedToBeCheckedByAnalysis() {
    return "true".equals(getEntity().getAttribute(TO_BE_CHECKED_BY_ANALYSIS));
  }

  public boolean isVirtual() {
    return "true".equals(getEntity().getAttribute(VIRTUAL));
  }
}
