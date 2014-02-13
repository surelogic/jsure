package com.surelogic.jsure.client.eclipse.views.metrics.scantime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
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
import com.surelogic.common.logging.SLLogger;
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
    return "Analysis Performance";
  }

  public ScanTimeMetricMediator(TabFolder folder, ViewPart view) {
    super(folder, view);
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
        return ((ScanTimeElement) element).highlightDueToThreshold(f_options);
      }
      return false;
    }
  };

  final ViewerFilter f_analysisToShowFilter = new ViewerFilter() {

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
      // exception for scan
      if (element instanceof ScanTimeElementScan)
        return true;

      if (element instanceof ScanTimeElement) {
        return ((ScanTimeElement) element).includeBasedOnAnalysisToShow(f_options);
      }
      return false;
    }
  };

  Label f_totalScanDuration = null;
  Scale f_thresholdScale = null;
  Text f_thresholdLabel = null;
  Label f_thresholdUnit = null;

  Combo f_analysisCombo = null;
  final String f_analysisComboAll = "All Analyses";
  boolean f_analysisComboFirstLoad = true;

  TreeViewer f_treeViewer = null;

  @NonNull
  ScanTimeViewContentProvider f_contentProvider = new ScanTimeViewContentProvider(this);

  @NonNull
  final ScanTimeOptions f_options = new ScanTimeOptions();

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
    GridLayout topLayout = new GridLayout(8, false);
    top.setLayout(topLayout);

    f_totalScanDuration = new Label(top, SWT.NONE);
    f_totalScanDuration.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    f_totalScanDuration.setForeground(f_totalScanDuration.getDisplay().getSystemColor(SWT.COLOR_BLUE));

    final Label analysisFilter = new Label(top, SWT.RIGHT);
    analysisFilter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    analysisFilter.setText("Show Times For:");

    f_analysisCombo = new Combo(top, SWT.READ_ONLY);
    GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
    gd.widthHint = 140;
    f_analysisCombo.setLayoutData(gd);
    setupAnalysisComboFor(null);
    f_analysisCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        String analysisToSelect = f_analysisCombo.getItem(f_analysisCombo.getSelectionIndex());
        if (f_analysisComboAll.equals(analysisToSelect))
          analysisToSelect = null;
        if (f_options.setAnalysisToShow(analysisToSelect)) {
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
    boolean showAbove = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.METRIC_SCAN_TIME_THRESHOLD_SHOW_ABOVE);
    if (showAbove)
      aboveThresholdItem.setSelection(true);
    else
      belowThresholdItem.setSelection(true);
    f_options.setThresholdShowAbove(showAbove);
    final SelectionAdapter radioSelection = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        boolean showAbove = aboveThresholdItem.getSelection();
        EclipseUtility.setBooleanPreference(JSurePreferencesUtility.METRIC_SCAN_TIME_THRESHOLD_SHOW_ABOVE, showAbove);
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
    f_thresholdScale.setMaximum(5000);
    f_thresholdScale.setPageIncrement(500);
    int savedThreshold = EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SCAN_TIME_THRESHOLD_MS);
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

    f_thresholdUnit = new Label(top, SWT.NONE);
    f_thresholdUnit.setLayoutData(gd);
    f_thresholdUnit.setText("ms");

    /*
     * Left-hand-side shows tree-table.
     */
    f_treeViewer = new TreeViewer(panel, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    f_treeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    f_treeViewer.setContentProvider(f_contentProvider);
    f_treeViewer.addFilter(f_analysisToShowFilter);
    f_treeViewer.addFilter(f_thresholdFilter);
    f_treeViewer.getTree().setHeaderVisible(true);
    f_treeViewer.getTree().setLinesVisible(true);
    f_treeViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent event) {
        final ScanTimeElement element = getTreeViewerSelectionOrNull();
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
    columnTree.getColumn().setWidth(EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SCAN_TIME_COL_TREE_WIDTH));
    columnTree.getColumn().addControlListener(new ColumnResizeListener(JSurePreferencesUtility.METRIC_SCAN_TIME_COL_TREE_WIDTH));

    final TreeViewerColumn columnDuration = new TreeViewerColumn(f_treeViewer, SWT.RIGHT);
    columnDuration.setLabelProvider(DURATION);
    columnDuration.getColumn().setWidth(
        EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_SCAN_TIME_COL_DURATION_WIDTH));
    columnDuration.getColumn().addControlListener(
        new ColumnResizeListener(JSurePreferencesUtility.METRIC_SCAN_TIME_COL_DURATION_WIDTH));
    columnDuration.getColumn().setText("Duration");

    f_actionExpand.setText(I18N.msg("jsure.eclipse.view.expand"));
    f_actionExpand.setToolTipText(I18N.msg("jsure.eclipse.view.expand.tip"));
    f_actionExpand.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_EXPAND_ALL));

    f_actionCollapse.setText(I18N.msg("jsure.eclipse.view.collapse"));
    f_actionCollapse.setToolTipText(I18N.msg("jsure.eclipse.view.collapse.tip"));
    f_actionCollapse.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));
    hookContextMenu(view);

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
    EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_SCAN_TIME_THRESHOLD_MS, threshold);
    if (f_options.setThreshold(threshold)) {
      f_treeViewer.refresh();
    }
  }

  void updateTotal(ScanTimeElement root) {
    final String total = root.getDurationAsHumanReadableString(f_options);
    f_totalScanDuration.setText(total);
  }

  void fixSortingIndicatorOnTreeTable() {
    if (f_treeViewer.getSorter() == f_alphaSorter) {
      f_treeViewer.getTree().setSortColumn(f_treeViewer.getTree().getColumn(0));
      f_treeViewer.getTree().setSortDirection(SWT.DOWN);
    } else {
      f_treeViewer.getTree().setSortColumn(f_treeViewer.getTree().getColumn(1));
      f_treeViewer.getTree().setSortDirection(f_options.getThresholdShowAbove() ? SWT.UP : SWT.DOWN);
    }
  }

  void setupAnalysisComboFor(@Nullable ArrayList<IMetricDrop> drops) {
    if (drops == null) {
      /*
       * No scan set to empty/all -- do not change the value in
       * <tt>f_options</tt> we want that to try to restore it when a new scan is
       * loaded.
       */
      f_analysisCombo.setItems(new String[] { f_analysisComboAll });
      f_analysisCombo.select(0);
    } else {
      /*
       * Save the selected analysis name to try to restore it after we load the
       * new scan.
       */
      @Nullable
      final String valueToTryToRestore;
      if (f_analysisComboFirstLoad) {
        f_analysisComboFirstLoad = false;
        String persistedValue = EclipseUtility.getStringPreference(JSurePreferencesUtility.METRIC_SCAN_TIME_ANALYSIS_TO_SHOW);
        // "" means null in persisted value -- so we translate here
        valueToTryToRestore = "".equals(persistedValue) ? null : persistedValue;
      } else {
        valueToTryToRestore = f_options.getAnalysisToShow();
      }
      /*
       * Determine the list of analysis names in this scan
       */
      final Set<String> analyses = new HashSet<String>();
      for (IMetricDrop drop : drops) {
        String analysisName = drop.getMetricInfoOrNull(IMetricDrop.SCAN_TIME_ANALYSIS_NAME);
        if (analysisName == null) {
          SLLogger.getLogger().log(Level.WARNING, I18N.err(312));
          analysisName = "(unknown analysis)";
        }
        analyses.add(analysisName);
      }
      LinkedList<String> choices = new LinkedList<String>(analyses);
      Collections.sort(choices);
      choices.addFirst(f_analysisComboAll);
      /*
       * Try to restore the selection.
       */
      int indexToSelect = 0; // all -- a default value
      if (valueToTryToRestore != null) {
        int possibleIndex = choices.indexOf(valueToTryToRestore);
        if (possibleIndex > 0)
          indexToSelect = possibleIndex;
      }
      f_analysisCombo.setItems(choices.toArray(new String[choices.size()]));
      f_analysisCombo.select(indexToSelect);
      final String setAnalysisToShow = indexToSelect == 0 ? null : choices.get(indexToSelect);
      f_options.setAnalysisToShow(setAnalysisToShow);
    }

  }

  @Override
  protected void refreshMetricContentsFor(@Nullable JSureScanInfo scan, @Nullable ArrayList<IMetricDrop> drops) {
    final ArrayList<IMetricDrop> metricDrops = DropSeaUtility.filterMetricsToOneType(IMetricDrop.Metric.SCAN_TIME, drops);
    f_treeViewer.setInput(new ScanTimeViewContentProvider.Input(scan, metricDrops));
    setupAnalysisComboFor(metricDrops);
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
        if (element.highlightDueToThreshold(f_options)) {
          image = SLImages.getDecoratedImage(image,
              new ImageDescriptor[] { null, null, SLImages.getImageDescriptor(CommonImages.DECR_ASTERISK), null, null });
          if (element instanceof ScanTimeElementAnalysis || element instanceof ScanTimeElementJavaDecl) {
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
      if (cell.getElement() instanceof ScanTimeElement) {
        final ScanTimeElement element = (ScanTimeElement) cell.getElement();
        String label = element.getDurationAsHumanReadableString(f_options);
        cell.setText(label);
        if (!element.hasDurationNs()) {
          cell.setForeground(EclipseColorUtility.getSubtleTextColor());
        }

        boolean highlighted = false;
        if (element.highlightDueToThreshold(f_options)) {
          if (element instanceof ScanTimeElementAnalysis || element instanceof ScanTimeElementJavaDecl) {
            cell.setBackground(EclipseColorUtility.getDiffHighlightColorNewChanged());
            highlighted = true;
          }
        }
        if (!highlighted)
          cell.setBackground(null);
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
    f_treeViewer.refresh();
  }
}
