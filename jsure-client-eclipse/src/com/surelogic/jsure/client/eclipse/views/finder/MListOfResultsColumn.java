package com.surelogic.jsure.client.eclipse.views.finder;

import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ref.IJavaRef;
import com.surelogic.common.ui.CascadingList;
import com.surelogic.common.ui.CascadingList.IColumn;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.TableUtility;
import com.surelogic.common.ui.jobs.SLUIJob;
import com.surelogic.dropsea.IProofDrop;
import com.surelogic.jsure.client.eclipse.Activator;
import com.surelogic.jsure.client.eclipse.model.selection.ISelectionObserver;
import com.surelogic.jsure.client.eclipse.model.selection.Selection;
import com.surelogic.jsure.client.eclipse.views.JSureDecoratedImageUtility;
import com.surelogic.jsure.client.eclipse.views.status.VerificationStatusView;

public final class MListOfResultsColumn extends MColumn implements ISelectionObserver {

  /**
   * The table used to display the results.
   */
  Table f_table = null;

  MListOfResultsColumn(final CascadingList cascadingList, final Selection selection, final MColumn previousColumn) {
    super(cascadingList, selection, previousColumn);
  }

  @Override
  void init() {
    getSelection().setShowingResults(true);
    getSelection().addObserver(this);
    changed();
  }

