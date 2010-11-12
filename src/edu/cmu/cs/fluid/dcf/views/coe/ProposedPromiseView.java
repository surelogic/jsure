package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;

import com.surelogic.common.eclipse.ColumnViewerSorter;
import com.surelogic.common.i18n.I18N;
import com.surelogic.jsure.client.eclipse.refactor.ProposedPromisesRefactoringAction;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
import edu.cmu.cs.fluid.sea.IProposedPromiseDropInfo;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

public class ProposedPromiseView extends AbstractDoubleCheckerView {

	private final ProposedPromiseContentProvider f_content = new ProposedPromiseContentProvider();

	private final Action f_annotate = new ProposedPromisesRefactoringAction() {

		@Override
		protected List<? extends IProposedPromiseDropInfo> getProposedDrops() {
			return getSelectedRows();
		}

		@Override
		protected String getDialogTitle() {
			return I18N.msg("jsure.eclipse.proposed.promises.edit");
		}
	};

	public ProposedPromiseView() {
		super(true, SWT.MULTI);
	}

	protected List<? extends IProposedPromiseDropInfo> getSelectedRows() {
		final IStructuredSelection selection = (IStructuredSelection) viewer
				.getSelection();
		final List<ProposedPromiseDrop> result = new ArrayList<ProposedPromiseDrop>();
		for (final Object element : selection.toList()) {
			if (element instanceof ProposedPromiseDrop) {
				result.add((ProposedPromiseDrop) element);
			}
		}
		return result;
	}

	@Override
	protected void makeActions() {
		f_annotate.setText(I18N.msg("jsure.eclipse.proposed.promises.edit"));
		f_annotate.setToolTipText(I18N
				.msg("jsure.eclipse.proposed.promises.tip"));
	}

	@Override
	protected void fillContextMenu(final IMenuManager manager,
			final IStructuredSelection s) {
		if (!s.isEmpty()) {
			manager.add(f_annotate);
		}
	}

	@Override
	protected void fillLocalPullDown(final IMenuManager manager) {
		// nothing
	}

	@Override
	protected void fillLocalToolBar(final IToolBarManager manager) {
		// nothing
	}

	@Override
	protected void setViewState() {
		// nothing
	}

	@Override
	protected void setupViewer() {
		int i = 0;
		for (final String label : ProblemsViewContentProvider.COLUMN_LABELS) {
			final TableViewerColumn column = new TableViewerColumn(tableViewer,
					SWT.LEFT);
			column.getColumn().setText(label);
			column.getColumn().setWidth(40 * label.length());

			setupSorter(column, i);
			i++;
		}

		viewer.setContentProvider(f_content);
		viewer.setLabelProvider(f_content);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().pack();
	}

	protected void setupSorter(final TableViewerColumn column, final int colIdx) {
		final boolean intSort = "Line".equals(column.getColumn().getText());
		new ColumnViewerSorter<ProposedPromiseDrop>(tableViewer, column
				.getColumn()) {
			@Override
			protected int doCompare(final Viewer viewer,
					final ProposedPromiseDrop e1, final ProposedPromiseDrop e2) {
				final ITableLabelProvider lp = (ITableLabelProvider) tableViewer
						.getLabelProvider();
				final String t1 = lp.getColumnText(e1, colIdx);
				final String t2 = lp.getColumnText(e2, colIdx);
				if (intSort) {
					return Integer.parseInt(t1) - Integer.parseInt(t2);
				}
				return t1.compareTo(t2);
			}

		};
	}

	@Override
	protected void updateView() {
		f_content.build();
	}

	@Override
	protected void handleDoubleClick(final IStructuredSelection selection) {
		final ProposedPromiseDrop d = (ProposedPromiseDrop) selection
				.getFirstElement();
		highlightLineInJavaEditor(d.getSrcRef());
	}
}
