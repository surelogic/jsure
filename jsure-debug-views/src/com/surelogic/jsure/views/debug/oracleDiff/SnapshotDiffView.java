package com.surelogic.jsure.views.debug.oracleDiff;

import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchPage;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.irfree.IDiffNode;
import com.surelogic.jsure.client.eclipse.editors.EditorUtil;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTreeView;
import com.surelogic.jsure.client.eclipse.views.verification.VerificationStatusView;

public class SnapshotDiffView extends AbstractScanTreeView<Object> {	
	public SnapshotDiffView() {
		super(SWT.NONE, Object.class, new SnapshotDiffContentProvider());
	}

	@Override
	protected void handleDoubleClick(Object d) {
		if (d instanceof IDiffNode) {
			IDrop drop = ((IDiffNode) d).getDrop();
			if (drop != null) {				
				EditorUtil.highlightLineInJavaEditor(drop.getJavaRef());
				
				final VerificationStatusView view = (VerificationStatusView) EclipseUIUtility.showView(
						VerificationStatusView.class.getName(), null, IWorkbenchPage.VIEW_VISIBLE);
				if (view != null) {
					view.attemptToShowAndSelectDropInViewer(drop);
				}
			}
		}
	}
}
