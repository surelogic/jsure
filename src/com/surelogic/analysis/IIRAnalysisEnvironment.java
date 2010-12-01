/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/IIRAnalysisEnvironment.java,v 1.1 2008/08/13 18:52:51 chance Exp $*/
package com.surelogic.analysis;

import java.io.IOException;
import java.io.OutputStream;

import edu.cmu.cs.fluid.sea.drops.CUDrop;

public interface IIRAnalysisEnvironment {
	IAnalysisMonitor getMonitor();
	
	void ensureClassIsLoaded(String qname);
	
	OutputStream makeResultStream(CUDrop cud) throws IOException;
	void closeResultStream() throws IOException;
}
