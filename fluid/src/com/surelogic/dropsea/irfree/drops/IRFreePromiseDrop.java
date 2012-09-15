package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CATEGORY_ATTR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.xml.sax.Attributes;

import com.surelogic.dropsea.IAnalysisResultDrop;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.ir.Category;
import com.surelogic.dropsea.ir.PromiseDrop;

public final class IRFreePromiseDrop extends IRFreeProofDrop implements IPromiseDrop {
  /**
   * Only for PromiseDrops
   */
  final List<IAnalysisResultDrop> checkedByResults;
  Category category;

  public void addCheckedByResult(IAnalysisResultDrop info) {
    if (PromiseDrop.useCheckedByResults) {
      checkedByResults.add(info);
    }
  }

  public IRFreePromiseDrop(String name, Attributes a) {
    super(name, a);
    checkedByResults = new ArrayList<IAnalysisResultDrop>(0);

    /*
     * The viewer expects promises to be in a category so we use an
     * "unparented drops" category for any promise that didn't load one from the
     * XML snapshot.
     * 
     * We don't want to do this for results so this check is here. If you set a
     * category for results the viewer gets unhappy.
     */
    category = Category.getInstance(getAttribute(CATEGORY_ATTR));
    if (category == null)
      category = Category.getInstance(149);
  }

  public Category getCategory() {
    return category;
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