  @Override
  void initOfNextColumnComplete() {
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(final IProgressMonitor monitor) {
        MListOfResultsColumn.super.initOfNextColumnComplete();
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  @Override
  void dispose() {
    super.dispose();
    getSelection().setShowingResults(false);
    getSelection().removeObserver(this);

    final int column = getColumnIndex();
    if (column != -1) {
      getCascadingList().emptyFrom(column);
    }
  }

  @Override
  int getColumnIndex() {
    if (f_table.isDisposed()) {
      return -1;
    } else {
      return getCascadingList().getColumnIndexOf(f_table);
    }
  }

  @Override
  public void forceFocus() {
    if (f_table != null) {
      f_table.forceFocus();
      getCascadingList().show(index);
    }
  }

  @Override
  public void selectionChanged(final Selection selecton) {
    changed();
  }

  private void changed() {
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(final IProgressMonitor monitor) {
        if (f_table != null && f_table.isDisposed()) {
          getSelection().removeObserver(MListOfResultsColumn.this);
        } else {
          try {
            refreshDisplay();
          } finally {
            initOfNextColumnComplete();
          }
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  final KeyListener f_keyListener = new KeyListener() {
    @Override
    public void keyPressed(final KeyEvent e) {
      if (e.character == 0x01 && f_table != null) {
        f_table.selectAll();
        e.doit = false; // Handled
      }
    }

    @Override
    public void keyReleased(final KeyEvent e) {
      // Nothing to do
    }
  };

  final Action f_openProofContext = new Action() {
    @Override
    public void run() {
      final IProofDrop info = getSelectedItem();
      if (info != null) {
        final VerificationStatusView view = (VerificationStatusView) EclipseUIUtility.showView(VerificationStatusView.class
            .getName());
        if (view != null)
          view.attemptToShowAndSelectDropInViewer(info);
      }
    }
  };

  final Listener f_selection = new Listener() {
    @Override
    public void handleEvent(final Event event) {
      final IProofDrop info = getSelectedItem();
      if (info != null) {
        /*
         * Highlight this line in the editor if possible.
         */
        final IJavaRef ref = info.getJavaRef();
        if (ref != null) {
          Activator.highlightLineInJavaEditor(ref);
        }
      }
    }
  };

  final IColumn f_iColumn = new IColumn() {
    @Override
    public Composite createContents(final Composite panel) {
      f_table = new Table(panel, SWT.FULL_SELECTION);
      // add one column so pack works right on all operating systems
      new TableColumn(f_table, SWT.NONE);
      f_table.setLinesVisible(true);
      f_table.addListener(SWT.Selection, f_selection);
      f_table.addKeyListener(f_keyListener);
      f_table.setItemCount(0);

      f_table.addListener(SWT.Traverse, new Listener() {
        @Override
        public void handleEvent(final Event e) {
          switch (e.detail) {
          case SWT.TRAVERSE_ESCAPE:
            setCustomTabTraversal(e);
            if (getPreviousColumn() instanceof MRadioMenuColumn) {
              final MRadioMenuColumn column = (MRadioMenuColumn) getPreviousColumn();
              column.escape(null);
            }
            break;
          case SWT.TRAVERSE_TAB_NEXT:
            // Cycle back to the first columns
            setCustomTabTraversal(e);
            getFirstColumn().forceFocus();
            break;
          case SWT.TRAVERSE_TAB_PREVIOUS:
            setCustomTabTraversal(e);
            getPreviousColumn().forceFocus();
            break;
          case SWT.TRAVERSE_RETURN:
            setCustomTabTraversal(e);
            break;
          }
        }
      });

      f_openProofContext.setImageDescriptor(SLImages.getImageDescriptor(CommonImages.IMG_JSURE_LOGO));
      f_openProofContext.setText("Open In Proof Context");
      f_openProofContext.setToolTipText("Open this result in the Verification Status view to show it within its proof context");

      final MenuManager menuMgr = new MenuManager("#PopupMenu");
      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(new IMenuListener() {
        @Override
        public void menuAboutToShow(final IMenuManager manager) {
          manager.add(f_openProofContext);
        }
      });
      final Menu menu = menuMgr.createContextMenu(f_table);
      f_table.setMenu(menu);

      updateTableContents();
      return f_table;
    }
  };

  void updateTableContents() {
    if (f_table.isDisposed()) {
      return;
    }

    f_table.setRedraw(false);

    final List<IProofDrop> rows = getSelection().getPorousDrops();

    final IProofDrop selected = getSelectedItem();
    f_table.removeAll();

    IProofDrop lastSelected = null;
    int i = 0;
    for (final IProofDrop data : rows) {
      final boolean rowSelected = data == selected;
      final TableItem item = new TableItem(f_table, SWT.NONE);
      setTableItemInfo(item, data);
      if (rowSelected) {
        selectItem(i, data);
        lastSelected = data;
      }
    }
    f_table.setRedraw(true);
    /*
     * Fix to bug 1115 (an XP specific problem) where the table was redrawn with
     * lines through the row text. Aaron Silinskas found that a second call
     * seemed to fix the problem (with a bit of flicker).
     */
    if (SystemUtils.IS_OS_WINDOWS_XP) {
      f_table.setRedraw(true);
    }
    final boolean showSelection = lastSelected != null;
    final UIJob job = new SLUIJob() {
      @Override
      public IStatus runInUIThread(final IProgressMonitor monitor) {
        TableUtility.packColumns(f_table);
        /*
         * We need the call below because OS X doesn't send the resize to the
         * cascading list.
         */
        getCascadingList().fixupSize();
        if (showSelection) {
          f_table.showSelection();
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  void selectItem(int i, IProofDrop data) {
    if (i != -1) {
      f_table.select(i);
    }
  }

  IProofDrop getSelectedItem() {
    final TableItem[] selected = f_table.getSelection();
    if (selected.length > 0) {
      final Object data = selected[0].getData();
      if (data instanceof IProofDrop)
        return (IProofDrop) data;
    }
    return null;
  }

  void refreshDisplay() {
    if (f_table == null) {
      final int addAfterColumn = getPreviousColumn().getColumnIndex();
      // create the display table
      getCascadingList().addColumnAfter(f_iColumn, addAfterColumn, false);
    } else {
      // update the table's contents
      updateTableContents();
    }
  }

  void setTableItemInfo(TableItem item, IProofDrop data) {
    final Image image = JSureDecoratedImageUtility.getImageForDrop(data, false);
    item.setText(data.getMessage());
    item.setImage(image);
    item.setData(data);
  }

  @Override
  void selectAll() {
    if (f_table.isFocusControl()) {
      f_table.selectAll();
    } else {
      super.selectAll();
    }
  }
}
