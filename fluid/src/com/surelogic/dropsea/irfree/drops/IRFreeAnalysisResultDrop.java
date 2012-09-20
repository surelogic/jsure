package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.ENCLOSED_IN_FOLDER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IPromiseDrop;

public abstract class IRFreeAnalysisResultDrop extends IRFreeProofDrop implements IAnalysisResultDrop {

  private final boolean f_isInResultFolder;

  IRFreeAnalysisResultDrop(Entity e, Class<?> irClass) {
    super(e, irClass);
    f_isInResultFolder = "true".equals(e.getAttribute(ENCLOSED_IN_FOLDER));
  }

  private final List<IRFreePromiseDrop> f_checkedPromises = new ArrayList<IRFreePromiseDrop>(0);;

  public void addCheckedPromise(IRFreePromiseDrop info) {
    f_checkedPromises.add(info);
  }

  public boolean hasChecked() {
    return !f_checkedPromises.isEmpty();
  }

  public Collection<? extends IPromiseDrop> getCheckedPromises() {
    return f_checkedPromises;
  }

  public boolean isInResultFolder() {
    return f_isInResultFolder;
  }
}
