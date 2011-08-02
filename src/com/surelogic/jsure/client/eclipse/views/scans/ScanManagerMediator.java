package com.surelogic.jsure.client.eclipse.views.scans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.jface.action.Action;
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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ILifecycle;
import com.surelogic.common.SLUtility;
import com.surelogic.common.core.jobs.EclipseJob;
import com.surelogic.common.i18n.I18N;
import com.surelogic.common.jobs.AbstractSLJob;
import com.surelogic.common.jobs.SLJob;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.ColumnViewerSorter;
import com.surelogic.common.ui.SLImages;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.client.eclipse.dialogs.DeleteScanDialog;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class ScanManagerMediator implements ILifecycle {

	private final CheckboxTableViewer f_table;
	private final Table f_swtTable;
	private final ICheckStateListener f_checkStateListener = new ICheckStateListener() {

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

	private final Action f_deleteScanAction = new Action() {

		@Override
		public void run() {
			final List<JSureScan> selected = getSelectedScans();
			if (!selected.isEmpty()) {
				final DeleteScanDialog d = new DeleteScanDialog(
						f_swtTable.getShell(), selected.get(0),
						selected.size() > 1);
				d.open();
				if (Window.CANCEL == d.getReturnCode()) {
					return;
				}

				final SLJob job = JSureDataDirHub.getInstance()
						.getDeleteScansJob(selected);
				EclipseJob.getInstance().schedule(job, true, false);
			}
			super.run();
		}
	};

	Action getDeleteScanAction() {
		return f_deleteScanAction;
	}

	private final Action f_refreshAction = new Action() {
		@Override
		public void run() {
			final SLJob job = new AbstractSLJob("Refresh the list of scans") {

				@Override
				public SLStatus run(SLProgressMonitor monitor) {
					monitor.begin();
					JSureDataDirHub.getInstance()
							.scanDirectoryOrDirectoriesDeleted();
					return SLStatus.OK_STATUS;
				}
			};
			EclipseJob.getInstance().schedule(job, true, false);
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
					final SLJob job = new AbstractSLJob(
							"Change the current scan") {

						@Override
						public SLStatus run(SLProgressMonitor monitor) {
							monitor.begin();
							JSureDataDirHub.getInstance().setCurrentScan(
									current);
							return SLStatus.OK_STATUS;
						}
					};
					EclipseJob.getInstance().schedule(job);
				}
			}
		}
	};

	Action getSetAsCurrentAction() {
		return f_setAsCurrentAction;
	}

	private List<JSureScan> getSelectedScans() {
		IStructuredSelection selected = (IStructuredSelection) f_table
				.getSelection();
		if (selected.isEmpty())
			return Collections.emptyList();

		final List<JSureScan> result = new ArrayList<JSureScan>();
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
	private static class MyColumnViewerSorter extends
			ColumnViewerSorter<JSureScan> {

		private final int f_columnIndex;

		public MyColumnViewerSorter(CheckboxTableViewer viewer,
				TableColumn column, int columnIndex) {
			super(viewer, column);
			f_columnIndex = columnIndex;
		}

		@Override
		protected int doCompare(Viewer viewer, JSureScan e1, JSureScan e2) {
			ITableLabelProvider lp = ((ITableLabelProvider) ((CheckboxTableViewer) viewer)
					.getLabelProvider());
			String t1 = lp.getColumnText(e1, f_columnIndex);
			String t2 = lp.getColumnText(e2, f_columnIndex);
			return t1.compareTo(t2);
		}

	}

	private void showCurrentScanInUi() {
		f_table.setAllChecked(false);
		final JSureScan current = JSureDataDirHub.getInstance()
				.getCurrentScan();
		if (current != null)
			f_table.setChecked(current, true);
	}

	private void reactToCheckStateChanged(final JSureScan current) {
		final SLJob job = new AbstractSLJob("Update current selection") {
			@Override
			public SLStatus run(SLProgressMonitor monitor) {
				JSureDataDirHub.getInstance().setCurrentScan(current);
				return SLStatus.OK_STATUS;
			}
		};
		EclipseJob.getInstance().schedule(job);
	}

	private void setToolbarState() {
		final boolean oneOrMoreScansSelected = f_swtTable.getSelectionCount() > 0;

		f_deleteScanAction.setEnabled(oneOrMoreScansSelected);

		boolean oneNonCheckedScanSelected = false;
		List<JSureScan> selected = getSelectedScans();
		if (selected.size() == 1) {
			final JSureScan selectedScan = selected.get(0);
			if (!selectedScan.equals(JSureDataDirHub.getInstance()
					.getCurrentScan()))
				oneNonCheckedScanSelected = true;
		}
		f_setAsCurrentAction.setEnabled(oneNonCheckedScanSelected);
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
		TableColumn dateColumn = addColumn("jsure.scan.view.table.col.date",
				SWT.LEFT, 150);
		TableColumn sizeColumn = addColumn("jsure.scan.view.table.col.size",
				SWT.RIGHT, 70);
		TableColumn projColumn = addColumn("jsure.scan.view.table.col.proj",
				SWT.LEFT, 600);

		/*
		 * Setup sorters
		 */
		final MyColumnViewerSorter dateColumnSorter = new MyColumnViewerSorter(
				f_table, dateColumn, 0);
		new MyColumnViewerSorter(f_table, sizeColumn, 1) {
			@Override
			protected int doCompare(Viewer viewer, JSureScan e1, JSureScan e2) {
				// we need to compare the scan sizes.
				return (int) (e1.getSizeInMB() - e2.getSizeInMB());
			}
		};
		new MyColumnViewerSorter(f_table, projColumn, 2);

		/*
		 * Set the default sort to the date (newest on top)
		 */
		dateColumnSorter.setSorter(dateColumnSorter, MyColumnViewerSorter.DESC);

		/*
		 * Setup content providers and input
		 */
		f_table.setContentProvider(new MyContentProvider());
		f_table.setLabelProvider(new MyLabelProvider());
		f_table.setInput(JSureDataDirHub.getInstance().getJSureDataDir());

		showCurrentScanInUi();

		f_table.addCheckStateListener(f_checkStateListener);

		f_swtTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(final Event event) {
				setToolbarState();
			}
		});

		f_swtTable.pack();
	}

	private TableColumn addColumn(String text, int alignment, int width) {
		final TableColumn col = new TableColumn(f_swtTable, alignment);
		col.setText(I18N.msg(text));
		col.setWidth(width);
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

	private static class MyContentProvider implements
			IStructuredContentProvider {

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
				return new Object[0];
		}

		@Override
		public void dispose() {
			// Nothing to do
		}
	}

	private static class MyLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (columnIndex == 1)
				return SLImages.getImage(CommonImages.IMG_DRUM);
			if (columnIndex == 2)
				return SLImages.getImage(CommonImages.IMG_PROJECT);
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			try {
				if (element instanceof JSureScan) {
					final JSureScan run = (JSureScan) element;
					switch (columnIndex) {
					case 0:
						final Date d = run.getProjects().getDate();
						return SLUtility.toStringHMS(d);
					case 1:
						return String.format("%1$.1f MB", run.getSizeInMB());
					case 2:
						return run.getProjects().getLabel();
					}
				}
			} catch (Exception e) {
				SLLogger.getLogger().log(Level.SEVERE, e.getMessage(), e);
			}
			return "BAD DATA";
		}
	}
}
