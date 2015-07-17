package com.surelogic.analysis.granules;

import java.util.ArrayList;
import java.util.List;

import com.surelogic.common.concurrent.Procedure;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;

public abstract class AbstractGranulator<T extends IAnalysisGranule> implements IAnalysisGranulator<T> {
  private final Class<T> type;
  private final List<T> granules = new ArrayList<>();

  protected AbstractGranulator(Class<T> t) {
    type = t;
  }

  @Override
  public Class<T> getType() {
    return type;
  }

  public int getThresholdToForkTasks() {
    return 2;
  }

  @Override
  public final List<T> getGranules() {
    try {
      return new ArrayList<>(granules);
    } finally {
      granules.clear();
    }
  }

  // TODO not thread-safe
  @Override
  public final int extractGranules(ITypeEnvironment tEnv, IRNode cu) {
    final int orig = granules.size();
    extractGranules(granules, tEnv, cu);
    return granules.size() - orig;
  }

  protected abstract void extractGranules(List<T> granules, ITypeEnvironment tEnv, IRNode cu);

  public final List<T> extractNewGranules(ITypeEnvironment tEnv, IRNode cu) {
    final List<T> rv = new ArrayList<>();
    extractGranules(rv, tEnv, cu);
    return rv;
  }

  @Override
  public Procedure<T> wrapAnalysis(Procedure<T> proc) {
    return proc;
  }
}
