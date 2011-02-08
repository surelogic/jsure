package edu.cmu.cs.fluid.dcf.views.coe;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

import com.surelogic.jsure.client.eclipse.views.AbstractResultsTableView;

import edu.cmu.cs.fluid.dcf.views.*;
import edu.cmu.cs.fluid.sea.*;

public class ProblemsView extends AbstractResultsTableView<IDropInfo> {
	private final Action f_copy = makeCopyAction("Copy", "Copy the selected problem to the clipboard");
	
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
}
