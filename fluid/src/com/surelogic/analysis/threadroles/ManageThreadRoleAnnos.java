/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.logging.Logger;

import com.surelogic.analysis.*;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public class ManageThreadRoleAnnos extends AbstractIRAnalysis<TRolesFirstPass,Unused> {
	private static final Logger LOG = SLLogger
      .getLogger("analysis.threadroles.managethreadroleannos");
	
	public ManageThreadRoleAnnos() {
		super(false, Unused.class);
	}

	public boolean analyzeAll() {
		return false;
	}
	
	@Override
	protected TRolesFirstPass constructIRAnalysis(IBinder binder) {
		return TRolesFirstPass.getInstance();
	}

	public void resetForAFullBuild(IIRProject project) {
		// TODO when called?
	    TRolesFirstPass.getInstance().resetForAFullBuild();
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
	    TRolesFirstPass.getInstance().doOneCU(cu, getBinder());
		return true;
	}

	@Override
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		final Iterable<IRNode> reprocessThese = TRolesFirstPass.getInstance().trfpEnd();
		// TODO move some of this code to finish()?
		return reprocessThese;
	}
}
