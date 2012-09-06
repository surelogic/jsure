package edu.cmu.cs.fluid.sea.xml;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.DERIVED_FROM_SRC_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.PROVED_ATTR;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.USES_RED_DOT_ATTR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.xml.sax.Attributes;

import edu.cmu.cs.fluid.sea.IProofDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;

public final class IRFreeProofDrop extends IRFreeDrop implements IProofDrop {
  /**
   * Only for PromiseDrops
   */
  final List<IRFreeProofDrop> checkedByResults;
  /**
   * Only for ResultDrops
   */
  final List<IRFreeProofDrop> checkedPromises;
  final List<IRFreeProofDrop> trustedPromises;
  final MultiMap<String, IRFreeProofDrop> orTrustedPromises;

  void addCheckedByResult(IRFreeProofDrop info) {
    if (PromiseDrop.useCheckedByResults) {
      checkedByResults.add(info);
    }
  }

  void addCheckedPromise(IRFreeProofDrop info) {
    if (!PromiseDrop.useCheckedByResults) {
      info.checkedByResults.add(this);
    }
    checkedPromises.add(info);
  }

  void addTrustedPromise(IRFreeProofDrop info) {
    trustedPromises.add(info);
  }

  void addOrTrustedPromise(String label, IRFreeProofDrop info) {
    orTrustedPromises.put(label, info);
  }

  IRFreeProofDrop(String name, Attributes a) {
    super(name, a);

    if (instanceOf(ResultDrop.class)) {
      checkedPromises = new ArrayList<IRFreeProofDrop>();
      trustedPromises = new ArrayList<IRFreeProofDrop>();
      orTrustedPromises = new MultiHashMap<String, IRFreeProofDrop>();
      checkedByResults = Collections.emptyList();
    } else {
      checkedPromises = Collections.emptyList();
      trustedPromises = Collections.emptyList();
      orTrustedPromises = null;
      if (instanceOf(PromiseDrop.class)) {
        checkedByResults = new ArrayList<IRFreeProofDrop>();
      } else {
        checkedByResults = Collections.emptyList();
      }
    }
  }

  public Collection<? extends IProofDrop> getChecks() {
    return checkedPromises;
  }

  public Collection<? extends IProofDrop> getTrusts() {
    return trustedPromises;
  }

  public boolean isConsistent() {
    return "true".equals(getAttribute(ResultDrop.CONSISTENT));
  }

  public boolean proofUsesRedDot() {
    return "true".equals(getAttribute(USES_RED_DOT_ATTR));

  }

  public boolean provedConsistent() {
    return "true".equals(getAttribute(PROVED_ATTR));
  }

  public boolean derivedFromSrc() {
    return "true".equals(getAttribute(DERIVED_FROM_SRC_ATTR));
  }

  public Collection<? extends IProofDrop> getCheckedBy() {
    return checkedByResults;
  }

  public Collection<? extends IProofDrop> getTrustsComplete() {
    Collection<IRFreeProofDrop> rv = new HashSet<IRFreeProofDrop>(trustedPromises);
    rv.addAll(orTrustedPromises.values());
    return rv;
  }

  public Collection<String> get_or_TrustLabelSet() {
    return orTrustedPromises.keySet();
  }

  public Collection<? extends IProofDrop> get_or_Trusts(String key) {
    return orTrustedPromises.get(key);
  }

  public boolean hasOrLogic() {
    return orTrustedPromises != null && !orTrustedPromises.isEmpty();
  }

  public boolean get_or_proofUsesRedDot() {
    return "true".equals(getAttribute(ResultDrop.OR_USES_RED_DOT));
  }

  public boolean get_or_provedConsistent() {
    return "true".equals(getAttribute(ResultDrop.OR_PROVED));
  }

  public boolean isVouched() {
    return "true".equals(getAttribute(ResultDrop.VOUCHED));
  }

  public boolean isTimeout() {
    return "true".equals(getAttribute(ResultDrop.TIMEOUT));
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

  public boolean isFromSrc() {
    return "true".equals(getAttribute(PromiseDrop.FROM_SRC));
  }

  public boolean isVirtual() {
    return "true".equals(getAttribute(PromiseDrop.VIRTUAL));
  }
}
