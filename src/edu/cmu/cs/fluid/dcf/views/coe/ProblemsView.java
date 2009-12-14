package edu.cmu.cs.fluid.dcf.views.coe;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

import edu.cmu.cs.fluid.dcf.views.AbstractDoubleCheckerView;
import edu.cmu.cs.fluid.sea.PromiseWarningDrop;

public class ProblemsView extends AbstractDoubleCheckerView {

	private final ProblemsViewContentProvider f_content = new ProblemsViewContentProvider();

	private final Action f_copy = new Action() {
		@Override
		public void run() {
			clipboard.setContents(new Object[] { getSelectedText() },
					new Transfer[] { TextTransfer.getInstance() });
		}
	};
	
	public ProblemsView() {
		super(true);
	}

	protected String getSelectedText() {
		IStructuredSelection selection = (IStructuredSelection) viewer
		.getSelection();
		StringBuilder sb = new StringBuilder();
		for (Object elt : selection.toList()) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			for(int i=0; i<3; i++) {
				if (i > 0) {
					sb.append(' ');
				}
				sb.append(f_content.getColumnText(elt, i));
			}
		}
		return sb.toString();
	}

	@Override
	protected void makeActions() {
	    f_copy.setText("Copy");
	    f_copy.setToolTipText("Copy the selected problem to the clipboard");
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
