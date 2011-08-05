package com.surelogic.jsure.client.eclipse.model.selection;

import edu.cmu.cs.fluid.sea.IProofDropInfo;

public interface IScanDataUpdateObserver {

	void scanDataUpdateSetup();

	void scanDataUpdateVisit(IProofDropInfo proofDropInfo);

	void scanDataUpdateTeardown();

}
