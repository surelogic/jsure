package com.surelogic.jsure.client.eclipse.views.results;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.jsure.client.eclipse.views.AbstractScanStructuredView;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTableView;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.IProposedPromiseDropInfo;

public class ProposedPromiseView extends
		AbstractScanStructuredView<IProposedPromiseDropInfo> implements
		EclipseUIUtility.IContextMenuFiller {

	private final ProposedPromiseContentProvider f_content;

	private final Action f_toggleView;

	private final Action f_toggleFilter;

	private final Action f_actionExpand = new Action() {
		@Override
		public void run() {
			final StructuredViewer viewer = getViewer();
			if (viewer instanceof TreeViewer) {
				final TreeViewer treeViewer = (TreeViewer) viewer;
				final ITreeSelection selection = (ITreeSelection) treeViewer
						.getSelection();
				if (selection == null || selection.isEmpty()) {
					treeViewer.expandToLevel(50);
				} else {
					for (Object obj : selection.toList()) {
						if (obj != null) {
							treeViewer.expandToLevel(obj, 50);
						} else {
							treeViewer.expandToLevel(50);
						}
					}
				}
			}
		}
	};

	private final Action f_actionCollapse = new Action() {
		@Override
		public void run() {
			final StructuredViewer viewer = getViewer();
			if (viewer instanceof TreeViewer) {
				final TreeViewer treeViewer = (TreeViewer) viewer;
				final ITreeSelection selection = (ITreeSelection) treeViewer
						.getSelection();
				if (selection == null || selection.isEmpty()) {
					treeViewer.expandToLevel(50);
				} else {
					for (Object obj : selection.toList()) {
						if (obj != null) {
							treeViewer.collapseToLevel(obj, 1);
						} else {
							treeViewer.collapseAll();
						}
					}
				}
			}
		}
	};

	private final Action f_actionCollapseAll = new Action() {
		@Override
		public void run() {
			final StructuredViewer viewer = getViewer();
			if (viewer instanceof TreeViewer) {
				final TreeViewer treeViewer = (TreeViewer) viewer;
				treeViewer.collapseAll();
			}
		}
	};

	private final Action f_copy = makeCopyAction("Copy",
			"Copy the selected item to the clipboard");

	public ProposedPromiseView() {
		super(SWT.MULTI, IProposedPromiseDropInfo.class);
		/*
		 * Read persisted state of toggles that control what is displayed.
		 */
		boolean persistedAsTree = EclipseUtility
				.getBooleanPreference(JSurePreferencesUtility.PROPOSED_PROMISES_AS_TREE);
		boolean persistedShowAbductiveOnly = EclipseUtility
				.getBooleanPreference(JSurePreferencesUtility.PROPOSED_PROMISES_SHOW_ABDUCTIVE_ONLY);
		/*
		 * Setup toggle to change view from a tree to a table.
		 */
		f_toggleView = new Action(
				I18N.msg("jsure.eclipse.proposed.promises.showAsTree"),
				IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				setViewerBeingShown(f_toggleView.isChecked());
			}
		};
		f_toggleView.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_JAVA_DECLS_TREE));
		f_toggleView.setToolTipText(I18N
				.msg("jsure.eclipse.proposed.promises.showAsTree.tip"));
		/*
		 * Set the view to tree or table
		 */
		f_toggleView.setChecked(persistedAsTree);
		f_content = new ProposedPromiseContentProvider(persistedAsTree);

		/*
		 * Setup toggle to filter list of promises
		 */
		f_toggleFilter = new Action(
				I18N.msg("jsure.eclipse.proposed.promises.showAbductiveOnly"),
				IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				setShowAbductiveOnly(f_toggleFilter.isChecked());
			}
		};
		f_toggleFilter.setChecked(persistedShowAbductiveOnly);
		f_toggleFilter.setToolTipText(I18N
				.msg("jsure.eclipse.proposed.promises.showAbductiveOnly.tip"));
		f_toggleFilter.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_ANNOTATION_ABDUCTIVE));
		/*
		 * Set collapse all toggle button
		 */
		f_actionCollapseAll.setEnabled(persistedAsTree);
	}

	@Override
	protected void makeActions() {
		f_annotate.setText(I18N.msg("jsure.eclipse.proposed.promise.edit"));
		f_annotate.setToolTipText(I18N
				.msg("jsure.eclipse.proposed.promise.tip"));
		f_annotate.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_ANNOTATION_PROPOSED));
		f_annotate.setEnabled(false); // wait until something is selected

		f_copy.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_EDIT_COPY));

		f_actionExpand.setText("Expand");
		f_actionExpand
				.setToolTipText("Expand the current selection or all if none");
		f_actionExpand.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_EXPAND_ALL));

		f_actionCollapse.setText("Collapse");
		f_actionCollapse
				.setToolTipText("Collapse the current selection or all if none");
		f_actionCollapse.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));

		f_actionCollapseAll.setText("Collapse All");
		f_actionCollapseAll.setToolTipText("Collapse All");
		f_actionCollapseAll.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_COLLAPSE_ALL));
	}

	@Override
	protected String updateViewer() {
		return f_content.build();
	}

	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		super.fillLocalPullDown(manager);
		manager.add(f_actionCollapseAll);
		manager.add(new Separator());
		manager.add(f_annotate);
		manager.add(new Separator());
		manager.add(f_toggleView);
		manager.add(f_toggleFilter);

		final IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), f_copy);
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		super.fillLocalToolBar(manager);
		manager.add(f_actionCollapseAll);
		manager.add(new Separator());
		manager.add(f_annotate);
		manager.add(new Separator());
		manager.add(f_toggleView);
		manager.add(f_toggleFilter);
	}

	@Override
	protected void setupViewer(StructuredViewer viewer) {
		super.setupViewer(viewer);

		EclipseUIUtility.hookContextMenu(this, viewer, this);
	}

	public void fillContextMenu(final IMenuManager manager,
			final IStructuredSelection s) {
		if (!s.isEmpty()) {
			for (Object o : s.toArray()) {
				if (o instanceof IProposedPromiseDropInfo) {
					final IProposedPromiseDropInfo p = (IProposedPromiseDropInfo) o;
					ISrcRef ref = p.getSrcRef();
					if (ref != null) {
						manager.add(f_annotate);
						manager.add(new Separator());
						break; // Only needs one
					}
				}
			}
			if (getViewer() instanceof TreeViewer) {
				manager.add(f_actionExpand);
				manager.add(f_actionCollapse);
				manager.add(new Separator());
			}
			manager.add(f_copy);
		}
	}

	@Override
	protected List<? extends IProposedPromiseDropInfo> getSelectedProposals() {
		return getSelectedRows();
	}

	@Override
	protected StructuredViewer[] newViewers(Composite parent, int extraStyle) {
		final TableViewer tableViewer = AbstractScanTableView.makeTableViewer(
				parent, extraStyle, f_content);
		final TreeViewer treeViewer = new TreeViewer(parent, SWT.H_SCROLL
				| SWT.V_SCROLL | extraStyle);

		/*
		 * We want a double-click to also expand the tree if necessary. This
		 * will take care of that functionality.
		 */
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				ITreeSelection sel = (ITreeSelection) treeViewer.getSelection();
				if (sel == null)
					return;
				Object obj = sel.getFirstElement();
				if (obj == null)
					return;
				// open up the tree one more level
				if (!treeViewer.getExpandedState(obj)) {
					treeViewer.expandToLevel(obj, 1);
				}
			}
		});

		treeViewer.setContentProvider(f_content);
		treeViewer.setLabelProvider(f_content);

		final ISelectionChangedListener listener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectionChangedHelper();
			}
		};
		tableViewer.addSelectionChangedListener(listener);
		treeViewer.addSelectionChangedListener(listener);

		return new StructuredViewer[] { tableViewer, treeViewer };
	}

	@Override
	protected final int getViewIndex() {
		return f_content.showAsTree() ? 1 : 0;
	}

	@Override
	protected final void appendText(StringBuilder sb, Object elt) {
		if (f_content.showAsTree()) {
			sb.append(f_content.getText(elt));
		} else {
			for (int i = 0; i < f_content.getColumnLabels().length; i++) {
				if (i != 0) {
					sb.append(' ');
				}
				sb.append(f_content.getColumnText(elt, i));
			}
		}
	}

	private void setViewerBeingShown(final boolean asTree) {
		/*
		 * Persist user preference
		 */
		EclipseUtility.setBooleanPreference(
				JSurePreferencesUtility.PROPOSED_PROMISES_AS_TREE, asTree);

		/*
		 * Set the viewer
		 */
		f_actionCollapseAll.setEnabled(asTree);
		f_content.setAsTree(asTree);
		getViewer().setInput(getViewSite());
		f_viewerbook.showPage(getCurrentControl());

		/*
		 * We need a job to fix the toolbar/view menu because the viewer changed
		 * and things may or may not be selected in the new view. This will do
		 * it a bit later when the change settles.
		 */
		Job job = new SLUIJob() {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				selectionChangedHelper();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void setShowAbductiveOnly(final boolean applyFilter) {
		/*
		 * Persist user preference
		 */
		EclipseUtility.setBooleanPreference(
				JSurePreferencesUtility.PROPOSED_PROMISES_SHOW_ABDUCTIVE_ONLY,
				applyFilter);

		/*
		 * Rebuild the content for the viewer
		 */
		getViewer().getControl().setRedraw(false);
		f_content.build();
		getViewer().getControl().setRedraw(true);
		getViewer().refresh();
	}

	private void selectionChangedHelper() {
		final boolean proposalsSelected = !getSelectedProposals().isEmpty();
		f_annotate.setEnabled(proposalsSelected);
	}
}
