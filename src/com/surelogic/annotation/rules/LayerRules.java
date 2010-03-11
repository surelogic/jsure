/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

public class LayerRules extends AnnotationRules {
	public static final String MAY_REFER_TO = "MayReferTo";

	private static final AnnotationRules instance = new LayerRules();

	private static final MayReferTo_ParseRule mayReferToRule = new MayReferTo_ParseRule();

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
		registerParseRuleStorage(fw, mayReferToRule);
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
 	
	static class MayReferTo_ParseRule
	extends	AbstractLayersParseRule<GuardedByNode, GuardedByPromiseDrop> {
		protected MayReferTo_ParseRule() {
			super(MAY_REFER_TO, typeDeclOps, GuardedByNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				LayerPromisesParser parser) throws RecognitionException {
			return parser.name().getTree();
		}

		@Override
		protected IPromiseDropStorage<GuardedByPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					GuardedByPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber<GuardedByNode> makeScrubber() {
			// TODO run this before Lock to create virtual declarations?
			// TODO group similar decls within a type?
			return new AbstractAASTScrubber<GuardedByNode>(this, ScrubberType.UNORDERED, 
					new String[] { LockRules.LOCK }, noStrings) {
				@Override
				protected PromiseDrop<GuardedByNode> makePromiseDrop(GuardedByNode a) {
					GuardedByPromiseDrop d = new GuardedByPromiseDrop(a);
					return storeDropIfNotNull(getStorage(), a, d);
				}
			};
		}
	}
}
