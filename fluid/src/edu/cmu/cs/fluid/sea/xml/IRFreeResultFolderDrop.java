package edu.cmu.cs.fluid.sea.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.xml.sax.Attributes;

import edu.cmu.cs.fluid.sea.IPromiseDrop;
import edu.cmu.cs.fluid.sea.IProofDrop;
import edu.cmu.cs.fluid.sea.IResultDrop;
import edu.cmu.cs.fluid.sea.IResultFolderDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;

public final class IRFreeResultFolderDrop extends IRFreeProofDrop implements IResultFolderDrop {
  /**
   * Only for ResultDrops
   */
  final List<IRFreePromiseDrop> checkedPromises;
  final List<IRFreeResultFolderDrop> subFolders;
  final List<IRFreeResultDrop> results;

  void addCheckedPromise(IRFreePromiseDrop info) {
    checkedPromises.add(info);
  }

  void addSubFolder(IRFreeResultFolderDrop info) {
	  subFolders.add(info);
  }

  void addResult(IRFreeResultDrop info) {
	  results.add(info);
  }

  IRFreeResultFolderDrop(String name, Attributes a) {
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

  public Collection<? extends IProofDrop> getContents() {
    Collection<IProofDrop> rv = new HashSet<IProofDrop>(results);
    rv.addAll(subFolders);
    return rv;
  }
}
