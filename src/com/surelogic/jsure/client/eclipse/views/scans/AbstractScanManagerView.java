package com.surelogic.jsure.client.eclipse.views.scans;

import java.io.*;

import org.eclipse.swt.widgets.*;

import com.surelogic.fluid.javac.scans.*;
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
	public final void createPartControl(Composite parent) {
		super.createPartControl(parent);
		updateViewState(ScanStatus.BOTH_CHANGED, DataDirStatus.CHANGED);
	}

	public void updateScans(DataDirStatus s, File dir) {
		updateViewState(ScanStatus.NEITHER_CHANGED, s);
	}
	
	public void scansChanged(ScanStatus status) {
		updateViewState(status, DataDirStatus.UNCHANGED);
	}
		
	/**
	 * @return The label to be shown in the title
	 */
	protected abstract String updateViewer(ScanStatus status, DataDirStatus dirStatus);
	
	/**
	 * Update the internal state, presumably after a new scan
	 */
	private void updateViewState(ScanStatus status, DataDirStatus dirStatus) {
		if (status.changed()) {
			final String label = updateViewer(status, dirStatus);
			f_viewerControl.getDisplay().asyncExec (new Runnable () {
			      public void run () {
			    	  if (label != null) {
			    		  if (getViewer() != null) {
			    			  getViewer().setInput(getViewSite());
			    		  }
				    	  // TODO what else is there to do with the label?
			    	  }
			    	  f_viewerControl.redraw();
			      }
			 });
		}
	}
}
