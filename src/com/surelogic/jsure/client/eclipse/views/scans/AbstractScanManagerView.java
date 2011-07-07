package com.surelogic.jsure.client.eclipse.views.scans;

import java.io.*;

import org.eclipse.swt.widgets.*;

import com.surelogic.javac.scans.*;
import com.surelogic.jsure.client.eclipse.views.*;
import com.surelogic.jsure.core.preferences.JSureEclipseHub;
import com.surelogic.jsure.core.scans.*;

/**
 * Helps to displays the baseline/current scan, as well as other scan info
 * 
 * @author Edwin
 */
public abstract class AbstractScanManagerView extends AbstractJSureView 
implements IJSureScanListener, IJSureScanManagerListener {	
	protected AbstractScanManagerView() {
		JSureEclipseHub.init();
		JSureScansHub.getInstance().addListener(this);
		JSureScanManager.getInstance().addListener(this);
	}

	@Override
	public void dispose() {
		JSureScansHub.getInstance().removeListener(this);
		JSureScanManager.getInstance().removeListener(this);
		
		super.dispose();
	}
	
	@Override
	public final void createPartControl(Composite parent) {
		super.createPartControl(parent);
		updateViewState(ScanStatus.BOTH_CHANGED, DataDirStatus.CHANGED);
	}

	public void updateScans(final DataDirStatus s, File dir) {
		f_viewerControl.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				updateViewState(ScanStatus.NEITHER_CHANGED, s);
			}
		});
	}
	
	public void scansChanged(final ScanStatus status) {
		f_viewerControl.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				updateViewState(status, DataDirStatus.UNCHANGED);
			}
		});
	}
		
	/**
	 * @return The label to be shown in the title
	 */
	protected abstract String updateViewer(ScanStatus status, DataDirStatus dirStatus);
	
	/**
	 * Update the internal state, presumably after a new scan
	 */
	private void updateViewState(ScanStatus status, DataDirStatus dirStatus) {
		if (status.changed() || dirStatus != DataDirStatus.UNCHANGED) {
			final String label = updateViewer(status, dirStatus);
			if (label != null) {
				f_viewerControl.getDisplay().asyncExec (new Runnable () {
					public void run () {

						if (getViewer() != null) {
							getViewer().setInput(getViewSite());
						}
						// TODO what else is there to do with the label?			    	  
						f_viewerControl.redraw();
					}
				});
			}
		}
	}
}
