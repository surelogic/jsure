package com.surelogic.jsure.client.eclipse.views.scans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ILifecycle;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.common.core.JDTUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.java.JavaProject;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.ColumnViewerSorter;
import com.surelogic.common.ui.SLImages;
import com.surelogic.common.ui.TableUtility;
import com.surelogic.java.persistence.JSureDataDir;
import com.surelogic.java.persistence.JSureScan;
import com.surelogic.jsure.client.eclipse.dialogs.DeleteScanDialog;
import com.surelogic.jsure.client.eclipse.dialogs.LogDialog;
import com.surelogic.jsure.client.eclipse.handlers.VerifyProjectHandler;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class ScanManagerMediator implements ILifecycle {

  /*
   * SWT on Mac OS X creates a special column for the check box in the table.
   * This doesn't happen on Windows or Linux so we have to manually do it.
   * Otherwise the table looks strange.
   * 
   * The below constants help to implement this extra column.
   */
  static final boolean EXTRA_COLUMN = !SystemUtils.IS_OS_MAC_OSX;
  static final int EXTRA_COLUMN_WIDTH = 27;
  static final int FIRST_COLUMN_INDEX = EXTRA_COLUMN ? 1 : 0;

  final CheckboxTableViewer f_table;
  final Table f_swtTable;
  final ICheckStateListener f_checkStateListener = new ICheckStateListener() {

    @Override
    public void checkStateChanged(CheckStateChangedEvent event) {
      Object element = event.getElement();
      if (element instanceof JSureScan) {
        final JSureScan scan = (JSureScan) element;
        if (event.getChecked())
          reactToCheckStateChanged(scan);
        else
          reactToCheckStateChanged(null);
      }
    }
  };

  final Action f_showScanLogAction = new Action() {

    @Override
    public void run() {
      final List<JSureScan> selected = getSelectedScans();
      if (!selected.isEmpty()) {
        final JSureScan scan = selected.get(0); // show first
        /*
         * This dialog is modeless so that we can open more than one.
         */
        final LogDialog d = new LogDialog(f_swtTable.getShell(), scan);
        d.open();
      }
    }
  };

  Action getShowScanLogAction() {
    return f_showScanLogAction;
  }

  final Action f_deleteScanAction = new Action() {

    @Override
    public void run() {
      final List<JSureScan> selected = getSelectedScans();
      if (!selected.isEmpty()) {
        final DeleteScanDialog d = new DeleteScanDialog(f_swtTable.getShell(), selected.get(0), selected.size() > 1);
        d.open();
        if (Window.CANCEL == d.getReturnCode()) {
          return;
        }

        final Job job = EclipseUtility.toEclipseJob(JSureDataDirHub.getInstance().getDeleteScansJob(selected));
        job.setUser(true);
        job.schedule();
      }
    }
  };

  Action getDeleteScanAction() {
    return f_deleteScanAction;
  }

  private final Action f_rescanAction = new Action() {
    @Override
    public void run() {
      final List<JSureScan> selected = getSelectedScans();
      if (selected.size() == 1) {
        final JSureScan current = selected.get(0);
        if (current != null) {
          try {
            // Collect the projects together
            final List<IJavaProject> selectedProjects = new ArrayList<>();
            for (final JavaProject p : current.getProjects()) {
              if (!p.isAsBinary()) {
                final IJavaProject jp = JDTUtility.getJavaProject(p.getName());
                if (jp == null) {
                  // Can't find one of the projects
                  MessageDialog.openInformation(f_swtTable.getShell(), "Unable to Re-Verify",
                      "Missing project '" + p.getName() + "'");
                  return;
                }
                selectedProjects.add(jp);
              }
            }
            VerifyProjectHandler.verify(selectedProjects);
          } catch (Exception e) {
            SLLogger.getLogger().log(Level.WARNING, "Problem reading projects file", e);
          }
        }
      }
    }
  };

  Action getRescanAction() {
    return f_rescanAction;
  }

  private final Action f_refreshAction = new Action() {
    @Override
    public void run() {
      final SLJob job = new AbstractSLJob("Refresh the list of scans") {

        @Override
        public SLStatus run(SLProgressMonitor monitor) {
          monitor.begin();
          JSureDataDirHub.getInstance().scanDirectoryOrDirectoriesDeleted();
          return SLStatus.OK_STATUS;
        }
      };
      final Job eJob = EclipseUtility.toEclipseJob(job);
      eJob.setUser(true);
      eJob.schedule();
    }
  };

  Action getRefreshAction() {
    return f_refreshAction;
  }

  private final Action f_setAsCurrentAction = new Action() {
    @Override
    public void run() {
      final List<JSureScan> selected = getSelectedScans();
      if (selected.size() == 1) {
        final JSureScan current = selected.get(0);
        if (current != null) {
          final SLJob job = new AbstractSLJob("Change the current scan") {

            @Override
            public SLStatus run(SLProgressMonitor monitor) {
              monitor.begin();
              JSureDataDirHub.getInstance().setCurrentScan(current);
              return SLStatus.OK_STATUS;
            }
          };
          final Job eJob = EclipseUtility.toEclipseJob(job);
          eJob.schedule();
        }
      }
    }
  };

  Action getSetAsCurrentAction() {
    return f_setAsCurrentAction;
  }

  List<JSureScan> getSelectedScans() {
    IStructuredSelection selected = (IStructuredSelection) f_table.getSelection();
    if (selected.isEmpty())
      return Collections.emptyList();

    final List<JSureScan> result = new ArrayList<>();
    for (Object o : selected.toList()) {
      if (o instanceof JSureScan) {
        result.add((JSureScan) o);
      }
    }
    return result;
  }

  /**
   * Used to help sort the columns. Performs a textual sort.
   * <p>
   * This implementation is intended to be overridden.
   */
  private static class MyColumnViewerSorter extends ColumnViewerSorter<JSureScan> {

    private final int f_columnIndex;

    public MyColumnViewerSorter(CheckboxTableViewer viewer, TableColumn column, int columnIndex) {
      super(viewer, column);
      f_columnIndex = columnIndex;
    }

    @Override
    protected int doCompare(Viewer viewer, JSureScan e1, JSureScan e2) {
      ITableLabelProvider lp = ((ITableLabelProvider) ((CheckboxTableViewer) viewer).getLabelProvider());
      String t1 = lp.getColumnText(e1, f_columnIndex);
      String t2 = lp.getColumnText(e2, f_columnIndex);
      return t1.compareTo(t2);
    }

  }

  void showCurrentScanInUi() {
    f_table.setAllChecked(false);
    final JSureScan current = JSureDataDirHub.getInstance().getCurrentScan();
    if (current != null)
      f_table.setChecked(current, true);

    if (f_swtTable.getItemCount() == 1)
      packColumns();
  }

  void packColumns() {
    TableUtility.packColumns(f_table);
    if (EXTRA_COLUMN)
      f_swtTable.getColumn(0).setWidth(EXTRA_COLUMN_WIDTH);
  }

  void reactToCheckStateChanged(final JSureScan current) {
    final SLJob job = new AbstractSLJob("Update current selection") {
      @Override
      public SLStatus run(SLProgressMonitor monitor) {
        JSureDataDirHub.getInstance().setCurrentScan(current);
        return SLStatus.OK_STATUS;
      }
    };
    final Job eJob = EclipseUtility.toEclipseJob(job);
    eJob.schedule();
  }

  void setToolbarState() {
    final boolean oneOrMoreScansSelected = f_swtTable.getSelectionCount() > 0;

    f_deleteScanAction.setEnabled(oneOrMoreScansSelected);

    boolean oneNonCheckedScanSelected = false;
    boolean oneScanSelectedWithProjectsInWorkspace = false;
    final List<JSureScan> selected = getSelectedScans();
    if (selected.size() == 1) {
      final JSureScan selectedScan = selected.get(0);
      if (!selectedScan.equals(JSureDataDirHub.getInstance().getCurrentScan())) {
        oneNonCheckedScanSelected = true;
      }
      oneScanSelectedWithProjectsInWorkspace = true;
      try {
        for (JavaProject p : selectedScan.getProjects()) {
          if (p.shouldExistAsIProject()) {
            if (JDTUtility.getJavaProject(p.getName()) == null) {
              oneScanSelectedWithProjectsInWorkspace = false;
              break;
            }
          }
        }
      } catch (Exception e) {
        SLLogger.getLogger().log(Level.WARNING, "Problem reading projects file", e);
        oneScanSelectedWithProjectsInWorkspace = false;
      }
    }
    f_setAsCurrentAction.setEnabled(oneNonCheckedScanSelected);
    f_rescanAction.setEnabled(oneScanSelectedWithProjectsInWorkspace);
    f_showScanLogAction.setEnabled(oneScanSelectedWithProjectsInWorkspace);
  }

  ScanManagerMediator(CheckboxTableViewer table) {
    f_table = table;
    f_swtTable = f_table.getTable();
  }

  @Override
  public void init() {
    f_swtTable.setHeaderVisible(true);
    f_swtTable.setLinesVisible(true);

    /*
     * Setup columns
     */
    if (EXTRA_COLUMN) {
      addColumn(null, SWT.LEFT);
    }
    TableColumn dateColumn = addColumn("jsure.scan.view.table.col.date", SWT.LEFT);
    TableColumn sizeColumn = addColumn("jsure.scan.view.table.col.size", SWT.RIGHT);
    TableColumn projColumn = addColumn("jsure.scan.view.table.col.proj", SWT.LEFT);
    TableColumn excludedColumn = addColumn("jsure.scan.view.table.col.excluded", SWT.LEFT);
    TableColumn asBytecodeColumn = addColumn("jsure.scan.view.table.col.as-bytecode", SWT.LEFT);

    /*
     * Setup sorters
     */
    int columnIndex = FIRST_COLUMN_INDEX;
    final MyColumnViewerSorter dateColumnSorter = new MyColumnViewerSorter(f_table, dateColumn, columnIndex++);
    new MyColumnViewerSorter(f_table, sizeColumn, columnIndex++) {
      @Override
      protected int doCompare(Viewer viewer, JSureScan e1, JSureScan e2) {
        // we need to compare the scan sizes.
        return (int) (e1.getSizeInMB() - e2.getSizeInMB());
      }
    };
    new MyColumnViewerSorter(f_table, projColumn, columnIndex++);
    new MyColumnViewerSorter(f_table, excludedColumn, columnIndex++);
    new MyColumnViewerSorter(f_table, asBytecodeColumn, columnIndex++);

    /*
     * Set the default sort to the date (newest on top)
     */
    dateColumnSorter.setSorter(dateColumnSorter, ColumnViewerSorter.DESC);

    /*
     * Setup content providers and input
     */
    f_table.setContentProvider(new MyContentProvider());
    f_table.setLabelProvider(new MyLabelProvider());
    f_table.setInput(JSureDataDirHub.getInstance().getJSureDataDir());

    showCurrentScanInUi();

    f_table.addCheckStateListener(f_checkStateListener);

    f_swtTable.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(final Event event) {
        setToolbarState();
      }
    });
    f_swtTable.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent e) {
        if ((e.character == SWT.DEL || e.character == SWT.BS) && e.stateMask == 0) {
          if (f_deleteScanAction.isEnabled()) {
            f_deleteScanAction.run();
          }
        }
      }
    });

    packColumns();
  }

  private TableColumn addColumn(String text, int alignment) {
    final TableColumn col = new TableColumn(f_swtTable, alignment);
    /*
     * If text is null we are creating the special column for the check box.
     */
    if (text != null) {
      col.setText(I18N.msg(text));
    }
    return col;
  }

  void setFocus() {
    f_swtTable.setFocus();
  }

  void refreshScanContents() {
    f_table.setInput(JSureDataDirHub.getInstance().getJSureDataDir());
    showCurrentScanInUi();
  }

  @Override
  public void dispose() {
    f_table.removeCheckStateListener(f_checkStateListener);
  }

  Display getDisplay() {
    return f_swtTable.getDisplay();
  }

  static class MyContentProvider implements IStructuredContentProvider {

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // Nothing to do
    }

    @Override
    public Object[] getElements(Object inputElement) {
      if (inputElement instanceof JSureDataDir) {
        final JSureDataDir dataDir = (JSureDataDir) inputElement;
        return dataDir.getScansAsArray();
      } else
        return SLUtility.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public void dispose() {
      // Nothing to do
    }
  }

  static class MyLabelProvider extends LabelProvider implements ITableLabelProvider {

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      if (columnIndex == FIRST_COLUMN_INDEX + 1)
        return SLImages.getImage(CommonImages.IMG_DRUM);
      if (columnIndex == FIRST_COLUMN_INDEX + 2) {
        if (element instanceof JSureScan) {
          final JSureScan run = (JSureScan) element;
          try {
            String oneProjectName = run.getProjects().getLabel().trim();
            if (oneProjectName.indexOf(',') == -1) {
              return SLImages.getImageForProject(oneProjectName);
            }
          } catch (Exception ignore) {
            // use default
          }
        }
        return SLImages.getImageForJavaProject();
      }
      return null;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
      if (EXTRA_COLUMN && columnIndex == 0)
        return null;
      try {
        if (element instanceof JSureScan) {
          final JSureScan run = (JSureScan) element;
          if (columnIndex == FIRST_COLUMN_INDEX) {
            final Date d = run.getProjects().getDate();
            return SLUtility.toStringDayHMS(d);
          }
          if (columnIndex == FIRST_COLUMN_INDEX + 1) {
            return String.format("%1$.1f MB", run.getSizeInMB());
          }
          if (columnIndex == FIRST_COLUMN_INDEX + 2) {
            return run.getProjects().getLabel();
          }
          if (columnIndex == FIRST_COLUMN_INDEX + 3) {
            return run.getProjects().getConciseExcludedFoldersAndPackages();
          }
          if (columnIndex == FIRST_COLUMN_INDEX + 4) {
            return run.getProjects().getConciseSourceFoldersLoadedAsBytecode();
          }
        }
      } catch (Exception e) {
        SLLogger.getLogger().log(Level.SEVERE, e.getMessage(), e);
      }
      return "BAD DATA";
    }
  }
}
