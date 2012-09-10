/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.equality;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.concurrency.util.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.concurrency.util.ValueObjectAnnotationTester;
import com.surelogic.analysis.layers.Messages;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;

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
	
	private ResultDropBuilder createFailureDrop(IRNode n) {
		ResultDropBuilder rd = ResultDropBuilder.create(this);
		rd.setCategory(Messages.DSC_LAYERS_ISSUES);
		rd.setNodeAndCompilationUnitDependency(n);	
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
//			ValueObjectPromiseDrop p = checkIfValueObject(e, t);
			final ValueObjectAnnotationTester tester = 
			    new ValueObjectAnnotationTester(b, AnnotationBoundsTypeFormalEnv.INSTANCE, false);
			
			if (tester.testType(t)) {
				ResultDropBuilder d = createFailureDrop(e);
				for (final PromiseDrop<? extends IAASTRootNode> p : tester.getPromises()) {
				  d.addCheckedPromise(p);
				}
				d.setMessage(DebugUnparser.toString(e)+" should not be compared using == or !=");			
			}
		}
		
//		/**
//		 * @return the promise drop if found
//		 */
//		ValueObjectPromiseDrop checkIfValueObject(final IRNode e, IJavaType t) {
//			if (!(t instanceof IJavaReferenceType)) {
//				return null;
//			}
//			if (t instanceof IJavaDeclaredType) {
//				IJavaDeclaredType dt = (IJavaDeclaredType) t;
//				return EqualityRules.getValueObjectDrop(dt.getDeclaration());								
//			}
//			ValueObjectPromiseDrop rv = null;
//			for(IJavaType st : t.getSupertypes(b.getTypeEnvironment())) {
//				rv = checkIfValueObject(e, st);
//				if (rv != null) {
//					return rv;
//				}
//			}
//			return rv;
//		}
	}
}
