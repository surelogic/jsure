/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.equality;

import java.util.*;

import com.surelogic.analysis.*;
import com.surelogic.analysis.layers.CycleDetector;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.annotation.rules.EqualityRules;
import com.surelogic.annotation.rules.LayerRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.sea.drops.layers.*;
import edu.cmu.cs.fluid.sea.drops.promises.ValueObjectPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.FilterIterator;
import edu.cmu.cs.fluid.util.Pair;

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
		ResultDropBuilder rd = ResultDropBuilder.create(this, "Equality");
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

		@Override
		public IBinder getBinder() {
			return b;
		}

		@Override
		public void clearCaches() {
			// Nothing to do yet
		}
		
		@Override
		public Void visitEqualityExpression(IRNode node) {
			final IRNode e1 = EqualityExpression.getOp1(node);
			checkIfValueObject(e1);
			
			final IRNode e2 = EqualityExpression.getOp2(node);
			checkIfValueObject(e2);
			return null;
		}
		
		void checkIfValueObject(IRNode e) {
			IJavaType t = b.getJavaType(e);
			ValueObjectPromiseDrop p = checkIfValueObject(e, t);
			if (p != null) {
				ResultDropBuilder d = createFailureDrop(e);
				d.addCheckedPromise(p);
				d.setMessage(DebugUnparser.toString(e)+" should not be compared using == or !=");			
			}
		}
		
		/**
		 * @return the promise drop if found
		 */
		ValueObjectPromiseDrop checkIfValueObject(final IRNode e, IJavaType t) {
			if (!(t instanceof IJavaReferenceType)) {
				return null;
			}
			if (t instanceof IJavaDeclaredType) {
				IJavaDeclaredType dt = (IJavaDeclaredType) t;
				return EqualityRules.getValueObjectDrop(dt.getDeclaration());								
			}
			ValueObjectPromiseDrop rv = null;
			for(IJavaType st : t.getSupertypes(b.getTypeEnvironment())) {
				rv = checkIfValueObject(e, st);
				if (rv != null) {
					return rv;
				}
			}
			return rv;
		}
	}
}
