package com.surelogic.jsure.client.eclipse.views.scans;

import java.io.File;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureDataDirHub.Status;
import com.surelogic.jsure.core.scans.JSureScansHub;
import com.surelogic.jsure.core.scans.JSureScansHub.ScanStatus;

public final class ScanManagerView extends ViewPart implements
		JSureScansHub.Listener, JSureDataDirHub.Listener {

	private ScanManagerMediator f_mediator = null;

	@Override
	public void createPartControl(Composite parent) {

		final CheckboxTableViewer table = CheckboxTableViewer.newCheckList(
				parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION
						| SWT.MULTI);
		f_mediator = new ScanManagerMediator(table);
		f_mediator.init();

		JSureScansHub.getInstance().addListener(this);
		JSureDataDirHub.getInstance().addListener(this);
	}

	@Override
	public void dispose() {
		JSureScansHub.getInstance().removeListener(this);
		JSureDataDirHub.getInstance().removeListener(this);

		if (f_mediator != null)
			f_mediator.dispose();
		f_mediator = null;

		super.dispose();
	}

	@Override
	public void setFocus() {
		final ScanManagerMediator mediator = f_mediator;
		if (mediator != null)
			mediator.setFocus();
	}

	@Override
	public void updateScans(Status event, File directory) {
		notifyMediatorInSwtThread();
	}

	@Override
	public void scansChanged(ScanStatus status) {
		notifyMediatorInSwtThread();
	}

	private void notifyMediatorInSwtThread() {
		System.out.println("notifyMediatorInSwtThread()");
		final ScanManagerMediator mediator = f_mediator;
		if (mediator != null)
			mediator.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					mediator.refreshScanContents();
				}
			});
	}
}
