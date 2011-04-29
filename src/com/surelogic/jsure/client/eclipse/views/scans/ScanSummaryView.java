package com.surelogic.jsure.client.eclipse.views.scans;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

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
import com.surelogic.fluid.javac.scans.*;
import com.surelogic.fluid.javac.jobs.RemoteJSureRun;
import com.surelogic.fluid.javac.persistence.*;
import com.surelogic.jsure.core.scans.*;

import edu.cmu.cs.fluid.sea.SeaStats;

import static edu.cmu.cs.fluid.sea.SeaStats.*;

/**
 * A simple view to show what scans are available to be selected as the baseline/current scan
 */
public class ScanSummaryView extends AbstractScanManagerView {
	final ContentProvider f_content = new ContentProvider();
	TableViewer tableViewer;
	
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
			column.getColumn().setWidth(f_content.getColumnWeight(i) * label.length());

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
		new ColumnViewerSorter<Summary>(tViewer, column.getColumn()) {
			@Override
			protected int doCompare(Viewer viewer, Summary e1, Summary e2) {
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
		// TODO
	}
	
	@Override
	protected void fillContextMenu(IMenuManager manager,
			IStructuredSelection s) {
		// TODO
	}
	
	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		// TODO Auto-generated method stub	
	}
	
	@Override
	public void scansChanged(final ScanStatus status) {
		// Ignore these updates
	}
	
	static class Summary {
		private final JSureRun run;
		final Properties props = new Properties();
		
		Summary(JSureRun r, InputStream in) throws IOException {
			run = r;
			props.load(in);
		}

		public String getKey(String key) {
			return props.getProperty(key, "0");
		}
	}
	
	private static final Summary[] noSummaries = new Summary[0];
	
	static class ContentProvider implements ITableContentProvider {
		JSureRun[] runs;
		Summary[] summaries;
		
		public String build(ScanStatus status, DataDirStatus dirStatus) {
			final JSureData data = JSureScanManager.getInstance().getData();
			if (dirStatus != DataDirStatus.UNCHANGED) {
                // Enough changed
				runs = data.getAllRuns();
				
				// Look for summaries
				final List<Summary> summaries = new ArrayList<Summary>();
				for(JSureRun r : runs) {
					File summary = new File(r.getDir(), RemoteJSureRun.SUMMARIES_ZIP);
					if (summary.exists()) {
						try {
							ZipFile zf = new ZipFile(summary);
							ZipEntry e = zf.getEntry(SeaStats.ALL_PROJECTS);
							if (e != null) {
								Summary s = new Summary(r, zf.getInputStream(e));
								summaries.add(s);
							}
						} catch(IOException e) {
							e.printStackTrace();
						}
					}
				}
				this.summaries = summaries.toArray(noSummaries);
			} else {
				this.summaries = noSummaries;
			}
			return "";
		}	
		
		// Setup methods
		public String[] getColumnLabels() {
			return labels;
		}		
		public boolean isIntSortedColumn(int colIdx) {
			return colIdx >= KEYS;
		}
		@Override
		public String getColumnTitle(int column) {
			return labels[column];
		}
		@Override
		public int getColumnWeight(int column) {
			return column >= KEYS ? 10 : 40;
		}
		@Override
		public int numColumns() {
			return labels.length;
		}
	
		static final String[] labels = {
			"Date", "Projects", PROMISES, CONSISTENT, INCONSISTENT, VOUCHES, ASSUMES, INFO, WARNING,
		};
		
		static final int KEYS = 2;
		
		// Display methods
		@Override
		public Object[] getElements(Object inputElement) {
			return summaries;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public String getColumnText(Object element, int columnIndex) {
			final Summary s = (Summary) element;
			try {
				switch (columnIndex) {
				case 0:
					final Date d = s.run.getProjects().getDate();
					return SLUtility.toStringHMS(d);
				case 1:
					return s.run.getProjects().getLabel();
				default:
					return s.getKey(labels[columnIndex]);
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
