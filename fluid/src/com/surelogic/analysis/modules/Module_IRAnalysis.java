/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.modules;

import com.surelogic.analysis.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public class Module_IRAnalysis extends AbstractWholeIRAnalysis<ModuleAnalysisAndVisitor, Unused> {
	public  Module_IRAnalysis() {
		super("Module");
	}

	@Override
	protected void clearCaches() {
		// TODO Auto-generated method stub
	}

	@Override
	public void init(IIRAnalysisEnvironment env) {
		super.init(env);
		ModuleAnalysisAndVisitor.getInstance().maStart(null);//resultDependUpon);
	}
	
	@Override
	protected ModuleAnalysisAndVisitor constructIRAnalysis(IBinder binder) {
		// Setup some fluid analysis stuff (Check that this is correct)
		final ITypeEnvironment tEnv = binder.getTypeEnvironment();

		// Hashtable options = JavaCore.getOptions();
	    /*
	    Map options = getJavaProject().getOptions(true);
	    String compiler   = (String) options.get(JavaCore.COMPILER_COMPLIANCE);
	    if (!compiler.equals("1.5")) {
	    */
	    if (tEnv.getMajorJavaVersion() < 5) {
	      reportProblem("@module declarations will not be found, since compiler is set to 1."+
	    		        tEnv.getMajorJavaVersion()+", instead of 1.5");
	    }	    
		// Init the drop that all Module assurance results link to
	    /*
		if (resultDependUpon != null) {
			resultDependUpon.invalidate();
			resultDependUpon = new ResultsDepDrop();
		} else {
			resultDependUpon = new ResultsDepDrop();
		}
		*/

		// runInVersion(new AbstractRunner() {
		//
		// public void run() {
		return ModuleAnalysisAndVisitor.getInstance();
	}

	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
		ModuleAnalysisAndVisitor.getInstance().doOneCU(cu, getBinder());
		return true;
	}

	@Override
	public void finish(IIRAnalysisEnvironment env) {
		ModuleAnalysisAndVisitor.getInstance().maEnd();
		super.finish(env);
	}
}
