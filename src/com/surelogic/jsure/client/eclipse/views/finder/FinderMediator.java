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
import com.surelogic.jsure.client.eclipse.model.selection.Filter;
import com.surelogic.jsure.client.eclipse.model.selection.ISelectionManagerObserver;
import com.surelogic.jsure.client.eclipse.model.selection.Selection;
import com.surelogic.jsure.client.eclipse.model.selection.SelectionManager;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class FinderMediator implements ILifecycle,
		JSureDataDirHub.CurrentScanChangeListener,
		CascadingList.ICascadingListObserver, ISelectionManagerObserver,
		IFindingsObserver {

	private final Composite f_parent;
	private final CascadingList f_finder;
	private final Link f_breadcrumbs;
	private final ToolItem f_clearSelectionItem;

	private final SelectionManager f_manager = SelectionManager.getInstance();

	private Selection f_workingSelection = null;

	private MColumn f_first = null;

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

		f_finder.addObserver(this);
		f_manager.addObserver(this);

		clearToNewWorkingSelection();

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

	private void disposeWorkingSelection() {
		if (f_first != null)
			f_first.dispose();
		f_breadcrumbs.setText("");
		if (f_workingSelection != null)
			f_workingSelection.dispose();
	}

	private void clearToPersistedViewState() {
		final Selection persistedViewState = f_manager.getViewState();
		if (persistedViewState != null) {
			/*
			 * We only want to restore the view state once per session of
			 * Eclipse so now we clear the view state out of the selection
			 * manager.
			 */
			f_manager.removeViewState();
			openSelection(persistedViewState);
		} else {
			clearToNewWorkingSelection();
		}
	}

	private void clearToNewWorkingSelection() {
		disposeWorkingSelection();
		f_workingSelection = f_manager.construct();
		f_workingSelection.initAndSyncToSea();
		updateSavedSelections();
		f_first = new MRadioMenuColumn(f_finder, f_workingSelection, null);
		f_first.setObserver(this);
		f_first.init();
	}

	private void openSelection(final Selection newSelection) {
		disposeWorkingSelection();
		f_workingSelection = newSelection;
		f_workingSelection.initAndSyncToSea();
		f_first = new MRadioMenuColumn(f_finder, f_workingSelection, null);
		f_first.init();
		f_workingSelection.refresh();

		MRadioMenuColumn prevMenu = (MRadioMenuColumn) f_first;
		for (Filter filter : f_workingSelection.getFilters()) {
			/*
			 * Set the right choice on the previous menu
			 */
			prevMenu.setSelection(filter.getFactory());
			/*
			 * Create a filter selection
			 */
			MFilterSelectionColumn fCol = new MFilterSelectionColumn(f_finder,
					f_workingSelection, prevMenu, filter);
			fCol.init();
			/*
			 * Create a menu
			 */
			prevMenu = new MRadioMenuColumn(f_finder, f_workingSelection, fCol);
			prevMenu.init();
		}
		if (f_workingSelection.isShowingResults()) {
			prevMenu.setSelection("Show");
			MListOfResultsColumn list = new MListOfResultsColumn(f_finder,
					f_workingSelection, prevMenu);
			list.setObserver(this);
			list.init();
		}
	}

	@Override
	public void notify(CascadingList cascadingList) {
		updateBreadcrumbs();
		updateSavedSelections();

	}

	public void savedSelectionsChanged(SelectionManager manager) {
		updateSavedSelections();
	}

	private void updateBreadcrumbs() {
		final StringBuilder b = new StringBuilder();
		int column = 0;
		boolean first = true;
		MColumn clColumn = f_first;
		do {
			if (clColumn instanceof MFilterSelectionColumn) {
				MFilterSelectionColumn fsc = (MFilterSelectionColumn) clColumn;
				final Filter filter = fsc.getFilter();
				final String name = filter.getFactory().getFilterLabel();
				if (first) {
					first = false;
					b.append(" ");
				} else {
					b.append(" | ");
				}
				b.append("<a href=\"").append(column).append("\">");
				b.append(name).append("</a>");
				column += 2; // selector and menu
			} else if (clColumn instanceof MListOfResultsColumn) {
				b.append(" | <a href=\"").append(column).append("\">Show</a>");
				((MListOfResultsColumn) clColumn).setObserver(this);
			}
			clColumn = clColumn.getNextColumn();
		} while (clColumn != null);
		f_breadcrumbs.setText(b.toString());
		final boolean somethingToClear = b.length() > 0;
		f_clearSelectionItem.setEnabled(somethingToClear);
		f_breadcrumbs.getParent().layout();
		f_parent.layout();
	}

	private void updateSavedSelections() {
		// TODO
	}

	public void selectionChanged(Selection selecton) {
		/*
		 * Nothing to do.
		 */
	}

	public void selectAll() {
		f_first.selectAll();
	}

	@Override
	public void findingsDisposed() {
		// TODO Auto-generated method stub

	}

	@Override
	public void findingsLimited(boolean isLimited) {
		// TODO Auto-generated method stub

	}

}
