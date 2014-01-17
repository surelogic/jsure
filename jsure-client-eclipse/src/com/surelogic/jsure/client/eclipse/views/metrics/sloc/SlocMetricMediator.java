package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import java.util.ArrayList;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.PageBook;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.ui.ColumnResizeListener;
import com.surelogic.common.ui.EclipseColorUtility;
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

  Label f_totalSlocScanned = null;
  Scale f_thresholdScale = null;
  Text f_thresholdLabel = null;

  TreeViewer f_treeViewer = null;

  @NonNull
  SlocViewContentProvider f_contentProvider = new SlocViewContentProvider(this);

  @Override
  protected Control initMetricDisplay(PageBook parent) {
    f_panel = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout();
    layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = layout.verticalSpacing = 0;
    f_panel.setLayout(layout);

    Composite top = new Composite(f_panel, SWT.BORDER);
    top.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    GridLayout topLayout = new GridLayout(4, false);
    top.setLayout(topLayout);

    f_totalSlocScanned = new Label(top, SWT.NONE);
    f_totalSlocScanned.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    f_totalSlocScanned.setForeground(f_totalSlocScanned.getDisplay().getSystemColor(SWT.COLOR_BLUE));

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
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
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

    final SashForm sash = new SashForm(f_panel, SWT.HORIZONTAL | SWT.SMOOTH);
    gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd.verticalIndent = 5;
    sash.setLayoutData(gd);
    sash.setLayout(new FillLayout());

    /*
     * Left-hand-side shows tree-table.
     */
    f_treeViewer = new TreeViewer(sash, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    f_treeViewer.setContentProvider(f_contentProvider);
    // f_treeViewer.setSorter(f_alphaLineSorter);
    f_treeViewer.getTree().setHeaderVisible(true);
    f_treeViewer.getTree().setLinesVisible(true);

    final TreeViewerColumn columnTree = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnTree.setLabelProvider(TREE);
    columnTree.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_COL_TREE_WIDTH));
    columnTree.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.METRIC_SLOC_COL_TREE_WIDTH));

    final TreeViewerColumn columnLineCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnLineCount.setLabelProvider(new MetricDataCellLabelProvider() {
      long getMetricValue(SlocElement metric) {
        return metric.getLineCount();
      }
    });
    columnLineCount.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_COL_LINE_COUNT_WIDTH));
    columnLineCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_SLOC_COL_LINE_COUNT_WIDTH));
    columnLineCount.getColumn().setText("SLOC");

    final TreeViewerColumn columnBlankLineCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnBlankLineCount.setLabelProvider(new MetricDataCellLabelProvider() {
      long getMetricValue(SlocElement metric) {
        return metric.getBlankLineCount();
      }
    });
    columnBlankLineCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_COL_BLANK_LINE_COUNT_WIDTH));
    columnBlankLineCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_SLOC_COL_BLANK_LINE_COUNT_WIDTH));
    columnBlankLineCount.getColumn().setText("Blank Lines");

    final TreeViewerColumn columnContainsCommentLineCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnContainsCommentLineCount.setLabelProvider(new MetricDataCellLabelProvider() {
      long getMetricValue(SlocElement metric) {
        return metric.getContainsCommentLineCount();
      }
    });
    columnContainsCommentLineCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_COL_CONTAINS_COMMENT_LINE_COUNT_WIDTH));
    columnContainsCommentLineCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_SLOC_COL_CONTAINS_COMMENT_LINE_COUNT_WIDTH));
    columnContainsCommentLineCount.getColumn().setText("Commented Lines");

    final TreeViewerColumn columnJavaDeclarationCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnJavaDeclarationCount.setLabelProvider(new MetricDataCellLabelProvider() {
      long getMetricValue(SlocElement metric) {
        return metric.getJavaDeclarationCount();
      }
    });
    columnJavaDeclarationCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_COL_JAVA_DECLARATION_COUNT_WIDTH));
    columnJavaDeclarationCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_SLOC_COL_JAVA_DECLARATION_COUNT_WIDTH));
    columnJavaDeclarationCount.getColumn().setText("Java Declarations");

    final TreeViewerColumn columnJavaStatementCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnJavaStatementCount.setLabelProvider(new MetricDataCellLabelProvider() {
      long getMetricValue(SlocElement metric) {
        return metric.getJavaStatementCount();
      }
    });
    columnJavaStatementCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_COL_JAVA_STATEMENT_COUNT_WIDTH));
    columnJavaStatementCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_SLOC_COL_JAVA_STATEMENT_COUNT_WIDTH));
    columnJavaStatementCount.getColumn().setText("Java Statements");

    final TreeViewerColumn columnSemicolonCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnSemicolonCount.setLabelProvider(new MetricDataCellLabelProvider() {
      long getMetricValue(SlocElement metric) {
        return metric.getSemicolonCount();
      }
    });
    columnSemicolonCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_COL_SEMICOLON_COUNT_WIDTH));
    columnSemicolonCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_SLOC_COL_SEMICOLON_COUNT_WIDTH));
    columnSemicolonCount.getColumn().setText("Semicolon Count");

    /*
     * Right-hand-side shows graph
     */
    final Composite rhs = new Composite(sash, SWT.BORDER);
    rhs.setLayout(new FillLayout());

    final Canvas canvas = new Canvas(rhs, SWT.DOUBLE_BUFFERED);
    canvas.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_RED));
    final SlocCanvasEventHandler handler = new SlocCanvasEventHandler(canvas);
    canvas.addPaintListener(handler);

    sash.setWeights(new int[] { EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_SASH_LHS_WEIGHT),
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_SASH_RHS_WEIGHT) });

    /*
     * When the left-hand-side composite is resized we'll just guess that the
     * sash is involved. Hopefully, this is conservative. This seems to be the
     * only way to do this.
     */
    f_treeViewer.getControl().addListener(SWT.Resize, new Listener() {
      @Override
      public void handleEvent(final Event event) {
        final int[] weights = sash.getWeights();
        if (weights != null && weights.length == 2) {
          EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_SLOC_SASH_LHS_WEIGHT, weights[0]);
          EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_SLOC_SASH_RHS_WEIGHT, weights[1]);
        }
      }
    });

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

  void updateTotal(long total) {
    f_totalSlocScanned.setText(SLUtility.toStringHumanWithCommas(total) + " SLOC scanned");
  }

  @Override
  protected void refreshMetricContentsFor(@Nullable JSureScanInfo scan, @Nullable ArrayList<IMetricDrop> drops) {
    final ArrayList<IMetricDrop> metricDrops = DropSeaUtility.filterMetricsToOneType(IMetricDrop.Metric.SLOC, drops);
    f_treeViewer.setInput(new SlocViewContentProvider.Input(scan, metricDrops));
  }

  @Override
  public void dispose() {
    // Nothing to do
  }

  static final StyledCellLabelProvider TREE = new StyledCellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {

      if (cell.getElement() instanceof SlocElement) {
        final SlocElement element = (SlocElement) cell.getElement();
        String label = element.getLabel();
        cell.setText(label);
        cell.setImage(element.getImage());
      } else
        super.update(cell);
    }
  };

  static abstract class MetricDataCellLabelProvider extends CellLabelProvider {

    /**
     * Implementations should extract the proper data for the column based upon
     * the passed metric.
     * 
     * @param metric
     *          a data containing Sloc metric element.
     * @return the metric data.
     */
    abstract long getMetricValue(SlocElement metric);

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof SlocElement) {
        final SlocElement element = (SlocElement) cell.getElement();
        final long data = getMetricValue(element);
        cell.setText(SLUtility.toStringHumanWithCommas(data));
        if (element instanceof SlocElementWithChildren)
          cell.setForeground(EclipseColorUtility.getSubtleTextColor());
      }
    }
  };

  private final class SlocCanvasEventHandler implements PaintListener {

    final Canvas f_canvas;

    public SlocCanvasEventHandler(Canvas canvas) {
      f_canvas = canvas;
    }

    public void paintControl(PaintEvent e) {
      final Rectangle clientArea = f_canvas.getClientArea();
      e.gc.setAntialias(SWT.ON);

      // Pie chart at top

      int chartDiameter = clientArea.width - 10;
      e.gc.setBackground(e.gc.getDevice().getSystemColor(SWT.COLOR_BLUE));
      e.gc.fillArc(5, 5, chartDiameter, chartDiameter, 5, 300);

      // Bar chart at bottom
    }
  }
}
