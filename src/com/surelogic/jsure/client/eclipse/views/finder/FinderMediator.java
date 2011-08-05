package com.surelogic.jsure.client.eclipse.views.finder;

import java.util.List;
import java.util.Set;

import com.surelogic.common.ILifecycle;
import com.surelogic.common.ui.CascadingList;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IDropInfo;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.Sea;

public final class FinderMediator implements ILifecycle,
		JSureDataDirHub.CurrentScanChangeListener {

	private final CascadingList f_finder;

	FinderMediator(CascadingList finder) {
		f_finder = finder;
	}

	@Override
	public void init() {
		JSureDataDirHub.getInstance().addCurrentScanChangeListener(this);

	}

	@Override
	public void dispose() {
		JSureDataDirHub.getInstance().removeCurrentScanChangeListener(this);
	}

	@Override
	public void currentScanChanged(JSureScan scan) {
		JSureScanInfo scanInfo = JSureDataDirHub.getInstance()
				.getCurrentScanInfo();

		Set<IDropInfo> promiseDrops = scanInfo
				.getDropsOfType(PromiseDrop.class);

		System.out.println("- PROMISES - - - - - - - - - - - - - - - - -- ");
		for (IDropInfo d : promiseDrops) {
			System.out.println(" PromiseDrop: " + d.getMessage());
		}

		Set<IDropInfo> resultDrops = scanInfo.getDropsOfType(ResultDrop.class);
		System.out.println("- RESULTS  - - - - - - - - - - - - - - - - -- ");
		for (IDropInfo d : resultDrops) {
			System.out.println(" ResultDrop: " + d.getMessage());
		}
	}
}
