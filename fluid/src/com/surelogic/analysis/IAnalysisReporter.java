package com.surelogic.analysis;

import java.util.logging.Level;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * The reporting API for supporting proposed promises / databases
 * 
 * @author chance
 */
public interface IAnalysisReporter {
	void reportInfo(IRNode n, String msg);

	IAnalysisReporter NULL = new IAnalysisReporter() {
		@Override
    public void reportInfo(IRNode n, String msg) {
			if (SLLogger.getLogger().isLoggable(Level.FINE)) {
				SLLogger.getLogger().fine("Doing nothing: " + msg);
			}
		}
	};
}
