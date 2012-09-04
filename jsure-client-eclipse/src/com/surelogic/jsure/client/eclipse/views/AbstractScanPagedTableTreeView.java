package com.surelogic.jsure.client.eclipse.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Switches between a table and a tree
 * 
 * @author Edwin
 */
public abstract class AbstractScanPagedTableTreeView<T> extends AbstractScanStructuredView<T> {
	private static final String TOGGLE_VIEW = "Toggle between tree and table";
	protected final IJSureTableTreeContentProvider f_content;

	private final Action f_toggleView = new Action(TOGGLE_VIEW, IAction.AS_CHECK_BOX) {
		@Override
		public void run() {
			toggleViewer();
		}
	};
	
	protected AbstractScanPagedTableTreeView(int style, Class<T> c, 
			IJSureTableTreeContentProvider content) {
		super(style, c);
		f_content = content;
		f_toggleView.setToolTipText(TOGGLE_VIEW);
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
	
	protected void toggleViewer() {		
		f_content.setAsTree(!f_content.showAsTree());		
		if (getViewer() != null) {
			getViewer().setInput(getViewSite());
			//getViewer().refresh();
		}
		f_viewerbook.showPage(getCurrentControl());
		getCurrentControl().redraw();
	}
	
	@Override
	protected String updateViewer() {
		return f_content.build();
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		super.fillLocalPullDown(manager);
		manager.add(f_toggleView);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		super.fillLocalToolBar(manager);
		manager.add(f_toggleView);
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
