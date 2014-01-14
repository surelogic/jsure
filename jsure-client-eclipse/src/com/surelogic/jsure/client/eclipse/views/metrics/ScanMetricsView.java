package com.surelogic.jsure.client.eclipse.views.metrics;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class ScanMetricsView extends ViewPart implements JSureDataDirHub.ContentsChangeListener,
    JSureDataDirHub.CurrentScanChangeListener {

  private ScanMetricsMediator f_mediator = null;

  @Override
  public void createPartControl(Composite parent) {
    parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_RED));
    JSureDataDirHub.getInstance().addContentsChangeListener(this);
    JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
  }

  @Override
  public void dispose() {
    try {
      JSureDataDirHub.getInstance().removeContentsChangeListener(this);
      JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);

      if (f_mediator != null)
        f_mediator.dispose();
      f_mediator = null;
    } finally {
      super.dispose();
    }
  }

  @Override
  public void setFocus() {
    final ScanMetricsMediator mediator = f_mediator;
    if (mediator != null)
      mediator.setFocus();
  }

  @Override
  public void scanContentsChanged(JSureDataDir dataDir) {
    notifyMediatorInSwtThread();
  }

  @Override
  public void currentScanChanged(JSureScan scan) {
    notifyMediatorInSwtThread();
  }

  private void notifyMediatorInSwtThread() {
    final ScanMetricsMediator mediator = f_mediator;
    if (mediator != null)
      EclipseUIUtility.asyncExec(new Runnable() {
        @Override
        public void run() {
          mediator.refreshScanContents();
        }
      });
  }
}
