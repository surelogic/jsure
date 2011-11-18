package com.surelogic.jsure.client.eclipse.perspectives;

import org.eclipse.jdt.ui.JavaElementImageDescriptor;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.surelogic.jsure.client.eclipse.views.finder.FinderView;
import com.surelogic.jsure.client.eclipse.views.results.PersistentResultsView;
import com.surelogic.jsure.client.eclipse.views.results.ProblemsView;
import com.surelogic.jsure.client.eclipse.views.results.ProposedPromiseView;
import com.surelogic.jsure.client.eclipse.views.scans.ScanManagerView;
import com.surelogic.jsure.client.eclipse.views.source.HistoricalSourceView;
import com.surelogic.jsure.client.eclipse.views.xml.XMLExplorerView;

/**
 * Defines the JSure perspective within the workbench.
 */
public final class CodeVerificationPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		final String scanMgt = ScanManagerView.class.getName();
		final String resultsView = PersistentResultsView.class.getName();
		final String finderView = FinderView.class.getName();
		final String proposedPromiseView = ProposedPromiseView.class.getName();
		final String problemsView = ProblemsView.class.getName();
		final String histSrcView = HistoricalSourceView.class.getName();
		final String xmlExplorerView = XMLExplorerView.class.getName();
		final String editorArea = layout.getEditorArea();

		final IFolderLayout scanMgtArea = layout.createFolder("scanMgtArea",
				IPageLayout.TOP, 0.15f, editorArea);
		scanMgtArea.addView(scanMgt);

		final IFolderLayout rightOfScanMgtArea = layout.createFolder(
				"rightOfScanMgtArea", IPageLayout.RIGHT, 0.6f, "scanMgtArea");
		rightOfScanMgtArea.addView(problemsView);

		final IFolderLayout resultsArea = layout.createFolder("resultsArea",
				IPageLayout.TOP, 0.4f, editorArea);
		resultsArea.addView(resultsView);

		final IFolderLayout rightOfResultsArea = layout.createFolder(
				"rightOfResultsArea", IPageLayout.RIGHT, 0.6f, "resultsArea");
		rightOfResultsArea.addView(proposedPromiseView);
		rightOfResultsArea.addView(xmlExplorerView);

		final IFolderLayout finderArea = layout.createFolder("finderArea",
				IPageLayout.TOP, 0.6f, editorArea);
		finderArea.addView(finderView);

		final IFolderLayout rightOfEditorArea = layout.createFolder(
				"rightOfEditorArea", IPageLayout.RIGHT, 0.5f, editorArea);
		rightOfEditorArea.addView(histSrcView);
	}
}
