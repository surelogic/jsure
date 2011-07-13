package com.surelogic.jsure.client.eclipse.views.finder;

import com.surelogic.common.ILifecycle;
import com.surelogic.jsure.core.scans.JSureScansHub;

public final class FinderMediator implements ILifecycle, JSureScansHub.Listener {

	public FinderMediator() {
	}

	@Override
	public void init() {
		JSureScansHub.getInstance().addListener(this);

	}

	@Override
	public void scansChanged(JSureScansHub.ScanStatus status) {

	}

	@Override
	public void dispose() {
		JSureScansHub.getInstance().removeListener(this);
	}
}
