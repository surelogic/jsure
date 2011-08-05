package com.surelogic.jsure.client.eclipse.views.finder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolItem;

import com.surelogic.common.ILifecycle;
import com.surelogic.common.ui.CascadingList;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class FinderMediator implements ILifecycle,
		JSureDataDirHub.CurrentScanChangeListener,
		CascadingList.ICascadingListObserver {

	private final Composite f_parent;
	private final CascadingList f_finder;
	private final Link f_breadcrumbs;
	private final ToolItem f_clearSelectionItem;

	FinderMediator(Composite parent, CascadingList finder, Link breadcrumbs,
			ToolItem clearSelectionItem) {
		f_parent = parent;
		f_finder = finder;
		f_breadcrumbs = breadcrumbs;
		f_clearSelectionItem = clearSelectionItem;
	}

	@Override
	public void init() {
		f_clearSelectionItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				clearToNewWorkingSelection();
			}
		});

		f_breadcrumbs.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				final int column = Integer.parseInt(event.text);
				f_finder.show(column);
			}
		});

		JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
	}

	@Override
	public void dispose() {
		JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);
	}

	void setFocus() {
		f_finder.setFocus();
	}

	@Override
	public void currentScanChanged(JSureScan scan) {
		// TODO
	}

	@Override
	public void notify(CascadingList cascadingList) {
		// TODO Auto-generated method stub

	}

	private void clearToNewWorkingSelection() {
		// TODO
	}

	private void updateBreadcrumbs() {
		// TODO
	}

}
