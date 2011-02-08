package com.surelogic.jsure.client.eclipse.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;

import com.surelogic.jsure.client.eclipse.views.results.PersistentResultsView;
import com.surelogic.jsure.client.eclipse.views.results.ProblemsView;
import com.surelogic.jsure.client.eclipse.views.results.ProposedPromiseView;

/**
 * Defines the JSure perspective within the workbench.
 */
public final class CodeVerificationPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		final String statusView = PersistentResultsView.class.getName();
		final String proposedPromiseView = ProposedPromiseView.class.getName();
		final String problemsView = ProblemsView.class.getName();
		final String packageExplorer = "org.eclipse.jdt.ui.PackageExplorer";
		final String editorArea = layout.getEditorArea();

		final IFolderLayout belowEditorArea = layout.createFolder(
				"belowEditorArea", IPageLayout.BOTTOM, 0.5f, editorArea);
		belowEditorArea.addView(statusView);

		final IPlaceholderFolderLayout proposedPromisesArea = layout
				.createPlaceholderFolder("proposedPromisesArea",
						IPageLayout.BOTTOM, 0.6f, "belowEditorArea");
		proposedPromisesArea.addPlaceholder(proposedPromiseView);

		final IPlaceholderFolderLayout problemsArea = layout
				.createPlaceholderFolder("problemsArea", IPageLayout.BOTTOM,
						0.5f, "proposedPromisesArea");
		problemsArea.addPlaceholder(problemsView);

		final IFolderLayout leftOfEditorArea = layout.createFolder(
				"leftOfEditorArea", IPageLayout.LEFT, 0.3f, editorArea);
		leftOfEditorArea.addView(packageExplorer);
	}
}
