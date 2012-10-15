package com.surelogic.jsure.client.eclipse.views.scans;

import static com.surelogic.dropsea.ir.SeaStats.ASSUMES;
import static com.surelogic.dropsea.ir.SeaStats.CONSISTENT;
import static com.surelogic.dropsea.ir.SeaStats.INCONSISTENT;
import static com.surelogic.dropsea.ir.SeaStats.INFO;
import static com.surelogic.dropsea.ir.SeaStats.PROMISES;
import static com.surelogic.dropsea.ir.SeaStats.VOUCHES;
import static com.surelogic.dropsea.ir.SeaStats.WARNING;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.IAxis.Position;
import org.swtchart.IAxisSet;
import org.swtchart.ILineSeries;
import org.swtchart.ILineSeries.PlotSymbolType;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.ISeriesSet;
import org.swtchart.Range;

import com.surelogic.common.SLUtility;
import com.surelogic.common.ui.ColumnViewerSorter;
import com.surelogic.common.ui.views.AbstractSLView;
import com.surelogic.common.ui.views.ITableContentProvider;
import com.surelogic.dropsea.ir.SeaStats;
import com.surelogic.javac.JavacTypeEnvironment;
import com.surelogic.javac.Projects;
import com.surelogic.javac.jobs.RemoteJSureRun;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;


/**
 * A simple view to show what scans are available to be selected as the
 * baseline/current scan
 */
