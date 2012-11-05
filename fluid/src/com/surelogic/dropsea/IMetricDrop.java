package com.surelogic.dropsea;

import com.surelogic.NonNull;

public interface IMetricDrop extends IDrop, ISnapshotDrop {

  /*
   * List all metrics below and use these constants when creating drops of
   * information about one of them.
   */

  public static final String SAFE_STATE_WRT_CONCURRENCY = "safe-state-wrt-concurrency";

  /**
   * Gets the metric this drop of information contributes too.
   * <p>
   * This must be non-{@code null} because, by definition, a metric drop must
   * contribute information about some metric.
   * 
   * @return the metric this drop of information contributes too. Should be a
   *         constant in {@link IMetricDrop}, such as
   *         {@link IMetricDrop#SAFE_STATE_WRT_CONCURRENCY}.
   */
  @NonNull
  String getMetric();
}
