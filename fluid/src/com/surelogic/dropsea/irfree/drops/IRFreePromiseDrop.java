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

  private final List<IRFreePromiseDrop> f_dependentPromises = new ArrayList<IRFreePromiseDrop>(0);
  private final List<IRFreePromiseDrop> f_deponentPromises = new ArrayList<IRFreePromiseDrop>(0);
  private final List<IRFreeAnalysisResultDrop> f_checkedByResults = new ArrayList<IRFreeAnalysisResultDrop>(0);
  private final boolean f_isAssumed;
  private final boolean f_isCheckedByAnalysis;
  private final boolean f_isIntendedToBeCheckedByAnalysis;
  private final boolean f_isVirtual;

  void addCheckedByResult(IRFreeAnalysisResultDrop info) {
    f_checkedByResults.add(info);
  }

  void addDependentPromise(IRFreePromiseDrop p) {
    f_dependentPromises.add(p);
  }

  void addDeponentPromise(IRFreePromiseDrop p) {
    f_deponentPromises.add(p);
  }

  IRFreePromiseDrop(Entity e, Class<?> irClass) {
    super(e, irClass);

    /*
     * The viewer expects promises to be in a category so we use an
     * "unparented drops" category for any promise that didn't load one from the
     * XML snapshot. This is updating a field in IRFreeDrop.
     */
    if (f_category == null)
      f_category = Category.getInstance(149);

    f_isAssumed = "true".equals(e.getAttribute(ASSUMED));
    f_isCheckedByAnalysis = "true".equals(e.getAttribute(CHECKED_BY_ANALYSIS));
    f_isIntendedToBeCheckedByAnalysis = "true".equals(e.getAttribute(TO_BE_CHECKED_BY_ANALYSIS));
    f_isVirtual = "true".equals(e.getAttribute(VIRTUAL));
  }

  @NonNull
  public Collection<? extends IAnalysisResultDrop> getCheckedBy() {
    return f_checkedByResults;
  }

  @NonNull
  public Collection<? extends IPromiseDrop> getDependentPromises() {
    return f_dependentPromises;
  }

  @NonNull
  public Collection<? extends IPromiseDrop> getDeponentPromises() {
    return f_deponentPromises;
  }

  public boolean isAssumed() {
    return f_isAssumed;
  }

  public boolean isCheckedByAnalysis() {
    return f_isCheckedByAnalysis;
  }

  public boolean isIntendedToBeCheckedByAnalysis() {
    return f_isIntendedToBeCheckedByAnalysis;
  }

  public boolean isVirtual() {
    return f_isVirtual;
  }
}
