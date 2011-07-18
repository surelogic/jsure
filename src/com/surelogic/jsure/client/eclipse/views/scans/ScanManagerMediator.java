package com.surelogic.jsure.client.eclipse.views.scans;

import java.util.Date;
import java.util.logging.Level;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.surelogic.common.CommonImages;
import com.surelogic.common.ILifecycle;
import com.surelogic.common.SLUtility;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.common.ui.SLImages;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureRun;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

public final class ScanManagerMediator implements ILifecycle {

	private final CheckboxTableViewer f_table;
	private final Table f_swtTable;

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
		addColumn("", SWT.LEFT, 50);
		addColumn("Date", SWT.LEFT, 150);
		addColumn("Size", SWT.RIGHT, 70);
		addColumn("Projects", SWT.LEFT, 300);

		f_table.setContentProvider(new MyContentProvider());
		f_table.setLabelProvider(new MyLabelProvider());
		f_table.setInput(JSureDataDirHub.getInstance().getJSureDataDir());
	}

	private void addColumn(String text, int alignment, int width) {
		final TableColumn col = new TableColumn(f_swtTable, alignment);
		col.setText(text);
		col.setWidth(width);
	}

	void setFocus() {

	}

	void refreshScanContents() {
		f_table.setInput(JSureDataDirHub.getInstance().getJSureDataDir());
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
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
			System.out.println("getElements() called");
			if (inputElement instanceof JSureDataDir) {
				final JSureDataDir dataDir = (JSureDataDir) inputElement;
				return dataDir.getAllRuns();
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
			if (columnIndex == 0)
				return SLImages.getImage(CommonImages.IMG_EMPTY);
			if (columnIndex == 3)
				return SLImages.getImage(CommonImages.IMG_PROJECT);
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			try {
				if (element instanceof JSureRun) {
					final JSureRun run = (JSureRun) element;
					switch (columnIndex) {
					case 0:
						return "";
					case 1:
						final Date d = run.getProjects().getDate();
						return SLUtility.toStringHMS(d);
					case 2:
						return String.format("%1$.1f MB", run.getSizeInMB());
					case 3:
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
