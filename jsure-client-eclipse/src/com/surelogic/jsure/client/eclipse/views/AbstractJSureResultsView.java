package com.surelogic.jsure.client.eclipse.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
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
import edu.cmu.cs.fluid.util.AbstractRunner;

/**
 * This class is designed to provide a TreeViewer when results are available
 * from analysis, and to show a message otherwise.
 */
public abstract class AbstractJSureResultsView extends ViewPart {

	final public static Point ICONSIZE = new Point(22, 16);

	/**
	 * leave {@code null} if the subclass doesn't want to use this capability.
	 */
	protected PageBook f_viewerbook = null;

	protected Label f_noResultsToShowLabel = null;
	protected TreeViewer treeViewer;

	protected static final String PLEASE_WAIT = "Performing analysis...please wait";
	protected static final String COMP_ERRORS = "Compilation errors exist...please fix them";

	private Action doubleClickAction;

	private final int f_extraStyle;

	protected AbstractJSureResultsView() {
		this(SWT.NONE);
	}

	protected AbstractJSureResultsView(int extraStyle) {
		f_extraStyle = extraStyle;
	}

	@Override
	public void createPartControl(Composite parent) {
		f_viewerbook = new PageBook(parent, SWT.NONE);
		f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
		f_noResultsToShowLabel.setText(I18N
				.msg("jsure.eclipse.view.no.scan.msg"));
		treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL | SWT.V_SCROLL
				| f_extraStyle);
		setupViewer();

		treeViewer.setInput(getViewSite());
		makeActions_private();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		// start empty until the initial build is done
		setViewerVisibility(false);

		finishCreatePartControl();
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
			treeViewer.setInput(getViewSite());
			f_viewerbook.showPage(treeViewer.getControl());
		} else {
			f_viewerbook.showPage(f_noResultsToShowLabel);
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				IStructuredSelection s = (IStructuredSelection) treeViewer
						.getSelection();
				AbstractJSureResultsView.this.fillContextMenu_private(manager,
						s);
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
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
				ISelection selection = treeViewer.getSelection();
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
		EditorUtil.highlightLineInJavaEditor(srcRef);
	}

	private void hookDoubleClickAction() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	protected final void showMessage(String message) {
		MessageDialog.openInformation(treeViewer.getControl().getShell(), this
				.getClass().getSimpleName(), message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public final void setFocus() {
		setViewState();
		treeViewer.getControl().setFocus();
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
		if (treeViewer != null) {
			try {
				edu.cmu.cs.fluid.ide.IDE.runAtMarker(new AbstractRunner() {
					public void run() {
						updateView();
					}
				});
				treeViewer.refresh();
			} catch (Exception e) {
				// @ignore since this SHOULD only happen on shutdown
			}
		}
	}

	/**
	 * Update the internal state, presumably after an double-checker run
	 */
	protected abstract void updateView();

	/*
	 * For use by view contribution actions in other plug-ins so that they can
	 * get a pointer to the TreeViewer
	 */
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") final Class adapter) {
		if (adapter == TreeViewer.class) {
			return treeViewer;
		} else {
			return super.getAdapter(adapter);
		}
	}
}