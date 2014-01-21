package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.part.PageBook;

import com.surelogic.Nullable;
import com.surelogic.dropsea.DropSeaUtility;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.views.metrics.AbstractScanMetricMediator;

/**
 * Displays information about verifying analysis performance during a JSure
 * scan.
 * <p>
 * The information is obtained from {@link IMetricDrop.Metric#SCAN_TIME} metric
 * drops.
 */
public final class ScanTimeMetricMediator extends AbstractScanMetricMediator {

  @Override
  protected String getMetricLabel() {
    return "Performance";
  }

  public ScanTimeMetricMediator(TabFolder folder) {
    super(folder);
  }

  Composite f_panel = null;

  @Override
  protected Control initMetricDisplay(PageBook parent) {
    f_panel = new Composite(parent, SWT.NONE);
    f_panel.setBackground(f_panel.getDisplay().getSystemColor(SWT.COLOR_BLUE));
    return f_panel;
  }

  @Override
  protected void refreshMetricContentsFor(@Nullable JSureScanInfo scan, @Nullable ArrayList<IMetricDrop> drops) {
    final ArrayList<IMetricDrop> metricDrops = DropSeaUtility.filterMetricsToOneType(IMetricDrop.Metric.SCAN_TIME, drops);
    System.out.println("Got " + metricDrops.size() + " SCAN_TIME metric drops.");
  }

  @Override
  public void takeActionCollapseAll() {
    // TODO Auto-generated method stub
  }

  @Override
  public void takeActionUseAlphaSort(boolean value) {
    // TODO Auto-generated method stub

  }

  @Override
  public void dispose() {
    // Nothing to do
  }

}
