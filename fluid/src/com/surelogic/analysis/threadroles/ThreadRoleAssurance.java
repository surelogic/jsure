/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.threadroles;

import com.surelogic.analysis.*;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.util.AbstractRunner;

public class ThreadRoleAssurance extends AbstractWholeIRAnalysis<TRoleSecondPass,CUDrop>{	
	private int cuCount = 0;
	  
	public ThreadRoleAssurance() {
		super("ColorAssurance");
	}

	@Override
	protected void clearCaches() {
		// TODO Auto-generated method stub
	}

	@Override
	protected TRoleSecondPass constructIRAnalysis(IBinder binder) {
		return TRoleSecondPass.getInstance();
	}

	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
		cuCount++;
		return true;
	}

	@Override
	public void finish(IIRAnalysisEnvironment env) {
	    runInVersion(new AbstractRunner() {
	        @Override
          public void run() {
	          LOG.info("Finishing color assurance");
	          TRoleSecondPass.getInstance().cspEnd(getBinder());
	          LOG.info("Color Assurance complete.");
	        }
	      });		
	}
}
