package com.surelogic.annotation.rules;

import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.annotation.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.aast.promise.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
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
					// Check consistency
					final ValueObjectPromiseDrop d = new ValueObjectPromiseDrop(a);
					
					final IRNode hashCode = findNonObjectImpl(a.getPromisedFor(), HASHCODE);
					final IRNode equals = findSingleObjectArgImpl(a.getPromisedFor(), EQUALS);
					if (hashCode == null) {
						// FIX
						makeResultDrop(d, false, "Missing "+HASHCODE+"() implementation", a); 
					}					
					if (equals == null) {		
						// FIX
						makeResultDrop(d, false, "Missing "+EQUALS+"() implementation", a); 
					}
					else if (hashCode != null) {
						// Has both
						makeResultDrop(d, true, "Overrides "+HASHCODE+"() implementation");
						makeResultDrop(d, true, "Overrides "+EQUALS+"() implementation");
					}
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
					// Check consistency
					final RefObjectPromiseDrop d = new RefObjectPromiseDrop(a);
					
					final IRNode hashCode = findNonObjectImpl(a.getPromisedFor(), HASHCODE);
					if (hashCode != null) {
						makeResultDrop(d, false, "Overrides "+HASHCODE+"() at "+JavaNames.getFullName(hashCode), a); // FIX
					}
					final IRNode equals = findSingleObjectArgImpl(a.getPromisedFor(), EQUALS);
					if (equals != null) {		
						makeResultDrop(d, false, "Overrides "+EQUALS+"() at "+JavaNames.getFullName(equals), a); // FIX
					}
					else if (hashCode == null) {
						makeResultDrop(d, true, "No "+HASHCODE+'/'+EQUALS+"() implementations");
					}
					return storeDropIfNotNull(a, d);
				}
			};
		}
	}

	static void makeResultDrop(PromiseDrop<?> p, boolean consistent, String msg, Object... args) {
		final ResultDrop r = new ResultDrop(Messages.DSC_LAYERS_ISSUES.getMessage());
		r.setMessage(msg, args);
		r.addCheckedPromise(p);
		if (consistent) {
			r.setConsistent();
		} else {
			r.setInconsistent();
		}
	}
	
	static final String HASHCODE = "hashCode";
	static final String EQUALS = "equals";
	
	/**
	 * Look for a no-arg method with the given name in this class or its superclasses
	 * (not counting java.lang.Object)
	 */
	static IRNode findNonObjectImpl(final IRNode tdecl, final String methodName) {
		final IIRProject p = JavaProjects.getEnclosingProject(tdecl);
		final AbstractSearch s = new AbstractSearch(p.getTypeEnv().getBinder(), methodName) {
			@Override
			boolean matchesArgs(IRNode params) {
				return JJNode.tree.numChildren(params) == 0;
			}			
		};
		p.getTypeEnv().getBinder().findClassBodyMembers(tdecl, s, false);
		return s.getResult();
	}
	
	/**
	 * Look for a single-Object-typed-arg method with the given name in this class or its superclasses
	 * (not counting java.lang.Object)
	 */
	static IRNode findSingleObjectArgImpl(IRNode tdecl, final String name) {
		final IIRProject p = JavaProjects.getEnclosingProject(tdecl);
		final AbstractSearch s = new AbstractSearch(p.getTypeEnv().getBinder(), name) {
			@Override
			boolean matchesArgs(IRNode params) {
				if (JJNode.tree.numChildren(params) != 1) {
					return false;
				}
				final IRNode param = Parameters.getFormal(params, 0);
				final IRNode type  = ParameterDeclaration.getType(param);
				return p.getTypeEnv().getObjectType().equals(p.getTypeEnv().getBinder().getJavaType(type));
			}			
		};
		p.getTypeEnv().getBinder().findClassBodyMembers(tdecl, s, false);
		return s.getResult();
	}
	
	static abstract class AbstractSearch extends AbstractSuperTypeSearchStrategy<IRNode> {
		AbstractSearch(IBinder bind, String name) {
			super(bind, "method ", name);
		}
		
		@Override
		protected void visitClass_internal(IRNode tdecl) {
			final String tName = JavaNames.getFullTypeName(tdecl);
			if (JavaGlobals.JLObject.equals(tName)) {
				result = null;
				searchAfterLastType = false;
			}
			for(IRNode m : VisitUtil.getClassMethodsOnly(tdecl)) {
				String name = MethodDeclaration.getId(m);
				if (this.name.equals(name)) {
					IRNode params = MethodDeclaration.getParams(m);
					if (matchesArgs(params)) {
						result = m;
						searchAfterLastType = false;
						return;
					}				
				}
			}
		}
		
		abstract boolean matchesArgs(IRNode params);
		
		@Override
		public final boolean visitSuperifaces() {
			return false;
		}
	}
}
