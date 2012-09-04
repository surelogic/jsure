package com.surelogic.annotation.rules;

import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.annotation.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.aast.promise.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
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
					final IRNode hashCode = findNonObjectImpl(a.getPromisedFor(), HASHCODE);
					final IRNode equals = findNonObjectImpl(a.getPromisedFor(), EQUALS);
					if (hashCode == null) {
						if (equals == null) {
							// FIX
							getContext().reportError("Type cannot be @"+VALUE_OBJECT+" due to missing hashCode/equals() implementations", a); 
						} else {
							// FIX
							getContext().reportError("Type cannot be @"+VALUE_OBJECT+" due to missing "+HASHCODE+"() implementation", a); 
						}
						return null;
					}						
					else if (equals == null) {		
						// FIX
						getContext().reportError("Type cannot be @"+VALUE_OBJECT+" due to missing "+EQUALS+"() implementation", a); 
						return null;
					}
					// Has both
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
					final IRNode hashCode = findNonObjectImpl(a.getPromisedFor(), HASHCODE);
					if (hashCode != null) {
						getContext().reportError("Type cannot be @"+REF_OBJECT+" due to "+JavaNames.getFullName(hashCode), a); // FIX
						return null;
					}
					final IRNode equals = findNonObjectImpl(a.getPromisedFor(), EQUALS);
					if (equals != null) {		
						getContext().reportError("Type cannot be @"+REF_OBJECT+" due to "+JavaNames.getFullName(equals), a); // FIX
						return null;
					}
					final RefObjectPromiseDrop d = new RefObjectPromiseDrop(a);
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}
	
	static final String HASHCODE = "hashCode";
	static final String EQUALS = "equals";
	
	/**
	 * Look for a no-arg method with the given name in this class or its superclasses
	 * (not counting java.lang.Object)
	 */
	static IRNode findNonObjectImpl(final IRNode tdecl, final String methodName) {
		if (tdecl == null) {
			return null;
		}
		IRNode impl = checkForNonObjectImpl(tdecl, methodName);
		if (impl != null) {
			return impl;
		}
		IIRProject p = JavaProjects.getEnclosingProject(tdecl);
		IJavaType t = p.getTypeEnv().convertNodeTypeToIJavaType(tdecl);
		return findNonObjectImpl(p.getTypeEnv(), t.getSuperclass(p.getTypeEnv()), methodName);
	}
	
	static IRNode findNonObjectImpl(final ITypeEnvironment env, final IJavaType t, final String methodName) {		
		if (t == null) {
			return null;
		}
		if (t instanceof IJavaDeclaredType) {
			// Check this type
			IJavaDeclaredType dt = (IJavaDeclaredType) t;
			IRNode impl = checkForNonObjectImpl(dt.getDeclaration(), methodName);
			if (impl != null) {
				return impl;
			}
		}
		return findNonObjectImpl(env, t.getSuperclass(env), methodName);
	}
	
	static IRNode checkForNonObjectImpl(final IRNode tdecl, final String methodName) {
		final String tName = JavaNames.getFullTypeName(tdecl);
		if (JavaGlobals.JLObject.equals(tName)) {
			return null;
		}
		for(IRNode m : VisitUtil.getClassMethods(tdecl)) {
			String name = MethodDeclaration.getId(m);
			if (methodName.equals(name)) {
				IRNode params = MethodDeclaration.getParams(m);
				if (JJNode.tree.numChildren(params) == 0) {
					return m;
				}
			}
		}
		return null;
	}
}
