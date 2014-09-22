package com.surelogic.jsure.client.eclipse.views.metrics.statewrt;

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
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Point;
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
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.views.metrics.AbstractScanMetricMediator;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

/**
 * Displays information about verifying analysis performance during a JSure
 * scan.
 * <p>
 * The information is obtained from
 * {@link IMetricDrop.Metric#STATE_WRT_CONCURRENCY} metric drops.
 */
public final class StateWrtMetricMediator extends AbstractScanMetricMediator {

  @Override
  protected String getMetricLabel() {
    return "Concurrency Policy";
  }

  public StateWrtMetricMediator(TabFolder folder, ViewPart view) {
    super(folder, view);
  }

  final ViewerSorter f_alphaSorter = new ViewerSorter() {

    @Override
    public int compare(Viewer viewer, Object e1, Object e2) {
      if (e1 instanceof StateWrtElement && e2 instanceof StateWrtElement) {
        return StateWrtElement.ALPHA.compare((StateWrtElement) e1, (StateWrtElement) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  /**
   * Compares elements by their based upon the current options.
   */
  public final Comparator<StateWrtElement> f_byMetricComparator = new Comparator<StateWrtElement>() {
    @Override
    public int compare(StateWrtElement o1, StateWrtElement o2) {
      if (o1 == null && o2 == null)
        return 0;
      if (o1 == null)
        return -1;
      if (o2 == null)
        return 1;

      final long o1MetricValue;
      final long o2MetricValue;
      switch (f_options.getSelectedColumnTitleIndex()) {
      case 1: // No declared policy
        o1MetricValue = o1.getOtherFieldCount();
        o2MetricValue = o2.getOtherFieldCount();
        break;
      case 2: // @Immutable
        o1MetricValue = o1.getImmutableFieldCount();
        o2MetricValue = o2.getImmutableFieldCount();
        break;
      case 3: // @ThreadSafe
        o1MetricValue = o1.getThreadSafeFieldCount();
        o2MetricValue = o2.getThreadSafeFieldCount();
        break;
      case 4: // @RegionLock/@GuardedBy
        o1MetricValue = o1.getLockProtectedFieldCount();
        o2MetricValue = o2.getLockProtectedFieldCount();
        break;
      case 5: // @ThreadConfined
        o1MetricValue = o1.getThreadConfinedFieldCount();
        o2MetricValue = o2.getThreadConfinedFieldCount();
        break;
      case 6: // @NotThreadSafe
        o1MetricValue = o1.getNotThreadSafeFieldCount();
        o2MetricValue = o2.getNotThreadSafeFieldCount();
        break;
      default: // Total (0 and default)
        o1MetricValue = o1.getFieldCountTotal();
        o2MetricValue = o2.getFieldCountTotal();
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
      if (e1 instanceof StateWrtElement && e2 instanceof StateWrtElement) {
        return f_byMetricComparator.compare((StateWrtElement) e1, (StateWrtElement) e2);
      }
      return super.compare(viewer, e1, e2);
    }
  };

  final ViewerFilter f_thresholdFilter = new ViewerFilter() {

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      // exception for scan
      if (element instanceof StateWrtElementScan)
        return true;

      if (element instanceof StateWrtElement) {
        return ((StateWrtElement) element).highlightDueToThreshold(f_options);
      }
      return false;
    }
  };

  Label f_modelledStateTotal = null;
  Scale f_thresholdScale = null;
  Text f_thresholdLabel = null;

  final String[] f_columnTitles = new String[] { "Declared Fields", "No Policy", "@Immutable", "@ThreadSafe",
      "@RegionLock/@GuardedBy", "@ThreadConfined", "@NotThreadSafe" };

  TreeViewer f_treeViewer = null;
  Canvas f_canvas = null;

  @NonNull
  StateWrtViewContentProvider f_contentProvider = new StateWrtViewContentProvider(this);

  @NonNull
  final StateWrtOptions f_options = new StateWrtOptions();

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

    f_modelledStateTotal = new Label(top, SWT.NONE);
    f_modelledStateTotal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    f_modelledStateTotal.setForeground(f_modelledStateTotal.getDisplay().getSystemColor(SWT.COLOR_BLUE));

    final Combo countCombo = new Combo(top, SWT.READ_ONLY);
    countCombo.setItems(f_columnTitles);
    countCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    int savedComboChoice = EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_COMBO_SELECTED_COLUMN);
    if (savedComboChoice < 0 || savedComboChoice >= f_columnTitles.length)
      savedComboChoice = 0; // if a bad value was saved
    countCombo.select(savedComboChoice);
    f_options.setSelectedColumnTitleIndex(savedComboChoice);
    countCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        final int value = countCombo.getSelectionIndex();
        EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_COMBO_SELECTED_COLUMN, value);
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
    boolean showAbove = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_THRESHOLD_SHOW_ABOVE);
    if (showAbove)
      aboveThresholdItem.setSelection(true);
    else
      belowThresholdItem.setSelection(true);
    f_options.setThresholdShowAbove(showAbove);
    final SelectionAdapter radioSelection = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean showAbove = aboveThresholdItem.getSelection();
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_THRESHOLD_SHOW_ABOVE, showAbove);
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
    f_thresholdScale.setMaximum(50);
    f_thresholdScale.setPageIncrement(10);
    int savedThreshold = EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_THRESHOLD);
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
        final StateWrtElement element = getTreeViewerSelectionOrNull();
        if (element != null) {
          // open up the tree one more level
          if (!f_treeViewer.getExpandedState(element)) {
            f_treeViewer.expandToLevel(element, 1);
          }
        }
      }
    });

    final TreeViewerColumn columnTree = new TreeViewerColumn(f_treeViewer, SWT.LEFT);
    columnTree.setLabelProvider(TREE);
    columnTree.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_TREE_WIDTH));
    columnTree.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_TREE_WIDTH));

    int tableColumnTitleIndex = 0;
    final TreeViewerColumn fieldCountTotal = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    fieldCountTotal.setLabelProvider(new MetricDataCellLabelProvider() {
      int getMetricValue(StateWrtElement metric) {
        return metric.getFieldCountTotal();
      }
    });
    fieldCountTotal.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_FIELD_COUNT_TOTAL_WIDTH));
    fieldCountTotal.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_FIELD_COUNT_TOTAL_WIDTH));
    fieldCountTotal.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

    final TreeViewerColumn noPolicyFieldCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    noPolicyFieldCount.setLabelProvider(new MetricDataCellLabelProvider() {
      int getMetricValue(StateWrtElement metric) {
        return metric.getOtherFieldCount();
      }
    });
    noPolicyFieldCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_OTHER_FIELD_COUNT_WIDTH));
    noPolicyFieldCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_OTHER_FIELD_COUNT_WIDTH));
    noPolicyFieldCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

    final TreeViewerColumn immutableFieldCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    immutableFieldCount.setLabelProvider(new MetricDataCellLabelProvider() {
      int getMetricValue(StateWrtElement metric) {
        return metric.getImmutableFieldCount();
      }
    });
    immutableFieldCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_IMMUTABLE_FIELD_COUNT_WIDTH));
    immutableFieldCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_IMMUTABLE_FIELD_COUNT_WIDTH));
    immutableFieldCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

    final TreeViewerColumn threadSafeFieldCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    threadSafeFieldCount.setLabelProvider(new MetricDataCellLabelProvider() {
      int getMetricValue(StateWrtElement metric) {
        return metric.getThreadSafeFieldCount();
      }
    });
    threadSafeFieldCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_THREADSAFE_FIELD_COUNT_WIDTH));
    threadSafeFieldCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_THREADSAFE_FIELD_COUNT_WIDTH));
    threadSafeFieldCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

    final TreeViewerColumn lockProtectedFieldCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    lockProtectedFieldCount.setLabelProvider(new MetricDataCellLabelProvider() {
      int getMetricValue(StateWrtElement metric) {
        return metric.getLockProtectedFieldCount();
      }
    });
    lockProtectedFieldCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_LOCK_PROTECTED_FIELD_COUNT_WIDTH));
    lockProtectedFieldCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_LOCK_PROTECTED_FIELD_COUNT_WIDTH));
    lockProtectedFieldCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

    final TreeViewerColumn threadConfinedFieldCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    threadConfinedFieldCount.setLabelProvider(new MetricDataCellLabelProvider() {
      int getMetricValue(StateWrtElement metric) {
        return metric.getThreadConfinedFieldCount();
      }
    });
    threadConfinedFieldCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_THREADCONFINED_FIELD_COUNT_WIDTH));
    threadConfinedFieldCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_THREADCONFINED_FIELD_COUNT_WIDTH));
    threadConfinedFieldCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

    final TreeViewerColumn notThreadSafeFieldCount = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    notThreadSafeFieldCount.setLabelProvider(new MetricDataCellLabelProvider() {
      int getMetricValue(StateWrtElement metric) {
        return metric.getNotThreadSafeFieldCount();
      }
    });
    notThreadSafeFieldCount.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_NOTTHREADSAFE_FIELD_COUNT_WIDTH));
    notThreadSafeFieldCount.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_NOTTHREADSAFE_FIELD_COUNT_WIDTH));
    notThreadSafeFieldCount.getColumn().setText(f_columnTitles[tableColumnTitleIndex++]);

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
    final StateWrtCanvasEventHandler handler = new StateWrtCanvasEventHandler();
    f_canvas.addPaintListener(handler);

    sash.setWeights(new int[] { EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_SASH_LHS_WEIGHT),
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_SASH_RHS_WEIGHT) });

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
          EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_SASH_LHS_WEIGHT, weights[0]);
          EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_SASH_RHS_WEIGHT, weights[1]);
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
    EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_VIEW_STATEWRT_THRESHOLD, threshold);
    if (f_options.setThreshold(threshold))
      f_treeViewer.refresh();
  }

  void updateTotal(long declared, long havePolicy) {
    int prc = SLUtility.safeDoubleToInt(Math.round(((double) havePolicy / (double) declared) * 100.0));
    f_modelledStateTotal.setText(prc + "% of fields declare a concurrency policy");
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
    final ArrayList<IMetricDrop> metricDrops = DropSeaUtility.filterMetricsToOneType(IMetricDrop.Metric.STATE_WRT_CONCURRENCY,
        drops);
    f_treeViewer.setInput(new StateWrtViewContentProvider.Input(scan, metricDrops));
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

      if (cell.getElement() instanceof StateWrtElement) {
        final StateWrtElement element = (StateWrtElement) cell.getElement();
        String label = element.getLabel();
        cell.setText(label);
        Image image = element.getImage();
        if (element.highlightDueToThreshold(f_options)) {
          image = SLImages.getDecoratedImage(image,
              new ImageDescriptor[] { null, null, SLImages.getImageDescriptor(CommonImages.DECR_ASTERISK), null, null });
          if (element.f_hasDirectMetricData) {
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
     *          a data containing metric element.
     * @return the metric data.
     */
    abstract int getMetricValue(StateWrtElement metric);

    @Override
    public void update(ViewerCell cell) {
      if (cell.getElement() instanceof StateWrtElement) {
        final StateWrtElement element = (StateWrtElement) cell.getElement();
        final long data = getMetricValue(element);
        cell.setText(SLUtility.toStringHumanWithCommas(data));
        if (!element.f_hasDirectMetricData) {
          cell.setForeground(EclipseColorUtility.getSubtleTextColor());
        } else {
          cell.setBackground(element.highlightDueToThreshold(f_options) ? EclipseColorUtility.getDiffHighlightColorNewChanged()
              : null);
        }
      }
    }
  };

  @Nullable
  StateWrtElement getTreeViewerSelectionOrNull() {
    final IStructuredSelection s = (IStructuredSelection) f_treeViewer.getSelection();
    final Object o = s.getFirstElement();
    if (o instanceof StateWrtElement) {
      StateWrtElement element = (StateWrtElement) o;
      return element;
    }
    return null;
  }

  /**
   * Draws the metrics graph on the screen.
   */
  final class StateWrtCanvasEventHandler implements PaintListener {

    public void paintControl(PaintEvent e) {
      final StateWrtElement element = getTreeViewerSelectionOrNull();
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

        /*
         * Pie chart at the top
         */
        double degPerLine = 1.0 / (((double) element.getFieldCountTotal()) / 360.0);

        int chartDiameter = clientArea.width - 10;

        final Color otherColor = e.gc.getDevice().getSystemColor(SWT.COLOR_RED);
        final Color immutableColor = EclipseColorUtility.getCompoundScheme1Color1();
        final Color threadSafeColor = EclipseColorUtility.getCompoundScheme1Color0();
        final Color lockProtectedColor = EclipseColorUtility.getCompoundScheme1Color2();
        final Color threadConfinedColor = EclipseColorUtility.getCompoundScheme1Color3();
        final Color notThreadSafeColor = e.gc.getDevice().getSystemColor(SWT.COLOR_WHITE);

        // Draw chart first
        e.gc.setBackground(notThreadSafeColor);
        e.gc.fillOval(5, fold + 5, chartDiameter, chartDiameter);

        final Pattern p = new Pattern(e.gc.getDevice(), SLImages.getImage(CommonImages.FILL_DIAGONOAL));

        int arcStart = 90;
        if (element.getOtherFieldCount() > 0) {
          final int arc = SLUtility.safeLongToInt(Math.round((double) element.getOtherFieldCount() * degPerLine));
          e.gc.setBackground(otherColor);
          e.gc.fillArc(5, fold + 5, chartDiameter, chartDiameter, arcStart, arc);
          e.gc.setBackgroundPattern(p);
          e.gc.fillArc(5, fold + 5, chartDiameter, chartDiameter, arcStart, arc);
          e.gc.setBackgroundPattern(null);
          arcStart += arc;
        }
        if (element.getImmutableFieldCount() > 0) {
          final int arc = SLUtility.safeLongToInt(Math.round((double) element.getImmutableFieldCount() * degPerLine));
          e.gc.setBackground(immutableColor);
          e.gc.fillArc(5, fold + 5, chartDiameter, chartDiameter, arcStart, arc);
          arcStart += arc;
        }
        if (element.getThreadSafeFieldCount() > 0) {
          final int arc = SLUtility.safeLongToInt(Math.round((double) element.getThreadSafeFieldCount() * degPerLine));
          e.gc.setBackground(threadSafeColor);
          e.gc.fillArc(5, fold + 5, chartDiameter, chartDiameter, arcStart, arc);
          arcStart += arc;
        }
        if (element.getLockProtectedFieldCount() > 0) {
          final int arc = SLUtility.safeLongToInt(Math.round((double) element.getLockProtectedFieldCount() * degPerLine));
          e.gc.setBackground(lockProtectedColor);
          e.gc.fillArc(5, fold + 5, chartDiameter, chartDiameter, arcStart, arc);
          arcStart += arc;
        }
        if (element.getThreadConfinedFieldCount() > 0) {
          final int arc = SLUtility.safeLongToInt(Math.round((double) element.getThreadConfinedFieldCount() * degPerLine));
          e.gc.setBackground(threadConfinedColor);
          e.gc.fillArc(5, fold + 5, chartDiameter, chartDiameter, arcStart, arc);
          arcStart += arc;
        }
        e.gc.drawOval(5, fold + 5, chartDiameter, chartDiameter);

        fold += chartDiameter + 10;
        String s = SLUtility.toStringHumanWithCommas(element.getFieldCountTotal()) + " declared fields";
        Point txtExtent = e.gc.stringExtent(s);
        int slocTxtWidth = txtExtent.x;
        e.gc.drawText(s, (chartDiameter / 2) + 5 - (slocTxtWidth / 2), fold, true);

        fold += txtExtent.y + 5;
        s = SLUtility.toStringHumanWithCommas(element.getOtherFieldCount()) + " no policy";
        txtExtent = e.gc.stringExtent(s);
        e.gc.setBackground(otherColor);
        e.gc.fillRectangle(5, fold, txtExtent.y, txtExtent.y);
        e.gc.setBackgroundPattern(p);
        e.gc.fillRectangle(5, fold, txtExtent.y, txtExtent.y);
        e.gc.setBackgroundPattern(null);
        p.dispose();
        e.gc.drawRectangle(5, fold, txtExtent.y - 1, txtExtent.y - 1);
        e.gc.drawText(s, 10 + txtExtent.y, fold, true);

        fold += txtExtent.y + 5;
        s = SLUtility.toStringHumanWithCommas(element.getImmutableFieldCount()) + " @Immutable";
        txtExtent = e.gc.stringExtent(s);
        e.gc.setBackground(immutableColor);
        e.gc.fillRectangle(5, fold, txtExtent.y, txtExtent.y);
        e.gc.drawRectangle(5, fold, txtExtent.y - 1, txtExtent.y - 1);
        e.gc.drawText(s, 10 + txtExtent.y, fold, true);

        fold += txtExtent.y + 5;
        s = SLUtility.toStringHumanWithCommas(element.getThreadSafeFieldCount()) + " @ThreadSafe";
        txtExtent = e.gc.stringExtent(s);
        e.gc.setBackground(threadSafeColor);
        e.gc.fillRectangle(5, fold, txtExtent.y, txtExtent.y);
        e.gc.drawRectangle(5, fold, txtExtent.y - 1, txtExtent.y - 1);
        e.gc.drawText(s, 10 + txtExtent.y, fold, true);

        fold += txtExtent.y + 5;
        s = SLUtility.toStringHumanWithCommas(element.getLockProtectedFieldCount()) + " @RegionLock/@GuardedBy";
        txtExtent = e.gc.stringExtent(s);
        e.gc.setBackground(lockProtectedColor);
        e.gc.fillRectangle(5, fold, txtExtent.y, txtExtent.y);
        e.gc.drawRectangle(5, fold, txtExtent.y - 1, txtExtent.y - 1);
        e.gc.drawText(s, 10 + txtExtent.y, fold, true);

        fold += txtExtent.y + 5;
        s = SLUtility.toStringHumanWithCommas(element.getThreadConfinedFieldCount()) + " @ThreadConfined";
        txtExtent = e.gc.stringExtent(s);
        e.gc.setBackground(threadConfinedColor);
        e.gc.fillRectangle(5, fold, txtExtent.y, txtExtent.y);
        e.gc.drawRectangle(5, fold, txtExtent.y - 1, txtExtent.y - 1);
        e.gc.drawText(s, 10 + txtExtent.y, fold, true);

        fold += txtExtent.y + 5;
        s = SLUtility.toStringHumanWithCommas(element.getNotThreadSafeFieldCount()) + " @NotThreadSafe";
        txtExtent = e.gc.stringExtent(s);
        e.gc.setBackground(notThreadSafeColor);
        e.gc.fillRectangle(5, fold, txtExtent.y, txtExtent.y);
        e.gc.drawRectangle(5, fold, txtExtent.y - 1, txtExtent.y - 1);
        e.gc.drawText(s, 10 + txtExtent.y, fold, true);
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
