package com.surelogic.jsecure.client.eclipse.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.surelogic.jsecure.client.eclipse.views.ScanManagerView;
import com.surelogic.jsecure.client.eclipse.views.adhoc.QueryEditorView;
import com.surelogic.jsecure.client.eclipse.views.adhoc.QueryResultsView;

public class JSecurePerspective implements IPerspectiveFactory {
	  @Override
	  public void createInitialLayout(IPageLayout layout) {
	    final String scanMgt = ScanManagerView.class.getName();
	    final String queryEditor = QueryEditorView.class.getName();
	    final String queryResults = QueryResultsView.class.getName();
	    final String editorArea = layout.getEditorArea();

	    final IFolderLayout scanMgtArea = layout.createFolder("scanMgtArea", IPageLayout.TOP, 0.4f, editorArea);
	    scanMgtArea.addView(scanMgt);
	    scanMgtArea.addView(queryEditor);
	    /*
	    final IFolderLayout rightOfScanMgtArea = layout.createFolder("rightOfScanMgtArea", IPageLayout.RIGHT, 0.6f, "scanMgtArea");
	    rightOfScanMgtArea.addView(problemsView);
        */
	    final IFolderLayout resultsArea = layout.createFolder("resultsArea", IPageLayout.TOP, 0.3f, editorArea);
	    resultsArea.addView(queryResults);
	    
	    /*
	    final IFolderLayout rightOfResultsArea = layout.createFolder("rightOfResultsArea", IPageLayout.RIGHT, 0.6f, "resultsArea");
	    rightOfResultsArea.addView(proposedAnnotationView);
	    rightOfResultsArea.addView(xmlExplorerView);

	    final IFolderLayout finderArea = layout.createFolder("finderArea", IPageLayout.TOP, 0.5f, editorArea);
	    finderArea.addView(finderView);

	    final IFolderLayout histSrcArea = layout.createFolder("histSrcArea", IPageLayout.RIGHT, 0.5f, editorArea);
	    histSrcArea.addView(histSrcView);
	    */
	  }
}
