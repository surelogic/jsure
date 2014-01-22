package com.surelogic.jsure.client.eclipse.views.metrics;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.javac.persistence.JSureScanInfo;
import com.surelogic.jsure.client.eclipse.views.metrics.scantime.ScanTimeMetricMediator;
import com.surelogic.jsure.client.eclipse.views.metrics.sloc.SlocMetricMediator;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class ScanMetricsView extends ViewPart implements JSureDataDirHub.ContentsChangeListener,
    JSureDataDirHub.CurrentScanChangeListener {

  TabFolder f_metricFolder = null;
  final CopyOnWriteArrayList<IScanMetricMediator> f_mediators = new CopyOnWriteArrayList<IScanMetricMediator>();

  @Override
  public void createPartControl(Composite parent) {
    f_metricFolder = new TabFolder(parent, SWT.NONE);

    /*
     * A D D
     * 
     * N E W
     * 
     * M E T R I C S
     * 
     * H E R E
     * 
     * Each new metric display needs a mediator implementation. Construct the
     * mediator and add it to the mediator list. The order of creation matches
     * the tab order in the user interface display.
     */

    f_mediators.add(new SlocMetricMediator(f_metricFolder)); // SLOC
    f_mediators.add(new ScanTimeMetricMediator(f_metricFolder)); // SCAN_TIME

    // Finished adding new metrics, initialize all of them
    for (IScanMetricMediator mediator : f_mediators)
      mediator.init();

    // Persist the selected tab when it changes
    f_metricFolder.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        int selectedTab = f_metricFolder.getSelectionIndex();
        EclipseUtility.setIntPreference(JSurePreferencesUtility.METRIC_VIEW_TAB_SELECTION, selectedTab);
      }
    });
    // Restore the selected tab (or use 0 which is the int preference default)
    f_metricFolder.setSelection(EclipseUtility.getIntPreference(JSurePreferencesUtility.METRIC_VIEW_TAB_SELECTION));

    f_actionCollapseAll.setText(I18N.msg("jsure.eclipse.view.collapse_all"));
    f_actionCollapseAll.setToolTipText(I18N.msg("jsure.eclipse.view.collapse_all.tip"));
    f_actionCollapseAll.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

    f_actionAlphaSort.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_ALPHA_SORT));
    f_actionAlphaSort.setText(I18N.msg("jsure.eclipse.metrics.sort_alphabetically"));
    f_actionAlphaSort.setToolTipText(I18N.msg("jsure.eclipse.metrics.sort_alphabetically.tip"));
    boolean alphaSort = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.METRIC_ALPHA_SORT);
    f_actionAlphaSort.setChecked(alphaSort);
    setAlphaSort(alphaSort);

    f_actionFilter.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_FILTER));
    f_actionFilter.setText(I18N.msg("jsure.eclipse.metrics.filter"));
    f_actionFilter.setToolTipText(I18N.msg("jsure.eclipse.metrics.filter.tip"));
    boolean filter = EclipseUtility.getBooleanPreference(JSurePreferencesUtility.METRIC_FILTER);
    f_actionFilter.setChecked(filter);
    setFilter(filter);

    contributeToActionBars();

    JSureDataDirHub.getInstance().addContentsChangeListener(this);
    JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);

    // setup a job to "fake" a scan change.
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        currentScanChanged(null);
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  final Action f_actionCollapseAll = new Action() {
    @Override
    public void run() {
      for (IScanMetricMediator mediator : f_mediators)
        mediator.takeActionCollapseAll();
    }
  };

  final Action f_actionAlphaSort = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      setAlphaSort(f_actionAlphaSort.isChecked());
    }
  };

  final Action f_actionFilter = new Action("", IAction.AS_CHECK_BOX) {
    @Override
    public void run() {
      setFilter(f_actionFilter.isChecked());
    }
  };

  void setAlphaSort(boolean value) {
    EclipseUtility.setBooleanPreference(JSurePreferencesUtility.METRIC_ALPHA_SORT, value);
    for (IScanMetricMediator mediator : f_mediators)
      mediator.takeActionUseAlphaSort(value);
  }

  void setFilter(boolean value) {
    EclipseUtility.setBooleanPreference(JSurePreferencesUtility.METRIC_FILTER, value);
    for (IScanMetricMediator mediator : f_mediators)
      mediator.takeActionUseFilter(value);
  }

  private void contributeToActionBars() {
    final IActionBars bars = getViewSite().getActionBars();

    final IMenuManager pulldown = bars.getMenuManager();
    pulldown.add(f_actionCollapseAll);
    pulldown.add(new Separator());
    pulldown.add(f_actionAlphaSort);
    pulldown.add(f_actionFilter);

    final IToolBarManager toolbar = bars.getToolBarManager();
    toolbar.add(f_actionCollapseAll);
    toolbar.add(new Separator());
    toolbar.add(f_actionAlphaSort);
    toolbar.add(f_actionFilter);
  }

  @Override
  public void dispose() {
    try {
      JSureDataDirHub.getInstance().removeContentsChangeListener(this);
      JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);

      for (IScanMetricMediator mediator : f_mediators)
        mediator.dispose();
      f_mediators.clear();
    } finally {
      super.dispose();
    }
  }

  @Override
  public void setFocus() {
    if (f_metricFolder != null)
      f_metricFolder.setFocus();
  }

  @Override
  public void scanContentsChanged(JSureDataDir dataDir) {
    notifyMediatorsInSwtThread();
  }

  @Override
  public void currentScanChanged(JSureScan scan) {
    notifyMediatorsInSwtThread();
  }

  final void notifyMediatorsInSwtThread() {
    if (!f_mediators.isEmpty())
      EclipseUIUtility.asyncExec(new Runnable() {
        @Override
        public void run() {
          // We only want to copy the metrics drops once, so we do it here
          final JSureScanInfo scan = JSureDataDirHub.getInstance().getCurrentScanInfo();
          final ArrayList<IMetricDrop> drops = scan != null ? scan.getMetricDrops() : null;
          for (IScanMetricMediator mediator : f_mediators)
            mediator.refreshScanContents(scan, drops);
        }
      });
  }
}
