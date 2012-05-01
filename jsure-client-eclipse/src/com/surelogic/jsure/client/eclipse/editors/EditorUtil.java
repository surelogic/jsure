package com.surelogic.jsure.client.eclipse.editors;

import java.util.logging.Level;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swt.SWTException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.views.source.HistoricalSourceView;
import com.surelogic.xml.TestXMLParserConstants;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.drops.PackageDrop;

public class EditorUtil {
	/**
	 * Open and highlight a line within the Java editor, if possible. Otherwise,
	 * try to open as a text file
	 * 
	 * @param srcRef
	 *            the source reference to highlight
	 * @throws CoreException
	 */
	public static void highlightLineInJavaEditor(ISrcRef srcRef)
			throws CoreException {
		if (srcRef != null) {
			Object f = srcRef.getEnclosingFile();
			IFile file;
			if (f instanceof IFile) {
				file = (IFile) f;
			} else if (f instanceof String) {
				String s = (String) f;
				if (s.indexOf('/') < 0) {
					// probably not a file, but see if it's a package
					handlePackage(s);
					return;
				}
				s = HistoricalSourceView.tryToMapPath(s);
				file = EclipseUtility.resolveIFile(s);

				if (file == null) {
					s = srcRef.getRelativePath();
					file = EclipseUtility.resolveIFile(s);
				}
			} else {
				return;
			}
			HistoricalSourceView.tryToOpenInEditor(srcRef.getPackage(),
					srcRef.getCUName(), srcRef.getLineNumber());

			if (file != null) {
				IJavaElement elt = JavaCore.create(file);
				if (elt instanceof IClassFile) {
					boolean success = handleIClassFile(srcRef);
					if (success) {
						return;
					}
				}
				if (!file.exists()) {
					return;
				}
				if (elt != null) {
					try {
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
								location.setAttribute(IMarker.CHAR_END,
										srcRef.getOffset() + srcRef.getLength());
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
					} catch (SWTException e) {
						if (!"Widget is disposed".equals(e.getMessage())) {
							SLLogger.getLogger().log(
									Level.WARNING,
									"Unexpected SWT exception while opening "
											+ elt.getElementName(), e);
						}
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
		}
	}

	private static void handlePackage(String pkg) {
		PromisesXMLEditor.openInEditor(PackageDrop.computeXMLPath(pkg), false);
	}

	private static boolean handleIClassFile(final ISrcRef srcRef)
			throws JavaModelException {
		final char slash = '/'; // File.separatorChar;
		final String pkg = srcRef.getPackage().replace('.', slash);
		String name = srcRef.getCUName();
		String nested = null;
		int dollar = name.indexOf('$');
		if (dollar >= 0) {
			if (name.endsWith(".class")) {
				nested = name.substring(dollar + 1, name.length() - 6);
			} else {
				nested = name.substring(dollar + 1);
			}
			nested = nested.replace('$', '.');
			name = name.substring(0, dollar);
		} else if (name.endsWith(".class")) {
			name = name.substring(0, name.length() - 6);
		}
		// Just try to open the editor and let it figure out what to do
		PromisesXMLEditor e = (PromisesXMLEditor) PromisesXMLEditor
				.openInEditor(pkg + slash + name
						+ TestXMLParserConstants.SUFFIX, false);
		if (e != null && nested != null) {
			e.focusOnNestedType(nested);
		}
		return e != null;
	}
}
