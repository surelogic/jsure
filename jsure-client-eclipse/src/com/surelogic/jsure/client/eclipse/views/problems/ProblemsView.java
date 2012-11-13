package com.surelogic.jsure.client.eclipse.views.problems;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.dropsea.IDrop;
import com.surelogic.dropsea.IProposedPromiseDrop;
import com.surelogic.jsure.client.eclipse.preferences.UninterestingPackageFilterPreferencePage;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTableView;


public final class ProblemsView extends AbstractScanTableView<IDrop>
		implements EclipseUIUtility.IContextMenuFiller {

	private final Action f_copy = makeCopyAction(
			I18N.msg("jsure.problems.view.copy"),
			I18N.msg("jsure.problems.view.copy.tooltip"));

	private final Action f_preferences = new Action() {

		@Override
		public void run() {
			final String[] FILTER = new String[] { UninterestingPackageFilterPreferencePage.class
					.getName() };
			PreferencesUtil.createPreferenceDialogOn(null, FILTER[0], FILTER,
					null).open();
		}
	};

	public ProblemsView() {
		super(SWT.MULTI, IDrop.class, new ProblemsViewContentProvider());
	}

	@Override
	protected void setupViewer(StructuredViewer viewer) {
		super.setupViewer(viewer);

		EclipseUIUtility.hookContextMenu(this, viewer, this);
	}

	public void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
		if (!s.isEmpty()) {
			for (Object o : s.toArray()) {
				final IDrop info = (IDrop) o;
				if (!info.getProposals().isEmpty()) {
					manager.add(f_annotate);
					manager.add(new Separator());
				}
			}
			manager.add(f_copy);
		}
	}

	@Override
	protected void makeActions() {
		f_copy.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_EDIT_COPY));
		f_annotate.setText(I18N.msg("jsure.problems.view.fix"));
		f_annotate.setToolTipText(I18N.msg("jsure.problems.view.fix.tooltip"));
		f_annotate.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_ANNOTATION_PROPOSED));

		final StructuredViewer viewer = getViewer();
		final ISelectionChangedListener listener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final boolean proposalsSelected = !getSelectedProposals()
						.isEmpty();
				f_annotate.setEnabled(proposalsSelected);
			}
		};
		viewer.addSelectionChangedListener(listener);
		f_annotate.setEnabled(false); // wait until something is selected

		f_preferences.setImageDescriptor(SLImages
				.getImageDescriptor(CommonImages.IMG_FILTER));
		f_preferences.setText(I18N.msg("jsure.problems.view.filter"));
		f_preferences.setToolTipText(I18N
				.msg("jsure.problems.view.filter.tooltip"));
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		super.fillLocalPullDown(manager);
		manager.add(f_annotate);
		manager.add(new Separator());
		manager.add(f_preferences);

		/*
		 * Add a global action handler for copy
		 */
		final IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), f_copy);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		super.fillLocalToolBar(manager);
		manager.add(f_annotate);
		manager.add(new Separator());
		manager.add(f_preferences);
	}

	@Override
	protected List<? extends IProposedPromiseDrop> getSelectedProposals() {
		List<IProposedPromiseDrop> proposals = new ArrayList<IProposedPromiseDrop>();
		for (IDrop info : getSelectedRows()) {
			proposals.addAll(info.getProposals());
		}
		return proposals;
	}

	private final UIJob f_backgroundColorJob = new SLUIJob() {
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			final TableViewer tableViewer = (TableViewer) getViewer();
			if (tableViewer != null) {
				final Table table = tableViewer.getTable();
				if (!table.isDisposed()) {
					if (table.getItemCount() == 0) {
						table.setBackground(null);
					} else {
						table.setBackground(table.getDisplay().getSystemColor(
								SWT.COLOR_YELLOW));
					}
				}
			}
			return Status.OK_STATUS;
		}
	};

	@Override
	protected String updateViewer() {
		final String result = super.updateViewer();
		f_backgroundColorJob.schedule(300);
		return result;
	}
}
