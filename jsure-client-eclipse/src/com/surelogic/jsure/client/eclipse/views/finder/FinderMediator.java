package com.surelogic.jsure.client.eclipse.views.finder;

import java.util.logging.Level;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolItem;

import com.surelogic.common.ILifecycle;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.CascadingList;
import com.surelogic.jsure.client.eclipse.dialogs.DeleteSearchDialog;
import com.surelogic.jsure.client.eclipse.dialogs.OpenSearchDialog;
import com.surelogic.jsure.client.eclipse.dialogs.SaveSearchAsDialog;
import com.surelogic.jsure.client.eclipse.model.selection.Filter;
import com.surelogic.jsure.client.eclipse.model.selection.ISelectionManagerObserver;
import com.surelogic.jsure.client.eclipse.model.selection.Selection;
import com.surelogic.jsure.client.eclipse.model.selection.SelectionManager;

public final class FinderMediator implements ILifecycle, CascadingList.ICascadingListObserver, ISelectionManagerObserver {

  final Composite f_parent;
  final CascadingList f_finder;
  final Link f_breadcrumbs;
  final ToolItem f_clearSelectionItem;
  final ToolItem f_openSearchItem;
  final ToolItem f_saveSearchAsItem;
  final ToolItem f_deleteSearchItem;
  final Link f_savedSelections;

  final SelectionManager f_manager = SelectionManager.getInstance();

  Selection f_workingSelection = null;

  MColumn f_first = null;

  FinderMediator(Composite parent, CascadingList finder, Link breadcrumbs, ToolItem clearSelectionItem, ToolItem openSearchItem,
      ToolItem saveSearchAsItem, ToolItem deleteSearchItem, Link savedSelections) {
    f_parent = parent;
    f_finder = finder;
    f_breadcrumbs = breadcrumbs;
    f_clearSelectionItem = clearSelectionItem;
    f_openSearchItem = openSearchItem;
    f_saveSearchAsItem = saveSearchAsItem;
    f_deleteSearchItem = deleteSearchItem;
    f_savedSelections = savedSelections;
  }

  @Override
  public void init() {
    f_clearSelectionItem.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        clearToNewWorkingSelection();
      }
    });

    f_breadcrumbs.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        final int column = Integer.parseInt(event.text);
        f_finder.show(column);
      }
    });

    f_openSearchItem.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        OpenSearchDialog dialog = new OpenSearchDialog(f_finder.getShell());
        if (Window.CANCEL != dialog.open()) {
          /*
           * Save the selection
           */
          Selection newSelection = dialog.getSelection();
          if (newSelection == null)
            return;
          openSelection(newSelection);
        }
      }
    });

    f_saveSearchAsItem.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        SaveSearchAsDialog dialog = new SaveSearchAsDialog(f_finder.getShell());
        if (Window.CANCEL != dialog.open()) {
          /*
           * Save the selection
           */
          String name = dialog.getName();
          if (name == null)
            return;
          name = name.trim();
          if ("".equals(name))
            return;
          f_manager.saveSelection(name, f_workingSelection);
        }
      }
    });

    f_deleteSearchItem.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        DeleteSearchDialog dialog = new DeleteSearchDialog(f_finder.getShell());
        dialog.open();
      }
    });

    f_savedSelections.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event event) {
        final String selectionName = event.text;
        /*
         * open the current selection.
         */
        final Selection newSelection = f_manager.getSavedSelection(selectionName);
        if (newSelection == null) {
          SLLogger.getLogger().log(Level.SEVERE, "Search '" + selectionName + "' is unknown (bug).", new Exception());
          return;
        }
        if (newSelection.getFilterCount() < 1) {
          SLLogger.getLogger().log(Level.SEVERE, "Search '" + selectionName + "' defines no filters (bug).", new Exception());
          return;
        }
        openSelection(newSelection);
      }
    });

    f_finder.addObserver(this);
    f_manager.addObserver(this);

    clearToPersistedViewState();
  }

  @Override
  public void dispose() {
    f_finder.removeObserver(this);
    f_manager.removeObserver(this);
    if (f_workingSelection != null) {
      f_manager.saveViewState(f_workingSelection);
    }
  }

  void setFocus() {
    f_finder.setFocus();
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
       * We only want to restore the view state once per session of Eclipse so
       * now we clear the view state out of the selection manager.
       */
      f_manager.removeViewState();
      openSelection(persistedViewState);
    } else {
      clearToNewWorkingSelection();
    }
  }

  void clearToNewWorkingSelection() {
    disposeWorkingSelection();
    f_workingSelection = f_manager.construct();
    f_workingSelection.initAndSyncToSea();
    updateSavedSelections();
    f_first = new MRadioMenuColumn(f_finder, f_workingSelection, null);
    f_first.init();
  }

  void openSelection(final Selection newSelection) {
    disposeWorkingSelection();
    f_workingSelection = newSelection;
    f_workingSelection.initAndSyncToSea();
    f_workingSelection.refresh();
    f_first = new MRadioMenuColumn(f_finder, f_workingSelection, null);
    f_first.init();

    MRadioMenuColumn prevMenu = (MRadioMenuColumn) f_first;
    for (Filter filter : f_workingSelection.getFilters()) {
      /*
       * Set the right choice on the previous menu
       */
      prevMenu.setSelection(filter.getFactory());
      /*
       * Create a filter selection
       */
      MFilterSelectionColumn fCol = new MFilterSelectionColumn(f_finder, f_workingSelection, prevMenu, filter);
      fCol.init();
      /*
       * Create a menu
       */
      prevMenu = new MRadioMenuColumn(f_finder, f_workingSelection, fCol);
      prevMenu.init();
    }
    if (f_workingSelection.isShowingResults()) {
      prevMenu.setSelection("Show");
      MListOfResultsColumn list = new MListOfResultsColumn(f_finder, f_workingSelection, prevMenu);
      list.init();
    }
  }

  @Override
  public void notify(CascadingList cascadingList) {
    updateBreadcrumbs();
    updateSavedSelections();

  }

  @Override
  public void savedSelectionsChanged(SelectionManager manager) {
    updateSavedSelections();
  }

  void updateBreadcrumbs() {
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
      }
      clColumn = clColumn.getNextColumn();
    } while (clColumn != null);
    f_breadcrumbs.setText(b.toString());
    final boolean somethingToClear = b.length() > 0;
    f_clearSelectionItem.setEnabled(somethingToClear);
    f_breadcrumbs.getParent().layout();
    f_parent.layout();
  }

  void updateSavedSelections() {
    StringBuilder b = new StringBuilder();
    final boolean saveable = f_workingSelection != null && f_workingSelection.getFilterCount() > 0;
    f_saveSearchAsItem.setEnabled(saveable);
    final boolean hasSavedSelections = !f_manager.isEmpty();
    f_openSearchItem.setEnabled(hasSavedSelections);
    f_deleteSearchItem.setEnabled(hasSavedSelections);

    if (hasSavedSelections) {
      b.append("Saved Searches:");

      for (String link : f_manager.getSavedSelectionNames()) {
        b.append("  <a href=\"");
        b.append(link);
        b.append("\">");
        b.append(link);
        b.append("</a>");
      }
    } else {
      b.append("(no saved searches)");
    }
    f_savedSelections.setText(b.toString());
    f_savedSelections.getParent().layout();
    f_finder.layout();
  }

  public void selectionChanged(Selection selecton) {
    /*
     * Nothing to do.
     */
  }

  public void selectAll() {
    f_first.selectAll();
  }
}
