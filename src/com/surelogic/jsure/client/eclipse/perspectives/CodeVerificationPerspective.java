package com.surelogic.jsure.client.eclipse.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.surelogic.jsure.client.eclipse.views.results.PersistentResultsView;
import com.surelogic.jsure.client.eclipse.views.results.ProblemsView;
import com.surelogic.jsure.client.eclipse.views.results.ProposedPromiseView;
import com.surelogic.jsure.client.eclipse.views.scans.ScanManagerView;
import com.surelogic.jsure.client.eclipse.views.source.JSureHistoricalSourceView;

/**
 * Defines the JSure perspective within the workbench.
 */
public final class CodeVerificationPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		final String scanMgt = ScanManagerView.class.getName();
		final String statusView = PersistentResultsView.class.getName();
		final String proposedPromiseView = ProposedPromiseView.class.getName();
		final String problemsView = ProblemsView.class.getName();
		final String histSrcView = JSureHistoricalSourceView.class.getName();
		final String editorArea = layout.getEditorArea();

		final IFolderLayout scanMgtArea = layout.createFolder("scanMgtArea",
				IPageLayout.TOP, 0.15f, editorArea);
		scanMgtArea.addView(scanMgt);

		final IFolderLayout resultsArea = layout.createFolder("resultsArea",
				IPageLayout.TOP, 0.45f, editorArea);
		resultsArea.addView(statusView);

		final IFolderLayout problemsArea = layout.createFolder("problemsArea",
				IPageLayout.BOTTOM, 0.7f, editorArea);
		problemsArea.addView(problemsView);

		final IFolderLayout rightOfEditorArea = layout.createFolder(
				"leftOfEditorArea", IPageLayout.RIGHT, 0.5f, editorArea);
		rightOfEditorArea.addView(proposedPromiseView);
		rightOfEditorArea.addView(histSrcView);
	}
}
