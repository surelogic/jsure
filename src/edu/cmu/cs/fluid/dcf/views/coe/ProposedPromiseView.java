package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop;

public class ProposedPromiseView extends AbstractDoubleCheckerView {

	private final ProposedPromiseContentProvider f_content = new ProposedPromiseContentProvider();

	private final Action f_annotate = new Action() {
		@Override
		public void run() {
			final List<ProposedPromiseDrop> selected = getSelectedRows();
			if (selected.isEmpty())
				return;
			/*
			 * TODO Proposed the edit to the code in the dialog HERE (you are in
			 * the SWT thread)
			 */
			for (ProposedPromiseDrop pp : selected) {
				// there are lots of getters use just get the whole annotation
				// here
				System.out
						.println("proposed promise " + pp.getJavaAnnotation());
			}
		}
	};

	public ProposedPromiseView() {
		super(true, SWT.MULTI);
	}

	protected List<ProposedPromiseDrop> getSelectedRows() {
		IStructuredSelection selection = (IStructuredSelection) viewer
				.getSelection();
		final List<ProposedPromiseDrop> result = new ArrayList<ProposedPromiseDrop>();
		for (Object element : selection.toList()) {
			if (element instanceof ProposedPromiseDrop) {
				result.add((ProposedPromiseDrop) element);
			}
		}
		return result;
	}

	@Override
	protected void makeActions() {
		f_annotate.setText("Add promises to code...");
		f_annotate
				.setToolTipText("Add the selected proposed promises as annotations in the code.");
	}

	@Override
	protected void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
		if (!s.isEmpty()) {
			manager.add(f_annotate);
		}
	}

	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		// nothing
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		// nothing
	}

	@Override
	protected void setViewState() {
		// nothing
	}

	@Override
	protected void setupViewer() {
		for (String label : ProblemsViewContentProvider.COLUMN_LABELS) {
			final TableViewerColumn column = new TableViewerColumn(tableViewer,
					SWT.LEFT);
			column.getColumn().setText(label);
			column.getColumn().setWidth(40 * label.length());
		}

		viewer.setContentProvider(f_content);
		viewer.setLabelProvider(f_content);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().pack();
	}

	@Override
	protected void updateView() {
		f_content.build();
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		PromiseWarningDrop d = (PromiseWarningDrop) selection.getFirstElement();
		highlightLineInJavaEditor(d.getSrcRef());
	}
}
