package com.surelogic.jsure.client.eclipse.views.metrics.mediators;

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
 * Displays information about the size of code examined during a JSure scan.
 * <p>
 * The information is obtained from {@link IMetricDrop.Metric#SLOC} metric
 * drops.
 */
public final class SlocMetricMediator extends AbstractScanMetricMediator {

  @Override
  protected String getMetricLabel() {
    return "Code Size";
  }

  public SlocMetricMediator(TabFolder folder) {
    super(folder);
  }

  Composite f_panel = null;

  @Override
  protected Control initMetricDisplay(PageBook parent) {
    f_panel = new Composite(parent, SWT.NONE);
    f_panel.setBackground(f_panel.getDisplay().getSystemColor(SWT.COLOR_RED));
    return f_panel;
  }

  @Override
  protected void refreshMetricContentsFor(@Nullable JSureScanInfo scan, @Nullable ArrayList<IMetricDrop> drops) {
    final ArrayList<IMetricDrop> metricDrops = DropSeaUtility.filterMetricsToOneType(IMetricDrop.Metric.SLOC, drops);
    // TODO
  }

  @Override
  public void dispose() {
    // Nothing to do
  }
}
