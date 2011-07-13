package com.surelogic.jsure.client.eclipse.views.finder;

import com.surelogic.common.ILifecycle;
import com.surelogic.jsure.core.scans.IJSureScanListener;
import com.surelogic.jsure.core.scans.JSureScansHub;
import com.surelogic.jsure.core.scans.ScanStatus;

public final class FinderMediator implements ILifecycle, IJSureScanListener {

	public FinderMediator() {
	}

	@Override
	public void init() {
		JSureScansHub.getInstance().addListener(this);

	}

	@Override
	public void scansChanged(ScanStatus status) {

	}

	@Override
	public void dispose() {
		JSureScansHub.getInstance().removeListener(this);
	}
}
