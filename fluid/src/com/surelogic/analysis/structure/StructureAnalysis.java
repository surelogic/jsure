/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.structure;

import java.util.Collection;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.annotation.rules.StructureRules;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.MustInvokeOnOverridePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.IntegerTable;

public final class StructureAnalysis extends AbstractWholeIRAnalysis<StructureAnalysis.PerThreadInfo,Unused> {
	public StructureAnalysis() {
		super("Structure");
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
	
	MultiMap<String,Integer> preFilter;
	
	private static Integer numChildren(IRNode params) {
		return IntegerTable.newInteger(JJNode.tree.numChildren(params));
	}
	
	@Override
	public void init(IIRAnalysisEnvironment env) {
		// Precompute the set of methods that could override
		preFilter = new MultiHashMap<String, Integer>();
		for(final MustInvokeOnOverridePromiseDrop d : 
			Sea.getDefault().getDropsOfType(MustInvokeOnOverridePromiseDrop.class)) {
			final IRNode method = d.getPromisedFor();
			final IRNode params = MethodDeclaration.getParams(method);
			preFilter.put(JJNode.getInfoOrNull(method), numChildren(params));
		}
	}
	
	private boolean preFilterMatches(IRNode n) {
		final String name = JJNode.getInfoOrNull(n);
		Collection<Integer> numParams = preFilter.get(name);
		if (numParams != null) {
			final IRNode params = MethodDeclaration.getParams(n);
			if (numParams.contains(numChildren(params))) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
		//System.out.println("Analyzing structure for: "+cud.javaOSFileName);
		if (preFilter.isEmpty()) {
			// Nothing to do here
			return true;
		}
		for(final IRNode n : JJNode.tree.topDown(cu)) {
			final Operator op = JJNode.tree.getOperator(n);
			if (MethodDeclaration.prototype.includes(op)) {
				if (preFilterMatches(n)) {
					IBinding parent = StructureRules.findParentWithMustInvokeOnOverride(getAnalysis().getBinder(), n);					
					if (parent != null) {
						ResultDrop rd = new ResultDrop(n);
						rd.setCategorizingMessage(Messages.DSC_LAYERS_ISSUES);
						
						// Check if it really invokes the parent
						final boolean found = getAnalysis().doAccept(cu);
						rd.setConsistent(found);		
					}
				}
			}
		}
		return true;
	}
	
	@Override
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		finishBuild();
		return super.analyzeEnd(env, p);
	}
	
	class PerThreadInfo extends Visitor<Boolean> implements IBinderClient {
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
		public Boolean visitEqualityExpression(IRNode node) {
			return null;
		}
	}
}
