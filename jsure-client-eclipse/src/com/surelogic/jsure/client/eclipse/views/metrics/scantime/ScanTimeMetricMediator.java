package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import java.util.ArrayList;
import java.util.Comparator;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.part.PageBook;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.CommonImages;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.ColumnResizeListener;
import com.surelogic.common.ui.EclipseColorUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.dropsea.DropSeaUtility;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.views.metrics.AbstractScanMetricMediator;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

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

  final ViewerSorter f_alphaSorter = new ViewerSorter() {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof ScanTimeElement && e2 instanceof ScanTimeElement) {
        return ScanTimeElement.ALPHA.compare((ScanTimeElement) e1, (ScanTimeElement) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  /**
   * Compares elements by their duration based upon the current options.
   */
  public final Comparator<ScanTimeElement> f_byMetricComparator = new Comparator<ScanTimeElement>() {
    @Override
    public int compare(ScanTimeElement o1, ScanTimeElement o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      final long o1MetricValue = o1.getDurationNs(f_options);
      final long o2MetricValue = o2.getDurationNs(f_options);
      if (f_options.f_thresholdShowAbove)
        return SLUtility.safeLongToInt(o2MetricValue - o1MetricValue);
      else
        return SLUtility.safeLongToInt(o1MetricValue - o2MetricValue);
    }
  };
  final ViewerSorter f_metricSorter = new ViewerSorter() {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof ScanTimeElement && e2 instanceof ScanTimeElement) {
        return f_byMetricComparator.compare((ScanTimeElement) e1, (ScanTimeElement) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  final ViewerFilter f_thresholdFilter = new ViewerFilter() {

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      // exception for scan
      if (element instanceof ScanTimeElementScan)
        return true;

      if (element instanceof ScanTimeElement) {
        return ((ScanTimeElement) element).highlightDueToSlocThreshold(f_options);
      }
      return false;
    }
  };

  Label f_totalSlocScanned = null;
  Scale f_thresholdScale = null;
  Text f_thresholdLabel = null;

  final String[] f_columnTitles = new String[] { "SLOC", "Blank Lines", "Commented Lines", "Java Declarations", "Java Statements",
      "Semicolon Count" };

  TreeViewer f_treeViewer = null;
  Canvas f_canvas = null;

  @NonNull
  ScanTimeViewContentProvider f_contentProvider = new ScanTimeViewContentProvider(this);

  @NonNull
  final ScanTimeOptions f_options = new ScanTimeOptions();

  @Override
  protected Control initMetricDisplay(PageBook parent) {
    final Composite panel = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout();
    layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = layout.verticalSpacing = 0;
    panel.setLayout(layout);

    Composite top = new Composite(panel, SWT.BORDER);
    top.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    GridLayout topLayout = new GridLayout(6, false);
    top.setLayout(topLayout);

    f_totalSlocScanned = new Label(top, SWT.NONE);
    f_totalSlocScanned.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    f_totalSlocScanned.setForeground(f_totalSlocScanned.getDisplay().getSystemColor(SWT.COLOR_BLUE));

    final Combo countCombo = new Combo(top, SWT.READ_ONLY);
    countCombo.setItems(f_columnTitles);
    countCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    int savedComboChoice = EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_SLOC_COMBO_SELECTED_COLUMN);
    if (savedComboChoice < 0 || savedComboChoice >= f_columnTitles.length)
      savedComboChoice = 0; // if a bad value was saved
    countCombo.select(savedComboChoice);
    f_options.setSelectedColumnTitleIndex(savedComboChoice);
    countCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        final int value = countCombo.getSelectionIndex();
        EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_VIEW_SLOC_COMBO_SELECTED_COLUMN, value);
        if (f_options.setSelectedColumnTitleIndex(value)) {
          fixSortingIndicatorOnTreeTable();
          f_treeViewer.refresh();
        }
      }
    });

    final ToolBar thresholdToolBar = new ToolBar(top, SWT.FLAT);
    thresholdToolBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    final ToolItem aboveThresholdItem = new ToolItem(thresholdToolBar, SWT.RADIO);
    aboveThresholdItem.setImage(SLImages.getImage(CommonImages.IMG_THRESHOLD_ABOVE));
    aboveThresholdItem.setToolTipText(I18N.msg("jsure.eclipse.metrics.threshold_above.tip"));
    final ToolItem belowThresholdItem = new ToolItem(thresholdToolBar, SWT.RADIO);
    belowThresholdItem.setImage(SLImages.getImage(CommonImages.IMG_THRESHOLD_BELOW));
    belowThresholdItem.setToolTipText(I18N.msg("jsure.eclipse.metrics.threshold_below.tip"));
    boolean showAbove = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.METRIC_VIEW_SLOC_THRESHOLD_SHOW_ABOVE);
    if (showAbove)
      aboveThresholdItem.setSelection(true);
    else
      belowThresholdItem.setSelection(true);
    f_options.setThresholdShowAbove(showAbove);
    final SelectionAdapter radioSelection = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean showAbove = aboveThresholdItem.getSelection();
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.METRIC_VIEW_SLOC_THRESHOLD_SHOW_ABOVE, showAbove);
        if (f_options.setThresholdShowAbove(showAbove)) {
          fixSortingIndicatorOnTreeTable();
          f_treeViewer.refresh();
        }
      }
    };
    aboveThresholdItem.addSelectionListener(radioSelection);
    belowThresholdItem.addSelectionListener(radioSelection);

    final Label threshold = new Label(top, SWT.RIGHT);
    threshold.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    threshold.setText("Highlight Threshold:");

    f_thresholdScale = new Scale(top, SWT.NONE);
    f_thresholdScale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    f_thresholdScale.setMinimum(1);
    f_thresholdScale.setMaximum(3000);
    f_thresholdScale.setPageIncrement(100);
    int savedThreshold = EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_SLOC_THRESHOLD);
    if (savedThreshold < f_thresholdScale.getMinimum())
      savedThreshold = f_thresholdScale.getMinimum();
    if (savedThreshold > f_thresholdScale.getMaximum())
      savedThreshold = f_thresholdScale.getMaximum();
    f_thresholdScale.setSelection(savedThreshold);
    f_options.setThreshold(savedThreshold);
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

    final SashForm sash = new SashForm(panel, SWT.HORIZONTAL | SWT.SMOOTH);
    gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    gd.verticalIndent = 5;
    sash.setLayoutData(gd);
    sash.setLayout(new FillLayout());

    /*
     * Left-hand-side shows tree-table.
     */
    f_treeViewer = new TreeViewer(sash, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    f_treeViewer.setContentProvider(f_contentProvider);
    f_treeViewer.addFilter(f_thresholdFilter);
    f_treeViewer.getTree().setHeaderVisible(true);
    f_treeViewer.getTree().setLinesVisible(true);
    f_treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        f_canvas.redraw();
      }
    });
    f_treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        final ScanTimeElement element = getTreeViewerSelectionOrNull();
        if (element != null) {
          if (element instanceof ScanTimeElementLeaf) {
            ((ScanTimeElementLeaf) element).tryToOpenInJavaEditor();
          } else {
            // open up the tree one more level
            if (!f_treeViewer.getExpandedState(element)) {
              f_treeViewer.expandToLevel(element, 1);
            }
          }
        }
      }
    });

    final TreeViewerColumn columnTree = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnTree.setLabelProvider(TREE);
    columnTree.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SCAN_TIME_COL_TREE_WIDTH));
    columnTree.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.METRIC_SCAN_TIME_COL_TREE_WIDTH));

    final TreeViewerColumn columnDuration = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnDuration.setLabelProvider(DURATION);
    columnDuration.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SCAN_TIME_COL_DURATION_WIDTH));
    columnDuration.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_SCAN_TIME_COL_DURATION_WIDTH));
    columnDuration.getColumn().setText("Duration (ns)");

    fixSortingIndicatorOnTreeTable();

    /*
     * Right-hand-side shows graph
     */
    final Composite rhs = new Composite(sash, SWT.BORDER);
    rhs.setLayout(new FillLayout());

    f_canvas = new Canvas(rhs, SWT.DOUBLE_BUFFERED);
    final SlocCanvasEventHandler handler = new SlocCanvasEventHandler();
    f_canvas.addPaintListener(handler);

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

    return panel;
  }

  void updateThresholdFromTextIfSafe() {
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

  void updateThresholdScale() {
    int threshold = f_thresholdScale.getSelection();
    f_thresholdLabel.setText(SLUtility.toStringHumanWithCommas(threshold));
    EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_VIEW_SLOC_THRESHOLD, threshold);
    if (f_options.setThreshold(threshold))
      f_treeViewer.refresh();
  }

  void updateTotal(long total) {
    f_totalSlocScanned.setText(SLUtility.toStringHumanWithCommas(total) + " SLOC scanned");
  }

  void fixSortingIndicatorOnTreeTable() {
    if (f_treeViewer.getSorter() == f_alphaSorter) {
      f_treeViewer.getTree().setSortColumn(null);
    } else {
      f_treeViewer.getTree().setSortColumn(f_treeViewer.getTree().getColumn(1));
      f_treeViewer.getTree().setSortDirection(f_options.getThresholdShowAbove() ? SWT.UP : SWT.DOWN);
    }
  }

  @Override
  protected void refreshMetricContentsFor(@Nullable JSureScanInfo scan, @Nullable ArrayList<IMetricDrop> drops) {
    final ArrayList<IMetricDrop> metricDrops = DropSeaUtility.filterMetricsToOneType(IMetricDrop.Metric.SCAN_TIME, drops);
    f_treeViewer.setInput(new ScanTimeViewContentProvider.Input(scan, metricDrops));
    f_treeViewer.expandToLevel(3);
  }

  @Override
  public void dispose() {
    // Nothing to do
  }

  /**
   * Handles the tree portion of the metrics view
   */
  final CellLabelProvider TREE = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {

      if (cell.getElement() instanceof ScanTimeElement) {
        final ScanTimeElement element = (ScanTimeElement) cell.getElement();
        String label = element.getLabel();
        cell.setText(label);
        Image image = element.getImage();
        if (element.highlightDueToSlocThreshold(f_options)) {
          image = SLImages.getDecoratedImage(image, new ImageDescriptor[] {
              SLImages.getImageDescriptor(CommonImages.DECR_ASTERISK), null, null, null, null });
          if (element instanceof ScanTimeElementLeaf) {
            cell.setBackground(EclipseColorUtility.getDiffHighlightColorNewChanged());
          }
        } else {
          cell.setBackground(null);
        }
        cell.setImage(image);
      }
    }
  };

  /**
   * Handles the duration column of the metrics view
   */
  final CellLabelProvider DURATION = new CellLabelProvider() {

    @Override
    public void update(ViewerCell cell) {

      if (cell.getElement() instanceof ScanTimeElementLeaf) {
        final ScanTimeElementLeaf element = (ScanTimeElementLeaf) cell.getElement();
        String label = element.getDurationAsHumanReadableString(f_options);
        cell.setText(label);
        Image image = element.getImage();
        if (element.highlightDueToSlocThreshold(f_options)) {
          image = SLImages.getDecoratedImage(image, new ImageDescriptor[] {
              SLImages.getImageDescriptor(CommonImages.DECR_ASTERISK), null, null, null, null });
          if (element instanceof ScanTimeElementLeaf) {
            cell.setBackground(EclipseColorUtility.getDiffHighlightColorNewChanged());
          }
        } else {
          cell.setBackground(null);
        }
        cell.setImage(image);
      }
    }
  };

  @Nullable
  ScanTimeElement getTreeViewerSelectionOrNull() {
    final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
    final Object o = s.getFirstElement();
    if (o instanceof ScanTimeElement) {
      ScanTimeElement element = (ScanTimeElement) o;
      return element;
    }
    return null;
  }

  /**
   * Draws the metrics graph on the screen.
   */
  final class SlocCanvasEventHandler implements PaintListener {

    public void paintControl(PaintEvent e) {
      final ScanTimeElement element = getTreeViewerSelectionOrNull();
      if (element != null) {
        final Rectangle clientArea = f_canvas.getClientArea();
        e.gc.setAntialias(SWT.ON);

        int fold = 0;

        e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

      }
    }
  }

  @Override
  public void takeActionCollapseAll() {
    f_treeViewer.collapseAll();
    f_treeViewer.expandToLevel(3);
  }

  @Override
  public void takeActionUseAlphaSort(boolean value) {
    if (value)
      f_treeViewer.setSorter(f_alphaSorter);
    else
      f_treeViewer.setSorter(f_metricSorter);
    fixSortingIndicatorOnTreeTable();
  }

  @Override
  public void takeActionUseFilter(boolean value) {
    f_options.setFilterResultsByThreshold(value);
    if (value)
      f_treeViewer.addFilter(f_thresholdFilter);
    else
      f_treeViewer.removeFilter(f_thresholdFilter);
  }
}
