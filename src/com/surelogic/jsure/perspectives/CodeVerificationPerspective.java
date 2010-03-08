package com.surelogic.jsure.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IPlaceholderFolderLayout;

import edu.cmu.cs.fluid.dcf.views.coe.ProblemsView;
import edu.cmu.cs.fluid.dcf.views.coe.ProposedPromiseView;
import edu.cmu.cs.fluid.dcf.views.coe.ResultsView;

/**
 * Defines the JSure perspective within the workbench.
 */
public final class CodeVerificationPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		final String statusView = ResultsView.class.getName();
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
