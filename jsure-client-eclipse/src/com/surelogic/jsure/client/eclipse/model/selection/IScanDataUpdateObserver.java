package com.surelogic.jsure.client.eclipse.model.selection;

import edu.cmu.cs.fluid.sea.IProofDrop;

public interface IScanDataUpdateObserver {

	void scanDataUpdateSetup();

	void scanDataUpdateVisit(IProofDrop proofDropInfo);

	void scanDataUpdateTeardown();

}
