package com.surelogic.jsure.client.eclipse.views.metrics;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.NonNull;
import com.surelogic.Nullable;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.dropsea.IMetricDrop;
import com.surelogic.javac.persistence.JSureScanInfo;

/**
 * This class handles showing that no scan is selected for each metric mediator.
 */
public abstract class AbstractScanMetricMediator implements IScanMetricMediator {

  final PageBook f_viewerbook;
  final TabItem f_tab;
  final Label f_noResultsToShowLabel;
  Control f_control = null;

  public AbstractScanMetricMediator(TabFolder folder) {
    // Avoid invoking dispatching methods here, do them in init()
    f_tab = new TabItem(folder, SWT.NONE);
    f_viewerbook = new PageBook(folder, SWT.NONE);
    f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
    f_noResultsToShowLabel.setText(I18N.msg("jsure.eclipse.view.no.scan.msg"));
  }

  public final void init() {
    f_tab.setText(getMetricLabel());
    f_tab.setControl(f_viewerbook);
    f_control = initMetricDisplay(f_viewerbook);
  }

  /**
   * Implementations should create the user interface "bones" of the metric
   * display. This method is only called once when the containing view is being
   * setup.
   * <p>
   * This method is always invoked within the SWT thread.
   * <p>
   * This method should only be called from {@link AbstractScanMetricMediator}.
   * 
   * @param parent
   *          the parent of the metric display.
   * @return a single child the metric display is contained within. For example,
   *         a <tt>Composite</tt> or a <tt>Table</tt>.
   */
  @NonNull
  protected abstract Control initMetricDisplay(PageBook parent);

  @NonNull
  protected abstract String getMetricLabel();

  public final void refreshScanContents(final @Nullable JSureScanInfo scan, final @Nullable ArrayList<IMetricDrop> drops) {
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {
        if (scan != null) {
          f_viewerbook.showPage(f_control);
          refreshMetricContentsFor(scan, drops);
        } else {
          // Show no results
          f_viewerbook.showPage(f_noResultsToShowLabel);
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  /**
   * Implementors use this method to update the metric display to show results
   * for the passed scan. The passed list of drops should not be mutated.
   * <p>
   * This method is always invoked within the SWT thread.
   * <p>
   * This method should only be called from {@link AbstractScanMetricMediator}.
   * 
   * @param scan
   *          results and information about a JSure scan.
   * @param drops
   *          metric drops about a JSure scan.
   */
  protected abstract void refreshMetricContentsFor(@Nullable JSureScanInfo scan, @Nullable ArrayList<IMetricDrop> drops);
}
