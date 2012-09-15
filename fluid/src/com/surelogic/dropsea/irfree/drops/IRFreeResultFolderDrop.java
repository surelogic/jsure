package com.surelogic.dropsea.irfree.drops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;

import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.ir.ResultDrop;

public final class IRFreeResultFolderDrop extends IRFreeProofDrop implements IResultFolderDrop {
  /**
   * Only for ResultDrops
   */
  final List<IRFreePromiseDrop> checkedPromises;
  final List<IRFreeResultFolderDrop> subFolders;
  final List<IRFreeResultDrop> results;

  public void addCheckedPromise(IRFreePromiseDrop info) {
    checkedPromises.add(info);
  }

  public void addSubFolder(IRFreeResultFolderDrop info) {
    subFolders.add(info);
  }

  public void addResult(IRFreeResultDrop info) {
    results.add(info);
  }

  public IRFreeResultFolderDrop(String name, Attributes a) {
    super(name, a);

    checkedPromises = new ArrayList<IRFreePromiseDrop>(0);
    subFolders = new ArrayList<IRFreeResultFolderDrop>(0);
    results = new ArrayList<IRFreeResultDrop>(0);
  }

  public Collection<? extends IPromiseDrop> getChecks() {
    return checkedPromises;
  }

  public Collection<? extends IResultDrop> getAnalysisResults() {
    return results;
  }

  public Collection<? extends IResultFolderDrop> getSubFolders() {
    return subFolders;
  }

  public boolean isConsistent() {
    return "true".equals(getAttribute(ResultDrop.CONSISTENT));
  }

  public Collection<? extends IAnalysisResultDrop> getContents() {
    Collection<IAnalysisResultDrop> rv = new HashSet<IAnalysisResultDrop>(results);
    rv.addAll(subFolders);
    return rv;
  }
}