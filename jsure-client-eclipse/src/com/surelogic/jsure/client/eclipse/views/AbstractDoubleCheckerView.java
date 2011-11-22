package com.surelogic.jsure.client.eclipse.views;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.common.i18n.I18N;
import com.surelogic.jsure.client.eclipse.editors.EditorUtil;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.SeaObserver;
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * This class is designed to provide a TreeViewer when results are available
 * from analysis, and to show a message otherwise.
 */
public abstract class AbstractDoubleCheckerView extends ViewPart implements
		SeaObserver {

	final public static Point ICONSIZE = new Point(22, 16);

	/**
	 * leave {@code null} if the subclass doesn't want to use this capability.
	 */
	protected PageBook f_viewerbook = null;

	protected Label f_noResultsToShowLabel = null;

	protected static final String PLEASE_WAIT = "Performing analysis...please wait";

	protected static final String COMP_ERRORS = "Compilation errors exist...please fix them";

	protected ColumnViewer viewer;
	protected TreeViewer treeViewer;
	protected TableViewer tableViewer;
	protected Clipboard clipboard;

	private Action doubleClickAction;

	private final boolean f_useTable;

	private final int f_extraStyle;

	protected AbstractDoubleCheckerView() {
		this(false);
	}

	protected AbstractDoubleCheckerView(boolean useTable) {
		this(useTable, SWT.NONE);
	}

	protected AbstractDoubleCheckerView(boolean useTable, int extraStyle) {
		f_useTable = useTable;
		f_extraStyle = extraStyle;
	}

	@Override
	public void createPartControl(Composite parent) {
		f_viewerbook = new PageBook(parent, SWT.NONE);
		f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
		f_noResultsToShowLabel.setText(I18N
				.msg("jsure.eclipse.view.no.scan.msg"));
		if (f_useTable) {
			viewer = tableViewer = new TableViewer(f_viewerbook, SWT.H_SCROLL
					| SWT.V_SCROLL | SWT.FULL_SELECTION | f_extraStyle);
		} else {
			viewer = treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL
					| SWT.V_SCROLL | f_extraStyle);
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

		subscribe();

		finishCreatePartControl();
	}

	protected void subscribe() {
		// subscribe to listen for analysis notifications
		// NotificationHub.addAnalysisListener(this);
		Sea.getDefault().addSeaObserver(this);
	}

	@Override
	public void dispose() {
		try {
			// NotificationHub.unsubscribe(this);
			Sea.getDefault().removeSeaObserver(this);
		} finally {
			super.dispose();
		}
	}

	protected void finishCreatePartControl() {
		// Nothing to do right now
	}

	/**
	 * Creates the content/label provider and sorter, as well as any other
	 * viewer state
	 */
	protected abstract void setupViewer();

	/**
	 * Toggles between the empty viewer page and the Fluid results
	 */
	protected final void setViewerVisibility(boolean showResults) {
		if (f_viewerbook.isDisposed())
			return;
		if (showResults) {
			viewer.setInput(getViewSite());
			f_viewerbook.showPage(viewer.getControl());
		} else {
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
		try {
			EditorUtil.highlightLineInJavaEditor(srcRef);
		} catch (org.eclipse.core.runtime.CoreException e) {
			showMessage("CoreException was thrown");
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

	protected final void loadViewState(File location) throws IOException {
		if (location == null || !location.exists()) {
			return;
		}
		final BufferedReader br = new BufferedReader(new FileReader(location));
		try {
			loadStrings(br, f_selectionPath);

			f_stringPaths.clear();
			LinkedList<String> path = null;
			do {
				path = loadStrings(br, null);
				if (path == null) {
					break;
				}
				f_stringPaths.add(path);
			} while (path != null);
		} finally {
			br.close();
		}
		restoreViewState();
	}

	/**
	 * Create a list if there's something to add
	 */
	private static LinkedList<String> loadStrings(BufferedReader br,
			LinkedList<String> strings) throws IOException {
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
			// System.out.println("Loaded: "+line);
		}
		return strings;
	}

	protected final void saveViewState(File location) throws IOException {
		saveViewState();
		if (location != null) {
			final PrintWriter pw = new PrintWriter(location);
			try {
				saveStrings(pw, f_selectionPath);
				for (LinkedList<String> ll : f_stringPaths) {
					saveStrings(pw, ll);
				}
			} finally {
				pw.close();
			}
		}
	}

	private static void saveStrings(PrintWriter pw, LinkedList<String> strings) {
		for (String s : strings) {
			// System.out.println("Saving: "+s);
			pw.println(s); // TODO what if there are newlines?
		}
		pw.println(); // Marker for the end of the list
	}

	protected final void saveViewState() {
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

	protected final void restoreViewState() {
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
		} else {
			// System.out.println("Not a tree: "+cp);
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
		/*
		 * if (elements.length == 0) {
		 * System.out.println("No elts to restore to"); }
		 */
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
					// System.out.println("Expanded: "+message);
					treeViewer.setExpandedState(element, true);
				} else {
					restoreSavedPath(tcp, path, element);
				}
			} else {
				// System.out.println("Couldn't find: "+message);
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
		/*
		 * if (elements.length == 0) {
		 * System.out.println("No elts to restore to"); }
		 */
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
					// System.out.println("Selected: "+message);
					viewer.setSelection(selection);
				} else {
					restoreSavedSelection(tcp, path, element, treePath);
				}
			} else {
				// System.out.println("Couldn't find: "+message);
			}
		}
		/*
		 * In the case that part of the selection went away during analysis (for
		 * example, if we had a red-X selected and we fixed it) then select the
		 * remaining root of the same path.
		 */
		if (!found && !treePath.isEmpty()) {
			ISelection selection = new TreeSelection(new TreePath(
					treePath.toArray()));
			viewer.setSelection(selection);
		}
	}

	/*
	 * Implementation of SeaObserver
	 */

	public void seaChanged() {
		if (f_viewerbook != null && !f_viewerbook.isDisposed()) {
			f_viewerbook.getDisplay().asyncExec(new Runnable() {
				public void run() {
					refreshView();
				}
			});
		}
	}

	/*
	 * For use by view contribution actions in other plug-ins so that they can
	 * get a pointer to the TreeViewer
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(final Class adapter) {
		if (adapter == TreeViewer.class) {
			return viewer;
		} else {
			return super.getAdapter(adapter);
		}
	}
}