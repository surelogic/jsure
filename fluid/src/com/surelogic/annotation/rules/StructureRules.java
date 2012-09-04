package com.surelogic.annotation.rules;

import com.surelogic.annotation.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.aast.promise.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;

public class StructureRules extends AnnotationRules {
	public static final String MUST_INVOKE_ON_OVERRIDE = "MustInvokeOnOverride";

	private static final MustInvokeOnOverride_ParseRule mustInvokeOnOverrideRule = new MustInvokeOnOverride_ParseRule();
	
	private static final StructureRules instance = new StructureRules();

	public static StructureRules getInstance() {
		return instance;
	}

	public static MustInvokeOnOverridePromiseDrop getSingletonDrop(IRNode type) {
		return getDrop(mustInvokeOnOverrideRule.getStorage(), type);
	}
	
	@Override
	public void register(PromiseFramework fw) {
		registerParseRuleStorage(fw, mustInvokeOnOverrideRule);
	}
	
	static class MustInvokeOnOverride_ParseRule extends 
	SimpleBooleanAnnotationParseRule<MustInvokeOnOverrideNode,MustInvokeOnOverridePromiseDrop> {
		protected MustInvokeOnOverride_ParseRule() {
			super(MUST_INVOKE_ON_OVERRIDE, typeDeclOps, MustInvokeOnOverrideNode.class);
		}	
		
		@Override
		protected IPromiseDropStorage<MustInvokeOnOverridePromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					MustInvokeOnOverridePromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<MustInvokeOnOverrideNode, MustInvokeOnOverridePromiseDrop>(this) {
				@Override
				protected PromiseDrop<MustInvokeOnOverrideNode> makePromiseDrop(MustInvokeOnOverrideNode a) {
					// TODO scrub
					final MustInvokeOnOverridePromiseDrop d = new MustInvokeOnOverridePromiseDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}
}
