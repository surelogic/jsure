package com.surelogic.annotation.rules;

import com.surelogic.annotation.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.aast.promise.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;

public class StructureRules extends AnnotationRules {
	public static final String MUST_INVOKE_ON_OVERRIDE = "MustInvokeOnOverride";

	private static final MustInvokeOnOverride_ParseRule mustInvokeOnOverrideRule = new MustInvokeOnOverride_ParseRule();
	
	private static final StructureRules instance = new StructureRules();

	public static StructureRules getInstance() {
		return instance;
	}

	public static MustInvokeOnOverridePromiseDrop getMustInvokeDrop(IRNode type) {
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
			return new AbstractAASTScrubber<MustInvokeOnOverrideNode, MustInvokeOnOverridePromiseDrop>
			           (this, ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY) {
				@Override
				protected PromiseDrop<MustInvokeOnOverrideNode> makePromiseDrop(MustInvokeOnOverrideNode a) {
				  	if (isInFinalContext(a.getPromisedFor())) {
			    		return null; // illegal
			    	}
					// TODO scrub
					final MustInvokeOnOverridePromiseDrop d = new MustInvokeOnOverridePromiseDrop(a);
					return storeDropIfNotNull(a, d);
				}
				
			    @Override
			    protected boolean processUnannotatedMethodRelatedDecl(final IRNode unannodMethod) {				    
			    	if (isInFinalContext(unannodMethod)) {
			    		return true; // ok
			    	}
			    	// check if parent(s) has @MustInvokeOnOverride
			    	for(IBinding overridden : getContext().getBinder(unannodMethod).findOverriddenParentMethods(unannodMethod)) {
			    		if (getMustInvokeDrop(overridden.getNode()) != null) {
			    			getContext().reportError(unannodMethod, "Should be marked with @"+MUST_INVOKE_ON_OVERRIDE+
			    					" like the method it overrides: "+JavaNames.getFullName(overridden.getNode()));
			    			return false;
			    		}			    		
			    	}
			    	return true; // ok
			    }
			};
		}
	}
	
	static boolean isInFinalContext(IRNode method) {
    	final boolean isFinal = JavaNode.getModifier(method, JavaNode.FINAL);
    	if (isFinal) {
    		return true; 
    	}
    	final IRNode type = VisitUtil.getEnclosingType(method);
    	final boolean typeIsFinal = JavaNode.getModifier(type, JavaNode.FINAL);
    	if (typeIsFinal) {
    		return true; 
    	}
    	return false;
	}
}
