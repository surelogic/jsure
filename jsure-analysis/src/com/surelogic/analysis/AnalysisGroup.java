package com.surelogic.analysis;

import java.util.*;

import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.granules.IAnalysisGranule;
import com.surelogic.dropsea.ir.drops.CUDrop;

/**
 * A group of analyses sharing the same IAnalysisGranulator
 * 
 * @author edwin
 */
public final class AnalysisGroup<Q extends IAnalysisGranule> extends ConcurrentAnalysis<Q>implements IAnalysisGroup<Q> {

  final Analyses parent;
  final IAnalysisGranulator<Q> granulator;
  final int offset; // Into the linear ordering
  final List<IIRAnalysis<Q>> analyses;

  @SuppressWarnings("unchecked")
  public AnalysisGroup(Analyses parent, IAnalysisGranulator<Q> g, int index, IIRAnalysis<Q>... analyses) {
    // internal or never
    super(analyses[0].runInParallel() != ConcurrencyType.EXTERNALLY);
    this.parent = parent;
    granulator = g;
    offset = index;
    this.analyses = new ArrayList<>(analyses.length);
    for (IIRAnalysis<Q> a : analyses) {
      this.analyses.add(a);
    }
  }

  public int getOffset() {
    return offset;
  }

  public boolean runSingleThreaded() {
    return runInParallel() != ConcurrencyType.EXTERNALLY;
  }

  @SuppressWarnings("unchecked")
  public Class<Q> getGranuleType() {
    if (granulator == null) {
      return (Class<Q>) CUDrop.class;
    }
    return granulator.getType();
  }

  public Analyses getParent() {
    return parent;
  }

  public IAnalysisGranulator<Q> getGranulator() {
    return granulator;
  }

  public String getLabel() {
    StringBuilder sb = new StringBuilder();
    for (IIRAnalysis<Q> a : this) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(a.name());
    }
    return sb.toString();
  }

  public int size() {
    return analyses.size();
  }

  boolean isEmpty() {
    return analyses.isEmpty();
  }

  public Iterator<IIRAnalysis<Q>> iterator() {
    return analyses.iterator();
  }

  public static <Q extends IAnalysisGranule> void handleAnalyzeEnd(IIRAnalysis<Q> a, IIRAnalysisEnvironment env,
      IIRProject project) {
    boolean moreToAnalyze;
    do {
      moreToAnalyze = false;
      for (Q granule : a.analyzeEnd(env, project)) {
        moreToAnalyze = true;

        // TODO parallelize?
        a.doAnalysisOnGranule(env, granule);
      }
    } while (moreToAnalyze);
  }
}
