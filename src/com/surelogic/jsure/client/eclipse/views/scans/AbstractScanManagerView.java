package com.surelogic.jsure.client.eclipse.views.scans;

import java.io.File;

import org.eclipse.swt.widgets.Composite;

import com.surelogic.jsure.client.eclipse.views.AbstractJSureView;
import com.surelogic.jsure.core.preferences.JSureEclipseHub;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.scans.IJSureScanListener;
import com.surelogic.scans.JSureScansHub;
import com.surelogic.scans.ScanStatus;

/**
 * Helps to displays the baseline/current scan, as well as other scan
 * information.
 */
public abstract class AbstractScanManagerView extends AbstractJSureView
		implements IJSureScanListener, JSureDataDirHub.Listener {
	protected AbstractScanManagerView() {
		JSureEclipseHub.init();
		JSureScansHub.getInstance().addListener(this);
		JSureDataDirHub.getInstance().addListener(this);
	}

	@Override
	public void dispose() {
		JSureScansHub.getInstance().removeListener(this);
		JSureDataDirHub.getInstance().removeListener(this);

		super.dispose();
	}

	@Override
	public final void createPartControl(Composite parent) {
		super.createPartControl(parent);
		updateViewState(ScanStatus.BOTH_CHANGED, JSureDataDirHub.Status.CHANGED);
	}

	public void updateScans(final JSureDataDirHub.Status s, File dir) {
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
				updateViewState(status, JSureDataDirHub.Status.UNCHANGED);
			}
		});
	}

	/**
	 * @return The label to be shown in the title
	 */
	protected abstract String updateViewer(ScanStatus status,
			JSureDataDirHub.Status dirStatus);

	/**
	 * Update the internal state, presumably after a new scan
	 */
	private void updateViewState(ScanStatus status,
			JSureDataDirHub.Status dirStatus) {
		if (status.changed() || dirStatus != JSureDataDirHub.Status.UNCHANGED) {
			final String label = updateViewer(status, dirStatus);
			if (label != null) {
				f_viewerControl.getDisplay().asyncExec(new Runnable() {
					public void run() {

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
