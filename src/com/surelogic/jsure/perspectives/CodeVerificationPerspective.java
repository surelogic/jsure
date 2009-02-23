package com.surelogic.jsure.perspectives;

import org.eclipse.ui.*;

/**
 * Defines the JSure perspective within the workbench.
 */
public final class CodeVerificationPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		final String statusView = "edu.cmu.cs.fluid.dcf.views.coe.ResultsView";
		final String packageExplorer = "org.eclipse.jdt.ui.PackageExplorer";
		final String editorArea = layout.getEditorArea();

		final IFolderLayout belowEditorArea = layout.createFolder(
				"belowEditorArea", IPageLayout.BOTTOM, 0.5f, editorArea);
		belowEditorArea.addView(statusView);

		final IFolderLayout leftOfEditorArea = layout.createFolder(
				"leftOfEditorArea", IPageLayout.LEFT, 0.3f, editorArea);
		leftOfEditorArea.addView(packageExplorer);
	}
}
