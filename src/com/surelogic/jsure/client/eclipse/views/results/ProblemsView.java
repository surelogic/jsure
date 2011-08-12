package com.surelogic.jsure.client.eclipse.views.results;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTableView;

import edu.cmu.cs.fluid.sea.IDropInfo;

public class ProblemsView extends AbstractScanTableView<IDropInfo> {
	private final Action f_copy = makeCopyAction("Copy",
			"Copy the selected problem to the clipboard");

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
			manager.add(f_copy);
		}
	}

	@Override
	protected void makeActions() {
		// nothing to do
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
