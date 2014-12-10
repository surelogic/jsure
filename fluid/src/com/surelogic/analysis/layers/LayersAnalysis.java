/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.analysis.layers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.annotation.rules.LayerRules;
import com.surelogic.common.Pair;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.PackageDrop;
import com.surelogic.dropsea.ir.drops.layers.AbstractReferenceCheckDrop;
import com.surelogic.dropsea.ir.drops.layers.AllowsReferencesFromPromiseDrop;
import com.surelogic.dropsea.ir.drops.layers.InLayerPromiseDrop;
import com.surelogic.dropsea.ir.drops.layers.LayerPromiseDrop;
import com.surelogic.dropsea.ir.drops.layers.MayReferToPromiseDrop;
import com.surelogic.dropsea.ir.drops.layers.TypeSetPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IHasBinding;
import edu.cmu.cs.fluid.java.operator.ClassExpression;
import edu.cmu.cs.fluid.java.operator.CompilationUnit;
import edu.cmu.cs.fluid.java.operator.NamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.UnnamedPackageDeclaration;
import edu.cmu.cs.fluid.java.operator.Visitor;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public final class LayersAnalysis extends AbstractWholeIRAnalysis<LayersAnalysis.LayersInfo,CUDrop> {
	public LayersAnalysis() {
		super("Layers");
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
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode cu) {
		//System.out.println("Analyzing layers for: "+cud.javaOSFileName);
		for(IRNode type : VisitUtil.getTypeDecls(cu)) {
			analyzeType(cu, type);
		}
		return true;
	}

	@Override
	public void finish(IIRAnalysisEnvironment env) {
		super.finish(env);
		
		for (final PackageDrop p : PackageDrop.getKnownPackageDrops()) {
			final IRNode pkg = CompilationUnit.getPkg(p.getCompilationUnitIRNode());
			for (LayerPromiseDrop layer : LayerRules.getLayers(pkg)) {
				if (!layer.hasDependents()) {
					ResultDrop r = createSuccessDrop(pkg, layer);
					r.setMessage(359);
				}
			}
		}
	}
	
	/*
	@Override
	public IAnalysisGranulator<TopLevelType> getGranulator() {
		return TopLevelType.granulator;
	}	
	
	@Override
	protected boolean doAnalysisOnGranule_wrapped(IIRAnalysisEnvironment env, TopLevelType n) {
		analyzeType(n.getCompUnit(), n.typeDecl);
		return true; 
	}
	*/
	static class MyResults {
		List<ResultDrop> resultsForMayReferTo = Collections.emptyList();
		List<ResultDrop> resultsForInLayer = Collections.emptyList();
	}
	
	class MyVisitor extends Visitor<MyResults> {
		final IRNode cu, type;
		final InLayerPromiseDrop inLayer;
		final MayReferToPromiseDrop mayReferTo;
		
		MyVisitor(final IRNode cu, final IRNode type) {
			this.cu = cu;
			this.type = type;
			inLayer = LayerRules.getInLayerDrop(type);
			mayReferTo = LayerRules.getMayReferToDrop(type);
		}
		
		void startVisit(IRNode n) {
			MyResults rv = visit(n);			
			addChecked(rv);
		}

		private void addChecked(MyResults r) {
			for(ResultDrop d : r.resultsForInLayer) {
				d.addChecked(inLayer);
			}			
			for(ResultDrop d : r.resultsForMayReferTo) {
				d.addChecked(mayReferTo);
			}	
		}	
		
		@Override
		public final MyResults visit(IRNode node) {
			// Stop merging and add checked promise			
			List<MyResults> results = doAcceptForChildrenWithResults(node);
			for(MyResults l : results) {
				addChecked(l);
			}
			return new MyResults();
		}
		
		@Override
		public final MyResults visitExpression(IRNode node) {
			return mergeVisit(node);
		}
		
		@Override
		public final MyResults visitType(IRNode node) {
			return mergeVisit(node);
		}
		
		@Override
		public final MyResults visitTypeActuals(IRNode node) {
			return mergeVisit(node);
		}
		
		@Override
		public final MyResults visitParameterizedType(IRNode node) {
			// No need to visit the base type, since this will complain too
			MyResults args = doAccept(ParameterizedType.getArgs(node));
			return combine(node, args);
		}
		
		private MyResults mergeVisit(IRNode node) {
			List<MyResults> results = doAcceptForChildrenWithResults(node);
			MyResults rv = mergeResults(results);
			return combine(node, rv);
		}
		
		private MyResults combine(IRNode node, MyResults fromChildren) {
			MyResults rv = fromChildren;
			if (rv == null) {
				rv = new MyResults();
			}
		    final Pair<ResultDrop,ResultDrop> rd = visitNode(node);
		    if (rd == null) {
				return rv;
			}
			rv.resultsForMayReferTo = combineResults(rv.resultsForMayReferTo, rd.first());
			rv.resultsForInLayer = combineResults(rv.resultsForInLayer, rd.second());
			return rv;
		}
		
		private MyResults mergeResults(List<MyResults> results) {
			MyResults rv = null;
			// TODO inefficient to merge results first?
			for(MyResults r : results) {
				if (rv == null) {
					rv = r;
				} else {
					rv = mergeResults(rv, r);
				}
			}
			return rv;
		}
		
		private MyResults mergeResults(MyResults r1, MyResults r2) {
			r1.resultsForMayReferTo = mergeResults(r1.resultsForMayReferTo, r2.resultsForMayReferTo);
			r1.resultsForInLayer = mergeResults(r1.resultsForInLayer, r2.resultsForInLayer);
			return r1;
		}

		private List<ResultDrop> mergeResults(List<ResultDrop> r1, List<ResultDrop> r2) {
			switch (r1.size()) {
			case 0:
				return r2;
			case 1:
				switch (r2.size()) {
				case 0: 
					return r1;
				case 1:
					List<ResultDrop> rv = new ArrayList<ResultDrop>(2);
					rv.addAll(r1);
					rv.addAll(r2);
					return rv;
				default:
					r2.addAll(r1);
					return r2;
				}
			default:
				if (!r2.isEmpty()) {
					r1.addAll(r2);					
				}
				return r1;
			}
		}
		
		private List<ResultDrop> combineResults(List<ResultDrop> results, ResultDrop rd) {
			if (rd != null) {				
				for(ResultDrop d : results) {
					rd.addTrusted(d);
				}
				return Collections.singletonList(rd);
			} else {
				return results;
			}
		}
	
		private Pair<ResultDrop,ResultDrop> visitNode(IRNode n) {
			final Operator op = JJNode.tree.getOperator(n);
			if (op instanceof IHasBinding
				/*	&& 
				!(PackageDeclaration.prototype.includes(op) || ImportName.prototype.includes(op))*/) {
				final IBinding b    = getAnalysis().getBinder().getIBinding(n);
				if (b == null || b.getNode() == null) {
					if (!ClassExpression.prototype.includes(n)) {
						System.out.println("No binding for "+DebugUnparser.toString(n));
					}
					return null;
				}
				final IRNode bindCu = VisitUtil.findCompilationUnit(b.getNode());
				if (cu.equals(bindCu)) {
					// You can always refer to yourself
					return null;
				}
				if ("java.lang".equals(VisitUtil.getPackageName(bindCu))) {
					// Always refer to java.lang
					return null;
				}
				IRNode bindT  = VisitUtil.getPrimaryType(bindCu);
				if (bindT == null) {
					bindT = CompilationUnit.getPkg(bindCu);
				}
				// TODO fix to get this from the method
				final AllowsReferencesFromPromiseDrop allows = getAnalysis().allowRefs(b.getNode());
				final ResultDrop rd = checkBinding(allows, b, type, n);
				final ResultDrop rd2 = checkBinding(mayReferTo, b, bindT, n);

				ResultDrop rd3 = null;
				if (inLayer != null) {
					for(final LayerPromiseDrop layer : getAnalysis().findLayers(inLayer)) {
						// Check if in the same layer
						boolean inSameLayer = inSameLayer(bindT, layer);
						if (!inSameLayer) {							
							rd3 = checkBinding(layer, b, bindT, n);
						}
					}
				}
				if (rd2 != null || rd3 != null) {
					return new Pair<ResultDrop,ResultDrop>(rd2, rd3);					
				}			
			}
			return null;
		}
		
		private ResultDrop checkBinding(AbstractReferenceCheckDrop<?> d, IBinding b, IRNode type, IRNode context) {
			if (d != null) {
				if (!d.check(type)) {
					/*
					final IRNode contextType = VisitUtil.getClosestType(context);

					System.out.println("Found bad ref in "+JavaNames.getFullTypeName(contextType));
					System.out.println("type = "+DebugUnparser.toString(type));
					System.out.println("context = "+DebugUnparser.toString(context));
					//inSameLayer(type, (LayerPromiseDrop) d);
					d.check(type);
					*/
					// Create error
					ResultDrop rd = createFailureDrop(context);			
					rd.setMessage(d.getResultMessageKind(), 
							            unparseArgs(d.getArgs(b.getNode(), type, context)));
					/*
					if (rd.getMessage().contains("Null")) {
						System.out.println("Found "+rd.getMessage());
					}
					*/
					if (d instanceof AllowsReferencesFromPromiseDrop) {
						rd.addChecked(d);
					}
					return rd;
				} else {
					ResultDrop success = createSuccessDrop(context, d instanceof AllowsReferencesFromPromiseDrop ? d : null);
					success.setMessage(Messages.PERMITTED_REFERENCE_TO, JavaNames.getFullName(b.getNode()));
					return success;
				}
			}
			return null;
		}
	}
	
	private void analyzeType(final IRNode cu, final IRNode type) {
		//System.out.println("Looking at "+JavaNames.getRelativeTypeName(type));
		MyVisitor v = new MyVisitor(cu, type);
		v.startVisit(type);	
	}

	boolean inSameLayer(IRNode bindT, final LayerPromiseDrop layer) {
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
		return inSameLayer;
	}
	
	ResultDrop createSuccessDrop(IRNode context, PromiseDrop<?> checked) {
		ResultDrop rd = new ResultDrop(context);
		if (checked != null) {
			rd.addChecked(checked);
		}
		rd.setConsistent();
		return rd;
	}
	
	ResultDrop createFailureDrop(IRNode context) {
		ResultDrop rd = new ResultDrop(context);
		rd.setInconsistent();
		return rd;
	}
		
	Object[] unparseArgs(Object[] args) {
		for(int i=0; i<args.length; i++) {
			args[i] = JavaNames.getFullName((IRNode) args[i]);
		}
		return args;
	}
	
	/**
	 * Used to check if types referred to by layers break a layering constraint
	 */
	// TODO potentially slow, because of checking multiple types in layers
	@Override
	public Iterable<CUDrop> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		final Map<String,List<IRNode>> layers = new HashMap<String, List<IRNode>>();
		final Map<String, Set<String>> layerRefs = new HashMap<String, Set<String>>();
		final CycleDetector detector = new CycleDetector() {
			private static final long serialVersionUID = 1L;
			
			final Set<Pair<String,String>> reported = new HashSet<Pair<String,String>>();
			
			@Override
			protected void reportFailure(String backedge, String last) {				
				final Set<String> origRefs = layerRefs.get(last);
				if (origRefs != null && origRefs.contains(backedge)) {
					// Ignore if it's an original layer reference
					return; 
				}
				final Pair<String,String> p = new Pair<String,String>(backedge, last);
				if (reported.contains(p)) {
					// Already reported
					return;
				}
				reported.add(p);
				
				LayerPromiseDrop layer = getAnalysis().getLayer(last);
				ResultDrop rd = createFailureDrop(layer.getNode());
				rd.addChecked(layer);				
				rd.setMessage(Messages.CYCLE, backedge); 
				
				final Map<String,TypeSetPromiseDrop> involved = new HashMap<String, TypeSetPromiseDrop>();
				for(IRNode type : layers.get(backedge)) {
					rd.addInformationHint(type, Messages.TYPE_INVOLVED, JavaNames.getFullTypeName(type));
					for(Map.Entry<String,TypeSetPromiseDrop> e : getAnalysis().getTypesets()) {
						if (e.getValue().check(type)) {
							involved.put(e.getKey(), e.getValue());
						}
					}
				}
				for(Map.Entry<String,TypeSetPromiseDrop> e : involved.entrySet()) {
					rd.addInformationHint(e.getValue().getNode(), Messages.TYPESET_INVOLVED, e.getKey());
				}
			}
		};
		collectLayerInfo(detector, layerRefs, layers);
		detector.checkAll();
		
		finishBuild();
		return super.analyzeEnd(env, p);
	}

	/**
	 * Includes info from typesets and other explicit package/type references
	 * @param layerRefs 
	 * @param layers 
	 */
	private void collectLayerInfo(final CycleDetector detector, 
			                      Map<String, Set<String>> layerRefs, 
			                      Map<String, List<IRNode>> layers) {		
		// Collect direct layer references
		for(Map.Entry<String, LayerPromiseDrop> e : getAnalysis().getLayers()) { 
			for(LayerPromiseDrop ref : e.getValue().getAAST().getReferencedLayers()) {
				String qname = computePackage(ref.getNode())+'.'+ref.getId();
				detector.addRef(e.getKey(), qname);
			}
		}
		// Make snapshot
		for(Map.Entry<String, Set<String>> e : detector.entrySet()) {
			layerRefs.put(e.getKey(), new HashSet<String>(e.getValue()));
		}
		
		// Collect indirect layer references (e.g. via typesets)
		for(InLayerPromiseDrop pd : Sea.getDefault().getDropsOfExactType(InLayerPromiseDrop.class)) {
			final IRNode type = pd.getNode();
			List<String> inLayers = null; // Initialized if needed
			for(Map.Entry<String, LayerPromiseDrop> e : getAnalysis().getLayers()) { 				
				if (e.getValue().getAAST().check(type)) {
					// Accessible, so add to layerRefs
					if (inLayers == null) {
						inLayers = computeLayerNames(pd);
					}
					detector.addRefs(e.getKey(), inLayers);
				}
			}
			if (inLayers != null) {
				for(String name : inLayers) {
					List<IRNode> types = layers.get(name);
					if (types == null) {
						types = new ArrayList<IRNode>();
						layers.put(name, types);
					}
					types.add(type);
				}
			}
		}
		//System.out.println("Done collecting layers info");
	}
 	
	private static String computePackage(IRNode context) {
		IRNode cu = VisitUtil.getEnclosingCompilationUnit(context);
		return VisitUtil.getPackageName(cu);
	}
	
	private static List<String> computeLayerNames(InLayerPromiseDrop pd) {
		List<String> layers = new ArrayList<String>();
		String pkg = null; // Initialized if needed
		
		for(String name : pd.getAAST().getLayers().getNames()) {						
			if (name.indexOf('.') >= 0) {
				// Already qualified
				layers.add(name);
			} else {
				if (pkg == null) {
					pkg = computePackage(pd.getNode());
				}
				layers.add(pkg+'.'+name);
			}
		}
		return layers;
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

		Iterable<Map.Entry<String,TypeSetPromiseDrop>> getTypesets() {
			return typesets.entrySet();
		}
		
		public LayerPromiseDrop getLayer(String qname) {
			return layers.get(qname);
		}

		public Iterable<LayerPromiseDrop> findLayers(final InLayerPromiseDrop inLayer) {
			return new FilterIterator<String,LayerPromiseDrop>(inLayer.getAAST().getLayers().getNames().iterator()) {
				@Override
				protected Object select(String qname) {
					if (!qname.contains(".")) {
						final IRNode cu = VisitUtil.findCompilationUnit(inLayer.getNode());
						final String pkg = VisitUtil.getPackageName(cu);
						qname = pkg+'.'+qname;
					}
					return layers.get(qname);
				}				
			};
		}

		public AllowsReferencesFromPromiseDrop allowRefs(IRNode type) {
			return allowRefs.get(type);
		}

		@Override
    public void clearCaches() {
			// Nothing to do, because of flushAnalysis			
		}

		@Override
    public IBinder getBinder() {
			return binder;
		}		
		
		Iterable<Map.Entry<String, LayerPromiseDrop>> getLayers() {
			return layers.entrySet();
		}
		
    LayersInfo init() {
      for (final PackageDrop p : PackageDrop.getKnownPackageDrops()) {
        final IRNode pkg = CompilationUnit.getPkg(p.getCompilationUnitIRNode());
        if (UnnamedPackageDeclaration.prototype.includes(pkg)) {
          continue;
        }
        final String pkgName = NamedPackageDeclaration.getId(pkg);
        for (TypeSetPromiseDrop typeset : LayerRules.getTypeSets(pkg)) {
          typesets.put(pkgName + '.' + typeset.getId(), typeset);
        }
        for (LayerPromiseDrop layer : LayerRules.getLayers(pkg)) {
          layers.put(pkgName + '.' + layer.getId(), layer);
        }
      }
      for (AllowsReferencesFromPromiseDrop a : Sea.getDefault().getDropsOfExactType(AllowsReferencesFromPromiseDrop.class)) {
        final IRNode type = a.getNode();
        allowRefs.put(type, a);
      }
      return this;
    }
	}
}
