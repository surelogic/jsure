package com.surelogic.jsure.client.eclipse.views;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.PageBook;

import com.surelogic.common.i18n.I18N;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

/**
 * Handles whether there is any scan to show. diff?
 * 
 * @author Edwin
 */
public abstract class AbstractJSureScanView extends AbstractJSureView implements
		JSureDataDirHub.CurrentScanChangeListener {
	protected static final String NO_RESULTS = I18N
			.msg("jsure.eclipse.view.no.scan.msg");

	protected PageBook f_viewerbook = null;

	protected Label f_noResultsToShowLabel = null;

	protected AbstractJSureScanView() {
		JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);
	}

	@Override
	public void dispose() {
		JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);

		super.dispose();
	}

	@Override
	public final void createPartControl(Composite parent) {
		f_viewerbook = new PageBook(parent, SWT.NONE);
		f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
		f_noResultsToShowLabel.setText(NO_RESULTS);

		super.createPartControl(f_viewerbook);
		updateViewState();
	}

	/**
	 * Setup the custom view
	 */
	protected abstract Control buildViewer(Composite parent);

	/**
	 * Enables various functionality if non-null
	 */
	protected StructuredViewer getViewer() {
		return null;
	}

	@Override
	public void setFocus() {
		f_viewerbook.setFocus();
	}

	@Override
	public void currentScanChanged(JSureScan scan) {
		updateViewState();
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
		f_viewerControl.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (label != null) {
					if (getViewer() != null) {
						getViewer().setInput(getViewSite());
					}
					setViewerVisibility(true);
				} else {
					setViewerVisibility(false);
				}
				// TODO is this right?
				f_viewerControl.redraw();
			}
		});
	}

	private final void setViewerVisibility(boolean showResults) {
		if (f_viewerbook.isDisposed())
			return;
		if (showResults) {
			f_viewerbook.showPage(f_viewerControl);
		} else {
			f_viewerbook.showPage(f_noResultsToShowLabel);
		}
	}
}
