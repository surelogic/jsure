package com.surelogic.jsure.client.eclipse.views;

import java.io.*;
import java.util.logging.Level;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;
import org.xml.sax.InputSource;


import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.views.AbstractSLView;
import com.surelogic.jsure.client.eclipse.views.JSureHistoricalSourceView;
import com.surelogic.jsure.core.driver.JavacEclipse;
import com.surelogic.xml.PackageAccessor;
import com.surelogic.xml.TestXMLParserConstants;
import com.surelogic.xml.XMLGenerator;

import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
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
					if (elt instanceof IClassFile) {
						final String root = JavacEclipse.getDefault().getStringPreference(IDEPreferences.JSURE_XML_DIRECTORY);
						final char slash  = File.separatorChar;
						final String pkg  = srcRef.getPackage().replace('.', slash);
						String name = srcRef.getCUName();
						if (name.endsWith(".class")) {
							name = name.substring(0, name.length() - 6);
						}
						final String path = root + slash + pkg + slash + name + TestXMLParserConstants.SUFFIX;
						final File xml = new File(path);
						if (!xml.exists()) {
							xml.getParentFile().mkdirs();

							// Create a template?
							try {
								PrintWriter pw = new PrintWriter(xml);
								// Try to copy from fluid first
								try {
									final InputSource is = PackageAccessor.readPackage(srcRef.getPackage(), name+TestXMLParserConstants.SUFFIX);									
									if (is.getCharacterStream() == null) {
										if (is.getByteStream() != null) {									
											is.setCharacterStream(new InputStreamReader(is.getByteStream()));
										} else {
											// Try generating instead
											throw new FileNotFoundException();
										}
									}
									pw.println("<!-- Generated from the original XML within JSure -->");
									char[] buf = new char[8192];
									int read; 
									while ((read = is.getCharacterStream().read(buf)) >= 0) {
										pw.write(buf, 0, read);
									}									
								} catch (FileNotFoundException e) {
									// No such XML in fluid, so generate something
									IRNode ast = null;
									if (ast != null) {
										// Currently unused, since there's no good way to get the right AST
										final String s = XMLGenerator.generateStringXML(ast, true);
										pw.println(s);
									} else {
										pw.println("<package name=\""+srcRef.getPackage()+"\">");
										pw.println("  <class name=\""+name+"\">");
										pw.println("  </class>");								
										pw.println("</package>");
									}
								} finally {								
									pw.close();
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						boolean success = EclipseUIUtility.openInEditor(path);
						if (success) {
							return;
						}
					}					
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

