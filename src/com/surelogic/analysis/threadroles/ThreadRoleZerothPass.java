/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.logging.Logger;

import com.surelogic.analysis.*;
import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public class ThreadRoleZerothPass extends AbstractIRAnalysis<TRolesFirstPass,Void> {
	private static final Logger LOG = SLLogger
      .getLogger("analysis.threadroles.managethreadroleannos");
	
	public ThreadRoleZerothPass() {
		super(false, Void.class);
	}

	public boolean analyzeAll() {
		return false;
	}
	
	public void init(IIRAnalysisEnvironment env) {
		TRolesFirstPass.preBuild();
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
		// Called by the latter
		// TRolesFirstPass.getInstance().doImportandRenameWalks(cu, getBinder());		
	    TRolesFirstPass.getInstance().doOneCUZerothPass(cu, getBinder());
		return true;
	}

	public IRNode[] analyzeEnd(IIRProject p) {
		return JavaGlobals.noNodes;
	}

	public void postAnalysis(IIRProject p) {
		// Nothing to do	
	}
}
