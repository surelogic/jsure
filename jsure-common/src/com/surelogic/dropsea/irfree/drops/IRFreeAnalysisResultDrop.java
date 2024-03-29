package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.dropsea.irfree.NestedJSureXmlReader.USED_BY_PROOF;

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

  private final List<IRFreePromiseDrop> f_checkedPromises = new ArrayList<>(0);;
  private final List<IRFreeProofDrop> f_trusted = new ArrayList<>(0);
  private final boolean f_usedByProof;

  void addCheckedPromise(IRFreePromiseDrop info) {
    f_checkedPromises.add(info);
  }

  void addTrusted(IRFreeProofDrop info) {
    f_trusted.add(info);
  }

  IRFreeAnalysisResultDrop(Entity e) {
    super(e);

    f_usedByProof = "true".equals(e.getAttribute(USED_BY_PROOF));
  }

  @Override
  public boolean hasChecked() {
    return !f_checkedPromises.isEmpty();
  }

  @Override
  public Collection<? extends IPromiseDrop> getChecked() {
    return f_checkedPromises;
  }

  @Override
  @NonNull
  public Collection<IProofDrop> getTrusted() {
    return new HashSet<IProofDrop>(f_trusted);
  }

  @Override
  public boolean hasTrusted() {
    return !f_trusted.isEmpty();
  }

  @Override
  public boolean usedByProof() {
    return f_usedByProof;
  }
}