public class ScanSummaryView extends AbstractSLView implements
		JSureDataDirHub.CurrentScanChangeListener {
	final ContentProvider f_content = new ContentProvider();
	final ProjectContentProvider f_projectsContent = new ProjectContentProvider();
	SashForm f_form;
	ListViewer projectList;
	TableViewer tableViewer;
	Chart summaryChart;
	Color[] colors;
	MenuDetectListener f_chartMenuListener;

	@Override
	public void dispose() {
		try {
			JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);
		} finally {
			super.dispose();
		}
	}

	@Override
	public final void createPartControl(Composite parent) {
		super.createPartControl(parent);
		JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
		updateViewState();
	}

	@Override
	public void currentScanChanged(JSureScan scan) {
		getCurrentControl().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				updateViewState();
			}
		});
	}

	/**
	 * Update the internal state, presumably after a new scan
	 */
	private void updateViewState() {
		final String label = updateViewer(true);
		if (label != null) {
			getCurrentControl().getDisplay().asyncExec(new Runnable() {
				public void run() {

					if (getViewer() != null) {
						getViewer().setInput(getViewSite());
					}
					// TODO what else is there to do with the label?
					getCurrentControl().redraw();
				}
			});
		}
	}

	@Override
	protected StructuredViewer getViewer() {
		return tableViewer;
	}

	private String updateViewer(boolean selectedProjsChanged) {
		try {
			final IStructuredSelection sel = (IStructuredSelection) projectList
					.getSelection();
			boolean changed = f_projectsContent.build();
			if (changed) {
				projectList.setInput(f_projectsContent);
				projectList.setSelection(sel);
			}

			String rv = f_content.build(changed || selectedProjsChanged);
			if (rv != null) {
				tableViewer.setInput(f_content);
				updateChart();
			}
			return rv;
		} finally {
			getCurrentControl().getDisplay().asyncExec(new Runnable() {
				public void run() {
					projectList.refresh();
					tableViewer.refresh();
					summaryChart.redraw();
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

		tableViewer = new TableViewer(f_form, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION);
		// Setup columns
		int i = 0;
		for (final String label : f_content.getColumnLabels()) {
			final TableViewerColumn column = new TableViewerColumn(tableViewer,
					SWT.LEFT);
			column.getColumn().setText(label);
			column.getColumn().setWidth(
					f_content.getColumnWeight(i) * label.length());

			setupSorter(tableViewer, column, i);
			i++;
		}

		tableViewer.setContentProvider(f_content);
		tableViewer.setLabelProvider(f_content);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().pack();

		colors = new Color[] { null, null,
				parent.getDisplay().getSystemColor(SWT.COLOR_BLUE), // PROMISES
				parent.getDisplay().getSystemColor(SWT.COLOR_GREEN),// CONSISTENT
				parent.getDisplay().getSystemColor(SWT.COLOR_RED), // INCONSISTENT
				parent.getDisplay().getSystemColor(SWT.COLOR_MAGENTA), // VOUCHES
				parent.getDisplay().getSystemColor(SWT.COLOR_CYAN), // ASSUMES
				parent.getDisplay().getSystemColor(SWT.COLOR_BLACK), // INFO
				parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY),// WARNING
		};

		summaryChart = new Chart(f_form, SWT.NONE);
		summaryChart.getTitle().setVisible(false);
		final ISeriesSet set = summaryChart.getSeriesSet();
		final PlotSymbolType[] symbols = PlotSymbolType.values();
		final int[] yAxes = new int[2];
		final IAxisSet axes = summaryChart.getAxisSet();
		yAxes[0] = axes.getYAxisIds()[0];
		yAxes[1] = axes.createYAxis();
		for (int j = 0; j < 2; j++) {
			final IAxis yAxis = axes.getYAxis(yAxes[j]);
			yAxis.getTitle().setVisible(false);
			if (j > 0) {
				yAxis.setPosition(Position.Secondary);
				yAxis.getTick().setForeground(colors[HIGH]);
			}
		}
		for (int j = KEYS; j < labels.length; j++) {
			final ILineSeries s = (ILineSeries) set.createSeries(
					SeriesType.LINE, labels[j]);
			s.setSymbolType(symbols[1 + j % (symbols.length - 1)]); // to avoid
																	// NONE
			s.setSymbolColor(colors[j]);
			s.setLineColor(colors[j]);
			s.setYAxisId(yAxes[j >= HIGH ? 1 : 0]);
		}
		f_chartMenuListener = new MenuDetectListener() {
			public void menuDetected(MenuDetectEvent e) {
				final Menu contextMenu = new Menu(summaryChart.getShell(),
						SWT.POP_UP);
				setupChartContextMenu(contextMenu);
				summaryChart.setMenu(contextMenu);
				summaryChart.getPlotArea().setMenu(contextMenu);
			}
		};
		summaryChart.addMenuDetectListener(f_chartMenuListener);
		summaryChart.getPlotArea().addMenuDetectListener(f_chartMenuListener);

		f_form.setWeights(new int[] { 20, 40, 40 });
		return tableViewer.getControl();
	}

	private void setupChartContextMenu(Menu menu) {
		MenuItem zoomIn = new MenuItem(menu, SWT.PUSH);
		zoomIn.setText("Zoom in");
		zoomIn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				summaryChart.getAxisSet().zoomIn();
				summaryChart.redraw();
			}
		});
	}

	protected void setupSorter(final TableViewer tViewer,
			final TableViewerColumn column, final int colIdx) {
		final boolean intSort = f_content.isIntSortedColumn(colIdx); // "Line".equals(column.getColumn().getText());
		new ColumnViewerSorter<Summary>(tViewer, column.getColumn()) {
			@Override
			protected int doCompare(Viewer viewer, Summary e1, Summary e2) {
				ITableLabelProvider lp = ((ITableLabelProvider) tViewer
						.getLabelProvider());
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
				} catch (NumberFormatException e) {
					return Integer.MIN_VALUE; // Not a number
				}
			}
		};
	}

	static class Summary {
		private final JSureScan run;
		final Properties props;

		Summary(JSureScan r, InputStream in) throws IOException {
			this(r, new Properties());
			props.load(in);
		}

		Summary(JSureScan r, Properties totals) {
			run = r;
			props = totals;
		}

		public String getKey(String key) {
			return props.getProperty(key, "0");
		}
	}

	private static final Summary[] noSummaries = new Summary[0];

	static final String[] labels = { "Date", "Projects", PROMISES, CONSISTENT,
			INCONSISTENT, VOUCHES, ASSUMES, INFO, WARNING, };

	static final int KEYS = 2;
	static final int HIGH = 7;

	static final Comparator<Summary> summaryByDate = new Comparator<Summary>() {
		@Override
		public int compare(Summary s1, Summary s2) {
			try {
				return s1.run.getProjects().getDate()
						.compareTo(s2.run.getProjects().getDate());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}
	};

	class ContentProvider implements ITableContentProvider {
		JSureScan[] runs;
		Summary[] summaries;

		public String build(boolean selectedProjectsChanged) {
			final JSureDataDir data = JSureDataDirHub.getInstance()
					.getJSureDataDir();
			// Enough changed
			runs = data.getScansAsArray();

			// Get selected projects
			final IStructuredSelection ss = (IStructuredSelection) projectList
					.getSelection();
			final Object[] selectedProjects = ss.toArray();

			// Look for summaries
			final List<Summary> summaries = new ArrayList<Summary>();
			runLoop: for (JSureScan r : runs) {
				if (selectedProjects.length > 0) {
					// Check if the run includes all of the selected
					// projects
					try {
						final Projects runProjects = r.getProjects();
						for (Object proj : selectedProjects) {
							if (runProjects.get((String) proj) == null) {
								continue runLoop;
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						continue runLoop;
					}
				}
				File summary = new File(r.getDir(),
						RemoteJSureRun.SUMMARIES_ZIP);
				if (summary.exists()) {
					try {
						final ZipFile zf = new ZipFile(summary);
						if (selectedProjects.length > 0) {
							// Use selected projects to filter
							// runs/summaries
							final Properties totals = new Properties();
							for (Object proj : selectedProjects) {
								ZipEntry ze = zf.getEntry((String) proj);
								if (ze == null) {
									// No results (i.e. all zero)
									break;
								}
								Properties props = new Properties();
								props.load(zf.getInputStream(ze));
								// Add to totals
								for (Map.Entry<Object, Object> e : props
										.entrySet()) {
									final int i = Integer.parseInt((String) e
											.getValue());
									final int j = Integer
											.parseInt((String) totals.getProperty(
													(String) e.getKey(), "0"));
									totals.put(e.getKey(),
											Integer.toString(i + j));
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
						zf.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			this.summaries = summaries.toArray(noSummaries);
			Arrays.sort(this.summaries, summaryByDate);
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
			} catch (Exception e) {
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

	class ProjectContentProvider implements IStructuredContentProvider,
			ILabelProvider {
		String[] projectNames = ArrayUtils.EMPTY_STRING_ARRAY;

		/**
		 * @return true if changed
		 */
		public boolean build() {
			final JSureDataDir data = JSureDataDirHub.getInstance()
					.getJSureDataDir();
			// Enough changed, so find all the relevant projects
			final Set<String> names = new HashSet<String>();
			for (JSureScan r : data.getScansAsArray()) {
				try {
					for (String p : r.getProjects().getProjectNames()) {
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
		boolean alreadyCalled = false;

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			// Added to prevent infinite loop,
			// since the selection gets changed when the view gets updated
			if (alreadyCalled) {
				return;
			}
			alreadyCalled = true;
			updateViewer(true);
			alreadyCalled = false;
		}
	}

	private final SimpleDateFormat format = new SimpleDateFormat(
			"HH:mm:ss\nyy-MM-dd");

	private void updateChart() {
		for (int j = KEYS; j < labels.length; j++) {
			final List<Double> series = new ArrayList<Double>();
			for (Summary s : f_content.summaries) {
				final String num = s.getKey(labels[j]);
				series.add(Double.parseDouble(num));
			}
			// Convert to double[]
			double[] ySeries = new double[series.size()];
			for (int i = 0; i < series.size(); i++) {
				ySeries[i] = series.get(i);
			}
			summaryChart.getSeriesSet().getSeries(labels[j])
					.setYSeries(ySeries);
		}
		// Set labels
		final List<String> xLabels = new ArrayList<String>();
		for (Summary s : f_content.summaries) {
			try {
				Date d = s.run.getProjects().getDate();
				xLabels.add(format.format(d));
			} catch (Exception e) {
				e.printStackTrace();
				xLabels.add(s.run.getDirName());
			}
		}
		final IAxisSet axisSet = summaryChart.getAxisSet();
		final IAxis xAxis = axisSet.getXAxis(0);
		xAxis.setCategorySeries(xLabels.toArray(new String[xLabels.size()]));
		xAxis.enableCategory(true);
		xAxis.getTitle().setVisible(false);

		axisSet.adjustRange();
		for (IAxis y : axisSet.getYAxes()) {
			if (y.getRange().lower != 0.0) {
				y.setRange(new Range(0, y.getRange().upper));
			}
		}
	}

}
