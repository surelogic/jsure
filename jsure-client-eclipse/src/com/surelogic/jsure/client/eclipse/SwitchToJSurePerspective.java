package com.surelogic.jsure.client.eclipse;

import org.eclipse.ui.progress.UIJob;

import com.surelogic.common.ILifecycle;
import com.surelogic.java.persistence.JSureScan;
import com.surelogic.jsure.client.eclipse.jobs.SwitchToJSurePerspectiveJob;
import com.surelogic.jsure.core.scans.JSureDataDirHub;

/**
 * This class handles notifying the user when they perhaps should switch to the
 * JSure perspective. Because {@link JSureDataDirHub} is not in the user
 * interface layer of JSure we have to register and listen for notifications and
 * interact with the user in the user interface layer of JSure. This class
 * performs these functions.
 * <p>
 * The {@link Activator} must call {@link #init()} and {@link #dispose()} for
 * this class to function properly.
 * 
 */
public final class SwitchToJSurePerspective implements ILifecycle,
		JSureDataDirHub.NewScanListener {

	private static final SwitchToJSurePerspective INSTANCE = new SwitchToJSurePerspective();

	public static SwitchToJSurePerspective getInstance() {
		return INSTANCE;
	}

	private SwitchToJSurePerspective() {
		// Singleton
	}

	@Override
	public void init() {
		JSureDataDirHub.getInstance().addNewScanListener(this);
	}

	@Override
	public void dispose() {
		JSureDataDirHub.getInstance().removeNewScanListener(this);
	}

	@Override
	public void newScan(JSureScan scan) {
		/*
		 * A reasonable indication that we should be in the JSure perspective,
		 * that is, if we are not already.
		 */
		final UIJob pjob = new SwitchToJSurePerspectiveJob();
		pjob.schedule();

	}
}
