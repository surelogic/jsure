/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.layers.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.layers.*;
import edu.cmu.cs.fluid.tree.Operator;

public class LayerRules extends AnnotationRules {
	public static final String IN_LAYER = "InLayer";
	public static final String LAYER = "Layer";
	public static final String TYPESET = "TypeSet";
	public static final String MAY_REFER_TO = "MayReferTo";
	public static final String ALLOWS_REFERENCES_FROM = "AllowsReferencesFrom";

	private static final AnnotationRules instance = new LayerRules();

	private static final InLayer_ParseRule inLayerRule = new InLayer_ParseRule();
	private static final Layer_ParseRule layerRule = new Layer_ParseRule();
	private static final TypeSet_ParseRule typeSetRule = new TypeSet_ParseRule();
	private static final MayReferTo_ParseRule mayReferToRule = new MayReferTo_ParseRule();
	private static final AllowsReferencesFrom_ParseRule allowsReferencesToRule = 
		new AllowsReferencesFrom_ParseRule();
	
	public static AnnotationRules getInstance() {
		return instance;
	}

	/*
	public static VouchPromiseDrop getVouchSpec(IRNode decl) {
		return getDrop(vouchRule.getStorage(), decl);
	}
	*/

	/**
	 * Returns the closest vouch applicable for the given IRNode, if any
	 */
	/*
	public static VouchPromiseDrop getEnclosingVouch(final IRNode n) {
		IRNode decl = VisitUtil.getClosestDecl(n);
		while (decl != null) {
			Operator op = JJNode.tree.getOperator(decl);
			if (ClassBodyDeclaration.prototype.includes(op)
					|| TypeDeclaration.prototype.includes(op)) {
				VouchPromiseDrop rv = getVouchSpec(decl);
				if (rv != null) {
					return rv;
				}
			}
			decl = VisitUtil.getEnclosingDecl(decl);
		}
		return null;
	}
	*/

	@Override
	public void register(PromiseFramework fw) {
		registerParseRuleStorage(fw, typeSetRule);
		registerParseRuleStorage(fw, layerRule);
		registerParseRuleStorage(fw, inLayerRule);
		registerParseRuleStorage(fw, mayReferToRule);
		registerParseRuleStorage(fw, allowsReferencesToRule);
	}

	static abstract class AbstractLayersParseRule<A extends IAASTRootNode, 
	                                              P extends PromiseDrop<? super A>> 
	extends AbstractAntlrParseRule<A,P,LayerPromisesParser> {
		protected AbstractLayersParseRule(String name, Operator[] ops, Class<A> dt) {
			super(name, ops, dt, AnnotationLocation.DECL);
		}		
		@Override
		protected LayerPromisesParser initParser(String contents) throws Exception {
			return SLLayerParse.prototype.initParser(contents);
		}
	}
 	
	static class TypeSet_ParseRule
	extends	AbstractLayersParseRule<TypeSetNode, TypeSetPromiseDrop> {
		protected TypeSet_ParseRule() {
			super(TYPESET, packageOps, TypeSetNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				LayerPromisesParser parser) throws RecognitionException {
			return parser.layer().getTree();
		}

		@Override
		protected IPromiseDropStorage<TypeSetPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					TypeSetPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber<TypeSetNode> makeScrubber() {
			return new AbstractAASTScrubber<TypeSetNode>(this, ScrubberType.UNORDERED) {
				@Override
				protected PromiseDrop<TypeSetNode> makePromiseDrop(TypeSetNode a) {
					TypeSetPromiseDrop d = new TypeSetPromiseDrop(a);
					return storeDropIfNotNull(getStorage(), a, d);
				}
			};
		}
	}
	
	static class Layer_ParseRule
	extends	AbstractLayersParseRule<LayerNode, LayerPromiseDrop> {
		protected Layer_ParseRule() {
			super(LAYER, packageOps, LayerNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				LayerPromisesParser parser) throws RecognitionException {
			return parser.layer().getTree();
		}

		@Override
		protected IPromiseDropStorage<LayerPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					LayerPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber<LayerNode> makeScrubber() {
			return new AbstractAASTScrubber<LayerNode>(this, ScrubberType.UNORDERED, TYPESET) {
				@Override
				protected PromiseDrop<LayerNode> makePromiseDrop(LayerNode a) {
					LayerPromiseDrop d = new LayerPromiseDrop(a);
					return storeDropIfNotNull(getStorage(), a, d);
				}
			};
		}
	}
	
	static class InLayer_ParseRule
	extends	AbstractLayersParseRule<InLayerNode, InLayerPromiseDrop> {
		protected InLayer_ParseRule() {
			super(IN_LAYER, typeDeclOps, InLayerNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				LayerPromisesParser parser) throws RecognitionException {
			return parser.inLayer().getTree();
		}

		@Override
		protected IPromiseDropStorage<InLayerPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					InLayerPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber<InLayerNode> makeScrubber() {
			return new AbstractAASTScrubber<InLayerNode>(this, ScrubberType.UNORDERED, LAYER) {
				@Override
				protected PromiseDrop<InLayerNode> makePromiseDrop(InLayerNode a) {
					InLayerPromiseDrop d = new InLayerPromiseDrop(a);
					return storeDropIfNotNull(getStorage(), a, d);
				}
			};
		}
	}
	
	static class MayReferTo_ParseRule
	extends	AbstractLayersParseRule<MayReferToNode, MayReferToPromiseDrop> {
		protected MayReferTo_ParseRule() {
			super(MAY_REFER_TO, typeDeclOps, MayReferToNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				LayerPromisesParser parser) throws RecognitionException {
			return parser.mayReferTo().getTree();
		}

		@Override
		protected IPromiseDropStorage<MayReferToPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					MayReferToPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber<MayReferToNode> makeScrubber() {
			return new AbstractAASTScrubber<MayReferToNode>(this, ScrubberType.UNORDERED, IN_LAYER) {
				@Override
				protected PromiseDrop<MayReferToNode> makePromiseDrop(MayReferToNode a) {
					MayReferToPromiseDrop d = new MayReferToPromiseDrop(a);
					return storeDropIfNotNull(getStorage(), a, d);
				}
			};
		}
	}
	
	static class AllowsReferencesFrom_ParseRule
	extends	AbstractLayersParseRule<AllowsReferencesFromNode, AllowsReferencesFromPromiseDrop> {
		protected AllowsReferencesFrom_ParseRule() {
			super(ALLOWS_REFERENCES_FROM, typeDeclOps, AllowsReferencesFromNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				LayerPromisesParser parser) throws RecognitionException {
			return parser.allowsReferencesFrom().getTree();
		}

		@Override
		protected IPromiseDropStorage<AllowsReferencesFromPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					AllowsReferencesFromPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber<AllowsReferencesFromNode> makeScrubber() {
			return new AbstractAASTScrubber<AllowsReferencesFromNode>(this, ScrubberType.UNORDERED, IN_LAYER) {
				@Override
				protected PromiseDrop<AllowsReferencesFromNode> makePromiseDrop(AllowsReferencesFromNode a) {
					AllowsReferencesFromPromiseDrop d = new AllowsReferencesFromPromiseDrop(a);
					return storeDropIfNotNull(getStorage(), a, d);
				}
			};
		}
	}
}
