/**
 * 
 */
package com.surelogic.analysis.jtb;

import java.util.logging.Logger;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.Unused;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.AbstractTypeEnvironment;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaFunctionType;
import edu.cmu.cs.fluid.java.bind.SingleMethodGroupSignatures;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;

/**
 * @author boyland
 *
 */
public class TestFunctionalInterfacePseudoAnalysis extends AbstractWholeIRAnalysis<TestFunctionalInterfacePseudoAnalysis.Visitor,Unused> {

	private static Logger LOG = SLLogger.getLogger("com.surelogic.test");
	
	/**
	 * @param inParallel
	 * @param type
	 */
	public TestFunctionalInterfacePseudoAnalysis() {
		super("TestFunctionalInterface");
		System.out.println("Started the test-is-functional pseudo-analysis");
		LOG.warning("Test analysis is being run.  Please excuse the mess.");
	}



	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
		runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
			@Override
			public void run() {
				runOverFile(compUnit);
			}
		});
		return true;
	}

	// synchronized so that log messages for various files are not intermingled.
	// this is a debugging analysis, not a real analysis.
	protected synchronized void runOverFile(final IRNode compUnit) {
		getAnalysis().doAccept(compUnit);
	}	

	public static class Visitor extends VoidTreeWalkVisitor implements IBinderClient {
	    private final IBinder binder;
	    
	    public Visitor(final IBinder b) {
	      this.binder = b;
	    }

	    @Override
	    public Void visitInterfaceDeclaration(IRNode idecl) {
	    	System.out.println("******************* THIS WILL NEVER SHOW UP!!!!! ******");
	    	System.err.println("!!!!!!!! THIS NEVER SHOWS UP EITHER !!!!!!!!");
	    	LOG.warning("For interface " + JavaNode.getInfo(idecl));
	    	SingleMethodGroupSignatures sigs = ((AbstractTypeEnvironment)binder.getTypeEnvironment()).getInterfaceSingleMethodSignatures(idecl);
			LOG.warning("  M = " + (sigs == null ? "<too many>" : sigs));
	    	IJavaFunctionType ft = binder.getTypeEnvironment().isFunctionalInterface(idecl);
	    	LOG.warning("  descriptor = " + ((ft == null) ? "<none>" : ft.toSourceText()));
	    	return null;	
	    }
	    
		@Override
		public IBinder getBinder() {
			return binder;
		}

		@Override
		public void clearCaches() {			
		}
		
	}

	@Override
	protected void clearCaches() {	
	}


	@Override
	protected Visitor constructIRAnalysis(IBinder binder) {
		return new Visitor(binder);
	}
}
