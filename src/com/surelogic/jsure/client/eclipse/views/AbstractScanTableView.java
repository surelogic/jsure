package com.surelogic.jsure.client.eclipse.views;

import java.util.*;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;

import com.surelogic.common.ui.ColumnViewerSorter;
import com.surelogic.fluid.javac.scans.*;
import com.surelogic.jsure.core.listeners.PersistentDropInfo;

import edu.cmu.cs.fluid.sea.*;

/**
 * Uses a TableViewer
 * 
 * @author Edwin
 */
public abstract class AbstractScanTableView<T extends IDropInfo> extends AbstractScanStructuredView {
	final IResultsTableContentProvider f_content;
	final Class<T> clazz;
	
	protected AbstractScanTableView(int style, Class<T> c, IResultsTableContentProvider content) {
		super(style);
		clazz = c;
		f_content = content;
	}
	
	@Override
	protected String updateViewer(ScanStatus status) {
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

			setupSorter(column, i);
			i++;
		}

		tableViewer.setContentProvider(f_content);
		tableViewer.setLabelProvider(f_content);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().pack();
		return tableViewer;
	}
	
	protected void setupSorter(final TableViewerColumn column, final int colIdx) {
		final boolean intSort = f_content.isIntSortedColumn(colIdx); //"Line".equals(column.getColumn().getText());
		new ColumnViewerSorter<T>(column.getViewer(), column.getColumn()) {
			@Override
			protected int doCompare(Viewer viewer, T e1, T e2) {
				ITableLabelProvider lp = ((ITableLabelProvider) column.getViewer().getLabelProvider());
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
	
	protected List<? extends T> getSelectedRows() {
		final IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
		final List<T> result = new ArrayList<T>();
		for (final Object element : selection.toList()) {
			if (clazz.isInstance(element)) {
				result.add(clazz.cast(element));
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected final void handleDoubleClick(final IStructuredSelection selection) {
		final T d = (T) selection.getFirstElement();
		if (d != null) {
			highlightLineInJavaEditor(d.getSrcRef());
			handleDoubleClick(d);
		}
	}
	
	protected void handleDoubleClick(T d) {
		// Nothing right now
	}
	
	protected Action makeCopyAction(String label, String tooltip) {
		Action a = new Action() {
			@Override
			public void run() {
				f_clipboard.setContents(new Object[] { getSelectedText() },
						new Transfer[] { TextTransfer.getInstance() });
			}
		};	
		a.setText(label);
		a.setToolTipText(tooltip);
		return a;
	}
}
