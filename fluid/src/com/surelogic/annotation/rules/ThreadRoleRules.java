/*$Header$*/
package com.surelogic.annotation.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.ThreadRoleDeclarationNode;
import com.surelogic.aast.promise.ThreadRoleGrantNode;
import com.surelogic.aast.promise.ThreadRoleImportNode;
import com.surelogic.aast.promise.ThreadRoleIncompatibleNode;
import com.surelogic.aast.promise.ThreadRoleNameListNode;
import com.surelogic.aast.promise.ThreadRoleNode;
import com.surelogic.aast.promise.ThreadRoleRenameNode;
import com.surelogic.aast.promise.ThreadRoleRevokeNode;
import com.surelogic.aast.promise.ThreadRoleTransparentNode;
import com.surelogic.annotation.DefaultSLThreadRoleAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.NullAnnotationParseRule;
import com.surelogic.annotation.ParseResult;
import com.surelogic.annotation.parse.SLThreadRoleAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubberContext;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.dropsea.ir.Drop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.threadroles.RegionTRoleDeclDrop;
import com.surelogic.dropsea.ir.drops.threadroles.SimpleCallGraphDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleCtxSummaryDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleDeclareDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleGrantDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleImportDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleIncompatibleDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleRenameDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleReqSummaryDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleRequireDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleRevokeDrop;
import com.surelogic.dropsea.ir.drops.threadroles.TRoleTransparentDrop;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.PromiseDropSeqStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.java.bind.PromiseConstants;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.tree.Operator;

public class ThreadRoleRules extends AnnotationRules {
	public static final boolean useThreadRoles = true;
	
	public static final String TRANSPARENT = "ThreadRoleTransparent";
//	public static final String TROLE_CONSTRAINT = "ThreadRole";
	public static final String TROLE_IMPORT = "ThreadRoleImport";
	public static final String TROLE_DECLARATION = "ThreadRoleDecl";
	public static final String TROLE_INCOMPATIBLE = "ThreadRoleIncompatible";
	public static final String TROLE_GRANT = "ThreadRoleGrant";
	public static final String TROLE_REVOKE = "ThreadRoleRevoke";
	public static final String TROLE_RENAME = "ThreadRoleRename";
	public static final String TROLE = "ThreadRole";
	public static final String TROLE_CARDINALITY = "ThreadRoleCardinality";
	public static final String REGION_REPORTTROLES = "RegionReportThreadRoles";
	public static final String REGION_TROLECONSTRAINT = "ThreadRoleConstrainedRegions";

	public static boolean tRoleDropsEnabled;

	private static final AnnotationRules instance = new ThreadRoleRules();

	private static final TRoleTransparent_ParseRule transparentRule = new TRoleTransparent_ParseRule();
//	private static final TRoleConstraint_ParseRule trConstraintRule = new TRoleConstraint_ParseRule();
	private static final TRoleImport_ParseRule trImportRule = new TRoleImport_ParseRule();
	private static final TRoleDeclare_ParseRule trDeclarationRule = new TRoleDeclare_ParseRule();
	private static final TRoleIncompatible_ParseRule trIncompatibleRule = new TRoleIncompatible_ParseRule();
	private static final TRoleGrant_ParseRule trGrantRule = new TRoleGrant_ParseRule();
	private static final TRoleRevoke_ParseRule trRevokeRule = new TRoleRevoke_ParseRule();
	private static final TRoleRename_ParseRule trRenameRule = new TRoleRename_ParseRule();
	private static final TRole_ParseRule tRoleRule = new TRole_ParseRule();
	// private static final ColorCardinality_ParseRule colorCardinalityRule =
	// new ColorCardinality_ParseRule();
	// private static final ColorizedRegion_ParseRule colorizedRegionRule = new
	// ColorizedRegion_ParseRule();
	// private static final ColorConstrainedRegion_ParseRule
	// colorConstrainedRegionRule = new ColorConstrainedRegion_ParseRule();

	private static SlotInfo<Set<TRoleRequireDrop>> reqInheritDropSetSI = 
		SimpleSlotFactory.prototype.newAttribute(null);

