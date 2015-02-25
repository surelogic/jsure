package com.surelogic.jsure.client.eclipse.views.metrics;

import java.util.ArrayList;

import com.surelogic.Nullable;
import com.surelogic.common.ILifecycle;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.java.persistence.JSureScanInfo;

public interface IScanMetricMediator extends ILifecycle {
  /**
   * Notification that a new scan has been selected.
   * <p>
   * Both parameters will be {@code null} if no scan is selected. They will both
   * be non-{@code null} if a scan is selected.
   * 
   * @param scan
   *          results and information about a JSure scan.
   * @param drops
   *          metric drops about a JSure scan.
   */
  void refreshScanContents(@Nullable JSureScanInfo scan, @Nullable ArrayList<IMetricDrop> drops);

  /**
   * View UI action to collapse all. This action should trigger the expected
   * action by each implementation. For example, collapse a tree in the metric
   * UI.
   */
  void takeActionCollapseAll();

  /**
   * View UI action to toggle between alphabetical and metric-natural sorting.
   * Metric-natural sorting is typically a count, for example, SLOC.
   * 
   * @param value
   *          {@code true} to use alphabetical sort, {@code false} to use
   *          metric-natural sorting.
   */
  void takeActionUseAlphaSort(boolean value);

  /**
   * View UI action to filter out non-highlighted elements. For example, many
   * metrics use a threshold to mark elements, this would filter out elements
   * not marked.
   * 
   * @param value
   *          {@code true} to filter out non-marked elements, {@code false} to
   *          apply no filter to elements.
   */
  void takeActionUseFilter(boolean value);
}
