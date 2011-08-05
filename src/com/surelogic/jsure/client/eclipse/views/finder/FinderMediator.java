package com.surelogic.jsure.client.eclipse.views.finder;

import java.util.List;

import com.surelogic.common.ILifecycle;
import com.surelogic.common.ui.CascadingList;
import com.surelogic.javac.persistence.JSureScan;
import com.surelogic.jsure.core.scans.JSureDataDirHub;
import com.surelogic.jsure.core.scans.JSureScanInfo;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.IProofDropInfo;

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

		List<IProofDropInfo> proofDrops = scanInfo.getProofDropInfo();

		System.out.println("- - - - - - - - - - - - - - - - - -- ");
		for (IProofDropInfo d : proofDrops) {
			System.out.println("DROP:");
			System.out.println(" getEntityName()=" + d.getEntityName());
			System.out.println(" getMessage()=: " + d.getMessage());
			System.out.println(" getType()=" + d.getType());
			ISrcRef sr = d.getSrcRef();
			if (sr == null) {
				System.out.println("  NO SOURCE REFERENCE");
			} else {
				System.out.println("  sr.getProject()=" + sr.getProject());
				System.out.println("  sr.getPackage()=" + sr.getPackage());
				System.out.println("  sr.getCUName()=" + sr.getCUName());
			}
			System.out
					.println(" getprovedConsistent()=" + d.provedConsistent());
			System.out.println(" proofUsesRedDot()=" + d.proofUsesRedDot());
			System.out.println(" isTimeout()=" + d.isTimeout());

		}
		System.out.println("- - - - - - - - - - - - - - - - - -- ");
	}
}
