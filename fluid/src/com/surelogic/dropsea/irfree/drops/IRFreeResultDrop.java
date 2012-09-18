package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.ASSUMED;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CHECKED_BY_ANALYSIS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONSISTENT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OR_PROVED;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OR_USES_RED_DOT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TIMEOUT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TO_BE_CHECKED_BY_ANALYSIS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.VIRTUAL;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.VOUCHED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.xml.sax.Attributes;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IResultDrop;

public final class IRFreeResultDrop extends IRFreeAnalysisResultDrop implements IResultDrop {

  private final List<IRFreeProofDrop> trusted = new ArrayList<IRFreeProofDrop>(0);
  private final MultiMap<String, IRFreeProofDrop> orTrusted = new MultiHashMap<String, IRFreeProofDrop>(0);

  public void addTrusted_and(IRFreeProofDrop info) {
    trusted.add(info);
  }

  public void addTrusted_or(String label, IRFreeProofDrop info) {
    orTrusted.put(label, info);
  }

  public IRFreeResultDrop(String name, Attributes a) {
    super(name, a);
  }

  @NonNull
  public Collection<? extends IProofDrop> getTrusted_and() {
    return trusted;
  }

  public boolean isConsistent() {
    return "true".equals(getAttribute(CONSISTENT));
  }

  @NonNull
  public Collection<IProofDrop> getAllTrusted() {
    Collection<IProofDrop> rv = new HashSet<IProofDrop>(trusted);
    rv.addAll(orTrusted.values());
    return rv;
  }

  @NonNull
  public Collection<String> getTrusted_orKeys() {
    return orTrusted.keySet();
  }

  @NonNull
  public Collection<? extends IProofDrop> getTrusted_or(String key) {
    final Collection<? extends IProofDrop> result = orTrusted.get(key);
    if (result != null)
      return result;
    else
      return Collections.emptySet();
  }

  public boolean hasOrLogic() {
    return orTrusted != null && !orTrusted.isEmpty();
  }

  public boolean hasTrusted() {
    return hasOrLogic() || !trusted.isEmpty();
  }

  public boolean get_or_proofUsesRedDot() {
    return "true".equals(getAttribute(OR_USES_RED_DOT));
  }

  public boolean get_or_provedConsistent() {
    return "true".equals(getAttribute(OR_PROVED));
  }

  public boolean isVouched() {
    return "true".equals(getAttribute(VOUCHED));
  }

  public boolean isTimeout() {
    return "true".equals(getAttribute(TIMEOUT));
  }

  public boolean isAssumed() {
    return "true".equals(getAttribute(ASSUMED));
  }

  public boolean isCheckedByAnalysis() {
    return "true".equals(getAttribute(CHECKED_BY_ANALYSIS));
  }

  public boolean isIntendedToBeCheckedByAnalysis() {
    return "true".equals(getAttribute(TO_BE_CHECKED_BY_ANALYSIS));
  }

  public boolean isVirtual() {
    return "true".equals(getAttribute(VIRTUAL));
  }
}
