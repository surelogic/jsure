package com.surelogic.annotation.rules;

import java.util.Iterator;

import com.surelogic.aast.AnnotationOrigin;
import com.surelogic.aast.promise.RefObjectNode;
import com.surelogic.aast.promise.ValueObjectNode;
import com.surelogic.analysis.IIRProject;
import com.surelogic.annotation.SimpleBooleanAnnotationParseRule;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.IAnnotationTraversalCallback;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.common.SLUtility;
import com.surelogic.common.i18n.I18N;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.RefObjectPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ValueObjectPromiseDrop;
import com.surelogic.javac.Projects;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.AbstractSuperTypeSearchStrategy;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.AnnotationDeclaration;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.EnumDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class EqualityRules extends AnnotationRules {
  private static final int OVERRIDES = 750;
  private static final int INHERITS = 751;
  private static final int CONFLICTS_WITH = 752;
  private static final int SHOULD_BE_ABSTRACT = 753;
  private static final int TRIVIALLY_VALUE_INTERFACE = 754;
  private static final int TRIVIALLY_REF = 755;
  private static final int DOES_NOT_MATCH_SUPERTYPE = 756;
  private static final int TRIVIALLY_VALUE_ABSTRACT = 757;
  private static final int BAD_ENUM = 763;
  private static final int MAY_NOT_IMPLEMENT_VAL_OBJECT = 765;
  
	public static final String VALUE_OBJECT = "ValueObject";
	public static final String REF_OBJECT = "ReferenceObject";
	 
  private static final String HASHCODE = "hashCode";
  private static final String EQUALS = "equals";
  
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
        protected final ValueObjectPromiseDrop makePromiseDrop(
            IAnnotationTraversalCallback<ValueObjectNode> cb,
            ValueObjectNode a, boolean isAssumption) {
          final ValueObjectPromiseDrop originalPromiseDrop = 
              makePromiseDrop(a, isAssumption);
          if (originalPromiseDrop != null) {
            final IRNode promisedFor = a.getPromisedFor();
            /* Add derived annotations to any AnonClassExpression that extends
             * from this class.  Do not add to EnumConstantClassDeclaration
             * nodes because enums are already assumed to be to reference object
             * and thus cannot also be value object.
             */
            for (final IRNode sub : getContext().getBinder(promisedFor).getTypeEnvironment().getRawSubclasses(promisedFor)) {
              final Operator subOp = JJNode.tree.getOperator(sub);
              if (AnonClassExpression.prototype.includes(subOp)) {
                final ValueObjectNode derived = new ValueObjectNode();
                derived.copyPromisedForContext(sub, a, AnnotationOrigin.SCOPED_ON_TYPE);
                cb.addDerived(derived, originalPromiseDrop);
              }
            }
          }
          return originalPromiseDrop;
        }

				@Override
				protected ValueObjectPromiseDrop makePromiseDrop(ValueObjectNode a, boolean isAssumption) {
					// Check consistency
					final IRNode tdecl = a.getPromisedFor();
					final Operator op = JJNode.tree.getOperator(tdecl);
					
					if (AnnotationDeclaration.prototype.includes(op)) {
					  getContext().reportModelingProblem(a, "Annotation declarations may not be annotated @ValueObject");
					  return null;
					}
					
					// Check if the class is java.lang.Enum
					if (EnumDeclaration.prototype.includes(op)) {
            getContext().reportModelingProblem(a, I18N.res(BAD_ENUM, VALUE_OBJECT));
            return null;
					}
					
					if (getRefObjectDrop(tdecl) != null) {
						// Conflict w/ RefObject
						getContext().reportModelingProblem(a, I18N.res(CONFLICTS_WITH, REF_OBJECT));
						return null;
					}
					// Check if abstract or has no subclasses
					final boolean isInterface = InterfaceDeclaration.prototype.includes(op);
					final boolean isAbstract = TypeUtil.isAbstract(tdecl);
					if (!isInterface && !isAbstract) {
						final IIRProject p = Projects.getEnclosingProject(tdecl);					
						Iterator<IRNode> it = p.getTypeEnv().getRawSubclasses(tdecl).iterator();
						if (it.hasNext()) {
							getContext().reportModelingProblem(a, I18N.res(SHOULD_BE_ABSTRACT, JavaNames.getRelativeTypeNameDotSep(it.next())));
							return null;
						}
					}
					
					final ValueObjectPromiseDrop d = new ValueObjectPromiseDrop(a);
					if (isInterface) {
						makeResultDrop(tdecl, d, true, TRIVIALLY_VALUE_INTERFACE, JavaNames.getRelativeTypeNameDotSep(tdecl));
					} else if (isAbstract) {
						makeResultDrop(tdecl, d, true, TRIVIALLY_VALUE_ABSTRACT, JavaNames.getRelativeTypeNameDotSep(tdecl));
					} else {
						computeResults(a.getPromisedFor(), d, true);
					}
					return storeDropIfNotNull(a, d);
				}
				
				@Override 
			    protected final boolean processUnannotatedType(final IJavaSourceRefType dt) {
					// Check if superclass has this annotation
					final IIRProject p = Projects.getEnclosingProject(dt.getDeclaration());	
					for(IJavaType st : dt.getSupertypes(p.getTypeEnv())) {
						IJavaDeclaredType sdt = (IJavaDeclaredType) st;
						if (getValueObjectDrop(sdt.getDeclaration()) != null) {
						  final String msg;
						  if (EnumDeclaration.prototype.includes(dt.getDeclaration())) {
						    msg = I18N.res(MAY_NOT_IMPLEMENT_VAL_OBJECT);
						  } else {
						    msg = I18N.res(DOES_NOT_MATCH_SUPERTYPE, VALUE_OBJECT);
						  }
							getContext().reportModelingProblem(dt.getDeclaration(), msg);
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
			  protected final RefObjectPromiseDrop makePromiseDrop(
			      IAnnotationTraversalCallback<RefObjectNode> cb,
			      RefObjectNode a, boolean isAssumption) {
			    final RefObjectPromiseDrop originalPromiseDrop = 
			        makePromiseDrop(a, isAssumption);
			    if (originalPromiseDrop != null) {
		        final IRNode promisedFor = a.getPromisedFor();
			      /* Add derived annotations to any AnonClassExpression that extends
			       * from this class.  Do not add to EnumConstantClassDeclaration
			       * nodes because enums are handled specially and already 
			       * assumed to be reference only.
			       */
			      for (final IRNode sub : getContext().getBinder(promisedFor).getTypeEnvironment().getRawSubclasses(promisedFor)) {
	            final Operator subOp = JJNode.tree.getOperator(sub);
	            if (AnonClassExpression.prototype.includes(subOp)) {
	              final RefObjectNode derived = new RefObjectNode();
	              derived.copyPromisedForContext(sub, a, AnnotationOrigin.SCOPED_ON_TYPE);
	              cb.addDerived(derived, originalPromiseDrop);
	            }
			      }
			    }
			    return originalPromiseDrop;
			  }

				@Override
				protected RefObjectPromiseDrop makePromiseDrop(RefObjectNode a, boolean isAssumption) {
					// Check consistency
					final IRNode tdecl = a.getPromisedFor();
					final Operator op = JJNode.tree.getOperator(tdecl);
          
          if (AnnotationDeclaration.prototype.includes(op)) {
            getContext().reportModelingProblem(a, "Annotation declarations may not be annotated @ReferenceObject");
            return null;
          }
          
          // Check if the class is java.lang.Enum
          if (EnumDeclaration.prototype.includes(op)) {
            getContext().reportModelingProblem(a, I18N.res(BAD_ENUM, REF_OBJECT));
            return null;
          }
					
					final RefObjectPromiseDrop d = new RefObjectPromiseDrop(a);			
					if (InterfaceDeclaration.prototype.includes(op)) {
						makeResultDrop(tdecl, d, true, TRIVIALLY_REF, JavaNames.getTypeName(tdecl));
					} else {
						computeResults(a.getPromisedFor(), d, false);
					}
					return storeDropIfNotNull(a, d);
				}
				
				@Override 
			    protected final boolean processUnannotatedType(final IJavaSourceRefType dt) {
					/* Check if superclass has this annotation.  Only check if the 
					 * class is not an enumeration: enums are implicitly @ReferenceObject.
					 */
				  if (!EnumDeclaration.prototype.includes(dt.getDeclaration())) {
  					final IIRProject p = Projects.getEnclosingProject(dt.getDeclaration());	
  					for(IJavaType st : dt.getSupertypes(p.getTypeEnv())) {
  						IJavaDeclaredType sdt = (IJavaDeclaredType) st;
  						if (getRefObjectDrop(sdt.getDeclaration()) != null) {
  							getContext().reportModelingProblem(dt.getDeclaration(), I18N.res(DOES_NOT_MATCH_SUPERTYPE, REF_OBJECT));
  							return false;
  						}
  					}
				  }
					return true;
				}
			};
		}
	}

	static void makeResultDrop(IRNode decl, PromiseDrop<?> p, boolean consistent, int num, Object... args) {
		final ResultDrop r = new ResultDrop(decl);
		r.setMessage(num, args);
		r.addChecked(p);
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

	/**
	 * Look for a no-arg method with the given name in this class or its superclasses
	 * (not counting java.lang.Object)
	 */
	static IRNode findNonObjectImpl(final IRNode tdecl, final String methodName) {
		final IIRProject p = Projects.getEnclosingProject(tdecl);
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
		final IIRProject p = Projects.getEnclosingProject(tdecl);
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
			if (SLUtility.JAVA_LANG_OBJECT.equals(tName)) {
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
