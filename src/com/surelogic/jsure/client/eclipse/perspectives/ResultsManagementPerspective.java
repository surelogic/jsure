package com.surelogic.jsure.client.eclipse.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.surelogic.jsure.client.eclipse.views.scans.*;

public final class ResultsManagementPerspective implements IPerspectiveFactory {
	@Override
	public void createInitialLayout(IPageLayout layout) {
		final String scansView = ScansView.class.getName();
		final String summaryView = ScanSummaryView.class.getName();

		final String editorArea = layout.getEditorArea();
		final IFolderLayout belowEditorArea = layout.createFolder(
				"belowEditorArea", IPageLayout.BOTTOM, 0.5f, editorArea);
		belowEditorArea.addView(scansView);
		belowEditorArea.addView(summaryView);
	}
}
