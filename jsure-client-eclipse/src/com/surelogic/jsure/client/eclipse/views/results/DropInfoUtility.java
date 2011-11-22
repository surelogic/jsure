package com.surelogic.jsure.client.eclipse.views.results;

import org.eclipse.ui.IWorkbenchPage;

import com.surelogic.Utility;
import com.surelogic.common.ui.EclipseUIUtility;

import edu.cmu.cs.fluid.sea.IProofDropInfo;

@Utility
public final class DropInfoUtility {
	public static void showDrop(IProofDropInfo d) {
		final ResultsView view = (ResultsView) EclipseUIUtility.showView(
				ResultsView.class.getName(), null, IWorkbenchPage.VIEW_VISIBLE);
		view.showDrop(d);
	}
}
