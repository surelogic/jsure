/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.logging.Logger;

import com.surelogic.analysis.*;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public class ManageThreadRoleAnnos extends AbstractIRAnalysis<TRolesFirstPass,Void> {
	private static final Logger LOG = SLLogger
      .getLogger("analysis.threadroles.managethreadroleannos");
	
	public ManageThreadRoleAnnos() {
		super(false, Void.class);
	}

	public boolean analyzeAll() {
		return false;
	}
	
	public void init(IIRAnalysisEnvironment env) {
		// Nothing to do
	}

	public void preAnalysis(IIRAnalysisEnvironment env, IIRProject p) {
		// Nothing to do	
	}
	
	@Override
	protected TRolesFirstPass constructIRAnalysis(IBinder binder) {
	    TRolesFirstPass.getInstance().trfpStart(binder);
		return TRolesFirstPass.getInstance();
	}

	public void resetForAFullBuild(IIRProject project) {
		// TODO when called?
	    TRolesFirstPass.getInstance().resetForAFullBuild();
	}
	
	@Override
	protected boolean doAnalysisOnAFile(CUDrop cud, IRNode cu,IAnalysisMonitor monitor) {	
	    TRolesFirstPass.getInstance().doOneCU(cu, getBinder());
		return true;
	}

	public Iterable<IRNode> analyzeEnd(IIRProject p) {
		final Iterable<IRNode> reprocessThese = TRolesFirstPass.getInstance().trfpEnd();
		// TODO move some of this code to finish()?
		return reprocessThese;
	}

	public void postAnalysis(IIRProject p) {
		// Nothing to do	
	}
	
	public void finish(IIRAnalysisEnvironment env) {
		// TODO something needed here?
	}
}
