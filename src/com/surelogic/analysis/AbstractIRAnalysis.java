/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/AbstractIRAnalysis.java,v 1.4 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis;

import java.util.concurrent.atomic.AtomicReference;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.AbstractRunner;

public abstract class AbstractIRAnalysis<T> implements IIRAnalysis {
	@SuppressWarnings("unchecked")
	protected final T nullAnalysis = (T) new Object();
	private IIRProject project;
	private IBinder binder;
	private final AtomicReference<T> analysis = new AtomicReference<T>();
	
	protected IBinder getBinder() {
		return binder;
	}
	
	protected T getAnalysis() {
		return analysis.get();
	}
	
	protected boolean flushAnalysis() {
		return false;
	}
	
	public String name() {
		return getClass().getSimpleName();
	}
	
	public final void analyzeBegin(IIRProject p) {
		final ITypeEnvironment tEnv = IDE.getInstance().getTypeEnv(p);
		final IBinder binder        = tEnv.getBinder(); 
		final IIRProject old        = project;
		project = p;		
		this.binder = binder;
		startAnalyzeBegin(p, binder);
		
		if (flushAnalysis() || old != p || analysis.get() == null) {			
			setupAnalysis();
		}
		finishAnalyzeBegin(p, binder);
	}
	
	private final void setupAnalysis() {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				T a = constructIRAnalysis(binder);
				analysis.set(a);
			}
		});
	}
	
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do yet
	}
	
	@SuppressWarnings("unchecked")
	protected T constructIRAnalysis(IBinder binder) {
		return nullAnalysis;
	}
	
	protected void finishAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do yet
	}
	
	public final boolean doAnalysisOnAFile(final CUDrop cud, 
			final IAnalysisMonitor monitor) {
		T analysis = getAnalysis();
		if (analysis == null) {
			setupAnalysis();
		}
		Object rv = runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			public void run() {
				result = doAnalysisOnAFile(cud, cud.cu, monitor);
			}
		});
		return rv == Boolean.TRUE;
	}
	protected abstract boolean doAnalysisOnAFile(CUDrop cud, IRNode cu, IAnalysisMonitor monitor);
	
	protected static Object runVersioned(AbstractRunner r) {
		return IDE.runVersioned(r);
	}

	protected static Object runInVersion(AbstractRunner r) {
		return IDE.runAtMarker(r);
	}

	protected static Operator getOperator(final IRNode n) {
		return JJNode.tree.getOperator(n);
	}
}
