package com.surelogic.jsure.client.eclipse.views.results;

import org.eclipse.ui.IWorkbenchPage;

import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.jsure.core.listeners.*;

import edu.cmu.cs.fluid.sea.*;

public final class DropInfoUtility {
	public static void showDrop(IProofDropInfo d) {
		if (PersistentDropInfo.useInfo) {
			final PersistentResultsView view = (PersistentResultsView) EclipseUIUtility
					.showView(PersistentResultsView.class.getName(), null,
							IWorkbenchPage.VIEW_VISIBLE);
			view.showDrop(d);
		} else {
			final ResultsView view = (ResultsView) EclipseUIUtility.showView(
					ResultsView.class.getName(), null,
					IWorkbenchPage.VIEW_VISIBLE);
			if (view != null && d instanceof ProofDrop) {
				view.showDrop((ProofDrop) d);
			}
		}
	}
}
