package com.surelogic.dropsea.irfree.drops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.xml.sax.Attributes;

import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IPromiseDrop;

public abstract class IRFreeAnalysisResultDrop extends IRFreeProofDrop implements IAnalysisResultDrop {

  IRFreeAnalysisResultDrop(String name, Attributes a) {
    super(name, a);
  }

  private final List<IRFreePromiseDrop> checkedPromises = new ArrayList<IRFreePromiseDrop>(0);;

  public void addCheckedPromise(IRFreePromiseDrop info) {
    checkedPromises.add(info);
  }

//  @Override
  public boolean hasChecked() {
    return !checkedPromises.isEmpty();
  }

//  @Override
  public Collection<? extends IPromiseDrop> getCheckedPromises() {
    return checkedPromises;
  }
}
