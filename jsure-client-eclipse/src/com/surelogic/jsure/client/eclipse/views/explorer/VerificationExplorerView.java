package com.surelogic.jsure.client.eclipse.views.explorer;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.i18n.I18N;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class VerificationExplorerView extends ViewPart implements JSureDataDirHub.CurrentScanChangeListener {

  private PageBook f_viewerbook = null;
  private Label f_noResultsToShowLabel = null;
  private TreeViewer f_treeViewer;

  @Override
  public void createPartControl(Composite parent) {
    f_viewerbook = new PageBook(parent, SWT.NONE);
    f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
    f_noResultsToShowLabel.setText(I18N.msg("jsure.eclipse.view.no.scan.msg"));
    f_treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    makeActions();
    hookContextMenu();
    contributeToActionBars();

    // start empty until the initial build is done
    setViewerVisibility(false);

    showScanOrEmptyLabel();

    JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
  }

  @Override
  public void dispose() {
    try {
      JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);
    } finally {
      super.dispose();
    }
  }

  private void makeActions() {
    // TODO Auto-generated method stub
  }

  private void hookContextMenu() {
    // TODO Auto-generated method stub
  }

  private void contributeToActionBars() {
    // TODO Auto-generated method stub
  }

  private void showScanOrEmptyLabel() {
    // TODO Auto-generated method stub
  }

  /**
   * Toggles between the empty viewer page and the Fluid results
   */
  private void setViewerVisibility(boolean showResults) {
    if (f_viewerbook.isDisposed())
      return;
    if (showResults) {
      f_treeViewer.setInput(getViewSite());
      f_viewerbook.showPage(f_treeViewer.getControl());
    } else {
      f_viewerbook.showPage(f_noResultsToShowLabel);
    }
  }

  @Override
  public void setFocus() {
    f_treeViewer.getControl().setFocus();
  }

  @Override
  public void currentScanChanged(JSureScan scan) {
    // TODO Auto-generated method stub
  }
}
