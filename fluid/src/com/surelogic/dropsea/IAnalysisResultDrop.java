package com.surelogic.dropsea;

import java.util.Collection;

public interface IAnalysisResultDrop extends IProofDrop, IReportedByAnalysisDrop {
	/**
	 * Gets the set of promise drops established, or checked, by this result.
	 * 
	 * @return the non-null (possibly empty) set of promise drops established, or
	 *         checked, by this result.
	 */
	Collection<? extends IPromiseDrop> getChecks();
}
