/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.promise.*;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.promises.*;

public class UtilityRules extends AnnotationRules {
	public static final String UTILITY = "Utility";

	private static final AnnotationRules instance = new UtilityRules();

	private static final Utility_ParseRule vouchRule = new Utility_ParseRule();

	public static AnnotationRules getInstance() {
		return instance;
	}

	public static UtilityPromiseDrop getUtilityDrop(IRNode type) {
		return getDrop(vouchRule.getStorage(), type);
	}

	@Override
	public void register(PromiseFramework fw) {
		registerParseRuleStorage(fw, vouchRule);
	}

	static class Utility_ParseRule
			extends
			DefaultSLAnnotationParseRule<UtilityNode, UtilityPromiseDrop> {
		protected Utility_ParseRule() {
			super(UTILITY, typeDeclOps, UtilityNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws RecognitionException {
			return new UtilityNode(context.mapToSource(0));
		}

		@Override
		protected IPromiseDropStorage<UtilityPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					UtilityPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber<UtilityNode> makeScrubber() {
			return new AbstractAASTScrubber<UtilityNode, UtilityPromiseDrop>(this) {
				@Override
				protected PromiseDrop<UtilityNode> makePromiseDrop(UtilityNode a) {
					IRNode promisedFor = a.getPromisedFor();
					if (InterfaceDeclaration.prototype.includes(promisedFor)) {
						// This can't be on an interface
						return null;
					}
					
					// TODO check for classes
					
					UtilityPromiseDrop d = new UtilityPromiseDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}
}