	private static SlotInfo<TRoleReqSummaryDrop> reqSummDropSI = 
		SimpleSlotFactory.prototype.newAttribute(null);
	private static SlotInfo<TRoleCtxSummaryDrop> ctxSummDropSI = SimpleSlotFactory.prototype
			.newAttribute(null);

	private static SlotInfo<TRoleReqSummaryDrop> reqInheritSummDropSI = SimpleSlotFactory.prototype
			.newAttribute(null);

	private static SlotInfo<SimpleCallGraphDrop> simpleCGDropSI = SimpleSlotFactory.prototype
			.newAttribute(null);

	static SlotInfo<TRoleReqSummaryDrop> regionTRoleDeclDropSI = SimpleSlotFactory.prototype
			.newAttribute(null);
	private static SlotInfo<Set<RegionTRoleDeclDrop>> regionTRoleDeclDropSetSI = SimpleSlotFactory.prototype
			.newAttribute(null);

	private static SlotInfo<Set<TRoleRequireDrop>> reqDropSetSI = SimpleSlotFactory.prototype
			.newAttribute(null);

	// private static SlotInfo colorizedDropSI =
	// SimpleSlotFactory.prototype.newAttribute(null);

	public static AnnotationRules getInstance() {
		return instance;
	}

	@Override
	public void register(PromiseFramework fw) {
		registerParseRuleStorage(fw, transparentRule);
//		registerParseRuleStorage(fw, trConstraintRule);
		registerParseRuleStorage(fw, trImportRule);
		registerParseRuleStorage(fw, trDeclarationRule);
		registerParseRuleStorage(fw, trIncompatibleRule);
		registerParseRuleStorage(fw, trGrantRule);
		registerParseRuleStorage(fw, trRevokeRule);
		registerParseRuleStorage(fw, trRenameRule);
		registerParseRuleStorage(fw, tRoleRule);
		// below is example of two rules for single annotation whose parsing
		// varies depending on its location. Copy and modify for @ThreadRole!
		fw.registerParseDropRule(new NullAnnotationParseRule(TROLE, PromiseConstants.ptFuncOps) {
			@Override
			public ParseResult parse(IAnnotationParsingContext context,
					String contents) {
				if (trDeclarationRule.declaredOnValidOp(context.getOp())) {
					return trDeclarationRule.parse(context, contents);
				} else {
					return tRoleRule.parse(context, contents);
				}
			}
		}, true);
//	    fw.registerParseDropRule(new NullAnnotationParseRule(IN_REGION, PromiseConstants.fieldOrTypeOp) {
//	    	@Override
//			public ParseResult parse(IAnnotationParsingContext context, String contents) {
//				if (FieldDeclaration.prototype.includes(context.getOp())) {
//					return inRegionRule.parse(context, contents);
//				} else {
//					return mapFieldsRule.parse(context, contents);
//				}
//			}    	
//	    }, true);
		// registerParseRuleStorage(fw, colorImportRule);
		// registerParseRuleStorage(fw, colorImportRule);
		// registerParseRuleStorage(fw, colorImportRule);
	}

