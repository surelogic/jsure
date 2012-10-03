package com.surelogic.dropsea.irfree.drops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.surelogic.NonNull;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;

public abstract class IRFreeAnalysisResultDrop extends IRFreeProofDrop implements IAnalysisResultDrop {

  private final List<IRFreePromiseDrop> f_checkedPromises = new ArrayList<IRFreePromiseDrop>(0);;
  private final List<IRFreeProofDrop> f_trusted = new ArrayList<IRFreeProofDrop>(0);

  void addCheckedPromise(IRFreePromiseDrop info) {
    f_checkedPromises.add(info);
  }

  void addTrusted(IRFreeProofDrop info) {
    f_trusted.add(info);
  }

  IRFreeAnalysisResultDrop(Entity e, Class<?> irClass) {
    super(e, irClass);
  }

  public boolean hasChecked() {
    return !f_checkedPromises.isEmpty();
  }

  public Collection<? extends IPromiseDrop> getChecked() {
    return f_checkedPromises;
  }

  @NonNull
  public Collection<IProofDrop> getTrusted() {
    return new HashSet<IProofDrop>(f_trusted);
  }

  public boolean hasTrusted() {
    return !f_trusted.isEmpty();
  }
}
