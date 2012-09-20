package com.surelogic.dropsea.irfree.drops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.IResultFolderDrop;

public final class IRFreeResultFolderDrop extends IRFreeAnalysisResultDrop implements IResultFolderDrop {

  final List<IRFreeResultFolderDrop> subFolders = new ArrayList<IRFreeResultFolderDrop>(0);
  final List<IRFreeResultDrop> results = new ArrayList<IRFreeResultDrop>(0);

  public void addSubFolder(IRFreeResultFolderDrop info) {
    subFolders.add(info);
  }

  public void addResult(IRFreeResultDrop info) {
    results.add(info);
  }

  public IRFreeResultFolderDrop(Entity e, Class<?> irClass) {
    super(e, irClass);
  }

  public Collection<? extends IResultDrop> getAnalysisResults() {
    return results;
  }

  public Collection<? extends IResultFolderDrop> getSubFolders() {
    return subFolders;
  }

  public Collection<? extends IAnalysisResultDrop> getContents() {
    Collection<IAnalysisResultDrop> rv = new HashSet<IAnalysisResultDrop>(results);
    rv.addAll(subFolders);
    return rv;
  }
}
