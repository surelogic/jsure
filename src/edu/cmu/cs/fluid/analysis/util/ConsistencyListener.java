package edu.cmu.cs.fluid.analysis.util;

import java.util.logging.Level;

import com.surelogic.annotation.test.TestResult;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.dc.IAnalysisListener;
import edu.cmu.cs.fluid.eclipse.Eclipse;
import edu.cmu.cs.fluid.sea.Sea;

public class ConsistencyListener implements IAnalysisListener {
	public static final ConsistencyListener prototype = new ConsistencyListener();

	private ConsistencyListener() {
		// nothing to do
	}

	public synchronized void analysisCompleted() {
		Eclipse.initialize();

		// update the whole-program proof
		long start = System.currentTimeMillis();
		Sea.getDefault().updateConsistencyProof();
		long proofEnd = System.currentTimeMillis();

		SLLogger.getLogger().log(Level.INFO,
				"Time to update proof = " + (proofEnd - start) + " ms");

		TestResult.checkConsistency();
	}

	public void analysisPostponed() {
		// nothing to do
	}

	public void analysisStarting() {
		// nothing to do
	}
}
