package edu.cmu.cs.fluid.dcf.views;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.eclipse.EclipseUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.client.eclipse.views.JSureHistoricalSourceView;

import edu.cmu.cs.fluid.dc.IAnalysisListener;
import edu.cmu.cs.fluid.dc.Nature;
import edu.cmu.cs.fluid.dc.NotificationHub;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.SeaObserver;
import edu.cmu.cs.fluid.sea.drops.ProjectDrop;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * This class is designed to provide a TreeViewer when results are available
 * from analysis, and to show a message otherwise.
 */
public abstract class AbstractDoubleCheckerView extends ViewPart implements
		IAnalysisListener, SeaObserver {
	protected static final Logger LOG = SLLogger
			.getLogger("AbstractDoubleCheckerView");

	final public static Point ICONSIZE = new Point(22, 16);

	/**
	 * leave {@code null} if the subclass doesn't want to use this capability.
	 */
	protected PageBook f_viewerbook = null;

	protected Label f_noResultsToShowLabel = null;

	protected static final String PLEASE_WAIT = "Performing analysis...please wait";

	protected static final String COMP_ERRORS = "Compilation errors exist...please fix them";

	protected static final String NO_RESULTS = "No results...please enable JSure on a project (or open a closed project)";

	protected ColumnViewer viewer;
	protected TreeViewer treeViewer;
	protected TableViewer tableViewer;
	protected Clipboard clipboard;

	private Action doubleClickAction;

	private final boolean f_useTable;

	/**
	 * The view title from the XML, or {@code null} if we couldn't get it.
	 */
	private String f_viewTitle;

	protected AbstractDoubleCheckerView() {
		this(false);
	}

	protected AbstractDoubleCheckerView(boolean useTable) {
		f_useTable = useTable;
	}

	@Override
	public final void createPartControl(Composite parent) {
		f_viewerbook = new PageBook(parent, SWT.NONE);
		f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
		f_noResultsToShowLabel.setText(NO_RESULTS);
		if (f_useTable) {
			viewer = tableViewer = new TableViewer(f_viewerbook, SWT.H_SCROLL
					| SWT.V_SCROLL | SWT.FULL_SELECTION);
		} else {
			viewer = treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL
					| SWT.V_SCROLL);
		}
		setupViewer();
		clipboard = new Clipboard(getSite().getShell().getDisplay());

		viewer.setInput(getViewSite());
		makeActions_private();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		// start empty until the initial build is done
		setViewerVisibility(false);
		// subscribe to listen for analysis notifications
		NotificationHub.addAnalysisListener(this);
		Sea.getDefault().addSeaObserver(this);

		f_viewTitle = getPartName();
	}

	/**
	 * Creates the content/label provider and sorter, as well as any other
	 * viewer state
	 */
	protected abstract void setupViewer();

	/**
	 * Used to create a fancy waiting screen while JSure analysis runs. A copy
	 * of the tree being shown in the display is made into an image. This image
	 * is then grayed out and displayed. When the analysis is completed this
	 * image is disposed.
	 */
	private Image f_fancyWait = null;

	/**
	 * Toggles between the empty viewer page and the Fluid results
	 */
	protected final void setViewerVisibility(boolean showResults) {
		if (f_viewerbook.isDisposed())
			return;
		if (showResults) {
			if (f_fancyWait != null) {
				f_fancyWait.dispose();
				f_fancyWait = null;
			}
			viewer.setInput(getViewSite());
			f_viewerbook.showPage(viewer.getControl());
		} else {
			/*
			 * Check if there is actually a project before we show grayed view.
			 * We also need to ensure that there are not compilation errors in
			 * the project.
			 */
			final String projName = ProjectDrop.getProject();
			if (projName != null
					&& !COMP_ERRORS.equals(f_noResultsToShowLabel.getText())) {
				final Control c = viewer.getControl();
				if (c != null && c.isVisible()) {
					try {
						final Display display = c.getDisplay();
						final Point tableSize = c.getSize();
						if (display != null && tableSize.x > 0
								&& tableSize.y > 0) {
							GC gc = new GC(c);
							final Image image = new Image(display, tableSize.x,
									tableSize.y);
							gc.copyArea(image, 0, 0);
							gc.dispose();
							f_fancyWait = new Image(display, image,
									SWT.IMAGE_GRAY);
							image.dispose();
							f_noResultsToShowLabel.setImage(f_fancyWait);
						}
					} catch (Exception e) {
						SLLogger
								.getLogger()
								.log(
										Level.SEVERE,
										"Failure to create gray Verification Status image to show while analysis is running",
										e);
					}
				}
			} 
			if (projName == null) {
				viewer.setInput(null);
			}
			f_viewerbook.showPage(f_noResultsToShowLabel);
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				IStructuredSelection s = (IStructuredSelection) viewer
						.getSelection();
				AbstractDoubleCheckerView.this.fillContextMenu_private(manager,
						s);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
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

	private void fillContextMenu_private(IMenuManager manager,
			IStructuredSelection s) {
		fillContextMenu(manager, s);
		manager.add(new Separator());
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void makeActions_private() {
		doubleClickAction = new Action() {
			@Override
			public void run() {
				ISelection selection = viewer.getSelection();
				handleDoubleClick((IStructuredSelection) selection);
			}
		};

		makeActions();
		setViewState();
	}

	protected abstract void fillContextMenu(IMenuManager manager,
			IStructuredSelection s);

	protected abstract void fillLocalPullDown(IMenuManager manager);

	protected abstract void fillLocalToolBar(IToolBarManager manager);

	protected abstract void makeActions();

	protected abstract void handleDoubleClick(IStructuredSelection selection);

	/**
	 * Open and highlight a line within the Java editor, if possible. Otherwise,
	 * try to open as a text file
	 * 
	 * @param srcRef
	 *            the source reference to highlight
	 */
	protected void highlightLineInJavaEditor(ISrcRef srcRef) {
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

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	protected final void showMessage(String message) {
		MessageDialog.openInformation(viewer.getControl().getShell(), this
				.getClass().getSimpleName(), message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public final void setFocus() {
		setViewState();
		viewer.getControl().setFocus();
	}

	/**
	 * Ensure that any relevant view state is set, based on the internal state
	 */
	protected abstract void setViewState();

	/**
	 * The lock is used to ensure only one thread runs this at a time. Eclipse
	 * may try while the user has pushed the button.
	 */
	protected final synchronized void refreshView() {
		if (viewer != null) {
			try {
				edu.cmu.cs.fluid.ide.IDE.runAtMarker(new AbstractRunner() {
					public void run() {
						updateView();
					}
				});
				viewer.refresh();
				updateViewTitle();
			} catch (Exception e) {
				// @ignore since this SHOULD only happen on shutdown
			}
		}
	}

	/**
	 * Update the internal state, presumably after an double-checker run
	 */
	protected abstract void updateView();

	private final List<LinkedList<String>> f_stringPaths = new ArrayList<LinkedList<String>>();

	private final LinkedList<String> f_selectionPath = new LinkedList<String>();

	private void saveViewState() {
		if (treeViewer != null) {
			f_stringPaths.clear();
			final TreePath[] treePaths = treeViewer.getExpandedTreePaths();
			for (TreePath path : treePaths) {
				final LinkedList<String> stringPath = new LinkedList<String>();
				f_stringPaths.add(stringPath);
				for (int i = 0; i < path.getSegmentCount(); i++) {
					String message = path.getSegment(i).toString();
					stringPath.add(message);
				}
			}

			f_selectionPath.clear();
			final ITreeSelection selection = (ITreeSelection) viewer
					.getSelection();
			if (selection != null) {
				final TreePath[] paths = selection.getPaths();
				if (paths != null && paths.length > 0) {
					final TreePath path = paths[0];
					for (int i = 0; i < path.getSegmentCount(); i++) {
						String message = path.getSegment(i).toString();
						f_selectionPath.add(message);
					}
				}
			}
		}
	}

	private void restoreViewState() {
		final IContentProvider cp = viewer.getContentProvider();
		if (cp instanceof ITreeContentProvider) {
			/*
			 * Restore the state of the tree (as best we can).
			 */
			final ITreeContentProvider tcp = (ITreeContentProvider) cp;
			for (LinkedList<String> path : f_stringPaths) {
				restoreSavedPath(tcp, path, null);
			}

			/*
			 * Restore the selection (scrolls the view back to where the user
			 * was).
			 */
			if (!f_selectionPath.isEmpty())
				restoreSavedSelection(tcp, f_selectionPath, null, null);
		}
	}

	private void restoreSavedPath(final ITreeContentProvider tcp,
			LinkedList<String> path, Object parent) {
		if (path.isEmpty())
			return;

		final Object[] elements;
		if (parent == null) {
			// at the root
			elements = tcp.getElements(null);
		} else {
			elements = tcp.getChildren(parent);
		}

		final String message = path.removeFirst();
		if (message == null)
			return;
		for (Object element : elements) {
			if (message.equals(element.toString())) {
				/*
				 * We have to be careful to only expand the last element in the
				 * path. This is because the getExpandedTreePaths states that
				 * it:
				 * 
				 * Returns a list of tree paths corresponding to expanded nodes
				 * in this viewer's tree, including currently hidden ones that
				 * are marked as expanded but are under a collapsed ancestor.
				 */
				if (path.isEmpty()) {
					treeViewer.setExpandedState(element, true);
				} else {
					restoreSavedPath(tcp, path, element);
				}
			}
		}
	}

	private void restoreSavedSelection(final ITreeContentProvider tcp,
			LinkedList<String> path, Object parent, List<Object> treePath) {
		if (path.isEmpty())
			return;

		final Object[] elements;
		if (parent == null) {
			// at the root
			elements = tcp.getElements(null);
			treePath = new ArrayList<Object>();
		} else {
			elements = tcp.getChildren(parent);
		}

		final String message = path.removeFirst();
		if (message == null)
			return;
		boolean found = false;
		for (Object element : elements) {
			if (message.equals(element.toString())) {
				found = true;
				treePath.add(element);
				if (path.isEmpty()) {
					/*
					 * Exact match of the old selection has been found.
					 */
					ISelection selection = new TreeSelection(new TreePath(
							treePath.toArray()));
					viewer.setSelection(selection);
				} else {
					restoreSavedSelection(tcp, path, element, treePath);
				}
			}
		}
		/*
		 * In the case that part of the selection went away during analysis (for
		 * example, if we had a red-X selected and we fixed it) then select the
		 * remaining root of the same path.
		 */
		if (!found && !treePath.isEmpty()) {
			ISelection selection = new TreeSelection(new TreePath(treePath
					.toArray()));
			viewer.setSelection(selection);
		}
	}

	/*
	 * Implementation of IAnalysisListener
	 */

	public final void analysisStarting() {
		LOG.fine("analysisStarting() called");
		if (f_viewerbook != null && !f_viewerbook.isDisposed()) {
			f_viewerbook.getDisplay().asyncExec(new Runnable() {
				public void run() {
					saveViewState();
					f_noResultsToShowLabel.setText(PLEASE_WAIT);
					setViewerVisibility(false);
				}
			});
		}
	}

	public final void analysisCompleted() {
		LOG.fine("analysisCompleted() called");
		if (f_viewerbook != null && !f_viewerbook.isDisposed()) {
			f_viewerbook.getDisplay().asyncExec(new Runnable() {
				public void run() {
					refreshView();
					restoreViewState();
					setViewerVisibility(true);
				}
			});
		}
	}

	public final void analysisPostponed() {
		LOG.fine("analysisPostponed() called");
		if (f_viewerbook != null && !f_viewerbook.isDisposed()) {
			f_viewerbook.getDisplay().asyncExec(new Runnable() {
				public void run() {
					f_noResultsToShowLabel.setText(COMP_ERRORS);
					setViewerVisibility(false);
				}
			});
		}
	}

	public final void seaChanged() {
		LOG.fine("seaChanged() called");
		if (f_viewerbook != null && !f_viewerbook.isDisposed()) {
			f_viewerbook.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!Nature.hasNatureAnyProject()) {
						f_noResultsToShowLabel.setText(NO_RESULTS);
						setViewerVisibility(false);
					}
				}
			});
		}		
	}

	/**
	 * Used to set the view title. We use this method to add the project of
	 * focus to JSure to the view title.
	 */
	private void updateViewTitle() {
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
		/*
		 * Try to get a project name from drop-sea and add it.
		 */
		final String projName = ProjectDrop.getProject();
		if (projName != null) {
			setPartName(f_viewTitle + " (" + projName + ")");
		} else {
			setPartName(f_viewTitle);
		}
	}

	/*
	 * For use by view contribution actions in other plug-ins so that they can
	 * get a pointer to the TreeViewer
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(final Class adapter) {
		if (adapter == TreeViewer.class) {
			return viewer;
		} else {
			return super.getAdapter(adapter);
		}
	}
}