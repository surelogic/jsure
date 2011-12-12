package com.surelogic.jsure.client.eclipse.views.results;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.views.AbstractScanStructuredView;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTableView;
import com.surelogic.jsure.core.preferences.JSurePreferencesUtility;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.IProposedPromiseDropInfo;

public class ProposedPromiseView extends
		AbstractScanStructuredView<IProposedPromiseDropInfo> {

	private final ProposedPromiseContentProvider f_content;

	private final Action f_toggleView;

	private final Action f_toggleFilter;

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
		final String toggleTreeToTableLabel = I18N
				.msg("jsure.eclipse.proposed.promises.showAsTree");
		f_toggleView = new Action(toggleTreeToTableLabel, IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				setViewerBeingShown(f_toggleView.isChecked());
			}
		};
		f_toggleView.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_JAVA_DECLS_TREE));
		f_toggleView.setToolTipText(toggleTreeToTableLabel);
		/*
		 * Set the view to tree or table
		 */
		f_toggleView.setChecked(persistedAsTree);
		f_content = new ProposedPromiseContentProvider(persistedAsTree);

		/*
		 * Setup toggle to filter list of promises
		 */
		final String toggleShowAbductiveOnlyLabel = I18N
				.msg("jsure.eclipse.proposed.promises.showAbductiveOnly");
		f_toggleFilter = new Action(toggleShowAbductiveOnlyLabel,
				IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				setShowAbductiveOnly(f_toggleFilter.isChecked());
			}
		};
		f_toggleFilter.setChecked(persistedShowAbductiveOnly);
		f_toggleFilter.setToolTipText(toggleShowAbductiveOnlyLabel);
		f_toggleFilter.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_ANNOTATION_ABDUCTIVE));
	}

	@Override
	protected void makeActions() {
		f_annotate.setText(I18N.msg("jsure.eclipse.proposed.promise.edit"));
		f_annotate.setToolTipText(I18N
				.msg("jsure.eclipse.proposed.promise.tip"));
		f_annotate.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_QUICK_ASSIST));

		f_copy.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_EDIT_COPY));
	}

	@Override
	protected String updateViewer() {
		return f_content.build();
	}

	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		super.fillLocalPullDown(manager);
		manager.add(f_toggleView);
		manager.add(f_toggleFilter);
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		super.fillLocalToolBar(manager);
		manager.add(f_toggleView);
		manager.add(f_toggleFilter);
	}

	@Override
	protected void fillContextMenu(final IMenuManager manager,
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

		treeViewer.setContentProvider(f_content);
		treeViewer.setLabelProvider(f_content);
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
		f_content.setAsTree(asTree);
		if (getViewer() != null) {
			getViewer().setInput(getViewSite());
		}
		f_viewerbook.showPage(getCurrentControl());
		getCurrentControl().redraw();
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
}
