package edu.cmu.cs.fluid.dcf.views.coe;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

import edu.cmu.cs.fluid.dcf.views.*;
import edu.cmu.cs.fluid.sea.*;

public class ProblemsView extends AbstractResultsTableView<IDropInfo> {
	private final Action f_copy = new Action() {
		@Override
		public void run() {
			clipboard.setContents(new Object[] { getSelectedText() },
					new Transfer[] { TextTransfer.getInstance() });
		}
	};
	
	public ProblemsView() {
		super(SWT.NONE, IDropInfo.class, new ProblemsViewContentProvider());
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
}
