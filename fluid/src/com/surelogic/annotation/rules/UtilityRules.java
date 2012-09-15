/*$Header: /cvs/fluid/fluid/src/com/surelogic/annotation/rules/ThreadEffectsRules.java,v 1.15 2007/08/08 16:07:12 chance Exp $*/
package com.surelogic.annotation.rules;

import org.antlr.runtime.RecognitionException;

import com.surelogic.aast.promise.SingletonNode;
import com.surelogic.aast.promise.UtilityNode;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.drops.typeAnnos.SingletonPromiseDrop;
import com.surelogic.dropsea.ir.drops.typeAnnos.UtilityPromiseDrop;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;

public class UtilityRules extends AnnotationRules {
	public static final String UTILITY = "Utility";
	public static final String SINGLETON = "Singleton";
	
	private static final AnnotationRules instance = new UtilityRules();

	private static final Utility_ParseRule utilityRule = new Utility_ParseRule();
	private static final Singleton_ParseRule singletonRule = new Singleton_ParseRule();

	public static AnnotationRules getInstance() {
		return instance;
	}

	public static SingletonPromiseDrop getSingletonDrop(IRNode type) {
		return getDrop(singletonRule.getStorage(), type);
	}

	public static UtilityPromiseDrop getUtilityDrop(IRNode type) {
		return getDrop(utilityRule.getStorage(), type);
	}
	
	@Override
	public void register(PromiseFramework fw) {
		registerParseRuleStorage(fw, utilityRule);
		registerParseRuleStorage(fw, singletonRule);
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
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<UtilityNode, UtilityPromiseDrop>(this) {
				@Override
				protected PromiseDrop<UtilityNode> makePromiseDrop(UtilityNode a) {
					final IRNode promisedFor = a.getPromisedFor();
          // This can't be on an interface
					if (InterfaceDeclaration.prototype.includes(promisedFor)) {
					  getContext().reportError(a, "Cannot use @Utility on an interface");
						return null;
					}
					
					// TODO check for classes
					
					final UtilityPromiseDrop d = new UtilityPromiseDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}
	
	static class Singleton_ParseRule extends
	DefaultSLAnnotationParseRule<SingletonNode, SingletonPromiseDrop> {
		protected Singleton_ParseRule() {
			super(SINGLETON, typeDeclOps, SingletonNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws RecognitionException {
			return new SingletonNode(context.mapToSource(0));
		}

		@Override
		protected IPromiseDropStorage<SingletonPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					SingletonPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<SingletonNode, SingletonPromiseDrop>(this) {
				@Override
				protected PromiseDrop<SingletonNode> makePromiseDrop(SingletonNode a) {
					final IRNode promisedFor = a.getPromisedFor();
					// This can't be on an interface
					if (InterfaceDeclaration.prototype.includes(promisedFor)) {
						getContext().reportError(a, "Cannot use @Singleton on an interface");
						return null;
					}

					// TODO check for classes

					final SingletonPromiseDrop d = new SingletonPromiseDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}
}
