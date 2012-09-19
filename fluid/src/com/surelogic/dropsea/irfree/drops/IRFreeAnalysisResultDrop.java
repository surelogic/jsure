package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.ENCLOSED_IN_FOLDER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IPromiseDrop;

public abstract class IRFreeAnalysisResultDrop extends IRFreeProofDrop implements IAnalysisResultDrop {

  IRFreeAnalysisResultDrop(Entity e, Class<?> irClass) {
    super(e, irClass);
  }

  private final List<IRFreePromiseDrop> checkedPromises = new ArrayList<IRFreePromiseDrop>(0);;

  public void addCheckedPromise(IRFreePromiseDrop info) {
    checkedPromises.add(info);
  }

  public boolean hasChecked() {
    return !checkedPromises.isEmpty();
  }

  public Collection<? extends IPromiseDrop> getCheckedPromises() {
    return checkedPromises;
  }

  public boolean isInResultFolder() {
    return "true".equals(getEntity().getAttribute(ENCLOSED_IN_FOLDER));
  }
}
