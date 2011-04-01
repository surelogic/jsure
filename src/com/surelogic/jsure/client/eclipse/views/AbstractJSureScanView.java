package com.surelogic.jsure.client.eclipse.views;

import java.io.*;
import java.util.LinkedList;
import java.util.logging.Level;

import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.*;


import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.*;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.fluid.javac.scans.*;
import com.surelogic.jsure.core.preferences.JSureEclipseHub;

import edu.cmu.cs.fluid.java.ISrcRef;

/**
 * Handles whether there is any scan to show.
 * diff?
 * 
 * @author Edwin
 */
public abstract class AbstractJSureScanView extends ViewPart implements IJSureScanListener {
	protected static final String NO_RESULTS = I18N.msg("jsure.eclipse.view.no.scan.msg");
	
	protected PageBook f_viewerbook = null;	

	protected Label f_noResultsToShowLabel = null;

	protected Clipboard f_clipboard;

	private Action f_doubleClickAction;
	
	private Control f_viewerControl;
	
	/**
	 * The view title from the XML, or {@code null} if we couldn't get it.
	 */
	private String f_viewTitle;
	
	protected AbstractJSureScanView() {
		JSureEclipseHub.init();
		JSureScansHub.getInstance().addListener(this);
	}

	@Override
	public final void createPartControl(Composite parent) {
		f_viewerbook = new PageBook(parent, SWT.NONE);
		f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
		f_noResultsToShowLabel.setText(NO_RESULTS);
		f_clipboard = new Clipboard(getSite().getShell().getDisplay());
		f_viewTitle = getPartName();
		
		f_viewerControl = buildViewer(f_viewerbook);
		makeActions();
		if (getViewer() != null) {
			hookDoubleClickAction(getViewer());
			hookContextMenu(getViewer());
		}
		contributeToActionBars();
		updateViewState(ScanStatus.BOTH_CHANGED);
	}

	/**
	 * Setup the custom view
	 */
	protected abstract Control buildViewer(Composite parent);
	
	/**
	 * Enables various functionality if non-null
	 */
	protected StructuredViewer getViewer() {
		return null;
	}
	
	@Override
	public void setFocus() {
		// TODO is this right with the pagebook?
		f_viewerControl.setFocus();
	}

	@Override
	public void scansChanged(ScanStatus status) {
		updateViewState(status);
	}
		
	/**
	 * @return The label to be shown in the title
	 */
	protected abstract String updateViewer(ScanStatus status);
	
	/**
	 * Update the internal state, presumably after a new scan
	 */
	private void updateViewState(ScanStatus status) {
		if (status.changed()) {
			final String label = updateViewer(status);
			f_viewerbook.getDisplay().asyncExec (new Runnable () {
			      public void run () {
			    	  if (label != null) {
			    		  if (getViewer() != null) {
			    			  getViewer().setInput(getViewSite());
			    		  }
			    		  setViewerVisibility(true);
			    		  updateViewTitle(label);			
			    	  } else {
			    		  setViewerVisibility(false);
			    		  updateViewTitle(null);
			    	  }
			    	  // TODO is this right?
			    	  f_viewerControl.redraw();
			      }
			 });
		}
	}
	
	private final void setViewerVisibility(boolean showResults) {
		if (f_viewerbook.isDisposed())
			return;
		if (showResults) {
			f_viewerbook.showPage(f_viewerControl);
		} else {
			f_viewerbook.showPage(f_noResultsToShowLabel);
		}
	}
	
	/**
	 * Used to set the view title. We use this method to add the project of
	 * focus to JSure to the view title.
	 */
	private void updateViewTitle(String label) {
		/*
		 * Set a default if we got a null for the view title from the plug-in
		 * XML.
		 */
		if (f_viewTitle == null) {
			f_viewTitle = "Verification Status";
			SLLogger.getLogger().log(
					Level.WARNING,
					"Didn't get a view title from XML using \"" + f_viewTitle
							+ "\"");
		}

		if (label != null) {
			setPartName(f_viewTitle + " (" + label + ")");
		} else {
			setPartName(f_viewTitle);
		}
	}
	
	/********************* Setup methods ******************************/
	
	private void hookContextMenu(final StructuredViewer viewer) {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				IStructuredSelection s = (IStructuredSelection) viewer.getSelection();
				AbstractJSureScanView.this.fillContextMenu_private(manager, s);
			}
		});
		Menu menu = menuMgr.createContextMenu(f_viewerControl);
		f_viewerControl.setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}
	
	private void fillContextMenu_private(IMenuManager manager, IStructuredSelection s) {
		fillContextMenu(manager, s);
		manager.add(new Separator());
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillGlobalActionHandlers(bars);
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	protected void fillGlobalActionHandlers(IActionBars bars) {
		// Nothing to do yet
	}
	
	protected abstract void fillLocalPullDown(IMenuManager manager);

	protected abstract void fillLocalToolBar(IToolBarManager manager);

	protected abstract void makeActions();
	
	private void hookDoubleClickAction(final StructuredViewer viewer) {
		f_doubleClickAction = new Action() {
			@Override
			public void run() {
				ISelection selection = viewer.getSelection();
				handleDoubleClick((IStructuredSelection) selection);
			}
		};
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				f_doubleClickAction.run();
			}
		});
	}
	
	protected void handleDoubleClick(IStructuredSelection selection) {
		// Nothing to do yet
	}

	protected void fillContextMenu(IMenuManager manager,
			IStructuredSelection s) {
		// Nothing to do yet
	}
	
	/********************* Utility methods ******************************/
	
	protected final void showMessage(String message) {
		MessageDialog.openInformation(f_viewerControl.getShell(), this
				.getClass().getSimpleName(), message);
	}
	
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
	
	/********************* Utility methods to help with persistent state ******************************/
	
	/**
	 * Create a list if there's something to add
	 */
	protected static LinkedList<String> loadStrings(BufferedReader br, LinkedList<String> strings) throws IOException {		
		String line;
		if (strings != null) {
			strings.clear();
		}
		while ((line = br.readLine()) != null) {
			if (line.length() == 0) {
				break;
			}
			if (strings == null) {
				strings = new LinkedList<String>();
			}
			strings.add(line);
			//System.out.println("Loaded: "+line);
		}
		return strings;
	}
	
	protected static void saveStrings(PrintWriter pw, LinkedList<String> strings) {
		for(String s : strings) {
			//System.out.println("Saving: "+s);
			pw.println(s); // TODO what if there are newlines?
		}
		pw.println(); // Marker for the end of the list
	}
}
