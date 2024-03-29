/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis;

import java.io.IOException;
import java.io.OutputStream;

import com.surelogic.dropsea.ir.drops.CUDrop;


public abstract class AbstractAnalysisEnvironment implements IIRAnalysisEnvironment {
	@Override
  public IAnalysisMonitor getMonitor() {
		return null;
	}

	@Override
  public OutputStream makeResultStream(CUDrop cud) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Override
  public void closeResultStream() throws IOException {
		// Nothing to do yet
	}
	
	@Override
  public void done() {
		// Nothing to do yet
	}
}
