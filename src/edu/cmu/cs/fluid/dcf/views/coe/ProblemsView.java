package edu.cmu.cs.fluid.dcf.views.coe;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;

public class ProblemsView extends AbstractDoubleCheckerView {

	private final ProblemsViewContentProvider f_content = new ProblemsViewContentProvider();

	public ProblemsView() {
		super(true);
	}

	@Override
	protected void makeActions() {
		// nothing
	}

	@Override
	protected void fillContextMenu(IMenuManager manager, IStructuredSelection s) {
		// nothing
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
