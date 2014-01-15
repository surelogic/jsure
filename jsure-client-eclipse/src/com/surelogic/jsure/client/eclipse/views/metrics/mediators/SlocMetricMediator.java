package com.surelogic.jsure.client.eclipse.views.metrics.mediators;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.PageBook;

import com.surelogic.Nullable;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.dropsea.DropSeaUtility;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.views.metrics.AbstractScanMetricMediator;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

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

  Scale f_thresholdScale = null;
  Text f_thresholdLabel = null;

  @Override
  protected Control initMetricDisplay(PageBook parent) {
    f_panel = new Composite(parent, SWT.NONE);
    f_panel.setBackground(f_panel.getDisplay().getSystemColor(SWT.COLOR_RED));

    GridLayout layout = new GridLayout();
    layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = layout.verticalSpacing = 0;
    f_panel.setLayout(layout);

    Composite top = new Composite(f_panel, SWT.BORDER);
    // top.setBackground(f_panel.getDisplay().getSystemColor(SWT.COLOR_BLUE));
    top.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    GridLayout topLayout = new GridLayout(4, false);
    top.setLayout(topLayout);

    Label total = new Label(top, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
    gd.widthHint = 100;
    total.setLayoutData(gd);
    total.setText("TOTAL");

    Label threshold = new Label(top, SWT.RIGHT);
    threshold.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    threshold.setText("Highlight Threshold (SLOC):");

    f_thresholdScale = new Scale(top, SWT.NONE);
    f_thresholdScale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    f_thresholdScale.setMinimum(1);
    f_thresholdScale.setMaximum(5000);
    f_thresholdScale.setPageIncrement(100);
    int savedThreshold = EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_SLOC_THRESHOLD);
    if (savedThreshold < f_thresholdScale.getMinimum())
      savedThreshold = f_thresholdScale.getMinimum();
    if (savedThreshold > f_thresholdScale.getMaximum())
      savedThreshold = f_thresholdScale.getMaximum();
    f_thresholdScale.setSelection(savedThreshold);

    f_thresholdScale.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateThresholdScale();
      }
    });

    f_thresholdLabel = new Text(top, SWT.SINGLE);
    gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
    gd.widthHint = 80;
    f_thresholdLabel.setLayoutData(gd);
    f_thresholdLabel.setText(SLUtility.toStringHumanWithCommas(savedThreshold));

    f_thresholdLabel.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        updateThresholdFromTextIfSafe();
      }
    });
    f_thresholdLabel.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (e.keyCode == SWT.CR)
          updateThresholdFromTextIfSafe();
      }
    });

    TreeViewer f_treeViewer = new TreeViewer(f_panel, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    // f_treeViewer.setContentProvider(f_contentProvider);
    // f_treeViewer.setSorter(f_alphaLineSorter);
    f_treeViewer.getTree().setHeaderVisible(true);
    f_treeViewer.getTree().setLinesVisible(true);
    f_treeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    return f_panel;
  }

  private void updateThresholdFromTextIfSafe() {
    String proposedThresholdString = f_thresholdLabel.getText();
    // remove all commas
    proposedThresholdString = proposedThresholdString.replaceFirst(",", "");
    try {
      int threshold = Integer.parseInt(proposedThresholdString);
      if (threshold < f_thresholdScale.getMinimum())
        threshold = f_thresholdScale.getMinimum();
      if (threshold > f_thresholdScale.getMaximum())
        threshold = f_thresholdScale.getMaximum();
      f_thresholdScale.setSelection(threshold);
    } catch (NumberFormatException ignore) {
      // not a number put back to scale value by the call below
    }
    updateThresholdScale();
  }

  private void updateThresholdScale() {
    int threshold = f_thresholdScale.getSelection();
    f_thresholdLabel.setText(SLUtility.toStringHumanWithCommas(threshold));
    EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_VIEW_SLOC_THRESHOLD, threshold);
  }

  @Override
  protected void refreshMetricContentsFor(@Nullable JSureScanInfo scan, @Nullable ArrayList<IMetricDrop> drops) {
    final ArrayList<IMetricDrop> metricDrops = DropSeaUtility.filterMetricsToOneType(IMetricDrop.Metric.SLOC, drops);
    System.out.println("Got " + metricDrops.size() + " SLOC metric drops.");
    for (IMetricDrop drop : metricDrops) {
      System.out.println("-----");
      System.out.println(drop.getJavaRef().toString());
      System.out.println("  line count = " + drop.getMetricInfoAsInt(IMetricDrop.SLOC_LINE_COUNT, 0));
      System.out.println("     ; count = " + drop.getMetricInfoAsInt(IMetricDrop.SLOC_SEMICOLON_COUNT, 0));
      System.out.println(" blank lines = " + drop.getMetricInfoAsInt(IMetricDrop.SLOC_BLANK_LINE_COUNT, 0));
    }
  }

  @Override
  public void dispose() {
    // Nothing to do
  }
}
