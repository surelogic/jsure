package com.surelogic.jsure.client.eclipse.views.scans;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

import com.surelogic.common.*;
import com.surelogic.common.ui.ColumnViewerSorter;
import com.surelogic.common.ui.views.ITableContentProvider;
import com.surelogic.javac.scans.*;
import com.surelogic.javac.persistence.*;
import com.surelogic.jsure.core.scans.*;

/**
 * A simple view to show what scans are available to be selected as the baseline/current scan
 */
public class ScansView extends AbstractScanManagerView {
	final ContentProvider f_content = new ContentProvider();
	TableViewer tableViewer;
	private Action f_deleteScanAction, f_setAsBaselineAction, f_setAsCurrentAction;
	
	@Override
	protected StructuredViewer getViewer() {
		return tableViewer;
	}
	
	@Override
	protected String updateViewer(ScanStatus status, DataDirStatus dirStatus) {
		try {
			String rv = f_content.build(status, dirStatus);
			if (rv != null) {
				tableViewer.setInput(f_content);
			}
			return rv;
		} finally {
			f_viewerControl.getDisplay().asyncExec (new Runnable () {
			      public void run () {
			    	  tableViewer.refresh();
			      }
			});
		}
	}
	
	@Override
	protected Control buildViewer(Composite parent) {
		tableViewer = new TableViewer(parent, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION);
		// Setup columns
		int i = 0;
		for (final String label : f_content.getColumnLabels()) {
			final TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.LEFT);
			column.getColumn().setText(label);
			column.getColumn().setWidth(40 * label.length());

			setupSorter(tableViewer, column, i);
			i++;
		}

		tableViewer.setContentProvider(f_content);
		tableViewer.setLabelProvider(f_content);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().pack();
		return tableViewer.getControl();
	}

	protected void setupSorter(final TableViewer tViewer, final TableViewerColumn column, final int colIdx) {
		final boolean intSort = f_content.isIntSortedColumn(colIdx); //"Line".equals(column.getColumn().getText());
		new ColumnViewerSorter<JSureRun>(tViewer, column.getColumn()) {
			@Override
			protected int doCompare(Viewer viewer, JSureRun e1, JSureRun e2) {
				ITableLabelProvider lp = ((ITableLabelProvider) tViewer.getLabelProvider());
				String t1 = lp.getColumnText(e1, colIdx);
				String t2 = lp.getColumnText(e2, colIdx);
				if (intSort) {
					return parseInt(t1) - parseInt(t2);
				}
				return t1.compareTo(t2);
			}
			private int parseInt(String i) {
				try {
					return Integer.parseInt(i);
				} catch(NumberFormatException e) {
					return Integer.MIN_VALUE; // Not a number
				}
			}
		};
	}
	
	@Override
	protected void makeActions() {
		f_deleteScanAction = new MultiSelectAction<JSureRun>("Delete scan(s)") {
			@Override
			public boolean run(IStructuredSelection s) {
				// TODO popup confirm dialog
				boolean deleted = super.run(s);
				if (deleted) {
					JSureScanManager.getInstance().removedScans();
				}
				return deleted;
			}
			@Override
			protected boolean run(JSureRun elt) {
				return FileUtility.recursiveDelete(elt.getDir());
			}
		};
		f_setAsBaselineAction = new SingleSelectAction<JSureRun>("Set as baseline") {
			@Override
			protected boolean run(JSureRun elt) {
				JSureScansHub.getInstance().setBaselineScan(elt.getDir());
				return true;
			}
		};
		f_setAsCurrentAction = new SingleSelectAction<JSureRun>("Set as current") {
			@Override
			protected boolean run(JSureRun elt) {
				JSureScansHub.getInstance().setCurrentScan(elt.getDir());
				return true;
			}
		};
	}
	
	@Override
	protected void fillContextMenu(IMenuManager manager,
			IStructuredSelection s) {
		if (!s.isEmpty()) {
			if (s.size() == 1) {
				manager.add(f_setAsCurrentAction);
				manager.add(f_setAsBaselineAction);
			}
			manager.add(f_deleteScanAction); 
		}
	}
	
	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		// TODO Auto-generated method stub	
	}
	
	static final Comparator<JSureRun> runsByDate = new Comparator<JSureRun>() {		
		@Override
		public int compare(JSureRun r1, JSureRun r2) {			
			try {
				return r1.getProjects().getDate().compareTo(r2.getProjects().getDate());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}
	};
	
	static class ContentProvider implements ITableContentProvider {
		JSureRun[] runs;
		JSureRun baseline, current;
		
		public String build(ScanStatus status, DataDirStatus dirStatus) {
			final JSureData data = JSureScanManager.getInstance().getData();
			if (dirStatus == DataDirStatus.UNCHANGED) {
				// Update scans
				if (status.baselineChanged()) {
					baseline = findScan(data, JSureScansHub.getInstance().getBaselineScanInfo());
				}
				if (status.currentChanged()) {
					current = findScan(data, JSureScansHub.getInstance().getCurrentScanInfo());
				}
			} else { // Enough changed
				runs = data.getAllRuns();
				Arrays.sort(runs, runsByDate);
				
				baseline = findScan(data, JSureScansHub.getInstance().getBaselineScanInfo());
				current = findScan(data, JSureScansHub.getInstance().getCurrentScanInfo());				
			}
			return "";
		}
		private JSureRun findScan(JSureData data, JSureScanInfo info) {
			if (info != null) {
				return data.findScan(info.getLocation());
			}
			return null;
		}		
		
		// Setup methods
		public String[] getColumnLabels() {
			return labels;
		}		
		public boolean isIntSortedColumn(int colIdx) {
			return colIdx == SIZE;
		}
		@Override
		public String getColumnTitle(int column) {
			return labels[column];
		}
		@Override
		public int getColumnWeight(int column) {
			return 40;
		}
		@Override
		public int numColumns() {
			return labels.length;
		}
	
		static final String[] labels = {
			"Status", "Date", "Projects", "Size in MB"
		};
		static final int SIZE = 3;
		
		// Display methods
		@Override
		public Object[] getElements(Object inputElement) {
			return runs;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public String getColumnText(Object element, int columnIndex) {
			final JSureRun r = (JSureRun) element;
			try {
				switch (columnIndex) {
				case 0:
					if (r == current) {
						return "Current";
					}
					if (r == baseline) {
						return "Baseline";
					}
					return "";
				case 1:
					final Date d = r.getProjects().getDate();
					return SLUtility.toStringHMS(d);
				case 2:
					return r.getProjects().getLabel();
				case SIZE:					
					return String.format("%1$.1f", r.getSizeInMB());
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
			return "";
		}
		
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub	
		}
		@Override
		public void addListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
		}
		@Override
		public boolean isLabelProperty(Object element, String property) {
			// TODO Auto-generated method stub
			return false;
		}
		@Override
		public void removeListener(ILabelProviderListener listener) {
			// TODO Auto-generated method stub
		}		
		@Override
		public void dispose() {
			// TODO Auto-generated method stub			
		}
	}
}
