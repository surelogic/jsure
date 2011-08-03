package com.surelogic.jsure.client.eclipse.views.ast;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.ui.*;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.views.contentoutline.*;

import com.surelogic.jsure.core.Eclipse;

public class FastViewer extends ContentOutline {
	@Override
	protected PageRec doCreatePage(IWorkbenchPart part) {
		// Try to get an outline page.
		Object obj = part.getAdapter(IFastViewerPage.class);
		if (obj instanceof IFastViewerPage) {
			IFastViewerPage page = (IFastViewerPage) obj;
			return setupPage(part, page);
		} else if (part instanceof IEditorPart) {
			IEditorPart editor = (IEditorPart) part;
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				ICompilationUnit icu = Eclipse.getActiveJavaFile(file);
				if (icu != null) {
					IFastViewerPage page = new FastViewerPage(icu);
					return setupPage(part, page);
				}
			}
		}
		// There is no content outline
		return null;
	}

	private PageRec setupPage(IWorkbenchPart part, IContentOutlinePage page) {
		if (page instanceof IPageBookViewPage)
			initPage((IPageBookViewPage) page);
		page.createControl(getPageBook());
		return new PageRec(part, page);
	}
}
