package edu.cmu.cs.fluid.sea.proxy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.IIRAnalysis;

import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;

/**
 * Temporary builder for {@link ResultDrop} instances to help analyses be
 * parallelized.
 */
public final class ResultDropBuilder extends AbstractResultDropBuilder {
  private boolean isTimeout = false;
  private boolean isConsistent = false;

  private Set<PromiseDrop<? extends IAASTRootNode>> trusted = new HashSet<PromiseDrop<? extends IAASTRootNode>>();
  private Map<String, Set<PromiseDrop<? extends IAASTRootNode>>> trustedOr = new HashMap<String, Set<PromiseDrop<? extends IAASTRootNode>>>();

  private ResultDropBuilder() {
  }

  public static ResultDropBuilder create(IIRAnalysis a) {
    final ResultDropBuilder rv = new ResultDropBuilder();
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

  public void addTrustedPromise(PromiseDrop<? extends IAASTRootNode> promise) {
    trusted.add(promise);
  }

  public void addTrustedPromise_or(String label, PromiseDrop<? extends IAASTRootNode> drop) {
    if (drop == null) {
      throw new IllegalArgumentException();
    }
    Set<PromiseDrop<? extends IAASTRootNode>> drops = trustedOr.get(label);
    if (drops == null) {
      drops = new HashSet<PromiseDrop<? extends IAASTRootNode>>();
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
    for (Entry<String, Set<PromiseDrop<? extends IAASTRootNode>>> e : trustedOr.entrySet()) {
      for (PromiseDrop<? extends IAASTRootNode> d : e.getValue()) {
        rd.addTrustedPromise_or(e.getKey(), d);
      }
    }
    return buildDrop(rd);
  }
}
