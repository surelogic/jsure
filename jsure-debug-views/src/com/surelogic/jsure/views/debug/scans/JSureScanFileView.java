package com.surelogic.jsure.views.debug.scans;

import java.io.File;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.surelogic.common.FileUtility;
import com.surelogic.common.jobs.remote.AbstractRemoteSLJob;
import com.surelogic.common.ui.EclipseUIUtility;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.javac.persistence.ScanProperty;
import com.surelogic.jsure.client.eclipse.views.AbstractJSureScanView;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

/**
 * A tabbed view to show log files in the selected scan directory
 * 
 * @author edwin
 */
public class JSureScanFileView extends AbstractJSureScanView {
	private Label f_fileLabel;
	private Text f_fileText;

	@Override
	protected Control buildViewer(Composite parent) {
		GridLayout gl = new GridLayout();
	    gl.horizontalSpacing = gl.verticalSpacing = 0;
	    gl.marginHeight = gl.marginWidth = 0;
	    parent.setLayout(gl);
		/*
		f_fileLabel = new Label(parent, SWT.NONE);
		f_fileLabel.setText(" ");
		f_fileLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		*/
		f_fileText = new Text(parent, SWT.MULTI | SWT.BORDER
				| SWT.V_SCROLL | SWT.H_SCROLL);
		f_fileText.setFont(JFaceResources.getTextFont());
		f_fileText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		f_fileText.setText("Placeholder ...");
		return f_fileText;
	}

	@Override
	protected String updateViewer() {
		JSureScan scan = JSureDataDirHub.getInstance().getCurrentScan();
		if (scan != null) {
			//f_fileLabel.setText(scan.getDirName());
			
			//AbstractRemoteSLJob.LOG_NAME;
			final String contents = FileUtility.getFileContentsAsString(new File(scan.getDir(), ScanProperty.SCAN_PROPERTIES));
		    EclipseUIUtility.asyncExec(new Runnable() {
		        @Override
		        public void run() {
					f_fileText.setText(contents);
		        }
		    });
			return scan.getDirName();
		}
		return null;
	}
}
