package com.surelogic.jsure.client.eclipse.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;

import com.surelogic.common.ui.ColumnViewerSorter;
import com.surelogic.jsure.core.listeners.PersistentDropInfo;

import edu.cmu.cs.fluid.sea.IDropInfo;

public abstract class AbstractResultsTableView<T extends IDropInfo> extends
		AbstractDoubleCheckerView {
	final IResultsTableContentProvider f_content;
	final Class<T> clazz;

	protected AbstractResultsTableView(int style, Class<T> c,
			IResultsTableContentProvider content) {
		super(true, style);
		clazz = c;
		f_content = content;
	}

	@Override
	protected void subscribe() {
		PersistentDropInfo.getInstance().addListener(this);
	}

	@Override
	protected void finishCreatePartControl() {
		if (PersistentDropInfo.getInstance().load()) {
			f_content.build();
			setViewerVisibility(true);
			System.out.println("Loaded snapshot for " + this);
		}
	}

	protected String getSelectedText() {
		IStructuredSelection selection = (IStructuredSelection) viewer
				.getSelection();
		StringBuilder sb = new StringBuilder();
		for (Object elt : selection.toList()) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			for (int i = 0; i < 3; i++) {
				if (i > 0) {
					sb.append(' ');
				}
				sb.append(f_content.getColumnText(elt, i));
			}
		}
		return sb.toString();
	}

	protected List<? extends T> getSelectedRows() {
		final IStructuredSelection selection = (IStructuredSelection) viewer
				.getSelection();
		final List<T> result = new ArrayList<T>();
		for (final Object element : selection.toList()) {
			if (clazz.isInstance(element)) {
				result.add(clazz.cast(element));
			}
		}
		return result;
	}

	@Override
	protected void makeActions() {
		// nothing
	}

	@Override
	protected void fillLocalPullDown(final IMenuManager manager) {
		// nothing
	}

	@Override
	protected void fillLocalToolBar(final IToolBarManager manager) {
		// nothing
	}

	@Override
	protected void setViewState() {
		// nothing
	}

	@Override
	protected void setupViewer() {
		int i = 0;
		for (final String label : f_content.getColumnLabels()) {
			final TableViewerColumn column = new TableViewerColumn(tableViewer,
					SWT.LEFT);
			column.getColumn().setText(label);
			column.getColumn().setWidth(40 * label.length());

			setupSorter(column, i);
			i++;
		}

		viewer.setContentProvider(f_content);
		viewer.setLabelProvider(f_content);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.getTable().pack();
	}

	protected void setupSorter(TableViewerColumn column, final int colIdx) {
		final boolean intSort = f_content.isIntSortedColumn(colIdx); // "Line".equals(column.getColumn().getText());
		new ColumnViewerSorter<T>(tableViewer, column.getColumn()) {
			@Override
			protected int doCompare(Viewer viewer, T e1, T e2) {
				ITableLabelProvider lp = ((ITableLabelProvider) tableViewer
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

	@Override
	protected final void updateView() {
		f_content.build();
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
				clipboard.setContents(new Object[] { getSelectedText() },
						new Transfer[] { TextTransfer.getInstance() });
			}
		};
		a.setText(label);
		a.setToolTipText(tooltip);
		return a;
	}
}
