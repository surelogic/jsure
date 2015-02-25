package com.surelogic.jsure.client.eclipse.views.metrics.sloc;

import java.util.ArrayList;
import java.util.Comparator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

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
import com.surelogic.java.persistence.JSureScanInfo;
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

  public SlocMetricMediator(TabFolder folder, ViewPart view) {
    super(folder, view);
  }

  final ViewerSorter f_alphaSorter = new ViewerSorter() {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof SlocElement && e2 instanceof SlocElement) {
        return SlocElement.ALPHA.compare((SlocElement) e1, (SlocElement) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  /**
   * Compares elements by their SLOC based upon the current options.
   */
  public final Comparator<SlocElement> f_byMetricComparator = new Comparator<SlocElement>() {
    @Override
    public int compare(SlocElement o1, SlocElement o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      final long o1MetricValue;
      final long o2MetricValue;
      switch (f_options.getSelectedColumnTitleIndex()) {
      case 1: // Blank Lines
        o1MetricValue = o1.f_blankLineCount;
        o2MetricValue = o2.f_blankLineCount;
        break;
      case 2: // Commented Lines
        o1MetricValue = o1.f_containsCommentLineCount;
        o2MetricValue = o2.f_containsCommentLineCount;
        break;
      case 3: // Java Declarations
        o1MetricValue = o1.f_javaDeclarationCount;
        o2MetricValue = o2.f_javaDeclarationCount;
        break;
      case 4: // Java Statements
        o1MetricValue = o1.f_javaStatementCount;
        o2MetricValue = o2.f_javaStatementCount;
        break;
      case 5: // Semicolon Count
        o1MetricValue = o1.f_semicolonCount;
        o2MetricValue = o2.f_semicolonCount;
        break;
      default: // SLOC (0 and default)
        o1MetricValue = o1.f_lineCount;
        o2MetricValue = o2.f_lineCount;
        break;
      }
      if (f_options.f_thresholdShowAbove)
        return SLUtility.safeLongToInt(o2MetricValue - o1MetricValue);
      else
        return SLUtility.safeLongToInt(o1MetricValue - o2MetricValue);
    }
  };
  final ViewerSorter f_metricSorter = new ViewerSorter() {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof SlocElement && e2 instanceof SlocElement) {
        return f_byMetricComparator.compare((SlocElement) e1, (SlocElement) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  final ViewerFilter f_thresholdFilter = new ViewerFilter() {

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      // exception for scan
      if (element instanceof SlocElementScan)
        return true;

      if (element instanceof SlocElement) {
        return ((SlocElement) element).highlightDueToSlocThreshold(f_options);
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
  SlocViewContentProvider f_contentProvider = new SlocViewContentProvider(this);

  @NonNull
  final SlocOptions f_options = new SlocOptions();

  final Action f_actionExpand = new Action() {
    @Override
    public void run() {
      final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
      final Object o = s.getFirstElement();
      if (o != null) {
        f_treeViewer.getTree().setRedraw(false);
        f_treeViewer.expandToLevel(o, 5);
        f_treeViewer.getTree().setRedraw(true);
      }
    }
  };

  final Action f_actionCollapse = new Action() {
    @Override
    public void run() {
      final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
      final Object o = s.getFirstElement();
      if (o != null) {
        f_treeViewer.getTree().setRedraw(false);
        f_treeViewer.collapseToLevel(o, 1);
        f_treeViewer.getTree().setRedraw(true);
      }
    }
  };

  @Override
  protected Control initMetricDisplay(PageBook parent, ViewPart view) {
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

    f_thresholdLabel = new Text(top, SWT.SINGLE | SWT.RIGHT);
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
        final SlocElement element = getTreeViewerSelectionOrNull();
        if (element != null) {
          element.tryToOpenInJavaEditor();
          // open up the tree one more level
          if (!f_treeViewer.getExpandedState(element)) {
            f_treeViewer.expandToLevel(element, 1);
          }
        }
      }
    });

    final TreeViewerColumn columnTree = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnTree.setLabelProvider(TREE);
    columnTree.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_COL_TREE_WIDTH));
    columnTree.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.METRIC_SLOC_COL_TREE_WIDTH));

    int tableColumnTitleIndex = 0;
    final TreeViewerColumn columnLineCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnLineCount.setLabelProvider(new MetricDataCellLabelProvider() {
      long getMetricValue(SlocElement metric) {
        return metric.getLineCount();
      }
    });
    columnLineCount.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SLOC_COL_LINE_COUNT_WIDTH));
    columnLineCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_SLOC_COL_LINE_COUNT_WIDTH));
    columnLineCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

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
    columnBlankLineCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

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
    columnContainsCommentLineCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

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
    columnJavaDeclarationCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

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
    columnJavaStatementCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

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
    columnSemicolonCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

    f_actionExpand.setText(I18N.msg("jsure.eclipse.view.expand"));
    f_actionExpand.setToolTipText(I18N.msg("jsure.eclipse.view.expand.tip"));
    f_actionExpand.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_EXPAND_ALL));

    f_actionCollapse.setText(I18N.msg("jsure.eclipse.view.collapse"));
    f_actionCollapse.setToolTipText(I18N.msg("jsure.eclipse.view.collapse.tip"));
    f_actionCollapse.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));
    hookContextMenu(view);

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

  private void hookContextMenu(ViewPart view) {
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      @Override
      public void menuAboutToShow(final IMenuManager manager) {
        final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
        if (!s.isEmpty()) {
          manager.add(f_actionExpand);
          manager.add(f_actionCollapse);
        }
        manager.add(new Separator());
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
      }
    });
    Menu menu = menuMgr.createContextMenu(f_treeViewer.getControl());
    f_treeViewer.getControl().setMenu(menu);
    view.getSite().registerContextMenu(menuMgr, f_treeViewer);
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
      f_treeViewer.getTree().setSortColumn(f_treeViewer.getTree().getColumn(0));
      f_treeViewer.getTree().setSortDirection(SWT.DOWN);
    } else {
      f_treeViewer.getTree().setSortColumn(f_treeViewer.getTree().getColumn(f_options.getSelectedColumnTitleIndex() + 1));
      f_treeViewer.getTree().setSortDirection(f_options.getThresholdShowAbove() ? SWT.UP : SWT.DOWN);
    }
  }

  @Override
  protected void refreshMetricContentsFor(@Nullable JSureScanInfo scan, @Nullable ArrayList<IMetricDrop> drops) {
    final ArrayList<IMetricDrop> metricDrops = DropSeaUtility.filterMetricsToOneType(IMetricDrop.Metric.SLOC, drops);
    f_treeViewer.setInput(new SlocViewContentProvider.Input(scan, metricDrops));
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

      if (cell.getElement() instanceof SlocElement) {
        final SlocElement element = (SlocElement) cell.getElement();
        String label = element.getLabel();
        cell.setText(label);
        Image image = element.getImage();
        if (element.highlightDueToSlocThreshold(f_options)) {
          image = SLImages.getDecoratedImage(image,
              new ImageDescriptor[] { null, null, SLImages.getImageDescriptor(CommonImages.DECR_ASTERISK), null, null });
          if (element instanceof SlocElementLeaf) {
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
   * Handles all the counts columns of the metrics view
   */
  abstract class MetricDataCellLabelProvider extends CellLabelProvider {

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
        else {
          // only highlight leaf cells
          cell.setBackground(element.highlightDueToSlocThreshold(f_options) ? EclipseColorUtility.getDiffHighlightColorNewChanged()
              : null);
        }
      }
    }
  };

  @Nullable
  SlocElement getTreeViewerSelectionOrNull() {
    final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
    final Object o = s.getFirstElement();
    if (o instanceof SlocElement) {
      SlocElement element = (SlocElement) o;
      return element;
    }
    return null;
  }

  /**
   * Draws the metrics graph on the screen.
   */
  final class SlocCanvasEventHandler implements PaintListener {

    public void paintControl(PaintEvent e) {
      final SlocElement element = getTreeViewerSelectionOrNull();
      if (element != null) {
        final Rectangle clientArea = f_canvas.getClientArea();
        e.gc.setAntialias(SWT.ON);

        int fold = 0;

        e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

        /*
         * Title to point out what the graph is showing
         */
        String title = element.getLabel();
        Point titleExtent = e.gc.stringExtent(title);
        int xPos = (clientArea.width / 2) - (titleExtent.x / 2);
        if (xPos < 20)
          xPos = 20;
        e.gc.drawImage(element.getImage(), xPos - 18, 10 + (titleExtent.y / 2) - 8);
        e.gc.drawText(title, xPos, 10, true);

        fold += titleExtent.y + 20;

        final Color blankColor = EclipseColorUtility.getAnalogousScheme1Color2();
        final Color commentedColor = EclipseColorUtility.getAnalogousScheme1Color0();
        final Color nbncColor = e.gc.getDevice().getSystemColor(SWT.COLOR_WHITE);
        /*
         * Pie chart at the top
         */
        double degPerLine = 1.0 / (((double) element.f_lineCount) / 360.0);

        int chartDiameter = clientArea.width - 10;

        // Draw chart first
        e.gc.setBackground(nbncColor);
        e.gc.fillOval(5, fold + 5, chartDiameter, chartDiameter);
        if (element.f_blankLineCount > 0) {
          int arcBlank = (int) ((double) element.f_blankLineCount * degPerLine);
          e.gc.setBackground(blankColor);
          e.gc.fillArc(5, fold + 5, chartDiameter, chartDiameter, 90, arcBlank);
        }
        if (element.f_containsCommentLineCount > 0) {
          int arcCommented = (int) ((double) element.f_containsCommentLineCount * degPerLine);
          e.gc.setBackground(commentedColor);
          e.gc.fillArc(5, fold + 5, chartDiameter, chartDiameter, 90, -arcCommented);
        }
        e.gc.drawOval(5, fold + 5, chartDiameter, chartDiameter);

        fold += chartDiameter + 10;
        String s = SLUtility.toStringHumanWithCommas(element.f_lineCount) + " SLOC";
        Point txtExtent = e.gc.stringExtent(s);
        int slocTxtWidth = txtExtent.x;
        e.gc.drawText(s, (chartDiameter / 2) + 5 - (slocTxtWidth / 2), fold, true);

        fold += txtExtent.y + 5;
        s = SLUtility.toStringHumanWithCommas(element.f_blankLineCount) + " blank";
        txtExtent = e.gc.stringExtent(s);
        e.gc.setBackground(blankColor);
        e.gc.fillRectangle(5, fold, txtExtent.y, txtExtent.y);
        e.gc.drawRectangle(5, fold, txtExtent.y - 1, txtExtent.y - 1);
        e.gc.drawText(s, 10 + txtExtent.y, fold, true);

        fold += txtExtent.y + 5;
        s = SLUtility.toStringHumanWithCommas(element.f_containsCommentLineCount) + " commented";
        txtExtent = e.gc.stringExtent(s);
        e.gc.setBackground(commentedColor);
        e.gc.fillRectangle(5, fold, txtExtent.y, txtExtent.y);
        e.gc.drawRectangle(5, fold, txtExtent.y - 1, txtExtent.y - 1);
        e.gc.drawText(s, 10 + txtExtent.y, fold, true);

        fold += txtExtent.y + 5;
        final long nonBlankNonCommentedLineCount = element.f_lineCount
            - (element.f_blankLineCount + element.f_containsCommentLineCount);
        s = SLUtility.toStringHumanWithCommas(nonBlankNonCommentedLineCount) + " non-blank/non-commented";
        txtExtent = e.gc.stringExtent(s);
        e.gc.setBackground(nbncColor);
        e.gc.fillRectangle(5, fold, txtExtent.y, txtExtent.y);
        e.gc.drawRectangle(5, fold, txtExtent.y - 1, txtExtent.y - 1);
        e.gc.drawText(s, 10 + txtExtent.y, fold, true);

        fold += txtExtent.y + 20;

        /*
         * Bar chart at the bottom
         */
        int top = fold;
        int bottom = clientArea.height - 10;
        /*
         * Only do this if we have room
         */
        if (top >= bottom - 80)
          return;
        int left = 5;
        int right = clientArea.width - 5;
        int pad = 5;
        int barWidth = (right - left) / 4;
        double pxlPerUnit = ((double) bottom - top) / ((double) element.f_lineCount);

        e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_BLACK));

        e.gc.drawLine(left, bottom, right, bottom);

        // Declarations for bar chart labeling
        String txt;
        Point extent;
        int x, y;
        Transform tr;

        // SLOC
        int barX = left + pad;
        int height = (int) (element.f_lineCount * pxlPerUnit);
        e.gc.setBackground(e.gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
        e.gc.fillRectangle(barX, bottom - height, barWidth - pad - pad, height);
        e.gc.drawRectangle(barX, bottom - height, barWidth - pad - pad, height);

        if (element.f_lineCount > 0) {
          txt = "SLOC";
          extent = e.gc.stringExtent(txt);
          x = barX + ((barWidth - pad - pad) / 2) - (extent.y / 2);
          y = (bottom - height / 2) + (extent.x / 2);
          tr = new Transform(e.gc.getDevice());
          tr.translate(x, y);
          tr.rotate(-90);
          e.gc.setTransform(tr);
          e.gc.drawText(txt, 0, 0, true);
          e.gc.setTransform(null);
          tr.dispose();

          txt = SLUtility.toStringHumanWithCommas(element.f_lineCount);
          extent = e.gc.stringExtent(txt);
          x = barX + ((barWidth - pad - pad) / 2) - (extent.x / 2);
          y = bottom - height - extent.y;
          e.gc.drawText(txt, x, y, true);
        }

        // Java Declarations
        barX += barWidth;
        height = (int) (element.f_javaDeclarationCount * pxlPerUnit);
        e.gc.setBackground(EclipseColorUtility.getAnalogousScheme1Color3());
        e.gc.fillRectangle(barX, bottom - height, barWidth - pad - pad, height);
        e.gc.drawRectangle(barX, bottom - height, barWidth - pad - pad, height);

        if (element.f_javaDeclarationCount > 0) {
          txt = "Decls";
          extent = e.gc.stringExtent(txt);
          x = barX + ((barWidth - pad - pad) / 2) - (extent.y / 2);
          y = (bottom - height / 2) + (extent.x / 2);
          tr = new Transform(e.gc.getDevice());
          tr.translate(x, y);
          tr.rotate(-90);
          e.gc.setTransform(tr);
          e.gc.drawText(txt, 0, 0, true);
          e.gc.setTransform(null);
          tr.dispose();

          txt = SLUtility.toStringHumanWithCommas(element.f_javaDeclarationCount);
          extent = e.gc.stringExtent(txt);
          x = barX + ((barWidth - pad - pad) / 2) - (extent.x / 2);
          y = bottom - height - extent.y;
          e.gc.drawText(txt, x, y, true);
        }

        // Java Statements
        barX += barWidth;
        height = (int) (element.f_javaStatementCount * pxlPerUnit);
        e.gc.setBackground(EclipseColorUtility.getAnalogousScheme1Color1());
        e.gc.fillRectangle(barX, bottom - height, barWidth - pad - pad, height);
        e.gc.drawRectangle(barX, bottom - height, barWidth - pad - pad, height);

        if (element.f_javaStatementCount > 0) {
          txt = "Stmts";
          extent = e.gc.stringExtent(txt);
          x = barX + ((barWidth - pad - pad) / 2) - (extent.y / 2);
          y = (bottom - height / 2) + (extent.x / 2);
          tr = new Transform(e.gc.getDevice());
          tr.translate(x, y);
          tr.rotate(-90);
          e.gc.setTransform(tr);
          e.gc.drawText(txt, 0, 0, true);
          e.gc.setTransform(null);
          tr.dispose();

          txt = SLUtility.toStringHumanWithCommas(element.f_javaStatementCount);
          extent = e.gc.stringExtent(txt);
          x = barX + ((barWidth - pad - pad) / 2) - (extent.x / 2);
          y = bottom - height - extent.y;
          e.gc.drawText(txt, x, y, true);
        }

        // Semicolon Count
        barX += barWidth;
        height = (int) (element.f_semicolonCount * pxlPerUnit);
        e.gc.setBackground(EclipseColorUtility.getAnalogousScheme1Color4());
        e.gc.fillRectangle(barX, bottom - height, barWidth - pad - pad, height);
        e.gc.drawRectangle(barX, bottom - height, barWidth - pad - pad, height);

        if (element.f_semicolonCount > 0) {
          txt = ";";
          extent = e.gc.stringExtent(txt);
          x = barX + ((barWidth - pad - pad) / 2) - (extent.y / 2);
          y = (bottom - height / 2) + (extent.x / 2);
          tr = new Transform(e.gc.getDevice());
          tr.translate(x, y);
          tr.rotate(-90);
          e.gc.setTransform(tr);
          e.gc.drawText(txt, 0, 0, true);
          e.gc.setTransform(null);
          tr.dispose();

          txt = SLUtility.toStringHumanWithCommas(element.f_semicolonCount);
          extent = e.gc.stringExtent(txt);
          x = barX + ((barWidth - pad - pad) / 2) - (extent.x / 2);
          y = bottom - height - extent.y;
          e.gc.drawText(txt, x, y, true);
        }
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
    if (value)
      f_treeViewer.addFilter(f_thresholdFilter);
    else
      f_treeViewer.removeFilter(f_thresholdFilter);
    f_treeViewer.refresh();
  }
}
