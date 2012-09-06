package edu.cmu.cs.fluid.sea.proxy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.IIRAnalysis;

import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;

/**
 * Temporary builder to help analyses be parallelized
 * 
 * @author Edwin
 */
@SuppressWarnings("rawtypes")
public final class ResultDropBuilder extends AbstractDropBuilder {
  private boolean isTimeout = false;
  private boolean isConsistent = false;
  private Set<PromiseDrop> checks = new HashSet<PromiseDrop>();
  private Set<PromiseDrop> trusted = new HashSet<PromiseDrop>();
  private Map<String, Set<PromiseDrop>> trustedOr = new HashMap<String, Set<PromiseDrop>>();

  private ResultDropBuilder() {
  }

  public static ResultDropBuilder create(IIRAnalysis a) {
    ResultDropBuilder rv = new ResultDropBuilder();
    a.handleBuilder(rv);
    return rv;
  }

  public void setTimeout() {
    isTimeout = true;
    isConsistent = false;
  }

  public void setConsistent(final boolean value) {
    isConsistent = value;
  }

  public void setConsistent() {
    isConsistent = true;
  }

  public void setInconsistent() {
    isConsistent = false;
  }

  public boolean isConsistent() {
    return isConsistent;
  }

  /**
   * Adds a promise to the set of promises this result establishes, or
   * <i>checks</i>.
   * 
   * @param promise
   *          the promise being supported by this result
   */
  public void addCheckedPromise(PromiseDrop promise) {
    if (promise == null) {
      throw new NullPointerException();
    }
    checks.add(promise);
  }

  /**
   * @return the set of promise drops established, or checked, by this result.
   *         All members of the returned set will are of the PromiseDrop type.
   */
  public Set<? extends PromiseDrop> getChecks() {
    return checks;
  }

  public void addTrustedPromise(PromiseDrop promise) {
    trusted.add(promise);
  }

  public void addTrustedPromise_or(String label, PromiseDrop drop) {
    if (drop == null) {
      throw new IllegalArgumentException();
    }
    Set<PromiseDrop> drops = trustedOr.get(label);
    if (drops == null) {
      drops = new HashSet<PromiseDrop>();
      trustedOr.put(label, drops);
    }
    drops.add(drop);
  }

  @Override
  public int build() {
    if (!isValid()) {
      return 0;
    }
    ResultDrop rd = new ResultDrop();
    rd.setConsistent(isConsistent);
    if (isTimeout) {
      rd.setTimeout();
    }
    for (PromiseDrop<? extends IAASTRootNode> check : checks) {
      rd.addCheckedPromise(check);
    }
    for (PromiseDrop<? extends IAASTRootNode> t : trusted) {
      rd.addTrustedPromise(t);
    }
    for (Entry<String, Set<PromiseDrop>> e : trustedOr.entrySet()) {
      for (PromiseDrop<? extends IAASTRootNode> d : e.getValue()) {
        rd.addTrustedPromise_or(e.getKey(), d);
      }
    }
    return buildDrop(rd);
  }
}
