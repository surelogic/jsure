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
import edu.cmu.cs.fluid.util.FilterIterator;

public final class LayersAnalysis extends AbstractWholeIRAnalysis<LayersAnalysis.LayersInfo,Void> {
	public static final Category DSC_LAYERS_ISSUES = Category.getInstance("Static structure");
	
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
		//System.out.println("Analyzing layers for: "+cud.javaOSFileName);
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
				if (b.getNode() == null) {
					System.out.println("No binding for "+DebugUnparser.toString(n));
					continue;
				}
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
				// TODO fix to get this from the method
				final AllowsReferencesFromPromiseDrop allows = getAnalysis().allowRefs(b.getNode());
				final ResultDrop rd = checkBinding(allows, b, type, n);
				if (allows != null && rd == null) {					
					ResultDrop success = createSuccessDrop(type, allows);
					success.setResultMessage(352, JavaNames.getRelativeTypeName(type));
				}
				final ResultDrop rd2 = checkBinding(mayReferTo, b, bindT, n);
				if (rd2 != null) {
					problemWithMayReferTo = true;
				}

				ResultDrop rd3 = null;
				if (inLayer != null) {
					for(final LayerPromiseDrop layer : getAnalysis().findLayers(inLayer)) {
						// Check if in the same layer
						boolean inSameLayer = false;					
						if (bindT != null) {
							final InLayerPromiseDrop bindInLayer = LayerRules.getInLayerDrop(bindT);
							if (bindInLayer != null) {
								for(final LayerPromiseDrop bindLayer : getAnalysis().findLayers(bindInLayer)) {
									if (layer == bindLayer) {
										inSameLayer = true;
										break;
									}
								}
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
		}
		if (!problemWithInLayer) {
			ResultDrop rd = createSuccessDrop(type, inLayer);	
			rd.setResultMessage(351, JavaNames.getRelativeTypeName(type));
		}	
		if (!problemWithMayReferTo) {
			ResultDrop rd = createSuccessDrop(type, mayReferTo);
			rd.setResultMessage(351, JavaNames.getRelativeTypeName(type));
		}	
		return true;
	}
	
	private static ResultDrop createSuccessDrop(IRNode type, PromiseDrop<?> checked) {
		ResultDrop rd = new ResultDrop("Layers -- no errors");
		rd.setCategory(DSC_LAYERS_ISSUES);
		rd.setNodeAndCompilationUnitDependency(type);			
		rd.addCheckedPromise(checked);
		rd.setConsistent();
		return rd;
	}
	
	private ResultDrop checkBinding(AbstractReferenceCheckDrop<?> d, IBinding b, IRNode type, IRNode context) {
		if (d != null) {
			if (!d.check(type)) {
				final IRNode contextType = VisitUtil.getClosestType(context);
				/*
				System.out.println("Found bad ref in "+JavaNames.getFullTypeName(contextType));
				d.check(type);
				*/
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

		public Iterable<LayerPromiseDrop> findLayers(final InLayerPromiseDrop inLayer) {
			return new FilterIterator<String,LayerPromiseDrop>(inLayer.getAST().getLayers().getNames().iterator()) {
				@Override
				protected Object select(String qname) {
					// TODO qualify the qname?
					return layers.get(qname);
				}				
			};
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
