package com.surelogic.dropsea.irfree.drops;


import static com.surelogic.common.jsure.xml.AbstractXMLReader.ASSUMED;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.CHECKED_BY_ANALYSIS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.TO_BE_CHECKED_BY_ANALYSIS;
import static com.surelogic.common.jsure.xml.AbstractXMLReader.VIRTUAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.xml.sax.Attributes;

import com.surelogic.NonNull;
import com.surelogic.dropsea.IPromiseDrop;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.dropsea.IResultDrop;
import com.surelogic.dropsea.IResultFolderDrop;
import com.surelogic.dropsea.ir.ResultDrop;

public final class IRFreeResultDrop extends IRFreeAnalysisResultDrop implements IResultDrop {

  private final List<IRFreePromiseDrop> trustedPromises = new ArrayList<IRFreePromiseDrop>(0);
  private final List<IRFreeResultFolderDrop> trustedFolders = new ArrayList<IRFreeResultFolderDrop>(0);
  private final MultiMap<String, IRFreePromiseDrop> orTrustedPromises = new MultiHashMap<String, IRFreePromiseDrop>(0);

  public void addTrustedPromise(IRFreePromiseDrop info) {
    trustedPromises.add(info);
  }

  public void addOrTrustedPromise(String label, IRFreePromiseDrop info) {
    orTrustedPromises.put(label, info);
  }

  public void addTrustedFolder(IRFreeResultFolderDrop info) {
    trustedFolders.add(info);
  }

  public IRFreeResultDrop(String name, Attributes a) {
    super(name, a);
  }

  public boolean isInResultFolder() {
    return "true".equals(getAttribute(ResultDrop.ENCLOSED_IN_FOLDER));
  }

  @NonNull
  public Collection<? extends IPromiseDrop> getTrustedPromises() {
    return trustedPromises;
  }

  @NonNull
  public Collection<? extends IResultFolderDrop> getTrustedFolders() {
    return trustedFolders;
  }

  public boolean isConsistent() {
    return "true".equals(getAttribute(ResultDrop.CONSISTENT));
  }

  @NonNull
  public Collection<IProofDrop> getAllTrusted() {
    Collection<IProofDrop> rv = new HashSet<IProofDrop>(trustedPromises);
    rv.addAll(trustedFolders);
    rv.addAll(orTrustedPromises.values());
    return rv;
  }

  @NonNull
  public Collection<String> getTrustedPromises_orKeys() {
    return orTrustedPromises.keySet();
  }

  @NonNull
  public Collection<? extends IPromiseDrop> getTrustedPromises_or(String key) {
    final Collection<? extends IPromiseDrop> result = orTrustedPromises.get(key);
    if (result != null)
      return result;
    else
      return Collections.emptySet();
  }

  public boolean hasOrLogic() {
    return orTrustedPromises != null && !orTrustedPromises.isEmpty();
  }

  public boolean hasTrusted() {
    return hasOrLogic() || !trustedPromises.isEmpty() || !trustedFolders.isEmpty();
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
