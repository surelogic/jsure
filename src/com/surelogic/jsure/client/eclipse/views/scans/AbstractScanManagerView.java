package com.surelogic.jsure.client.eclipse.views.scans;

import org.eclipse.swt.widgets.Composite;

import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.client.eclipse.views.AbstractJSureView;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

/**
 * Helps to displays the baseline/current scan, as well as other scan
 * information.
 */
public abstract class AbstractScanManagerView extends AbstractJSureView
		implements JSureDataDirHub.Listener {
	protected AbstractScanManagerView() {
		JSureDataDirHub.getInstance().addListener(this);
	}

	@Override
	public void dispose() {
		JSureDataDirHub.getInstance().removeListener(this);

		super.dispose();
	}

	@Override
	public final void createPartControl(Composite parent) {
		super.createPartControl(parent);
		updateViewState();
	}

	@Override
	public void scanContentsChanged(JSureDataDir dataDir) {
		f_viewerControl.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				updateViewState();
			}
		});
	}

	@Override
	public void currentScanChanged(JSureScan scan) {
		f_viewerControl.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				updateViewState();
			}
		});
	}

	public void updateScans() {
		f_viewerControl.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				updateViewState();
			}
		});
	}

	public void scansChanged() {
		f_viewerControl.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				updateViewState();
			}
		});
	}

	/**
	 * @return The label to be shown in the title
	 */
	protected abstract String updateViewer();

	/**
	 * Update the internal state, presumably after a new scan
	 */
	private void updateViewState() {
		final String label = updateViewer();
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
