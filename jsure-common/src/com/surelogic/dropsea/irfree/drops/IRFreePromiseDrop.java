package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.ASSUMED;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.CHECKED_BY_ANALYSIS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.PLACE_IN_SUBFOLDER;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.SHOW_AT_TOP_LEVEL;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.TO_BE_CHECKED_BY_ANALYSIS;
import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.VIRTUAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.DropType;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IPromiseDrop;

public class IRFreePromiseDrop extends IRFreeProofDrop implements IPromiseDrop {

  private final List<IRFreePromiseDrop> f_dependentPromises = new ArrayList<>(0);
  private final List<IRFreePromiseDrop> f_deponentPromises = new ArrayList<>(0);
  private final List<IRFreeAnalysisResultDrop> f_checkedByResults = new ArrayList<>(0);
  private final boolean f_isAssumed;
  private final boolean f_isCheckedByAnalysis;
  private final boolean f_isIntendedToBeCheckedByAnalysis;
  private final boolean f_isVirtual;
  private final boolean f_showAtTopLevel;
  private final boolean f_placeInASubFolder;

  void addCheckedByResult(IRFreeAnalysisResultDrop info) {
    f_checkedByResults.add(info);
  }

  void addDependentPromise(IRFreePromiseDrop p) {
    f_dependentPromises.add(p);
  }

  void addDeponentPromise(IRFreePromiseDrop p) {
    f_deponentPromises.add(p);
  }

  IRFreePromiseDrop(Entity e) {
    super(e);
    f_isAssumed = "true".equals(e.getAttribute(ASSUMED));
    f_isCheckedByAnalysis = "true".equals(e.getAttribute(CHECKED_BY_ANALYSIS));
    f_isIntendedToBeCheckedByAnalysis = "true".equals(e.getAttribute(TO_BE_CHECKED_BY_ANALYSIS));
    f_isVirtual = "true".equals(e.getAttribute(VIRTUAL));
    f_showAtTopLevel = "true".equals(e.getAttribute(SHOW_AT_TOP_LEVEL));
    f_placeInASubFolder = "true".equals(e.getAttribute(PLACE_IN_SUBFOLDER));
  }

  @NonNull
  @Override
  public DropType getDropType() {
    return DropType.PROMISE;
  }

  @Override
  @NonNull
  public Collection<? extends IAnalysisResultDrop> getCheckedBy() {
    return f_checkedByResults;
  }

  @Override
  @NonNull
  public Collection<? extends IPromiseDrop> getDependentPromises() {
    return f_dependentPromises;
  }

  @Override
  @NonNull
  public Collection<? extends IPromiseDrop> getDeponentPromises() {
    return f_deponentPromises;
  }

  @Override
  public boolean isAssumed() {
    return f_isAssumed;
  }

  @Override
  public boolean isCheckedByAnalysis() {
    return f_isCheckedByAnalysis;
  }

  @Override
  public boolean isIntendedToBeCheckedByAnalysis() {
    return f_isIntendedToBeCheckedByAnalysis;
  }

  @Override
  public boolean isVirtual() {
    return f_isVirtual;
  }

  @Override
  public boolean showAtTopLevel() {
    return f_showAtTopLevel;
  }

  @Override
  public boolean placeInASubFolder() {
    return f_placeInASubFolder;
  }
}
