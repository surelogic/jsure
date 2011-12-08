package com.surelogic.jsure.client.eclipse.views;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Switches between a table and a tree
 * 
 * @author Edwin
 */
public abstract class AbstractScanPagedTableTreeView<T> extends AbstractScanStructuredView<T> {
	final IJSureTableTreeContentProvider f_content;
	
	protected AbstractScanPagedTableTreeView(int style, Class<T> c, 
			IJSureTableTreeContentProvider content) {
		super(style, c);
		f_content = content;
	}

	@Override
	protected StructuredViewer[] newViewers(Composite parent, int extraStyle) {
		final TableViewer tableViewer = AbstractScanTableView.makeTableViewer(parent, extraStyle, f_content);		
		final TreeViewer treeViewer = new TreeViewer(parent, SWT.H_SCROLL
				| SWT.V_SCROLL | extraStyle);

		treeViewer.setContentProvider(f_content);
		treeViewer.setLabelProvider(f_content);
		return new StructuredViewer[] { tableViewer, treeViewer };
	}

	@Override
	protected final int getViewIndex() {
		return f_content.showAsTree() ? 1 : 0;
	}
	
	@Override
	protected String updateViewer() {
		return f_content.build();
	}

	@Override
	protected final void appendText(StringBuilder sb, Object elt) {
		if (f_content.showAsTree()) {
			sb.append(f_content.getText(elt));
		} else {
			for(int i=0; i<f_content.getColumnLabels().length; i++) {
				if (i != 0) {
					sb.append(' ');
				}
				sb.append(f_content.getColumnText(elt, i));
			}
		}		
	}	
}
