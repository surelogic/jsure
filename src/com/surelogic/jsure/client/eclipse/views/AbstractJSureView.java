package com.surelogic.jsure.client.eclipse.views;

import java.util.logging.Level;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;


import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.views.AbstractSLView;
import com.surelogic.jsure.client.eclipse.views.JSureHistoricalSourceView;

import edu.cmu.cs.fluid.java.ISrcRef;

/**
 * Various code that's proven handy in JSure views
 * 
 * @author Edwin
 */
public abstract class AbstractJSureView extends AbstractSLView {
	/**
	 * Open and highlight a line within the Java editor, if possible. Otherwise,
	 * try to open as a text file
	 * 
	 * @param srcRef
	 *            the source reference to highlight
	 */
	protected final void highlightLineInJavaEditor(ISrcRef srcRef) {
		if (srcRef != null) {
			try {
				Object f = srcRef.getEnclosingFile();
				IFile file;
				if (f instanceof IFile) {
					file = (IFile) f;
				} else if (f instanceof String) {
					String s = (String) f;
					if (s.indexOf('/') < 0) {
						return; // probably not a file
					}
					s = JSureHistoricalSourceView.tryToMapPath(s);
					file = EclipseUtility.resolveIFile(s);
					
					if (file == null) {
						s = srcRef.getRelativePath();
						file = EclipseUtility.resolveIFile(s);
					}
				} else {
					return;
				}
				JSureHistoricalSourceView.tryToOpenInEditor(srcRef.getPackage(), 
						srcRef.getCUName(), srcRef.getLineNumber());

				if (file != null) {
					IJavaElement elt = JavaCore.create(file);
					if (elt != null) {					    
						IEditorPart ep = JavaUI.openInEditor(elt);						

						IMarker location = null;
						try {
							location = ResourcesPlugin.getWorkspace().getRoot()
							.createMarker("edu.cmu.fluid");
							final int offset = srcRef.getOffset();
							if (offset >= 0 && offset != Integer.MAX_VALUE
									&& srcRef.getLength() >= 0) {
								location.setAttribute(IMarker.CHAR_START,
										srcRef.getOffset());
								location.setAttribute(IMarker.CHAR_END, srcRef
										.getOffset()
										+ srcRef.getLength());
							}
							if (srcRef.getLineNumber() > 0) {
								location.setAttribute(IMarker.LINE_NUMBER,
										srcRef.getLineNumber());
							}
						} catch (org.eclipse.core.runtime.CoreException e) {
							SLLogger.getLogger().log(Level.SEVERE,
									"Failure to create an IMarker", e);
						}
						if (location != null) {
							IDE.gotoMarker(ep, location);
						}
					} else { // try to open as a text file
						IWorkbench bench = PlatformUI.getWorkbench();
						IWorkbenchWindow win = bench.getActiveWorkbenchWindow();
						if (win == null && bench.getWorkbenchWindowCount() > 0) {
							win = bench.getWorkbenchWindows()[0];
						}
						IWorkbenchPage page = win.getActivePage();
						IDE.openEditor(page, file);
					}
				}
			} catch (PartInitException e) {
				showMessage("PartInitException was thrown");
			} catch (org.eclipse.core.runtime.CoreException e) {
				showMessage("CoreException was thrown");
			}
		}
	}
}

