package com.surelogic.jsure.client.eclipse.views;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Uses a TreeViewer
 * 
 * @author Edwin
 */
public abstract class AbstractScanTreeView<T> extends
		AbstractScanStructuredView<T> {
	final IJSureTreeContentProvider f_content;

	protected AbstractScanTreeView(int style, Class<T> c,
			IJSureTreeContentProvider content) {
		super(style, c);
		f_content = content;
	}

	@Override
	protected String updateViewer() {
		return f_content.build();
	}

	@Override
	protected StructuredViewer newViewer(Composite parent, int extraStyle) {
		final TreeViewer treeViewer = new TreeViewer(f_viewerbook, SWT.H_SCROLL
				| SWT.V_SCROLL | extraStyle);

		treeViewer.setContentProvider(f_content);
		treeViewer.setLabelProvider(f_content);
		return treeViewer;
	}

	/********************* Methods to handle selections ******************************/

	protected String getSelectedText() {
		IStructuredSelection selection = (IStructuredSelection) getViewer()
				.getSelection();
		StringBuilder sb = new StringBuilder();
		for (Object elt : selection.toList()) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			sb.append(f_content.getText(elt));
		}
		return sb.toString();
	}
}
