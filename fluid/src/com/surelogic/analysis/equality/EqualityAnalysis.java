/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.equality;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.analysis.type.constraints.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.type.constraints.ValueObjectAnnotationTester;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.BinopExpression;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;

public final class EqualityAnalysis extends AbstractWholeIRAnalysis<EqualityAnalysis.PerThreadInfo,Unused> {
	public EqualityAnalysis() {
		super("Equality");
	}
	
	@Override
	protected boolean flushAnalysis() {
		return true;
	}
	
	@Override
	protected void clearCaches() {
		// Nothing to do, because of flushAnalysis
	}
	
	@Override
	protected PerThreadInfo constructIRAnalysis(IBinder binder) {
		return new PerThreadInfo(binder);
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
		//System.out.println("Analyzing equality for: "+cud.javaOSFileName);
		getAnalysis().doAccept(cu);
		return true;
	}
	
	@Override
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		finishBuild();
		return super.analyzeEnd(env, p);
	}
	
	private ResultDrop createFailureDrop(IRNode n) {
		ResultDrop rd = new ResultDrop(n);
		rd.setCategorizingString(Messages.DSC_LAYERS_ISSUES);
		rd.setInconsistent();
		return rd;
	}
	
	class PerThreadInfo extends VoidTreeWalkVisitor implements IBinderClient {
		final IBinder b;
		
		PerThreadInfo(IBinder binder) {
			b = binder;
		}

//		@Override
		public IBinder getBinder() {
			return b;
		}

//		@Override
		public void clearCaches() {
			// Nothing to do yet
		}
		
		@Override
		public Void visitEqualityExpression(IRNode node) {
			final IRNode e1 = BinopExpression.getOp1(node);
			checkIfValueObject(e1);
			
			final IRNode e2 = BinopExpression.getOp2(node);
			checkIfValueObject(e2);
			return null;
		}
		
		void checkIfValueObject(IRNode e) {
			IJavaType t = b.getJavaType(e);
			final ValueObjectAnnotationTester tester = 
			    new ValueObjectAnnotationTester(b, AnnotationBoundsTypeFormalEnv.INSTANCE, false);
			
			if (tester.testType(t)) {
				ResultDrop d = createFailureDrop(e);
				for (final PromiseDrop<? extends IAASTRootNode> p : tester.getPromises()) {
				  d.addChecked(p);
				}
				d.setMessage(758, DebugUnparser.toString(e));
			}
		}
	}
}
