package com.surelogic.jsure.client.eclipse.views;

import java.util.logging.Level;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;

import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.*;

import com.surelogic.common.i18n.*;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.jsure.core.preferences.JSureEclipseHub;
import com.surelogic.jsure.core.scans.IJSureScanListener;
import com.surelogic.jsure.core.scans.JSureScansHub;
import com.surelogic.jsure.core.scans.ScanStatus;

/**
 * Handles whether there is any scan to show.
 * diff?
 * 
 * @author Edwin
 */
public abstract class AbstractJSureScanView extends AbstractJSureView implements IJSureScanListener {
	protected static final String NO_RESULTS = I18N.msg("jsure.eclipse.view.no.scan.msg");
	private static final boolean updateTitles = false;
	
	protected PageBook f_viewerbook = null;	

	protected Label f_noResultsToShowLabel = null;
	
	/**
	 * The view title from the XML, or {@code null} if we couldn't get it.
	 */
	private String f_viewTitle;
	
	protected AbstractJSureScanView() {
		JSureEclipseHub.init();
		JSureScansHub.getInstance().addListener(this);
	}

	@Override
	public void dispose() {
		JSureScansHub.getInstance().removeListener(this);
		
		super.dispose();
	}
	
	@Override
	public final void createPartControl(Composite parent) {
		f_viewerbook = new PageBook(parent, SWT.NONE);
		f_noResultsToShowLabel = new Label(f_viewerbook, SWT.NONE);
		f_noResultsToShowLabel.setText(NO_RESULTS);
		f_viewTitle = getPartName();
		
		super.createPartControl(f_viewerbook);
		updateViewState(ScanStatus.BOTH_CHANGED);
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
		// TODO is this right with the pagebook?
		f_viewerControl.setFocus();
	}

	@Override
	public void scansChanged(ScanStatus status) {
		updateViewState(status);
	}
		
	/**
	 * @return The label to be shown in the title
	 */
	protected abstract String updateViewer(ScanStatus status);
	
	/**
	 * Update the internal state, presumably after a new scan
	 */
	private void updateViewState(ScanStatus status) {
		if (status.changed()) {
			final String label = updateViewer(status);
			f_viewerControl.getDisplay().asyncExec (new Runnable () {
			      public void run () {
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
