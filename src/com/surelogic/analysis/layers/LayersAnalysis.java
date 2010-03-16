/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.layers;

import java.util.*;

import com.surelogic.analysis.*;
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
import edu.cmu.cs.fluid.tree.Operator;

public final class LayersAnalysis extends AbstractWholeIRAnalysis<LayersAnalysis.LayersInfo,Void> {
	public static final Category DSC_LAYERS_ISSUES = Category.getInstance("Layers");
	
	public LayersAnalysis() {
		super("Layers");
	}

	public void init(IIRAnalysisEnvironment env) {
		// Nothing to do?		
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
	protected LayersInfo constructIRAnalysis(IBinder binder) {
		return new LayersInfo(binder).init();
	}
 	
	@Override
	protected boolean doAnalysisOnAFile(CUDrop cud, IRNode cu, IAnalysisMonitor monitor) {	
		final IRNode type = VisitUtil.getPrimaryType(cu);
		final InLayerPromiseDrop inLayer = LayerRules.getInLayerDrop(type);
		final MayReferToPromiseDrop mayReferTo = LayerRules.getMayReferToDrop(type);	

		// No way to shortcircuit, due to possible AllowsReferencesTo
		boolean problemWithInLayer = inLayer == null;
		boolean problemWithMayReferTo = mayReferTo == null;
		for(IRNode n : JJNode.tree.topDown(cu)) {
			final Operator op = JJNode.tree.getOperator(n);
			if (op instanceof IHasBinding && 
				!(PackageDeclaration.prototype.includes(op) || ImportName.prototype.includes(op))) {
				final IBinding b    = getAnalysis().getBinder().getIBinding(n);
				final IRNode bindCu = VisitUtil.getEnclosingCompilationUnit(b.getNode());
				if (cu.equals(bindCu)) {
					// You can always refer to yourself
					continue;
				}
				if ("java.lang".equals(VisitUtil.getPackageName(bindCu))) {
					continue; // Always refer to java.lang?
				}
				IRNode bindT  = VisitUtil.getPrimaryType(bindCu);
				if (bindT == null) {
					bindT = CompilationUnit.getPkg(bindCu);
				}
				
				final AllowsReferencesFromPromiseDrop allows = getAnalysis().allowRefs(bindT);
				final ResultDrop rd = checkBinding(allows, b, type, n);
				final ResultDrop rd2 = checkBinding(mayReferTo, b, bindT, n);
				if (rd2 != null) {
					problemWithMayReferTo = true;
				}

				ResultDrop rd3 = null;
				if (inLayer != null) {
					final LayerPromiseDrop layer = getAnalysis().findLayer(inLayer);
					// Check if in the same layer
					boolean inSameLayer = false;
					if (bindT != null) {
						final InLayerPromiseDrop bindInLayer = LayerRules.getInLayerDrop(bindT);
						if (bindInLayer != null) {
							inSameLayer = layer == getAnalysis().findLayer(bindInLayer);
						}
					}
					if (!inSameLayer) {
						rd3 = checkBinding(layer, b, bindT, n);
						if (rd3 != null) {
							rd3.addCheckedPromise(inLayer);
							problemWithInLayer = true;
						}
					}
				}
			}
		}
		if (!problemWithInLayer) {
			ResultDrop rd = new ResultDrop("Layers -- no errors");
			rd.setCategory(DSC_LAYERS_ISSUES);
			rd.setNodeAndCompilationUnitDependency(type);			
			rd.setResultMessage(351, JavaNames.getRelativeTypeName(type));
			rd.addCheckedPromise(inLayer);
			rd.setConsistent();
		}	
		if (!problemWithMayReferTo) {
			ResultDrop rd = new ResultDrop("Layers -- no errors");
			rd.setCategory(DSC_LAYERS_ISSUES);
			rd.setNodeAndCompilationUnitDependency(type);			
			rd.setResultMessage(351, JavaNames.getRelativeTypeName(type));
			rd.addCheckedPromise(mayReferTo);
			rd.setConsistent();
		}	
		return true;
	}
	
	private ResultDrop checkBinding(AbstractReferenceCheckDrop<?> d, IBinding b, IRNode type, IRNode context) {
		if (d != null) {
			if (!d.check(type)) {
				//d.check(type);
				
				// Create error
				ResultDrop rd = new ResultDrop("Layers");
				rd.setNodeAndCompilationUnitDependency(context);				
				rd.setResultMessage(d.getResultMessageKind(), 
						            unparseArgs(d.getArgs(b.getNode(), type, context)));
				rd.setCategory(DSC_LAYERS_ISSUES);
				/*
				if (rd.getMessage().contains("Null")) {
					System.out.println("Found "+rd.getMessage());
				}
				*/
				if (!(d instanceof LayerPromiseDrop)) {
					rd.addCheckedPromise(d);
				}
				return rd;
			}
		}
		return null;
	}
	
	private Object[] unparseArgs(Object[] args) {
		for(int i=0; i<args.length; i++) {
			args[i] = JavaNames.getFullName((IRNode) args[i]);
		}
		return args;
	}
	
	static class LayersInfo implements IBinderClient{
		final IBinder binder;
		final Map<String,TypeSetPromiseDrop> typesets = new HashMap<String, TypeSetPromiseDrop>();
		final Map<String,LayerPromiseDrop> layers = new HashMap<String, LayerPromiseDrop>();
		final Map<IRNode,AllowsReferencesFromPromiseDrop> allowRefs = 
			new HashMap<IRNode, AllowsReferencesFromPromiseDrop>();
		
		public LayersInfo(IBinder b) {
			binder = b;
		}

		public LayerPromiseDrop findLayer(InLayerPromiseDrop inLayer) {
			final String qname = inLayer.getAST().getLayer();
			// TODO qualify the qname?
			return layers.get(qname);
		}

		public AllowsReferencesFromPromiseDrop allowRefs(IRNode type) {
			return allowRefs.get(type);
		}

		public void clearCaches() {
			// Nothing to do, because of flushAnalysis			
		}

		public IBinder getBinder() {
			return binder;
		}		
		
		LayersInfo init() {
			for(final PackageDrop p : PackageDrop.allPackages()) {
				final IRNode pkg = CompilationUnit.getPkg(p.cu);
				if (UnnamedPackageDeclaration.prototype.includes(pkg)) {
					continue;
				}
				final String pkgName = NamedPackageDeclaration.getId(pkg);
				for(TypeSetPromiseDrop typeset : LayerRules.getTypeSets(pkg)) {
					typesets.put(pkgName+'.'+typeset.getId(), typeset);
				}
				for(LayerPromiseDrop layer : LayerRules.getLayers(pkg)) {
					layers.put(pkgName+'.'+layer.getId(), layer);
				}
			}
			for(AllowsReferencesFromPromiseDrop a : Sea.getDefault().getDropsOfExactType(AllowsReferencesFromPromiseDrop.class)) {
				final IRNode type = a.getNode();
				allowRefs.put(type, a);
			}
			return this;
		}
	}
}
