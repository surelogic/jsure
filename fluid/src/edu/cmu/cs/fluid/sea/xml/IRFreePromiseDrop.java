package edu.cmu.cs.fluid.sea.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.xml.sax.Attributes;

import edu.cmu.cs.fluid.sea.IAnalysisResultDrop;
import edu.cmu.cs.fluid.sea.IPromiseDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public final class IRFreePromiseDrop extends IRFreeProofDrop implements IPromiseDrop {
  /**
   * Only for PromiseDrops
   */
  final List<IAnalysisResultDrop> checkedByResults;

  void addCheckedByResult(IAnalysisResultDrop info) {
    if (PromiseDrop.useCheckedByResults) {
      checkedByResults.add(info);
    }
  }

  IRFreePromiseDrop(String name, Attributes a) {
    super(name, a);
    checkedByResults = new ArrayList<IAnalysisResultDrop>(0);
  }

  public Collection<? extends IAnalysisResultDrop> getCheckedBy() {
    return checkedByResults;
  }

  public boolean isAssumed() {
    return "true".equals(getAttribute(PromiseDrop.ASSUMED));
  }

  public boolean isCheckedByAnalysis() {
    return "true".equals(getAttribute(PromiseDrop.CHECKED_BY_ANALYSIS));
  }

  public boolean isIntendedToBeCheckedByAnalysis() {
    return "true".equals(getAttribute(PromiseDrop.TO_BE_CHECKED_BY_ANALYSIS));
  }

  public boolean isVirtual() {
    return "true".equals(getAttribute(PromiseDrop.VIRTUAL));
  }
}