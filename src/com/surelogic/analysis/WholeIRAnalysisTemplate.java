/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/WholeIRAnalysisTemplate.java,v 1.2 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public class WholeIRAnalysisTemplate<T extends IBinderClient> extends AbstractWholeIRAnalysis<T> {
	public WholeIRAnalysisTemplate() {
		super("foo");
	}

	public void init(IIRAnalysisEnvironment env) {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do
	}
	
	@Override
	protected T constructIRAnalysis(IBinder binder) {
		return null;
	}
	
	@Override
	protected void clearCaches() {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected boolean doAnalysisOnAFile(CUDrop cud, IRNode cu, IAnalysisMonitor monitor) {
		// TODO Auto-generated method stub
		return true;
	}
}
