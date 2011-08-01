package com.surelogic.jsure.client.eclipse.views.results;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ui.SLImages;
import com.surelogic.jsure.client.eclipse.views.AbstractScanTableView;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.sea.IProofDropInfo;
import edu.cmu.cs.fluid.sea.ResultDrop;

public class JSureProblemsView extends AbstractScanTableView<IProofDropInfo> {
	private Action f_copy;

	public JSureProblemsView() {
		super(SWT.NONE, IProofDropInfo.class, new ContentProvider());
	}

	@Override
	protected void makeActions() {
		f_copy = makeCopyAction("Copy",
				"Copy the selected problem to the clipboard");
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
		DropInfoUtility.showDrop(d);
	}

	static class ContentProvider extends
			AbstractResultsTableContentProvider<IProofDropInfo> {
		ContentProvider() {
			super("Problems");
		}

		@Override
		protected String getAndSortResults(List<IProofDropInfo> contents) {
			final JSureScanInfo info = JSureDataDirHub.getInstance()
					.getCurrentScanInfo();
			if (info == null) {
				return null;
			}
			Set<? extends IProofDropInfo> drops = info
					.getDropsOfType(ResultDrop.class);
			for (IProofDropInfo id : drops) {
				if (!id.isConsistent()) {
					contents.add(id);
				}
			}
			Collections.sort(contents, sortByLocation);
			return info.getLabel();
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
