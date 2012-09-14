package edu.cmu.cs.fluid.sea.xml;

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
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;


public final class IRFreeResultDrop extends IRFreeProofDrop implements IResultDrop {
  /**
   * Only for ResultDrops
   */
  final List<IRFreePromiseDrop> checkedPromises;
  final List<IRFreePromiseDrop> trustedPromises;
  final List<IRFreeResultFolderDrop> trustedFolders;
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

  void addTrustedFolder(IRFreeResultFolderDrop info) {
	  trustedFolders.add(info);
  }
  
  IRFreeResultDrop(String name, Attributes a) {
    super(name, a);

    checkedPromises = new ArrayList<IRFreePromiseDrop>(0);
    trustedPromises = new ArrayList<IRFreePromiseDrop>(0);
    trustedFolders = new ArrayList<IRFreeResultFolderDrop>(0);
    orTrustedPromises = new MultiHashMap<String, IRFreePromiseDrop>(0);
  }

  public boolean isInResultFolder() {
    return "true".equals(getAttribute(ResultDrop.ENCLOSED_IN_FOLDER));
  }

  @NonNull
  public Collection<? extends IPromiseDrop> getChecks() {
    return checkedPromises;
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
