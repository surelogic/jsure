package com.surelogic.jsure.views.debug.oracleDiff;

import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchPage;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.irfree.IDiffNode;
import com.surelogic.dropsea.irfree.IDiffNode.Status;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTreeView;
import com.surelogic.jsure.client.eclipse.views.status.VerificationStatusView;

public class SnapshotDiffView extends AbstractScanTreeView<Object> {
  public SnapshotDiffView() {
    super(SWT.NONE, Object.class, new SnapshotDiffContentProvider());
  }

  @Override
  protected void handleDoubleClick(Object d) {
    if (d instanceof IDiffNode) {
      IDiffNode n = (IDiffNode) d;
      IDrop drop = n.getDrop();
      if (drop != null) {
        Activator.highlightLineInJavaEditor(drop.getJavaRef(), n.getDiffStatus() == Status.OLD);

        final VerificationStatusView view = (VerificationStatusView) EclipseUIUtility.showView(
            VerificationStatusView.class.getName(), null, IWorkbenchPage.VIEW_VISIBLE);
        if (view != null) {
          view.attemptToShowAndSelectDropInViewer(drop);
        }
      }
    }
  }
}
