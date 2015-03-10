/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/IIRAnalysisEnvironment.java,v 1.1 2008/08/13 18:52:51 chance Exp $*/
package com.surelogic.analysis;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.surelogic.dropsea.ir.drops.CUDrop;


/**
 * Map is used to share info 
 *
 * @author Edwin
 */
public interface IIRAnalysisEnvironment extends Map<Object,Object> {
	IAnalysisMonitor getMonitor();
	
	void ensureClassIsLoaded(String qname);
	
	OutputStream makeResultStream(CUDrop cud) throws IOException;
	void closeResultStream() throws IOException;
	void done();
}