	static class TRoleTransparent_ParseRule
			extends
			DefaultSLThreadRoleAnnotationParseRule<ThreadRoleTransparentNode, TRoleTransparentDrop> {
		protected TRoleTransparent_ParseRule() {
			super(TRANSPARENT, functionDeclOps, ThreadRoleTransparentNode.class);
		}

		/*
		@Override
		protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) throws Exception {
			return new ThreadRoleTransparentNode();
		}
		*/

		@Override
		protected Object parseTRoleAnno(IAnnotationParsingContext context,
				SLThreadRoleAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.threadRoleTransparent().getTree();
		}

		@Override
		protected IPromiseDropStorage<TRoleTransparentDrop> makeStorage() {
			return BooleanPromiseDropStorage.create(name(),
					TRoleTransparentDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ThreadRoleTransparentNode, TRoleTransparentDrop>(this) {
				@Override
				protected PromiseDrop<ThreadRoleTransparentNode> makePromiseDrop(
						ThreadRoleTransparentNode a) {
					TRoleTransparentDrop d = new TRoleTransparentDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}

//	static class TRoleConstraint_ParseRule
//			extends
//			DefaultSLThreadRoleAnnotationParseRule<ThreadRoleConstraintNode, TRoleConstraintDrop> {
//		protected TRoleConstraint_ParseRule() {
//			super(TROLE_CONSTRAINT, methodDeclOps, ThreadRoleConstraintNode.class);
//		}
//
//		@Override
//		protected Object parseTRoleAnno(IAnnotationParsingContext context,
//				SLThreadRoleAnnotationsParser parser) throws Exception,
//				RecognitionException {
//			return parser.threadRoleConstraint().getTree();
//		}
//
//		@Override
//		protected IPromiseDropStorage<TRoleConstraintDrop> makeStorage() {
//			return SinglePromiseDropStorage.create(name(),
//					TRoleConstraintDrop.class);
//		}
//
//		@Override
//		protected IAnnotationScrubber<ThreadRoleConstraintNode> makeScrubber() {
//			return new AbstractAASTScrubber<ThreadRoleConstraintNode>(this,
//					ScrubberType.UNORDERED) {
//				@Override
//				protected PromiseDrop<ThreadRoleConstraintNode> makePromiseDrop(
//						ThreadRoleConstraintNode a) {
//					return storeDropIfNotNull(getStorage(), a,
//							new TRoleConstraintDrop(a));
//				}
//			};
//		}
//	}

	static class TRole_ParseRule extends
			DefaultSLThreadRoleAnnotationParseRule<ThreadRoleNode, TRoleRequireDrop> {
		protected TRole_ParseRule() {
			super(TROLE, functionDeclOps, ThreadRoleNode.class);
		}

		@Override
		protected Object parseTRoleAnno(IAnnotationParsingContext context,
				SLThreadRoleAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.threadRole().getTree();
		}

		@Override
		protected IPromiseDropStorage<TRoleRequireDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					TRoleRequireDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ThreadRoleNode, TRoleRequireDrop>(this,
					ScrubberType.UNORDERED) {
				@Override
				protected TRoleRequireDrop makePromiseDrop(ThreadRoleNode a) {
					return storeDropIfNotNull(a, new TRoleRequireDrop(a));
				}
			};
		}
	}

	static class TRoleImport_ParseRule extends
			DefaultSLThreadRoleAnnotationParseRule<ThreadRoleImportNode, TRoleImportDrop> {
		protected TRoleImport_ParseRule() {
			super(TROLE_IMPORT, typeDeclOps, ThreadRoleImportNode.class);
		}

		@Override
		protected Object parseTRoleAnno(IAnnotationParsingContext context,
				SLThreadRoleAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.threadRoleImport().getTree();
		}

		@Override
		protected IPromiseDropStorage<TRoleImportDrop> makeStorage() {
			return PromiseDropSeqStorage.create(name(), TRoleImportDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ThreadRoleImportNode, TRoleImportDrop>(this,
					ScrubberType.UNORDERED) {
				@Override
				protected PromiseDrop<ThreadRoleImportNode> makePromiseDrop(
						ThreadRoleImportNode a) {
					return storeDropIfNotNull(a,
							scrubTRoleImport(getContext(), a,
									new TRoleImportDrop(a)));
				}
			};

		}
	}

	private static TRoleImportDrop scrubTRoleImport(
			IAnnotationScrubberContext context, ThreadRoleImportNode a,
			TRoleImportDrop result) {
		return result;
	}

	static abstract class TRoleNameList_ParseRule<A extends ThreadRoleNameListNode, P extends PromiseDrop<? super A>>
			extends DefaultSLThreadRoleAnnotationParseRule<A, P> {
		Class<P> pcls;

		protected TRoleNameList_ParseRule(String name, Operator[] ops,
				Class<A> dt, Class<P> pcl) {
			super(name, ops, dt);
			pcls = pcl;
		}

		@Override
		protected IPromiseDropStorage<P> makeStorage() {
			return PromiseDropSeqStorage.create(name(), pcls);
		}
	}

	static class TRoleGrant_ParseRule extends
			TRoleNameList_ParseRule<ThreadRoleGrantNode, TRoleGrantDrop> {

		protected TRoleGrant_ParseRule() {
			super(TROLE_GRANT, blockOps, ThreadRoleGrantNode.class,
					TRoleGrantDrop.class);
		}

		@Override
		protected Object parseTRoleAnno(IAnnotationParsingContext context,
				SLThreadRoleAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.threadRoleGrant().getTree();
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ThreadRoleGrantNode, TRoleGrantDrop>(this) {
				@Override
				protected TRoleGrantDrop makePromiseDrop(ThreadRoleGrantNode a) {
					TRoleGrantDrop d = new TRoleGrantDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}

	static class TRoleRevoke_ParseRule extends
			TRoleNameList_ParseRule<ThreadRoleRevokeNode, TRoleRevokeDrop> {

		protected TRoleRevoke_ParseRule() {
			super("ThreadRoleRevoke", blockOps, ThreadRoleRevokeNode.class,
					TRoleRevokeDrop.class);
		}

		@Override
		protected Object parseTRoleAnno(IAnnotationParsingContext context,
				SLThreadRoleAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.threadRoleRevoke().getTree();
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ThreadRoleRevokeNode, TRoleRevokeDrop>(this) {
				@Override
				protected TRoleRevokeDrop makePromiseDrop(ThreadRoleRevokeNode a) {
					TRoleRevokeDrop d = new TRoleRevokeDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}

	static class TRoleDeclare_ParseRule extends
			TRoleNameList_ParseRule<ThreadRoleDeclarationNode, TRoleDeclareDrop> {

		protected TRoleDeclare_ParseRule() {
			super(TROLE_DECLARATION, packageTypeDeclOps, ThreadRoleDeclarationNode.class,
					TRoleDeclareDrop.class);
		}

		@Override
		protected Object parseTRoleAnno(IAnnotationParsingContext context,
				SLThreadRoleAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.threadRoleDeclare().getTree();
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ThreadRoleDeclarationNode, TRoleDeclareDrop>(this) {
				@Override
				protected TRoleDeclareDrop makePromiseDrop(
						ThreadRoleDeclarationNode a) {
					TRoleDeclareDrop d = new TRoleDeclareDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}

	static class TRoleIncompatible_ParseRule
			extends
			TRoleNameList_ParseRule<ThreadRoleIncompatibleNode, TRoleIncompatibleDrop> {

		protected TRoleIncompatible_ParseRule() {
			super(TROLE_INCOMPATIBLE, declOps, ThreadRoleIncompatibleNode.class,
					TRoleIncompatibleDrop.class);
		}

		@Override
		protected Object parseTRoleAnno(IAnnotationParsingContext context,
				SLThreadRoleAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.threadRoleIncompatible().getTree();
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ThreadRoleIncompatibleNode, TRoleIncompatibleDrop>(this) {
				@Override
				protected TRoleIncompatibleDrop makePromiseDrop(
						ThreadRoleIncompatibleNode a) {
					TRoleIncompatibleDrop d = new TRoleIncompatibleDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}

	static class TRoleRename_ParseRule extends
			DefaultSLThreadRoleAnnotationParseRule<ThreadRoleRenameNode, TRoleRenameDrop> {
		protected TRoleRename_ParseRule() {
			super(TROLE_RENAME, declOps, ThreadRoleRenameNode.class);
		}

		@Override
		protected Object parseTRoleAnno(IAnnotationParsingContext context,
				SLThreadRoleAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.threadRoleRename().getTree();
		}

		@Override
		protected IPromiseDropStorage<TRoleRenameDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					TRoleRenameDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ThreadRoleRenameNode, TRoleRenameDrop>(this,
					ScrubberType.UNORDERED) {
				@Override
				protected PromiseDrop<ThreadRoleRenameNode> makePromiseDrop(
						ThreadRoleRenameNode a) {
					return storeDropIfNotNull(a, TRoleRenameDrop.buildTRoleRenameDrop(a));
				}
			};
		}
	}

	// static class ColorCardinality_ParseRule
	// extends
	// DefaultSLColorAnnotationParseRule<ColorCardSpecNode,ColorCardSpecDrop> {
	// protected ColorCardinality_ParseRule() {
	// super(COLOR_CARDINALITY, declOps, ColorCardSpecNode.class);
	// }
	//
	// @Override
	// protected Object parse(IAnnotationParsingContext context,
	// SLThreadRoleAnnotationsParser parser) throws Exception, RecognitionException {
	// return parser.colorCardSpec().getTree();
	// }
	//    
	// @Override
	// protected IPromiseDropStorage<TRoleRenameDrop> makeStorage() {
	// return SinglePromiseDropStorage.create(name(), ColorCardSpecDrop.class);
	// }
	// @Override
	// protected IAnnotationScrubber<ColorRenameNode> makeScrubber() {
	// return new AbstractAASTScrubber<ColorCardSpecNode>(this,
	// ScrubberType.UNORDERED, ColorRules.COLOR_CARDINALITY) {
	// @Override
	// protected PromiseDrop<ColorRenameNode> makePromiseDrop(ColorCardSpecNode
	// a) {
	// return storeDropIfNotNull(getStorage(),
	// a,
	// ColorCardSpecDrop.buildColorCardSpecDrop(a));
	// }
	// };
	// }
	// }

	// static class ColorizedRegion_ParseRule
	// extends
	// DefaultSLColorAnnotationParseRule<ColorizedRegionNode,ColorizedRegionDeclDrop>
	// {
	// protected ColorizedRegion_ParseRule() {
	// super(COLOR_RENAME, declOps, ColorizedRegionNode.class);
	// }
	//
	// @Override
	// protected Object parse(IAnnotationParsingContext context,
	// SLThreadRoleAnnotationsParser parser) throws Exception, RecognitionException {
	// return parser.colorizedRegion().getTree();
	// }
	//    
	// @Override
	// protected IPromiseDropStorage<ColorizedRegionDrop> makeStorage() {
	// return SinglePromiseDropStorage.create(name(),
	// ColorizedRegionDrop.class);
	// }
	// @Override
	// protected IAnnotationScrubber<ColorizedRegionNode> makeScrubber() {
	// return new AbstractAASTScrubber<ColorizedRegionNode>(this,
	// ScrubberType.UNORDERED, ColorRules.COLOR_CONSTRAINT) {
	// @Override
	// protected PromiseDrop<ColorizedRegionNode>
	// makePromiseDrop(ColorizedRegionNode a) {
	// return storeDropIfNotNull(getStorage(),
	// a,
	// ColorizedRegionDrop.buildColorizedRegionDrop(a));
	// }
	// };
	// }
	// }

	public static SimpleCallGraphDrop getCGDrop(IRNode node) {
		return node.getSlotValue(simpleCGDropSI);
	}

	public static void setCGDrop(IRNode node, SimpleCallGraphDrop cgDrop) {
		if (!tRoleDropsEnabled)
			return;
		node.setSlotValue(simpleCGDropSI, cgDrop);
		//cgDrop.setAttachedTo(node, simpleCGDropSI);
	}
	
	public static TRoleRequireDrop getReqDrop(IRNode node) {
		return getDrop(tRoleRule.getStorage(), node);
	}

	public static Collection<TRoleRequireDrop> getReqDrops(IRNode node) {
		final Set<TRoleRequireDrop> mrcs = getMutableRequiresTRoleSet(node);
		Collection<TRoleRequireDrop> res = new HashSet<TRoleRequireDrop>(mrcs
				.size());
		res.addAll(mrcs);
		return res;
	}

	public static Collection<TRoleRequireDrop> getInheritedRequireDrops(
			IRNode node) {
		return getCopyOfMutableSet(node, reqInheritDropSetSI);
	}

	public static TRoleReqSummaryDrop getReqSummDrop(IRNode node) {
		return node.getSlotValue(reqSummDropSI);
	}

	public static void setReqSummDrop(IRNode node, TRoleReqSummaryDrop summ) {
		if (!tRoleDropsEnabled)
			return;
		node.setSlotValue(reqSummDropSI, summ);
		//summ.setAttachedTo(node, reqSummDropSI);
	}

	public static TRoleReqSummaryDrop getInheritedReqSummDrop(IRNode node) {
		return node.getSlotValue(reqInheritSummDropSI);
	}

	public static void setInheritedReqSummDrop(IRNode node,
			TRoleReqSummaryDrop summ) {
		if (!tRoleDropsEnabled)
			return;
		node.setSlotValue(reqInheritSummDropSI, summ);
		//summ.setAttachedTo(node, reqInheritSummDropSI);
	}

	public static TRoleCtxSummaryDrop getCtxSummDrop(IRNode node) {
		return node.getSlotValue(ctxSummDropSI);
	}

	public static void setCtxSummDrop(IRNode node, TRoleCtxSummaryDrop summ) {
		if (!tRoleDropsEnabled)
			return;
		node.setSlotValue(ctxSummDropSI, summ);
		//summ.setAttachedTo(node, ctxSummDropSI);
	}

	public static Collection<? extends TRoleImportDrop> getTRoleImports(
			IRNode node) {
		Collection<TRoleImportDrop> res = new HashSet<TRoleImportDrop>();
		for (TRoleImportDrop cid : getDrops(trImportRule.getStorage(), node)) {
			res.add(cid);
		}
		return res;
	}

	public static Collection<TRoleIncompatibleDrop> getTRoleIncompatibles(
			IRNode node) {
		Iterable<TRoleIncompatibleDrop> incIter = getDrops(
				trIncompatibleRule.getStorage(), node);
		Collection<TRoleIncompatibleDrop> res = new ArrayList<TRoleIncompatibleDrop>(
				1);
		for (TRoleIncompatibleDrop cid : incIter) {
			res.add(cid);
		}
		return res;
	}

	public static boolean isTRoleRelevant(IRNode node) {
		boolean res = (getBooleanDrop(transparentRule.getStorage(), node) == null);
		return res;
	}

	private static <T extends Drop> Set<T> getMutableSet(IRNode forNode,
			SlotInfo<Set<T>> si) {
		Set<T> result = forNode.getSlotValue(si);
		if (result == null) {
			result = new HashSet<T>();
			forNode.setSlotValue(si, result);
		}
		return result;
	}

	private static <T extends Drop> Set<T> getCopyOfMutableSet(IRNode forNode,
			SlotInfo<Set<T>> si) {
		Set<T> current = getMutableSet(forNode, si);
		if (current.size() == 0) {
			return new HashSet<T>(0);
		}
		Set<T> result = new HashSet<T>(current.size());
		Iterator<T> currIter = current.iterator();
		while (currIter.hasNext()) {
			T dr = currIter.next();
			if (dr.isValid()) {
				result.add(dr);
			}
		}
		if (result.size() < current.size()) {
			// we must have skipped over some invalid entries. update the saved
			// set.
			current = new HashSet<T>(result.size());
			current.addAll(result);
			forNode.setSlotValue(si, current);
		}
		return result;
	}

	/**
	 * Remove all invalid drops from a MutableXXXSet. Do this by building a new
	 * set and transferring only valid drops from old to newSet. Finish by
	 * installing the new set as the mutableXXXSet for node.
	 * 
	 * @param node
	 *            the node whose set should be updated
	 * @param si
	 *            the SlotInfo to get the set from.
	 */
	static <T extends Drop> void purgeMutableSet(IRNode node,
			SlotInfo<Set<T>> si) {
		Set<T> old = getMutableSet(node, si);
		final int newSize = Math.max(old.size() - 1, 0);
		Set<T> newSet = new HashSet<T>(newSize);
		Iterator<T> oldIter = old.iterator();
		while (oldIter.hasNext()) {
			T dr = oldIter.next();
			if (dr.isValid()) {
				newSet.add(dr);
			}
		}
		node.setSlotValue(si, newSet);
	}

	// public static Set<ColorContextDrop> getMutableColorContextSet(IRNode
	// forNode) {
	// return getMutableSet(forNode, contextDropSetSI);
	// }

	public static Set<RegionTRoleDeclDrop> getMutableRegionTRoleDeclsSet(
			IRNode forNode) {
		return getMutableSet(forNode, regionTRoleDeclDropSetSI);
	}

	public static Set<TRoleRequireDrop> getMutableRequiresTRoleSet(
			IRNode forNode) {
		return getMutableSet(forNode, reqDropSetSI);
	}

	public static Set<TRoleRequireDrop> getMutableInheritedRequiresSet(
			IRNode forNode) {
		return getMutableSet(forNode, reqInheritDropSetSI);
	}

}
