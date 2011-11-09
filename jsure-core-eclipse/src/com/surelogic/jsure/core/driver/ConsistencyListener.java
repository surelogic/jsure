package com.surelogic.jsure.core.driver;

import com.surelogic.annotation.test.TestResult;
import com.surelogic.jsure.core.Eclipse;
import com.surelogic.jsure.core.listeners.IAnalysisListener;

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

		System.err.println("Time to update proof = " + (proofEnd - start) + " ms");

		TestResult.checkConsistency();
	}

	public void analysisPostponed() {
		// nothing to do
	}

	public void analysisStarting() {
		// nothing to do
	}
}
