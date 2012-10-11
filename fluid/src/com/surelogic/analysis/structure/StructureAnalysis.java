/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.structure;

import java.util.*;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.Unused;
import com.surelogic.annotation.rules.StructureRules;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.MustInvokeOnOverridePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.TreeWalkVisitor;
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
	final Map<IRNode,MustInvokeOnOverridePromiseDrop> unchecked = new HashMap<IRNode, MustInvokeOnOverridePromiseDrop>();
	
	private static Integer numChildren(IRNode params) {
		return IntegerTable.newInteger(JJNode.tree.numChildren(params));
	}
	
	@Override
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		// Precompute the set of methods that could override
		preFilter = new MultiHashMap<String, Integer>();
		for(final MustInvokeOnOverridePromiseDrop d : 
			Sea.getDefault().getDropsOfType(MustInvokeOnOverridePromiseDrop.class)) {
			final IRNode method = d.getPromisedFor();
			final IRNode params = MethodDeclaration.getParams(method);
			preFilter.put(JJNode.getInfoOrNull(method), numChildren(params));
			unchecked.put(method, d);
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
						synchronized (unchecked) {
							unchecked.remove(parent.getNode());
						}						
						// Check if it really invokes the parent						
						final IRNode call = getAnalysis().findSuperCall(n, parent);
						final ResultDrop rd;
						if (call != null) {
							rd = new ResultDrop(call);
							rd.setMessage(801, JavaNames.genRelativeFunctionName(n));
							rd.setConsistent();
						} else {
							rd = new ResultDrop(n);
							rd.setMessage(802, JavaNames.genRelativeFunctionName(n));
							rd.setInconsistent();
						}
						rd.addChecked(StructureRules.getMustInvokeDrop(parent.getNode()));	
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
	
	@Override
	public void finish(IIRAnalysisEnvironment env) {
		for(MustInvokeOnOverridePromiseDrop d : unchecked.values()) {
			ResultDrop rd = new ResultDrop(d.getPromisedFor());
			rd.setMessage(800);
			rd.addChecked(d);
			rd.setConsistent();
		}
	}
	
	class PerThreadInfo extends TreeWalkVisitor<IRNode> implements IBinderClient {
		final IBinder b;
		IBinding parentToMatch;
		
		PerThreadInfo(IBinder binder) {
			b = binder;
		}

		public IRNode findSuperCall(IRNode n, IBinding parent) {
			parentToMatch = parent;
			return doAccept(n);
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
		protected IRNode mergeResults(List<IRNode> results) {
			for(IRNode b : results) {
				if (b != null) {
					return b;
				}
			}
			return null;
		}
	
		@Override
		public IRNode visitMethodCall(IRNode node) {
			IBinding b =  this.b.getIBinding(node);
			if (b.equals(parentToMatch)) {
				return node;
			}
			if (b.getNode() == parentToMatch.getNode()) {
				return node;
			}
			return null;
		}
	}
}
