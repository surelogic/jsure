package com.surelogic.jsure.client.eclipse.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.*;

import edu.cmu.cs.fluid.sea.IDropInfo;

/**
 * Uses a StructuredViewer
 * 
 * @author Edwin
 */
public abstract class AbstractScanStructuredView<T> extends AbstractJSureScanView {
	private final int f_extraStyle;
	private StructuredViewer f_viewer;
	final Class<T> clazz;
	
	protected AbstractScanStructuredView(Class<T> c) {
		this(SWT.NONE, c);
	}
	
	protected AbstractScanStructuredView(int style, Class<T> c) {
		f_extraStyle = style;
		clazz = c;
	}
	
	@Override
	protected final StructuredViewer getViewer() {
		return f_viewer;
	}
	
	@Override
	protected Control buildViewer(Composite parent) {
		f_viewer = newViewer(parent, f_extraStyle);
		return f_viewer.getControl();
	}

	protected abstract StructuredViewer newViewer(Composite parent, int extraStyle);
	

	@Override
	protected void fillLocalPullDown(IMenuManager manager) {
		// TODO Auto-generated method stub		
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		// TODO Auto-generated method stub	
	}
	
	/********************* Methods to handle selections ******************************/
	
	protected final List<? extends T> getSelectedRows() {
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
		    if (d instanceof IDropInfo) {
		        IDropInfo di = (IDropInfo) d;		    
		        highlightLineInJavaEditor(di.getSrcRef());
		    }
			handleDoubleClick(d);
		}
	}
	
	protected void handleDoubleClick(T d) {
		// Nothing right now
	}	
	
	protected final Action makeCopyAction(String label, String tooltip) {
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
	
	protected abstract String getSelectedText();
}
