package com.surelogic.jsure.client.eclipse.views;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.surelogic.common.ui.ColumnViewerSorter;
import com.surelogic.jsure.core.scans.JSureScansHub;

/**
 * Uses a TableViewer
 * 
 * @author Edwin
 */
public abstract class AbstractScanTableView<T> extends AbstractScanStructuredView<T> {
	final IResultsTableContentProvider f_content;
	
	protected AbstractScanTableView(int style, Class<T> c, IResultsTableContentProvider content) {
		super(style, c);
		f_content = content;
	}
	
	@Override
	protected String updateViewer(JSureScansHub.ScanStatus status) {
		return f_content.build(status);
	}
	
	@Override
	protected StructuredViewer newViewer(Composite parent, int extraStyle) {
		final TableViewer tableViewer = new TableViewer(parent, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | extraStyle);
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
		return tableViewer;
	}
	
	protected void setupSorter(final TableViewer tViewer, final TableViewerColumn column, final int colIdx) {
		final boolean intSort = f_content.isIntSortedColumn(colIdx); //"Line".equals(column.getColumn().getText());
		new ColumnViewerSorter<T>(tViewer, column.getColumn()) {
			@Override
			protected int doCompare(Viewer viewer, T e1, T e2) {
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

	/********************* Methods to handle selections ******************************/
	
	protected String getSelectedText() {
		IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
		StringBuilder sb = new StringBuilder();
		for (Object elt : selection.toList()) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			for(int i=0; i<3; i++) {
				if (i > 0) {
					sb.append(' ');
				}
				sb.append(f_content.getColumnText(elt, i));
			}
		}
		return sb.toString();
	}
}
