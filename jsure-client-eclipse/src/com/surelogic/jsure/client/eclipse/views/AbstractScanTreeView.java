package com.surelogic.jsure.client.eclipse.views;

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
	protected StructuredViewer[] newViewers(Composite parent, int extraStyle) {
		final TreeViewer treeViewer = new TreeViewer(parent, SWT.H_SCROLL
				| SWT.V_SCROLL | extraStyle);

		treeViewer.setContentProvider(f_content);
		treeViewer.setLabelProvider(f_content);
		return new StructuredViewer[] { treeViewer };
	}

	@Override
	public TreeViewer getViewer() {
		StructuredViewer s = super.getViewer();
		if (s instanceof TreeViewer)
			return (TreeViewer) s;
		else
			throw new IllegalStateException(
					"BUG: viewer should be a TreeViewer");
	}

	@Override
	protected void makeActions() {
		// TODO Auto-generated method stub

	}

	/********************* Methods to handle selections ******************************/

	protected void appendText(StringBuilder sb, Object elt) {
		sb.append(f_content.getText(elt));
	}
}
