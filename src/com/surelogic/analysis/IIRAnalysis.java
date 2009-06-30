/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/IIRAnalysis.java,v 1.4 2008/08/14 20:31:20 chance Exp $*/
package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public interface IIRAnalysis {	
	String name();
	boolean analyzeAll();
	
	void init(IIRAnalysisEnvironment env);
	void preAnalysis(IIRAnalysisEnvironment env, IIRProject p);
	void analyzeBegin(IIRProject p);
	void doAnalysisOnAFile(CUDrop cud);
	IRNode[] analyzeEnd(IIRProject p);
	void postAnalysis(IIRProject p);
}
