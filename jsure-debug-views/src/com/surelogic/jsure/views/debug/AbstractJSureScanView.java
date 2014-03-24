package com.surelogic.jsure.views.debug;

import java.util.logging.Level;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.part.PageBook;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.views.AbstractSLView;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

/**
 * Handles whether there is any scan to show. diff?
 * 
 * @author Edwin
 */
public abstract class AbstractJSureScanView extends AbstractSLView implements JSureDataDirHub.CurrentScanChangeListener {

  protected static final String NO_RESULTS = I18N.msg("jsure.eclipse.view.no.scan.msg");

  protected PageBook f_viewerbook = null;

  protected Label f_noResultsToShowLabel = null;

  protected AbstractJSureScanView() {
    JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
  }

  @Override
  public void dispose() {
    JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);
    super.dispose();
  }

  @Override
  public final void createPartControl(Composite parent) {
    f_viewerbook = new PageBook(parent, SWT.NONE);
    f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
    f_noResultsToShowLabel.setText(NO_RESULTS);

    super.createPartControl(f_viewerbook);
    updateViewState();
  }

  /**
   * Enables various functionality if non-null
   */
  @Override
  protected StructuredViewer getViewer() {
    return null;
  }

  @Override
  public void setFocus() {
    f_viewerbook.setFocus();
  }

  @Override
  public void currentScanChanged(JSureScan scan) {
    updateViewState();
  }

  /**
   * Can be used to lookup a view implementation and notify it that changes
   * occurred. Does nothing if the view is not open. Logs a problem if the types
   * come out wrong from the Eclipse lookup of the view.
   * 
   * @param clazz
   *          the type of the view implementation.
   */
  public static void notifyScanViewOfChangeIfOpened(Class<? extends AbstractJSureScanView> clazz) {
    final IViewPart view = EclipseUIUtility.getView(clazz.getName());
    if (view != null) { // view opened?
      if (view instanceof AbstractJSureScanView) {
        final AbstractJSureScanView scanView = (AbstractJSureScanView) view;
        scanView.currentScanChanged(JSureDataDirHub.getInstance().getCurrentScan());
      } else {
        SLLogger.getLogger().log(Level.SEVERE, I18N.err(236, clazz.getName(), view));
      }
    }
  }

  /**
   * Not run in the SWT thread
   * 
   * @return The label to be shown in the title
   */
  protected abstract String updateViewer();

  /**
   * Update the internal state, presumably after a new scan
   */
  private void updateViewState() {
    final String label = updateViewer();
    EclipseUIUtility.asyncExec(new Runnable() {
      @Override
      public void run() {
        if (label != null) {
          if (getViewer() != null) {
            getViewer().setInput(getViewSite());
          }
          setViewerVisibility(true);
        } else {
          setViewerVisibility(false);
        }
        // TODO is this right?
        getCurrentControl().redraw();
      }
    });
  }

  private final void setViewerVisibility(boolean showResults) {
    if (f_viewerbook.isDisposed())
      return;
    if (showResults) {
      f_viewerbook.showPage(getCurrentControl());
    } else {
      f_viewerbook.showPage(f_noResultsToShowLabel);
    }
  }
}
