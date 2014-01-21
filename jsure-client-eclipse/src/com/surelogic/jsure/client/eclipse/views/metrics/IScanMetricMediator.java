package com.surelogic.jsure.client.eclipse.views.metrics;

import java.util.ArrayList;

import com.surelogic.Nullable;
import com.surelogic.common.ILifecycle;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.javac.persistence.JSureScanInfo;

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
  
  void takeActionCollapseAll();
}
