package com.surelogic.jsure.client.eclipse.views.results;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTableView;

import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.IProposedPromiseDropInfo;

public class ProblemsView extends AbstractScanTableView<IDropInfo> {

	private final Action f_copy = makeCopyAction(
			I18N.msg("jsure.problems.view.copy"),
			I18N.msg("jsure.problems.view.copy.tooltip"));

	public ProblemsView() {
		super(SWT.NONE, IDropInfo.class, new ProblemsViewContentProvider());
	}

	@Override
	protected void fillGlobalActionHandlers(IActionBars bars) {
		bars.setGlobalActionHandler(ActionFactory.COPY.getId(), f_copy);
	}

	@Override
	protected void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
		if (!s.isEmpty()) {
			for (Object o : s.toArray()) {
				final IDropInfo info = (IDropInfo) o;
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
				.getImageDescriptor(CommonImages.IMG_QUICK_ASSIST));
	}

	@Override
	protected List<? extends IProposedPromiseDropInfo> getSelectedProposals() {
		List<IProposedPromiseDropInfo> proposals = new ArrayList<IProposedPromiseDropInfo>();
		for (IDropInfo info : getSelectedRows()) {
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
