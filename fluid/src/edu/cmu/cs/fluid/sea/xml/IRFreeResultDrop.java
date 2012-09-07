package edu.cmu.cs.fluid.sea.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.xml.sax.Attributes;

import edu.cmu.cs.fluid.sea.IPromiseDrop;
import edu.cmu.cs.fluid.sea.IResultDrop;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;

public final class IRFreeResultDrop extends IRFreeProofDrop implements IResultDrop {
  /**
   * Only for ResultDrops
   */
  final List<IRFreePromiseDrop> checkedPromises;
  final List<IRFreePromiseDrop> trustedPromises;
  final MultiMap<String, IRFreePromiseDrop> orTrustedPromises;
  
  void addCheckedPromise(IRFreePromiseDrop info) {
    checkedPromises.add(info);
  }

  void addTrustedPromise(IRFreePromiseDrop info) {
    trustedPromises.add(info);
  }

  void addOrTrustedPromise(String label, IRFreePromiseDrop info) {
    orTrustedPromises.put(label, info);
  }

  IRFreeResultDrop(String name, Attributes a) {
    super(name, a);

    checkedPromises = new ArrayList<IRFreePromiseDrop>(0);
    trustedPromises = new ArrayList<IRFreePromiseDrop>(0);
    orTrustedPromises = new MultiHashMap<String, IRFreePromiseDrop>(0);
  }

  public boolean hasEnclosingFolder() {
	return "true".equals(getAttribute(ResultDrop.ENCLOSED_IN_FOLDER));
  }
  
  public Collection<? extends IPromiseDrop> getChecks() {
    return checkedPromises;
  }

  public Collection<? extends IPromiseDrop> getTrusts() {
    return trustedPromises;
  }

  public boolean isConsistent() {
    return "true".equals(getAttribute(ResultDrop.CONSISTENT));
  }

  public Collection<? extends IPromiseDrop> getTrustsComplete() {
    Collection<IRFreePromiseDrop> rv = new HashSet<IRFreePromiseDrop>(trustedPromises);
    rv.addAll(orTrustedPromises.values());
    return rv;
  }

  public Collection<String> get_or_TrustLabelSet() {
    return orTrustedPromises.keySet();
  }

  public Collection<? extends IPromiseDrop> get_or_Trusts(String key) {
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

  public boolean isVirtual() {
    return "true".equals(getAttribute(PromiseDrop.VIRTUAL));
  }
}
