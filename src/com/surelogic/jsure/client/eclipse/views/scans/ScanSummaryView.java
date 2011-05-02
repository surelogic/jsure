package com.surelogic.jsure.client.eclipse.views.scans;

import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import com.surelogic.common.*;
import com.surelogic.common.ui.ColumnViewerSorter;
import com.surelogic.common.ui.views.ITableContentProvider;
import com.surelogic.fluid.javac.JavacTypeEnvironment;
import com.surelogic.fluid.javac.Projects;
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
	final ProjectContentProvider f_projectsContent = new ProjectContentProvider();
	SashForm f_form;
	ListViewer projectList;
	TableViewer tableViewer;
	
	@Override
	protected StructuredViewer getViewer() {
		return tableViewer;
	}
	
	@Override
	protected String updateViewer(ScanStatus status, DataDirStatus dirStatus) {
		return updateViewer(status, dirStatus, false);
	}
	
	private String updateViewer(ScanStatus status, DataDirStatus dirStatus, boolean selectedProjsChanged) {
		try {
			final IStructuredSelection sel = (IStructuredSelection) projectList.getSelection();
			boolean changed = f_projectsContent.build(dirStatus);
			if (changed) {
				projectList.setInput(f_projectsContent);
				projectList.setSelection(sel);
			}		
			
			String rv = f_content.build(status, dirStatus, changed || selectedProjsChanged);
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
		f_form = new SashForm(parent, SWT.HORIZONTAL);
		f_form.setLayout(new FillLayout());
		
		projectList = new ListViewer(f_form);
		projectList.setContentProvider(f_projectsContent);
		projectList.setLabelProvider(f_projectsContent);
		projectList.getList().pack();
		projectList.addSelectionChangedListener(new ProjectSelectionListener());
		
		tableViewer = new TableViewer(f_form, SWT.H_SCROLL
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
		
		f_form.setWeights(new int[] {20,80});
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
		final Properties props;
		
		Summary(JSureRun r, InputStream in) throws IOException {
			this(r, new Properties());
			props.load(in);
		}

		Summary(JSureRun r, Properties totals) {
			run = r;
			props = totals;
		}

		public String getKey(String key) {
			return props.getProperty(key, "0");
		}
	}
	
	private static final Summary[] noSummaries = new Summary[0];
	
	static final String[] labels = {
		"Date", "Projects", PROMISES, CONSISTENT, INCONSISTENT, VOUCHES, ASSUMES, INFO, WARNING,
	};
	
	static final int KEYS = 2;
	
	class ContentProvider implements ITableContentProvider {
		JSureRun[] runs;
		Summary[] summaries;
		
		public String build(ScanStatus status, DataDirStatus dirStatus, boolean selectedProjectsChanged) {
			final JSureData data = JSureScanManager.getInstance().getData();
			if (selectedProjectsChanged || dirStatus != DataDirStatus.UNCHANGED) {
                // Enough changed
				runs = data.getAllRuns();

				// Get selected projects
				final IStructuredSelection ss = (IStructuredSelection) projectList.getSelection();
				final Object[] selectedProjects = ss.toArray();
				
				// Look for summaries
				final List<Summary> summaries = new ArrayList<Summary>();
				runLoop:
				for(JSureRun r : runs) {					
					if (selectedProjects.length > 0) {
						// Check if the run includes all of the selected projects
						try {
							final Projects runProjects = r.getProjects();
							for(Object proj : selectedProjects) {
								if (runProjects.get((String) proj) == null) {
									continue runLoop;
								}
							}
						} catch(Exception e) {
							e.printStackTrace();
							continue runLoop;
						}
					}
					File summary = new File(r.getDir(), RemoteJSureRun.SUMMARIES_ZIP);
					if (summary.exists()) {
						try {
							final ZipFile zf = new ZipFile(summary);
							if (selectedProjects.length > 0) {
								// Use selected projects to filter runs/summaries							
								final Properties totals = new Properties();
								for(Object proj : selectedProjects) {
									ZipEntry ze = zf.getEntry((String) proj);
									Properties props = new Properties();
									props.load(zf.getInputStream(ze));
									// Add to totals
									for(Map.Entry<Object,Object> e : props.entrySet()) {
										final int i = Integer.parseInt((String) e.getValue());
										final int j = Integer.parseInt((String) totals.getProperty((String) e.getKey(), "0"));
										totals.put(e.getKey(), Integer.toString(i+j));
									}
								}
								Summary s = new Summary(r, totals);
								summaries.add(s);
							} else {							
								ZipEntry e = zf.getEntry(SeaStats.ALL_PROJECTS);
								if (e != null) {
									Summary s = new Summary(r, zf.getInputStream(e));
									summaries.add(s);
								}
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
	
	class ProjectContentProvider implements IStructuredContentProvider, ILabelProvider {
		String[] projectNames = new String[0];
		
		/**
		 * @return true if changed
		 */
		public boolean build(DataDirStatus dirStatus) {
			final JSureData data = JSureScanManager.getInstance().getData();
			if (dirStatus != DataDirStatus.UNCHANGED) {
                // Enough changed, so find all the relevant projects
				final Set<String> names = new HashSet<String>();
				for(JSureRun r : data.getAllRuns()) {
					try {
						for(String p : r.getProjects().getProjectNames()) {
							if (!p.startsWith(JavacTypeEnvironment.JRE_NAME)) {
								names.add(p);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				projectNames = names.toArray(projectNames);
				Arrays.sort(projectNames);
				return true;
			}
			return false;
		}
		
		@Override
		public Object[] getElements(Object inputElement) {
			return projectNames;
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// TODO Auto-generated method stub
		}

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			return (String) element;
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
	}

	class ProjectSelectionListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			updateViewer(ScanStatus.NEITHER_CHANGED, DataDirStatus.UNCHANGED, true);
		}		
	}
}
