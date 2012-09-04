package com.surelogic.annotation.rules;

import com.surelogic.annotation.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.aast.promise.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;

public class EqualityRules extends AnnotationRules {
	public static final String VALUE_OBJECT = "ValueObject";
	public static final String REF_OBJECT = "ReferenceObject";

	private static final ValueObject_ParseRule valueObjectRule = new ValueObject_ParseRule();
	private static final RefObject_ParseRule refObjectRule = new RefObject_ParseRule();
	
	private static final EqualityRules instance = new EqualityRules();

	public static EqualityRules getInstance() {
		return instance;
	}

	public static ValueObjectPromiseDrop getSingletonDrop(IRNode type) {
		return getDrop(valueObjectRule.getStorage(), type);
	}

	public static RefObjectPromiseDrop getUtilityDrop(IRNode type) {
		return getDrop(refObjectRule.getStorage(), type);
	}
	
	@Override
	public void register(PromiseFramework fw) {
		registerParseRuleStorage(fw, valueObjectRule);
		registerParseRuleStorage(fw, refObjectRule);
	}
	
	static class ValueObject_ParseRule extends 
	SimpleBooleanAnnotationParseRule<ValueObjectNode,ValueObjectPromiseDrop> {
		protected ValueObject_ParseRule() {
			super(VALUE_OBJECT, typeDeclOps, ValueObjectNode.class);
		}	
		
		@Override
		protected IPromiseDropStorage<ValueObjectPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					ValueObjectPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ValueObjectNode, ValueObjectPromiseDrop>(this) {
				@Override
				protected PromiseDrop<ValueObjectNode> makePromiseDrop(ValueObjectNode a) {
					// TODO scrub
					final ValueObjectPromiseDrop d = new ValueObjectPromiseDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}
	
	static class RefObject_ParseRule extends 
	SimpleBooleanAnnotationParseRule<RefObjectNode,RefObjectPromiseDrop> {
		protected RefObject_ParseRule() {
			super(REF_OBJECT, typeDeclOps, RefObjectNode.class);
		}	
		
		@Override
		protected IPromiseDropStorage<RefObjectPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					RefObjectPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<RefObjectNode, RefObjectPromiseDrop>(this) {
				@Override
				protected PromiseDrop<RefObjectNode> makePromiseDrop(RefObjectNode a) {
					// TODO scrub
					final RefObjectPromiseDrop d = new RefObjectPromiseDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}
}
