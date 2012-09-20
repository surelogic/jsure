package com.surelogic.dropsea.irfree.drops;

import static com.surelogic.common.jsure.xml.AbstractXMLReader.CONSISTENT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OR_PROVED;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.OR_USES_RED_DOT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TIMEOUT;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.VOUCHED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.NonNull;
import com.surelogic.common.xml.Entity;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IResultDrop;

public final class IRFreeResultDrop extends IRFreeAnalysisResultDrop implements IResultDrop {

  private final List<IRFreeProofDrop> f_trusted = new ArrayList<IRFreeProofDrop>(0);
  private final MultiMap<String, IRFreeProofDrop> f_orTrusted = new MultiHashMap<String, IRFreeProofDrop>(0);
  private final boolean f_isConsistent;
  private final boolean f_or_proofUsesRedDot;
  private final boolean f_or_provedConsistent;
  private final boolean f_isVouched;
  private final boolean f_isTimeout;

  void addTrusted_and(IRFreeProofDrop info) {
    f_trusted.add(info);
  }

  void addTrusted_or(String label, IRFreeProofDrop info) {
    f_orTrusted.put(label, info);
  }

  IRFreeResultDrop(Entity e, Class<?> irClass) {
    super(e, irClass);

    f_isConsistent = "true".equals(e.getAttribute(CONSISTENT));
    f_or_proofUsesRedDot = "true".equals(e.getAttribute(OR_USES_RED_DOT));
    f_or_provedConsistent = "true".equals(e.getAttribute(OR_PROVED));
    f_isVouched = "true".equals(e.getAttribute(VOUCHED));
    f_isTimeout = "true".equals(e.getAttribute(TIMEOUT));
  }

  @NonNull
  public Collection<? extends IProofDrop> getTrusted_and() {
    return f_trusted;
  }

  public boolean isConsistent() {
    return f_isConsistent;
  }

  @NonNull
  public Collection<IProofDrop> getAllTrusted() {
    Collection<IProofDrop> rv = new HashSet<IProofDrop>(f_trusted);
    rv.addAll(f_orTrusted.values());
    return rv;
  }

  @NonNull
  public Collection<String> getTrusted_orKeys() {
    return f_orTrusted.keySet();
  }

  @NonNull
  public Collection<? extends IProofDrop> getTrusted_or(String key) {
    final Collection<? extends IProofDrop> result = f_orTrusted.get(key);
    if (result != null)
      return result;
    else
      return Collections.emptySet();
  }

  public boolean hasOrLogic() {
    return f_orTrusted != null && !f_orTrusted.isEmpty();
  }

  public boolean hasTrusted() {
    return hasOrLogic() || !f_trusted.isEmpty();
  }

  public boolean get_or_proofUsesRedDot() {
    return f_or_proofUsesRedDot;
  }

  public boolean get_or_provedConsistent() {
    return f_or_provedConsistent;
  }

  public boolean isVouched() {
    return f_isVouched;
  }

  public boolean isTimeout() {
    return f_isTimeout;
  }
}
