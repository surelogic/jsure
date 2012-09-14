package com.surelogic.jsure.client.eclipse.model.selection;

import com.surelogic.dropsea.IProofDrop;

public interface IScanDataUpdateObserver {

	void scanDataUpdateSetup();

	void scanDataUpdateVisit(IProofDrop proofDropInfo);

	void scanDataUpdateTeardown();

}
