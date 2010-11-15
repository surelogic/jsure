package edu.cmu.cs.fluid.dcf.views.coe;

import java.util.*;


import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.SLImages;
import com.surelogic.common.eclipse.ViewUtility;

import edu.cmu.cs.fluid.dcf.views.*;
import edu.cmu.cs.fluid.sea.*;

public class JSureProblemsView extends AbstractResultsTableView<IProofDropInfo> {
	private Action f_copy;
	
	public JSureProblemsView() {
		super(SWT.NONE, IProofDropInfo.class, new ContentProvider());
	}

	@Override
	protected void makeActions() {
		f_copy = makeCopyAction("Copy", "Copy the selected problem to the clipboard");
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
	
	protected void handleDoubleClick(IProofDropInfo d) {
		final ResultsView view = (ResultsView) ViewUtility.showView(ResultsView.class.getName());
		if (view != null && d instanceof ProofDrop) {
			view.showDrop((ProofDrop) d);
		}
	}
	
	static class ContentProvider extends AbstractResultsTableContentProvider<IProofDropInfo> {
		ContentProvider() {
			super("Problems");
		}

		@Override
		protected void getAndSortResults(List<IProofDropInfo> contents) {
			Set<? extends ProofDrop> drops = Sea.getDefault().getDropsOfType(ResultDrop.class);
			for (ProofDrop id : drops) {
				if (!id.isConsistent()) {
					contents.add(id);
				}
			}
			Collections.sort(contents, sortByLocation);
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 0) {
				return SLImages.getImage(CommonImages.IMG_RED_X);
			} else {
				return null;
			}
		}
	}
}
