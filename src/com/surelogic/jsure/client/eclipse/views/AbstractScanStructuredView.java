package com.surelogic.jsure.client.eclipse.views;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

/**
 * Uses a StructuredViewer
 * 
 * @author Edwin
 */
public abstract class AbstractScanStructuredView extends AbstractJSureScanView {
	private final int f_extraStyle;
	private StructuredViewer f_viewer;

	protected AbstractScanStructuredView() {
		this(SWT.NONE);
	}
	
	protected AbstractScanStructuredView(int style) {
		f_extraStyle = style;
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
}
