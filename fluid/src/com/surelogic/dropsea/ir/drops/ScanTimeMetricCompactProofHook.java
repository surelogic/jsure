package com.surelogic.dropsea.ir.drops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.dropsea.KeyValueUtility;
import com.surelogic.dropsea.ir.AbstractSeaConsistencyProofHook;
import com.surelogic.dropsea.ir.MetricDrop;
import com.surelogic.dropsea.ir.Sea;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public final class ScanTimeMetricCompactProofHook extends AbstractSeaConsistencyProofHook {

  /**
   * Threshold is half a second.
   */
  final long f_thresholdNs = TimeUnit.MILLISECONDS.toNanos(500);

  class Key {
    Key(String project, String pkg, String cu, String analysisName) {
      this.project = project;
      this.pkg = pkg;
      this.cu = cu;
      this.analysisName = analysisName;
    }

    final String project;
    final String pkg;
    final String cu;
    final String analysisName;

    @Override
    public String toString() {
      return project + ":" + pkg + "/" + cu + "[" + analysisName + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((analysisName == null) ? 0 : analysisName.hashCode());
      result = prime * result + ((cu == null) ? 0 : cu.hashCode());
      result = prime * result + ((pkg == null) ? 0 : pkg.hashCode());
      result = prime * result + ((project == null) ? 0 : project.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof Key))
        return false;
      Key other = (Key) obj;
      if (!getOuterType().equals(other.getOuterType()))
        return false;
      if (analysisName == null) {
        if (other.analysisName != null)
          return false;
      } else if (!analysisName.equals(other.analysisName))
        return false;
      if (cu == null) {
        if (other.cu != null)
          return false;
      } else if (!cu.equals(other.cu))
        return false;
      if (pkg == null) {
        if (other.pkg != null)
          return false;
      } else if (!pkg.equals(other.pkg))
        return false;
      if (project == null) {
        if (other.project != null)
          return false;
      } else if (!project.equals(other.project))
        return false;
      return true;
    }

    private ScanTimeMetricCompactProofHook getOuterType() {
      return ScanTimeMetricCompactProofHook.this;
    }
  }

  class Value {
    long totalDurationNs = 0;
    final ArrayList<MetricDrop> drops = new ArrayList<MetricDrop>();
    IRNode cu = null;
    IMetricDrop.Metric metric = null;
  }

  @Override
  public void postConsistencyProof(Sea sea) {
    HashMap<Key, Value> cuToDrops = new HashMap<Key, Value>();
    for (MetricDrop drop : sea.getDropsOfType(MetricDrop.class)) {
      // Only consider scan timing information
      if (drop.getMetric() != IMetricDrop.Metric.SCAN_TIME)
        continue;

      int durationNs = drop.getMetricInfoAsInt(IMetricDrop.SCAN_TIME_DURATION_NS, 0);
      if (durationNs < 0) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(311, durationNs));
        durationNs = 0;
      }
      String analysisName = drop.getMetricInfoOrNull(IMetricDrop.SCAN_TIME_ANALYSIS_NAME);
      if (analysisName == null) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(312));
        analysisName = "(unknown analysis)";
      }
      final IJavaRef javaRef = drop.getJavaRef();
      if (javaRef == null) {
        SLLogger.getLogger().log(Level.WARNING, I18N.err(316, drop));
        continue; // can't deal with this
      }
      final String project = javaRef.getEclipseProjectName();
      final String pkg = drop.getJavaRef().getPackageName();
      String cu = drop.getJavaRef().getSimpleFileName();

      final Key key = new Key(project, pkg, cu, analysisName);

      Value value = cuToDrops.get(key);
      if (value == null) {
        value = new Value();
        cuToDrops.put(key, value);
      }
      value.drops.add(drop);
      value.totalDurationNs += durationNs;
      if (value.cu == null)
        value.cu = VisitUtil.findCompilationUnit(drop.getNode());
      if (value.metric == null)
        value.metric = drop.getMetric();
    }

    for (Map.Entry<Key, Value> entry : cuToDrops.entrySet()) {
      if (entry.getValue().totalDurationNs > f_thresholdNs) {
        SLLogger.getLogger().info("Compacted SCAN_TIMEs for " + entry.getKey());
        // make a new drop
        MetricDrop metricOnCu = new MetricDrop(entry.getValue().cu, entry.getValue().metric);
        metricOnCu.addOrReplaceMetricInfo(KeyValueUtility.getLongInstance(IMetricDrop.SCAN_TIME_DURATION_NS,
            entry.getValue().totalDurationNs));
        metricOnCu.addOrReplaceMetricInfo(KeyValueUtility.getStringInstance(IMetricDrop.SCAN_TIME_ANALYSIS_NAME,
            entry.getKey().analysisName));
        for (MetricDrop remove : entry.getValue().drops)
          remove.invalidate();
      }
    }
  }
}
