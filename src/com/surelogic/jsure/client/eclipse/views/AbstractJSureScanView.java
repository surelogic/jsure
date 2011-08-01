package com.surelogic.jsure.client.eclipse.views;

import java.util.logging.Level;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.PageBook;

import com.surelogic.common.i18n.I18N;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.javac.persistence.JSureDataDir;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

/**
 * Handles whether there is any scan to show. diff?
 * 
 * @author Edwin
 */
public abstract class AbstractJSureScanView extends AbstractJSureView implements
		JSureDataDirHub.Listener {
	protected static final String NO_RESULTS = I18N
			.msg("jsure.eclipse.view.no.scan.msg");
	private static final boolean updateTitles = false;

	protected PageBook f_viewerbook = null;

	protected Label f_noResultsToShowLabel = null;

	/**
	 * The view title from the XML, or {@code null} if we couldn't get it.
	 */
	private String f_viewTitle;

	protected AbstractJSureScanView() {
		JSureDataDirHub.getInstance().addListener(this);
	}

	@Override
	public void dispose() {
		JSureDataDirHub.getInstance().removeListener(this);

		super.dispose();
	}

	@Override
	public final void createPartControl(Composite parent) {
		f_viewerbook = new PageBook(parent, SWT.NONE);
		f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
		f_noResultsToShowLabel.setText(NO_RESULTS);
		f_viewTitle = getPartName();

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
	public void scanContentsChanged(JSureDataDir dataDir) {
		// Ignore
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
					updateViewTitle(label);
				} else {
					setViewerVisibility(false);
					updateViewTitle(null);
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

	/**
	 * Used to set the view title. We use this method to add the project of
	 * focus to JSure to the view title.
	 */
	private void updateViewTitle(String label) {
		if (!updateTitles) {
			return;
		}
		/*
		 * Set a default if we got a null for the view title from the plug-in
		 * XML.
		 */
		if (f_viewTitle == null) {
			f_viewTitle = "Verification Status";
			SLLogger.getLogger().log(
					Level.WARNING,
					"Didn't get a view title from XML using \"" + f_viewTitle
							+ "\"");
		}

		if (label != null) {
			setPartName(f_viewTitle + " (" + label + ")");
		} else {
			setPartName(f_viewTitle);
		}
	}
}
