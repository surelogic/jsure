package com.surelogic.jsure.client.eclipse.views;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.surelogic.common.ui.ColumnViewerSorter;

/**
 * Uses a TableViewer
 * 
 * @author Edwin
 */
public abstract class AbstractScanTableView<T> extends
		AbstractScanStructuredView<T> {
	final IResultsTableContentProvider f_content;

	protected AbstractScanTableView(int style, Class<T> c,
			IResultsTableContentProvider content) {
		super(style, c);
		f_content = content;
	}

	@Override
	protected String updateViewer() {
		return f_content.build();
	}

	@Override
	protected StructuredViewer[] newViewers(Composite parent, int extraStyle) {
		return new StructuredViewer[] { makeTableViewer(parent, extraStyle, f_content) };
	}

	static TableViewer makeTableViewer(Composite parent, int extraStyle, IResultsTableContentProvider content) {
		final TableViewer tableViewer = new TableViewer(parent, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.FULL_SELECTION | extraStyle);
		// Setup columns
		int i = 0;
		for (final String label : content.getColumnLabels()) {
			final TableViewerColumn column = new TableViewerColumn(tableViewer,
					SWT.LEFT);
			column.getColumn().setText(label);
			column.getColumn().setWidth(40 * label.length());

			final boolean intSort = content.isIntSortedColumn(i); 
			setupSorter(tableViewer, column, i, intSort);
			i++;
		}

		tableViewer.setContentProvider(content);
		tableViewer.setLabelProvider(content);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().pack();
		return tableViewer;
	}
	
	static <T> void setupSorter(final TableViewer tViewer,
			final TableViewerColumn column, final int colIdx, final boolean intSort) {
		new ColumnViewerSorter<T>(tViewer, column.getColumn()) {
			@Override
			protected int doCompare(Viewer viewer, T e1, T e2) {
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

	/********************* Methods to handle selections ******************************/

	protected void appendText(StringBuilder sb, Object elt) {
		for (int i = 0; i < 3; i++) {
			if (i > 0) {
				sb.append(' ');
			}
			sb.append(f_content.getColumnText(elt, i));
		}
	}
}
