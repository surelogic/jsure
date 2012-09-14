package com.surelogic.annotation.rules;

import java.util.Iterator;

import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.annotation.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.aast.promise.*;
import com.surelogic.common.i18n.I18N;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.util.TypeUtil;
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

	public static ValueObjectPromiseDrop getValueObjectDrop(IRNode type) {
		return getDrop(valueObjectRule.getStorage(), type);
	}

	public static RefObjectPromiseDrop getRefObjectDrop(IRNode type) {
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
			return new AbstractAASTScrubber<ValueObjectNode, ValueObjectPromiseDrop>(this, ScrubberType.INCLUDE_SUBTYPES_BY_HIERARCHY, REF_OBJECT) {
				@Override
				protected PromiseDrop<ValueObjectNode> makePromiseDrop(ValueObjectNode a) {
					// Check consistency
					final IRNode tdecl = a.getPromisedFor();
					if (getRefObjectDrop(tdecl) != null) {
						// Conflict w/ RefObject
						getContext().reportError(a, I18N.res(752, REF_OBJECT));
						return null;
					}
					// Check if abstract or has no subclasses
					final boolean isInterface = TypeUtil.isInterface(tdecl);
					final boolean isAbstract = TypeUtil.isAbstract(tdecl);
					if (!isInterface && !isAbstract) {
						final IIRProject p = JavaProjects.getEnclosingProject(tdecl);					
						Iterator<IRNode> it = p.getTypeEnv().getRawSubclasses(tdecl).iterator();
						if (it.hasNext()) {
							getContext().reportError(a, I18N.res(753, JavaNames.getFullTypeName(it.next())));
							return null;
						}
					}
					
					final ValueObjectPromiseDrop d = new ValueObjectPromiseDrop(a);
					if (isInterface) {
						makeResultDrop(tdecl, d, true, 754, JavaNames.getTypeName(tdecl));
					} else if (isAbstract) {
						makeResultDrop(tdecl, d, true, 757, JavaNames.getTypeName(tdecl));
					} else {
						computeResults(a.getPromisedFor(), d, true);
					}
					return storeDropIfNotNull(a, d);
				}
				
				@Override 
			    protected final boolean processUnannotatedType(final IJavaSourceRefType dt) {
					// Check if superclass has this annotation
					final IIRProject p = JavaProjects.getEnclosingProject(dt.getDeclaration());	
					for(IJavaType st : dt.getSupertypes(p.getTypeEnv())) {
						IJavaDeclaredType sdt = (IJavaDeclaredType) st;
						if (getValueObjectDrop(sdt.getDeclaration()) != null) {
							getContext().reportError(dt.getDeclaration(), I18N.res(756, VALUE_OBJECT));
							return false;
						}
					}
					return true;
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
			return new AbstractAASTScrubber<RefObjectNode, RefObjectPromiseDrop>(this, ScrubberType.INCLUDE_SUBTYPES_BY_HIERARCHY) {
				@Override
				protected PromiseDrop<RefObjectNode> makePromiseDrop(RefObjectNode a) {
					// Check consistency
					final IRNode tdecl = a.getPromisedFor();
					final boolean isInterface = TypeUtil.isInterface(tdecl);
					
					final RefObjectPromiseDrop d = new RefObjectPromiseDrop(a);			
					if (isInterface) {
						makeResultDrop(tdecl, d, true, 755, JavaNames.getTypeName(tdecl));
					} else {
						computeResults(a.getPromisedFor(), d, false);
					}
					return storeDropIfNotNull(a, d);
				}
				
				@Override 
			    protected final boolean processUnannotatedType(final IJavaSourceRefType dt) {
					// Check if superclass has this annotation
					final IIRProject p = JavaProjects.getEnclosingProject(dt.getDeclaration());	
					for(IJavaType st : dt.getSupertypes(p.getTypeEnv())) {
						IJavaDeclaredType sdt = (IJavaDeclaredType) st;
						if (getRefObjectDrop(sdt.getDeclaration()) != null) {
							getContext().reportError(dt.getDeclaration(), I18N.res(756, REF_OBJECT));
							return false;
						}
					}
					return true;
				}
			};
		}
	}

	static void makeResultDrop(IRNode decl, PromiseDrop<?> p, boolean consistent, int num, Object... args) {
		final ResultDrop r = new ResultDrop(decl);
		r.setResultMessage(num, args);
		r.addCheckedPromise(p);
		if (consistent) {
			r.setConsistent();
		} else {
			r.setInconsistent();
		}
	}
	
  static void computeResults(IRNode tdecl, final PromiseDrop<?> d, boolean ifOverrides) {
    final IRNode hashCode = findNonObjectImpl(tdecl, HASHCODE);
    if (hashCode != null) {
      makeResultDrop(tdecl, d, ifOverrides, OVERRIDES, HASHCODE, "", JavaNames.getFullName(hashCode));
    } else {
      makeResultDrop(tdecl, d, !ifOverrides, INHERITS, HASHCODE, "");
    }
    final IRNode equals = findSingleObjectArgImpl(tdecl, EQUALS);
    if (equals != null) {
      makeResultDrop(tdecl, d, ifOverrides, OVERRIDES, EQUALS, "Object", JavaNames.getFullName(equals));
    } else {
      makeResultDrop(tdecl, d, !ifOverrides, INHERITS, EQUALS, "Object");
    }
  }
	
	static final String HASHCODE = "hashCode";
	static final String EQUALS = "equals";
	static final int OVERRIDES = 750;
	static final int INHERITS = 751;
	
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
