package com.surelogic.annotation.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.runtime.RecognitionException;

import com.surelogic.Part;
import com.surelogic.aast.AASTStatus;
import com.surelogic.aast.AnnotationOrigin;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.bind.ILockBinding;
import com.surelogic.aast.bind.IRegionBinding;
import com.surelogic.aast.bind.ISourceRefType;
import com.surelogic.aast.bind.IType;
import com.surelogic.aast.bind.IVariableBinding;
import com.surelogic.aast.java.ExpressionNode;
import com.surelogic.aast.java.FieldRefNode;
import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.java.QualifiedThisExpressionNode;
import com.surelogic.aast.java.ThisExpressionNode;
import com.surelogic.aast.java.TypeExpressionNode;
import com.surelogic.aast.java.VariableUseExpressionNode;
import com.surelogic.aast.promise.AbstractBooleanNode;
import com.surelogic.aast.promise.AbstractLockDeclarationNode;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.aast.promise.AnnotationBoundsNode.BoundsVisitor;
import com.surelogic.aast.promise.ClassLockExpressionNode;
import com.surelogic.aast.promise.ContainableNode;
import com.surelogic.aast.promise.ImmutableNode;
import com.surelogic.aast.promise.JUCLockNode;
import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.aast.promise.LockNameNode;
import com.surelogic.aast.promise.LockSpecificationNode;
import com.surelogic.aast.promise.MutableNode;
import com.surelogic.aast.promise.NotContainableNode;
import com.surelogic.aast.promise.NotThreadSafeNode;
import com.surelogic.aast.promise.PolicyLockDeclarationNode;
import com.surelogic.aast.promise.ProhibitsLockNode;
import com.surelogic.aast.promise.QualifiedLockNameNode;
import com.surelogic.aast.promise.ReadLockNode;
import com.surelogic.aast.promise.RegionNameNode;
import com.surelogic.aast.promise.RequiresLockNode;
import com.surelogic.aast.promise.ReturnsLockNode;
import com.surelogic.aast.promise.SimpleLockNameNode;
import com.surelogic.aast.promise.ThreadConfinedNode;
import com.surelogic.aast.promise.ThreadSafeNode;
import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.aast.promise.WriteLockNode;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.concurrency.heldlocks.FieldKind;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.AnnotationLocation;
import com.surelogic.annotation.DefaultSLAnnotationParseRule;
import com.surelogic.annotation.IAnnotationParsingContext;
import com.surelogic.annotation.MarkerAnnotationParseRule;
import com.surelogic.annotation.ParseResult;
import com.surelogic.annotation.SimpleBooleanAnnotationParseRule;
import com.surelogic.annotation.parse.SLAnnotationsParser;
import com.surelogic.annotation.scrub.AASTStore;
import com.surelogic.annotation.scrub.AbstractAASTScrubber;
import com.surelogic.annotation.scrub.IAnnotationScrubber;
import com.surelogic.annotation.scrub.AnnotationScrubberContext;
import com.surelogic.annotation.scrub.IAnnotationTraversalCallback;
import com.surelogic.annotation.scrub.ScrubberType;
import com.surelogic.annotation.scrub.SimpleScrubber;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.ModelingProblemDrop;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.VouchFieldIsPromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;
import com.surelogic.dropsea.ir.drops.locks.ProhibitsLockPromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.RequiresLockPromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.ReturnsLockPromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.ThreadConfinedPromiseDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.AnnotationBoundsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ContainablePromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ImmutablePromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.MutablePromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.NotContainablePromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.NotThreadSafePromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ThreadSafePromiseDrop;
import com.surelogic.javac.Projects;
import com.surelogic.java.persistence.JSureScanInfo;
import com.surelogic.promise.BooleanPromiseDropStorage;
import com.surelogic.promise.IPromiseDropStorage;
import com.surelogic.promise.PromiseDropSeqStorage;
import com.surelogic.promise.SinglePromiseDropStorage;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IBinding;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.bind.PromiseFramework;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.Visibility;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

public class LockRules extends AnnotationRules {
  private static final String JAVA_LANG_ENUM = "java.lang.Enum";

  public static final String LOCK = "RegionLock";
//	private static final String IS_LOCK = "IsLock";
	public static final String REQUIRES_LOCK = "RequiresLock";
	private static final String RETURNS_LOCK = "ReturnsLock";
	private static final String PROHIBITS_LOCK = "ProhibitsLock";
	private static final String POLICY_LOCK = "PolicyLock";
  public static final String CONTAINABLE = "Containable";
  public static final String THREAD_SAFE = "ThreadSafe";
  private static final String NOT_THREAD_SAFE = "NotThreadSafe";
  private static final String NOT_CONTAINABLE = "NotContainable";
  private static final String MUTABLE = "Mutable";
  public static final String IMMUTABLE = "Immutable";
  private static final String LOCK_FIELD_VISIBILITY = "LockFieldVisibility";
  private static final String REGION_INITIALIZER = "Region Initializer";
  public static final String VOUCH_FIELD_IS = "Vouch Field Is";
  public static final String ANNO_BOUNDS = "AnnotationBounds";
  public static final String THREAD_CONFINED = "ThreadConfined";
  
  public static final String IMMUTABLE_PROP = "immutable";
  public static final String CONTAINABLE_PROP = "containable";
  public static final String REFERENCE_PROP = "referenceObject";
  public static final String THREAD_SAFE_PROP = "threadSafe";
  public static final String VALUE_PROP = "valueObject";
  
	private static final AnnotationRules instance = new LockRules();

	private static final IProtectedRegions protectedRegions = new ProtectedRegions();
	
	private static final InitRegionSet initRegionSet = new InitRegionSet(protectedRegions);
  private static final Lock_ParseRule lockRule = new Lock_ParseRule(protectedRegions);
	private static final PolicyLock_ParseRule policyRule = new PolicyLock_ParseRule();
	private static final RequiresLock_ParseRule requiresLockRule = new RequiresLock_ParseRule();
	private static final ReturnsLock_ParseRule returnsLockRule = new ReturnsLock_ParseRule();
	//private static final ProhibitsLock_ParseRule prohibitsLockRule = new ProhibitsLock_ParseRule();
	private static final AnnotationBounds_ParseRule annoBoundsRule = new AnnotationBounds_ParseRule();
  private static final Containable_ParseRule containableRule = new Containable_ParseRule();
  private static final ThreadSafe_ParseRule threadSafeRule = new ThreadSafe_ParseRule();
  private static final NotThreadSafe_ParseRule notThreadSafeRule = new NotThreadSafe_ParseRule();
  private static final ImmutableParseRule immutableRule = new ImmutableParseRule();
  private static final Mutable_ParseRule mutableRule = new Mutable_ParseRule();
  private static final NotContainable_ParseRule notContainableRule = new NotContainable_ParseRule();
  private static final VouchFieldIs_ParseRule vouchFieldIsRule = new VouchFieldIs_ParseRule();
  private static final ThreadConfined_ParseRule threadConfinedRule = new ThreadConfined_ParseRule();
  
  private interface IProtectedRegions {
	  void clear();
	  boolean addIfNotAlreadyProtected(ITypeEnvironment tenv, String qualifiedRegionName,
		        final IJavaDeclaredType clazz);
  }
  
  private static class ProtectedRegions implements IProtectedRegions {
	private final Map<String,IProtectedRegions> projects = 
		new HashMap<String,IProtectedRegions>();
	  
	@Override
  public boolean addIfNotAlreadyProtected(ITypeEnvironment tenv, 
			String qualifiedRegionName, IJavaDeclaredType clazz) {
		final IIRProject p = Projects.getEnclosingProject(clazz.getDeclaration());
		IProtectedRegions state = projects.get(p.getName());
		if (state == null) {
			state = new Project_ProtectedRegions();
			projects.put(p.getName(), state);
		}
		return state.addIfNotAlreadyProtected(tenv, qualifiedRegionName, clazz);
	}

	@Override
  public void clear() {
		projects.clear();
	}
  }
  
  private static class Project_ProtectedRegions implements IProtectedRegions {
    private final Map<String, Set<IJavaType>> protectedRegions = new HashMap<String, Set<IJavaType>>();
    
    @Override
    public synchronized void clear() {
      protectedRegions.clear();
    }
    
    private Set<IJavaType> getClassSet(final String key) {
      Set<IJavaType> classSet = protectedRegions.get(key);
      if (classSet == null) {
        classSet = new HashSet<IJavaType>();
        protectedRegions.put(key, classSet);
      }
      return classSet;
    }
       
    /**
     * @return <code>true</code> if the region was not already protected for 
     * this class.
     */
    @Override
    public synchronized boolean addIfNotAlreadyProtected(
        final ITypeEnvironment tenv, final String qualifiedRegionName,
        final IJavaDeclaredType clazz) {
      final Set<IJavaType> classSet = getClassSet(qualifiedRegionName);
      for (final IJavaType other : classSet) {
        if (clazz.isSubtype(tenv, other) || other.isSubtype(tenv, clazz)) {
          return false;
        }
      }
      classSet.add(clazz);
      return true;
    }
  }
  
  
	public static AnnotationRules getInstance() {
		return instance;
	}

	
	
	public static Iterable<LockModel> getModels(IRNode type) {
		return getDrops(lockRule.getStorage(), type);
	}

	public static RequiresLockPromiseDrop getRequiresLock(IRNode vdecl) {
		return getDrop(requiresLockRule.getStorage(), vdecl);
	}

	public static ReturnsLockPromiseDrop getReturnsLock(IRNode vdecl) {
    ReturnsLockPromiseDrop rv = getDrop(returnsLockRule.getStorage(), vdecl);
    if (rv == null) {
      return rv;
    }
    return rv;
	}

	public static AnnotationBoundsPromiseDrop getAnnotationBounds(final IRNode tDecl) {
	  return getBooleanDrop(annoBoundsRule.getStorage(), tDecl);
	}
	
	
	
  private static <N extends AbstractModifiedBooleanNode, A extends ModifiedBooleanPromiseDrop<N>> A getX_Type(final IPromiseDropStorage<A> storage, final IRNode tdecl) {
	  final A drop = getBooleanDrop(storage, tdecl);
    if (drop == null) {
      return null;
    } else {
      return drop.isImplementationOnly() ? null : drop;
    }
	}

  
  
  /**
   * A type is containable if it is annotated as containable, and containability
   * applies to the instance part of the type.
   */
  public static boolean isContainableType(final IRNode cdecl) {
    final ContainablePromiseDrop containableType = getContainableType(cdecl);
    return containableType != null && containableType.getAppliesTo() != Part.Static;
  }
  
  /**
   * Return whether the type is thread safe.  Only returns the promise drop if
   * the drop is not implementation only.
   * @param cdecl
   * @return
   */
  public static ContainablePromiseDrop getContainableType(final IRNode cdecl) {
    return getX_Type(containableRule.getStorage(), cdecl);
  }
  
  /**
   * Get whether the implementation is containable.  Returns the promise
   * drop whether or not it is implementation-only.
   */
  public static ContainablePromiseDrop getContainableImplementation(final IRNode cdecl) {
    return getBooleanDrop(containableRule.getStorage(), cdecl);
  }
  
  public static NotContainablePromiseDrop getNotContainable(IRNode cdecl) {
    return getBooleanDrop(notContainableRule.getStorage(), cdecl);
  }
  
  
  
  /**
   * A type is threadsafe if it is annotated as threadsafe, and threadsafety
   * applies to the instance part of the type.
   */
  public static boolean isThreadSafeType(final IRNode cdecl) {
    final ThreadSafePromiseDrop threadSafeType = getThreadSafeType(cdecl);
    return threadSafeType != null && threadSafeType.getAppliesTo() != Part.Static;
  }

  /**
   * Return whether the type is thread safe.  Only returns the promise drop if
   * the drop is not implementation only.
   * @param cdecl
   * @return
   */
  public static ThreadSafePromiseDrop getThreadSafeType(final IRNode cdecl) {
    return getX_Type(threadSafeRule.getStorage(), cdecl);
  }

  /**
   * Get whether the implementation is thread safe.  Returns the promise
   * drop whether or not it is implementation-only.
   */
  public static ThreadSafePromiseDrop getThreadSafeImplementation(final IRNode cdecl) {
    return getBooleanDrop(threadSafeRule.getStorage(), cdecl);
  }

  public static NotThreadSafePromiseDrop getNotThreadSafe(IRNode cdecl) {
	  return getBooleanDrop(notThreadSafeRule.getStorage(), cdecl);
  }

  
  
  /**
   * A type is immutable if it is annotated as immutable, and immutability
   * applies to the instance part of the type.
   */
  public static boolean isImmutableType(final IRNode cdecl) {
    final ImmutablePromiseDrop immutableType = getImmutableType(cdecl);
    return immutableType != null && immutableType.getAppliesTo() != Part.Static;
  }
  
  /**
   * Return whether the type is immutable.  Only returns the promise drop if
   * the drop is not implementation only.
   * @param cdecl
   * @return
   */
  public static ImmutablePromiseDrop getImmutableType(final IRNode cdecl) {
    return getX_Type(immutableRule.getStorage(), cdecl);
  }
  
  /**
   * Get whether the implementation is immutable.  Returns the promise
   * drop whether or not it is implementation-only.
   */
  public static ImmutablePromiseDrop getImmutableImplementation(final IRNode cdecl) {
    return getBooleanDrop(immutableRule.getStorage(), cdecl);
  }
  
  public static MutablePromiseDrop getMutable(IRNode cdecl) {
    return getBooleanDrop(mutableRule.getStorage(), cdecl);
  }
  
  
  
  /**
   * Is the type threadsafe, that is it annotated with ThreadSafe or 
   * Immutable?
   * @see isThreadSafeType
   * @see isImmutableType
   */
  public static boolean isThreadSafe(final IRNode tdecl) {
    return isImmutableType(tdecl) || isThreadSafeType(tdecl);
  }
  
  /**
   * Get the most specific promise that allows us to consider this type
   * to be thread safe.  If the class is annotated as Immutable, then the
   * immutable promise is returned in preference to any ThreadSafe annotation
   * that may be present.
   * @see getThreadSafeType
   * @see getImmutableType
   */
  public static ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> getThreadSafeTypePromise(final IRNode tdecl) {
    final ImmutablePromiseDrop immutable = getImmutableType(tdecl);
    return (immutable == null) ? getThreadSafeType(tdecl) : immutable;
  }
  
  /**
   * Get the most specific promise that allows us to consider this implementation
   * to be thread safe.  If the class is annotated as Immutable, then the
   * immutable promise is returned in preference to any ThreadSafe annotation
   * that may be present.  Differs from {@link #getThreadSafeTypePromise} by
   * considering implementation-only annotations.
   * @see getThreadSafeImplementation
   * @see getImmutableImplementation
   */
  public static ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> getThreadSafeImplPromise(final IRNode tdecl) {
    final ImmutablePromiseDrop immutable = getImmutableImplementation(tdecl);
    return (immutable == null) ? getThreadSafeImplementation(tdecl) : immutable;
  }
  
  /**
   * Finds the enclosing declaration from a Type node
   * (either VarDecl or ParamDecl
   */
  public static VouchFieldIsPromiseDrop findVouchFieldIsDrop(final IRNode type) {
	  // Find the first non-Type node
	  IRNode here = type;
	  Operator op = JJNode.tree.getOperator(here);
	  while (Type.prototype.includes(op)) {
		  here = JJNode.tree.getParentOrNull(here);
		  op = JJNode.tree.getOperator(here);
	  }
	  if (VariableDeclList.prototype.includes(op)) {
		  // Actually on the variable declarator
		  IRNode decls = VariableDeclList.getVars(here);
		  IRNode decl  = VariableDeclarators.getVar(decls, 0);
		  return getVouchFieldIs(decl);
	  }
	  return getVouchFieldIs(here);
  }
  
  // The drop is actually on the variable declarator
  public static VouchFieldIsPromiseDrop getVouchFieldIs(final IRNode fieldDecl) {
    return getDrop(vouchFieldIsRule.getStorage(), fieldDecl);
  }
  
  public static ThreadConfinedPromiseDrop getThreadConfinedDrop(final IRNode fieldDecl) {
	    return getBooleanDrop(threadConfinedRule.getStorage(), fieldDecl);
  }
  
  @Override
  public void register(PromiseFramework fw) {
    registerScrubber(fw, initRegionSet);
		registerParseRuleStorage(fw, policyRule);
		registerParseRuleStorage(fw, lockRule);
		registerParseRuleStorage(fw, requiresLockRule);
		registerParseRuleStorage(fw, returnsLockRule);
		//registerParseRuleStorage(fw, prohibitsLockRule);
		registerParseRuleStorage(fw, annoBoundsRule);
    registerParseRuleStorage(fw, containableRule);
    registerParseRuleStorage(fw, threadSafeRule);
    registerParseRuleStorage(fw, notThreadSafeRule);
    registerParseRuleStorage(fw, immutableRule);
    registerParseRuleStorage(fw, mutableRule);
    registerParseRuleStorage(fw, notContainableRule);
    registerParseRuleStorage(fw, vouchFieldIsRule);
    registerParseRuleStorage(fw, threadConfinedRule);
    registerScrubber(fw, new LockFieldVisibilityScrubber());
	}

  /**
	 * Parse rule for the ReturnsLock annotation
	 * 
	 * @author ethan
	 */
	public static class ReturnsLock_ParseRule
			extends
			DefaultSLAnnotationParseRule<ReturnsLockNode, ReturnsLockPromiseDrop> {

		protected ReturnsLock_ParseRule() {
			super(RETURNS_LOCK, methodDeclOp, ReturnsLockNode.class, AnnotationLocation.RETURN_VAL);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.surelogic.annotation.DefaultSLAnnotationParseRule#parse(com.surelogic.annotation.IAnnotationParsingContext,
		 *      com.surelogic.annotation.parse.SLAnnotationsParser)
		 */
		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.returnsLock().getTree();
		}


		@Override
		protected IPromiseDropStorage<ReturnsLockPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					ReturnsLockPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ReturnsLockNode, ReturnsLockPromiseDrop>(this,
					ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY, LOCK, POLICY_LOCK) {
				@Override
        protected PromiseDrop<ReturnsLockNode> makePromiseDrop(
						ReturnsLockNode a) {
					return storeDropIfNotNull(a, scrubReturnsLock(getContext(), a));
				}
        
        @Override
        protected boolean processUnannotatedMethodRelatedDecl(
            final IRNode returnDecl) {
          /* If any of the immediate ancestors are annotated, then we have 
           * an error because unannotated is the same as saying we don't return
           * any particular lock.  Violate covariance.
           */
          boolean good = true;
          final IRNode mdecl = JavaPromise.getPromisedFor(returnDecl);
          for (final IBinding pBinding : getContext().getBinder(mdecl).findOverriddenParentMethods(mdecl)) {
            final IRNode parent = pBinding.getNode();
            final IRNode parentReturn = JavaPromise.getReturnNode(parent);
            final ReturnsLockPromiseDrop superDrop = getReturnsLock(parentReturn);
            if (superDrop != null && !superDrop.isAssumed()) {
              // Ancestor is annotated
              good = false;
              getContext().reportModelingProblem(mdecl,
                  "Method does not return same lock as the method it overrides: {0}",
                  JavaNames.genRelativeFunctionName(parent));
            }
          }
          return good;
        }
			};
		}

	}

	/**
	 * The scrubbing code for the ReturnsLock annotation Copied from
	 * {@link LockAnnotation} ReturnsLock_ParseRules and modified to work with
	 * the new system.
	 */
	private static ReturnsLockPromiseDrop scrubReturnsLock(
			AnnotationScrubberContext context, ReturnsLockNode node) {
		final IRNode returnNode = node.getPromisedFor();
    final IRNode annotatedMethod = JavaPromise.getPromisedFor(returnNode);
    final LockNameNode lockName = node.getLock();

    boolean okay = false;
		LockModel lockDecl = null; 
		if (lockName != null) {
			/*
			 * >> LocksOK << --- Lock name is bindable
			 */
			// Check if the lock name is good; if not we cannot continue
			lockDecl = isLockNameOkay(
			    JavaNode.getModifier(annotatedMethod, JavaNode.STATIC),
			    lockName, context, TypeUtil.isBinary(annotatedMethod));
			okay = (lockDecl != null);
		}
		
		/* Check consistency with ancestors */
		if (okay) {
      for (final IBinding pBinding : context.getBinder(annotatedMethod).findOverriddenParentMethods(annotatedMethod)) {
        final IRNode parent = pBinding.getNode();
        final IRNode parentReturn = JavaPromise.getReturnNode(parent);
        final ReturnsLockPromiseDrop superDrop = getReturnsLock(parentReturn);
        /* Okay is the ancestor is not annotated, because that means it has
         * no declared behavior.  Covariance allows us to add a new restriction.
         */
        if (superDrop != null) {
          // Ancestor is annotated: must match
          
          final Map<IRNode, Integer> positionMap =
            buildParameterMap(annotatedMethod, parent);
          
          final LockNameNode superLock = superDrop.getAAST().getLock();
          if (!lockName.namesSameLockAs(superLock, positionMap, LockSpecificationNode.How.COVARIANT)) {
            okay = false;
            context.reportModelingProblem(annotatedMethod,
                "Method does not return same lock as the method it overrides: {0}",
                JavaNames.genRelativeFunctionName(parent));
          }
        }
      }
		}
		
    if (okay) {
      final ReturnsLockPromiseDrop returnDrop = new ReturnsLockPromiseDrop(node);
      lockDecl.addDependent(returnDrop);
      return returnDrop;
    } else {
      return null;
    }
	}

  public static class ProhibitsLock_ParseRule
	extends
	DefaultSLAnnotationParseRule<ProhibitsLockNode, ProhibitsLockPromiseDrop> {
		public ProhibitsLock_ParseRule() {
			super(PROHIBITS_LOCK, functionDeclOps, ProhibitsLockNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.requiresLock().getTree();
		}

		@Override
		protected IPromiseDropStorage<ProhibitsLockPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					ProhibitsLockPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<ProhibitsLockNode, ProhibitsLockPromiseDrop>(this,
					ScrubberType.UNORDERED, LOCK,
					POLICY_LOCK, RETURNS_LOCK, REQUIRES_LOCK) {
				@Override
				protected PromiseDrop<ProhibitsLockNode> makePromiseDrop(
						ProhibitsLockNode a) {
					return storeDropIfNotNull(a, scrubProhibitsLock(getContext(), a));

				}
			};
		}
	}
	
	/**
	 * Scrubbing code for the ProhibitsLock annotation. If the annotation is good, return a new {@link RequiresLockPromiseDrop}, otherwise
	 * return <code>null</code>
	 * 
	 * @param context
	 * @param node The ProhibitsLockNode created by the parser
	 * @return A new ProhibitsLockPromiseDrop if it is a valid annotation, null otherwise
	 */
	private static ProhibitsLockPromiseDrop scrubProhibitsLock(
			AnnotationScrubberContext context, ProhibitsLockNode node) {
		// TODO what else?
		return new ProhibitsLockPromiseDrop(node);
	}
	
	/**
	 * Parse rule for the @RequiresLock annotation
	 * 
	 * @author ethan
	 */
	public static class RequiresLock_ParseRule
			extends
			DefaultSLAnnotationParseRule<RequiresLockNode, RequiresLockPromiseDrop> {

		public RequiresLock_ParseRule() {
			super(REQUIRES_LOCK, functionDeclOps, RequiresLockNode.class);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.surelogic.annotation.DefaultSLAnnotationParseRule#parse(com.surelogic.annotation.IAnnotationParsingContext,
		 *      com.surelogic.annotation.parse.SLAnnotationsParser)
		 */
		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.requiresLock().getTree();
		}


		@Override
		protected IPromiseDropStorage<RequiresLockPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(),
					RequiresLockPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
		  /* Order by hierarchy so that we know that any ancestral RequiresLock
		   * annotations are scrubbed.
		   */
			return new AbstractAASTScrubber<RequiresLockNode, RequiresLockPromiseDrop>(this,
					ScrubberType.INCLUDE_OVERRIDDEN_METHODS_BY_HIERARCHY, LOCK,
					POLICY_LOCK, RETURNS_LOCK) {
				@Override
        protected PromiseDrop<RequiresLockNode> makePromiseDrop(
						RequiresLockNode a, boolean isAssumption) {
					return storeDropIfNotNull(a, scrubRequiresLock(getContext(), a, isAssumption));
				}
        
        @Override
        protected boolean processUnannotatedMethodRelatedDecl(
            final IRNode methodDecl) {
          /* Always good to be unannotated, because we are contravariant.
           * (Problem case is adding annotations.)
           */
          return true;
        }
			};
		}
	}

	/**
	 * Scrubbing code for the RequiresLock annotation. If the annotation is good, return a new {@link RequiresLockPromiseDrop}, otherwise
	 * return <code>null</code>
	 * 
	 * @param context
	 * @param node The RequiresLockNode created by the parser
	 * @return A new RequiresLockPromise drop if it is a valid annotation, null otherwise
	 */
	static RequiresLockPromiseDrop scrubRequiresLock(
			AnnotationScrubberContext context, RequiresLockNode node, final boolean isAssumption) {
		final IRNode annotatedMethod = node.getPromisedFor();
		final Operator promisedForOp = JJNode.tree.getOperator(annotatedMethod);

		boolean allGood = true; // assume the best
		final Set<LockSpecificationNode> uniqueLocks = new HashSet<LockSpecificationNode>();
		final List<LockSpecificationNode> locks = node.getLockList();
    final List<LockModel> lockDecls = new ArrayList<LockModel>();
		for (LockSpecificationNode lockSpec : locks) {
		  final LockNameNode lockName = lockSpec.getLock();
		  final boolean namesReadWriteLock = lockSpec instanceof JUCLockNode;
			boolean currentGood = true;

			/*
			 * >> LocksOK << --- Lock name is bindable
			 */
			// If the lock name is bad we cannot continue
			final LockModel lockModel =
			  isLockNameOkay(TypeUtil.isStatic(annotatedMethod),
			      lockName, context, TypeUtil.isBinary(annotatedMethod));
			if (lockModel != null) {
				lockDecls.add(lockModel);

				if (namesReadWriteLock) {
	        /*
	         * We can only name a read lock or a write lock if the underlying
	         * lock is a ReadWriteLock.
	         */
				  if (!lockModel.getAAST().isReadWriteLock()) {
				    currentGood = false;
				    context.reportError(
				        "Lock is not a ReadWriteLock: cannot require the read or write lock",
				        lockSpec);
				  }
				} else {
  				/*
  				 * We cannot require a ReadWriteLock directly, only its
  				 * component read/write locks.
  				 */
  				if (lockModel.getAAST().isReadWriteLock()) {
  					currentGood = false;
  					context.reportError(
  					    "Cannot require a ReadWriteLock: must require either the read or the write lock",
  							lockSpec);
  				}
				}
				
				/*
				 * We need to know if the lock name references "this" either
				 * implicitly or explicitly. This is used for two tests below.
				 */
				final boolean lockRefsThis;
				if (lockName instanceof QualifiedLockNameNode) {
					QualifiedLockNameNode qLockName = (QualifiedLockNameNode) lockName;
					final ExpressionNode expression = qLockName.getBase();
					lockRefsThis = (expression instanceof ThisExpressionNode);
				} else if (lockName instanceof SimpleLockNameNode) {
					lockRefsThis = !lockModel.getAAST().isLockStatic();
				} else {
					// Shouldn't get here
					lockRefsThis = false;
				}

				/*
				 * If we are annotating a constructor, then the lock cannot
				 * refer to the receiver. (LockName checking above takes care of
				 * static methods referring to the receiver.) This duplicates
				 * some of the work of isLockNameOkay, but we keep it here
				 * because it is not related to whether the lock name is
				 * meaningful.
				 */
				if (ConstructorDeclaration.prototype.includes(promisedForOp)) {
					if (lockRefsThis) {
						context.reportError(
						    "Constructor cannot require a lock on the object being constructed",
								lockSpec);
						currentGood = false;
					}
				}

				/*
				 * General issue: The required lock should be visible at the
				 * callsites of the method, otherwise the method cannot be
				 * called. Here we are just looking at the method, not its
				 * callsites. But we can flag obviously bad cases: The lock
				 * comes from the receiver or is a static lock from the same
				 * class (not an ancestor) and is not at least as visible as the
				 * method/constructor.
				 */
				boolean staticLockFromSameClass = false;
				if (lockModel.getAAST().isLockStatic()) {
					final IRNode lockDeclClass =
					  VisitUtil.getClosestType(lockModel.getNode());
					final IRNode methodDeclClass =
					  VisitUtil.getEnclosingType(annotatedMethod);
					staticLockFromSameClass =
					  lockDeclClass.equals(methodDeclClass);
				}
				if (lockRefsThis || staticLockFromSameClass) {
					final Visibility lockViz =
					  getLockFieldVisibility(lockModel.getAAST(), context.getBinder(annotatedMethod));
					final Visibility methodViz = Visibility.getVisibilityOf(annotatedMethod);
					if (!lockViz.atLeastAsVisibleAs(methodViz)) {
						context.reportError(
						    "lock \"" + lockSpec.getLock().getId() + "\" (" +
						    lockViz.nameLowerCase() + 
						    ") is less visible than requiring method/constructor (" +
						    methodViz.nameLowerCase() + ")",
								lockSpec);
						currentGood = false;
					}
				}

				// Check that each lock is only named once
				boolean notUnique = false;
				for (final LockSpecificationNode prevLock : uniqueLocks) {
				  if (areLockSpecificationsEqual(lockSpec, prevLock)) {
				    notUnique = true;
				    break;
				  }
				}
				if (notUnique) {
          context.reportError(
              "lock \"" + lockSpec + "\" is named more than once", lockSpec);
          currentGood = false;
				} else {
				  uniqueLocks.add(lockSpec);
				}
			}
			else {
				currentGood = false;
			}

			allGood &= currentGood;
		}
    RequiresLockPromiseDrop drop = getRequiresLock(annotatedMethod);
    if (drop != null) {
      drop.invalidate();
    }
    
    // Check for consistency with ancestors
    if (allGood) {
      for (final IBinding pBinding : context.getBinder(annotatedMethod).findOverriddenParentMethods(annotatedMethod)) {
        final IRNode parent = pBinding.getNode();
        
        // See if the ancestor is annotated
        final RequiresLockPromiseDrop parentDrop = getRequiresLock(parent);
        if (parentDrop == null) {
          /* Ancestor is not annotated.  Immediate problem because RequiresLock
           * is contravariant: We cannot *add* constraints.  
           * 
           * But, okay if the current annotation is the empty set of locks.
           */
          if (!locks.isEmpty() && !isAssumption) {
            allGood = false;
            context.reportModelingProblem(node, "Overridden method {0} is not annotated with @RequiresLock",
                JavaNames.genRelativeFunctionName(parent));
          }
        } else {
          /* Every lock in the current annotation must be present in the 
           * parent annotation.  We can remove locks, but not add them.
           */
          final Map<IRNode, Integer> positionMap =
            buildParameterMap(annotatedMethod, parent);
          final List<LockSpecificationNode> parentLocks = parentDrop.getAAST().getLockList();
          outer: for (final LockSpecificationNode lock : locks) {
            for (final LockSpecificationNode parentLock : parentLocks) {
              if (lock.satisfiesSpecfication(parentLock, positionMap, LockSpecificationNode.How.CONTRAVARIANT)) {
                continue outer;
              }
            }
            allGood = false;
            context.reportModelingProblem(node, "Cannot add lock {0} to @RequiresLock annotation of {1}",
                lock.unparse(false),
                JavaNames.genRelativeFunctionName(parent));
          }
        }
      }      
    }
    
		if (allGood) {
      RequiresLockPromiseDrop returnDrop = new RequiresLockPromiseDrop(node);
      for(LockModel lockDecl : lockDecls) {
        lockDecl.addDependent(returnDrop);
      }
      // returnDrop.setCategory(JavaGlobals.LOCK_REQUIRESLOCK_CAT);		
			return returnDrop;
    }
		return null;
	}

	/**
	 * Parse rule for the Lock annotation
	 * 
	 * @author ethan
	 */
	public static class Lock_ParseRule extends
			DefaultSLAnnotationParseRule<LockDeclarationNode, LockModel> {
	  private IProtectedRegions protectedRegions;
	  
		protected Lock_ParseRule(final IProtectedRegions pr) {
			super(LOCK, typeDeclOps, LockDeclarationNode.class);
			protectedRegions = pr;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.surelogic.annotation.DefaultSLAnnotationParseRule#parse(com.surelogic.annotation.IAnnotationParsingContext,
		 *      com.surelogic.annotation.parse.SLAnnotationsParser)
		 */
		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws Exception,
				RecognitionException {
			return parser.lock().getTree();
		}

		@Override
		protected IPromiseDropStorage<LockModel> makeStorage() {
			return PromiseDropSeqStorage.create(name(), LockModel.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<LockDeclarationNode, LockModel>(this,
					ScrubberType.BY_HIERARCHY, LockRules.REGION_INITIALIZER,
					RegionRules.REGIONS_DONE, VOUCH_FIELD_IS) {
				@Override
        protected PromiseDrop<AbstractLockDeclarationNode> makePromiseDrop(
						LockDeclarationNode a) {
					return AnnotationRules.storeDropIfNotNull(lockRule.getStorage(),
							scrubLock(getContext(), protectedRegions, a));
				}
			};
		}
	}

	/**
   * Scrubbing code for the Lock annotation
   * 
   * @param context
   *            The context to use to report errors
   * @param a
   *            The LockDeclarationNode
   * @return A scrubbed LockModel
   * @throws Exception
   */
  private static LockModel scrubLock(
      final AnnotationScrubberContext context,
      final IProtectedRegions protectedRegions,
  		final LockDeclarationNode lockDecl) {
    return scrubAbstractLock(context, protectedRegions, lockDecl, LOCK_DECLARATION_CONTINUATION);
  }

  private static abstract class LockScrubContinuation<T extends AbstractLockDeclarationNode> {
    protected void handleAssumeFinal(final LockModel lockModel, final IRNode lockFieldNode) {
      if (lockFieldNode != null) {
        final VouchFieldIsPromiseDrop vouchFieldIs =
          LockRules.getVouchFieldIs(lockFieldNode);
        if (vouchFieldIs != null && vouchFieldIs.isFinal()) {
          final String reason = vouchFieldIs.getReason();
          final ResultDrop rd = new ResultDrop(lockFieldNode);
          rd.addChecked(lockModel);
          rd.setConsistent(true);
          rd.addTrusted(vouchFieldIs);
          final String id = VariableDeclarator.getId(lockFieldNode);
          if (reason == VouchFieldIsNode.NO_REASON) {
            rd.setMessage(
                com.surelogic.analysis.concurrency.driver.Messages.VOUCHED_FINAL, id);
          } else {
            rd.setMessage(
                com.surelogic.analysis.concurrency.driver.Messages.VOUCHED_FINAL_WITH_REASON,
                id, reason);
          }
        }
      }
    }
    
    public abstract LockModel continueScrubbing(
        AnnotationScrubberContext context,
        IProtectedRegions protectedRegions,
        IJavaDeclaredType promisedForType,
        T lockDecl,
        boolean declIsGood,
        boolean fieldIsStatic,
        IRNode lockFieldNode);
  }
  
  private static final LockScrubContinuation<LockDeclarationNode> LOCK_DECLARATION_CONTINUATION = new LockScrubContinuation<LockDeclarationNode>() {
    @Override
    public LockModel continueScrubbing(
        final AnnotationScrubberContext context,
        final IProtectedRegions protectedRegions,
        final IJavaDeclaredType promisedForType,
        final LockDeclarationNode lockDecl,
        final boolean declIsGoodIn, final boolean fieldIsStatic,
        final IRNode lockFieldNode) {
      boolean declIsGood = declIsGoodIn;
      final IBinder binder = context.getBinder(lockDecl.getPromisedFor());
      final ExpressionNode field = lockDecl.getField();
      final RegionNameNode region = lockDecl.getRegion();
      final IRegionBinding regionBinding = region.resolveBinding();
      
      // Check that the region isn't already associated with a lock
      final String regionName = regionBinding.getModel().getRegionName();
      if (!protectedRegions.addIfNotAlreadyProtected(
          context.getBinder(promisedForType.getDeclaration()).getTypeEnvironment(), regionName, promisedForType)) {
        context.reportModelingProblem(lockDecl, "Region \"{0}\" is already protected by a lock", regionName);
        declIsGood = false;
      }
      
      // Region cannot be a final field
      if (regionBinding.getModel().isFinal()) {
        context.reportModelingProblem(lockDecl, "Field \"{0}\" is final: It cannot be protected by a lock", region.getId());
        declIsGood = false;
      }
      
      /*
       * >> WFLockDefs << (3) the associated region must exist (4)
       * instance field or this cannot be associated with a static
       * region (4a) Static region must be from the same class as the
       * declaration (4b) [13 Mar 2007] Static field cannot be
       * associated with an instance region (5) lock must be as
       * visible as the associated region (6) [12 June 2005] If
       * inherited, the region must not contain any fields
       */
      
      // Static region must be associated with a static field
      // (4,above).
      if (regionBinding.getModel().isStatic()) {
        if (!fieldIsStatic) { // (4)
          context.reportError(
              "Static region \"" //$NON-NLS-1$
              + region
              + "\" should be protected by a static field", lockDecl); //$NON-NLS-1$
          declIsGood = false;
        }

        /*
         * Static region must be declared in the same class as
         * the lock declaration.
         */
        if (!lockDecl.getPromisedFor().equals(
            VisitUtil.getClosestType(regionBinding.getModel().getNode()))) {
          context.reportError(
              "Static region \"" + //$NON-NLS-1$
              region
              + "\" may only be protected by a lock in the class" + //$NON-NLS-1$
              " that declares it",
              lockDecl); //$NON-NLS-1$
          declIsGood = false;
        }
      }
      else { // region is not static
        // (4b) the field must not be static
        if (fieldIsStatic) {
          context.reportError(
              "Instance region \"" //$NON-NLS-1$
              + region
              + "\" must be protected by an instance field, or \""
              + region 
              + "\" must be declared as static or contained in a static region (static regions may include instance state)", lockDecl);
          declIsGood = false;
        }
      }
      
      /*
       * Check (6) If the region is inherited it must not contain
       * any fields. First check if the region is inherited: Get
       * the class in which the region is declared and the class
       * in which the lock is declared
       */
      final IRNode regionIsFromClass =
        VisitUtil.getClosestType(regionBinding.getModel().getNode());

      if (!lockDecl.getPromisedFor().equals(regionIsFromClass)) {
        // Check if region contains fields
        if (regionContainsFields(
            promisedForType.getSuperclass(binder.getTypeEnvironment()),
            regionBinding.getModel(), binder)) {
          context.reportError(
              "Inherited region \"" + region + //$NON-NLS-1$
              "\" contains fields; it cannot be protected.",
              lockDecl); //$NON-NLS-1$
          declIsGood = false;
        }
      }
  
      if (declIsGood) {
        // fill in the rest of the drop information
        final String qualifiedName = computeQualifiedName(lockDecl);
        final LockModel model = LockModel.create(lockDecl, qualifiedName); 

        // Add the protected region
        model.addDependent(regionBinding.getModel());        
        // Get the AssumeFinal promise, if any
        handleAssumeFinal(model, lockFieldNode);
        
        /* One last test: Analysis does not currently support using locks 
         * from qualified receivers.  See bug 992.  If annotation is 
         * otherwise correct, but its lock implementation is a qualified 
         * receiver, or a field from a qualified receiver, then we reject it
         * noting that the feature is not yet supported.  We output this as a
         * regular drop-sea warning, because the model isn't broken or nonsensical,
         * just not supported by current analyses.
         */ 
        if ((lockDecl.getField() instanceof QualifiedThisExpressionNode)
            || ((lockDecl.getField() instanceof FieldRefNode)
                && (((FieldRefNode) lockDecl.getField()).getObject() instanceof QualifiedThisExpressionNode))) {
          final ModelingProblemDrop wd = new ModelingProblemDrop(lockDecl.getPromisedFor());
          wd.setMessage(com.surelogic.analysis.concurrency.driver.Messages.LockAnalysis_ds_UnsupportedModel);
          wd.setCategorizingMessage(com.surelogic.analysis.concurrency.driver.Messages.DSC_UNSUPPORTED_MODEL);
          model.addDependent(wd);
        }
        if (JSureScanInfo.printBadLocks && RegionModel.INSTANCE.equals(regionBinding.getModel().getRegionName())) {
            // Make region models for all the fields linked to Instance
        	// TODO what about subclasses?
        	for(IRNode containedField : VisitUtil.getClassFieldDeclarators(promisedForType.getDeclaration())) {
        		final int mods = VariableDeclarator.getMods(containedField);        		
        		if (JavaNode.getModifier(mods, JavaNode.FINAL)) {
        			final IRNode type = VariableDeclarator.getType(containedField);
        			if (PrimitiveType.prototype.includes(type)) {
        				continue; // It's a constant, so we can ignore it
        			}
        		}
        		RegionRules.setupRegionModelForField(model, regionBinding, containedField);        
        	}
        }
        return model;
      }
      return null;
    }
  };
  
  private static final LockScrubContinuation<PolicyLockDeclarationNode> POLICY_LOCK_DECLARATION_CONTINUATION = new LockScrubContinuation<PolicyLockDeclarationNode>() {
    @Override
    public LockModel continueScrubbing(
        final AnnotationScrubberContext context,
        final IProtectedRegions protectedRegions,
        final IJavaDeclaredType promisedForType,
        final PolicyLockDeclarationNode lockDecl, final boolean declIsGood,
        final boolean fieldIsStatic, final IRNode lockFieldNode) {
      final String qualifiedName = computeQualifiedName(lockDecl);     
      if (declIsGood) {
    	final LockModel model = LockModel.create(lockDecl, qualifiedName); 

        // Get the AssumeFinal promise, if any
        handleAssumeFinal(model, lockFieldNode);

        
        /* One last test: Analysis does not currently support using locks 
         * from qualified receivers.  See bug 992.  If annotation is 
         * otherwise correct, but its lock implementation is a qualified 
         * receiver, or a field from a qualified receiver, then we reject it
         * noting that the feature is not yet supported.  We output this as a
         * regular drop-sea warning, because the model isn't broken or nonsensical,
         * just not supported by current analyses.
         */ 
        if ((lockDecl.getField() instanceof QualifiedThisExpressionNode)
            || ((lockDecl.getField() instanceof FieldRefNode)
                && (((FieldRefNode) lockDecl.getField()).getObject() instanceof QualifiedThisExpressionNode))) {
          final ModelingProblemDrop wd = new ModelingProblemDrop(lockDecl.getPromisedFor());
          wd.setMessage(com.surelogic.analysis.concurrency.driver.Messages.LockAnalysis_ds_UnsupportedModel);
          wd.setCategorizingMessage(com.surelogic.analysis.concurrency.driver.Messages.DSC_UNSUPPORTED_MODEL);
          model.addDependent(wd);
        }

        return model;
      }
      return null;
    }
  };
  
  private static VouchFieldIsPromiseDrop scrubVouchFieldIs(
      final AnnotationScrubberContext context, final VouchFieldIsNode a) {
    final IRNode promisedFor = a.getPromisedFor();
    final IJavaType javaType = context.getBinder(promisedFor).getJavaType(promisedFor);
    switch (a.getKind()) {
    case Final:
      // Final: Must make sure the field is not actually declared final
      // Cannot use TypeUtils.isFinal() because that checks for @Vouch
      boolean isAlreadyFinal = false;
      if (TypeUtil.isInterface(VisitUtil.getEnclosingType(promisedFor))) {
        isAlreadyFinal = true; // declared in an interface
      } else {
        final IRNode gp = JJNode.tree.getParent(JJNode.tree.getParent(promisedFor));
        isAlreadyFinal = JavaNode.getModifier(gp, JavaNode.FINAL);
      }
      if (isAlreadyFinal) {
        context.reportError("Field is already declared to be final; no need to vouch it", a);
        return null;
      }
      break;
    case ThreadSafe:
      if (javaType instanceof IJavaPrimitiveType) {
        context.reportModelingProblem(a, "Cannot be used on primitively typed field");
        return null;
      }
// 2011-11-07 Removed this check: Stop gap measure for dealing with thread safe collections;
//      else if ((javaType instanceof IJavaSourceRefType) && 
//          (getNotThreadSafe(((IJavaSourceRefType) javaType).getDeclaration()) != null)) {
//        context.reportError(a, "Cannot be used when the type of field is explicitly @NotThreadSafe");
//        return null;
//      }
      break;
    case Containable:
      if (javaType instanceof IJavaPrimitiveType) {
        context.reportModelingProblem(a, "Cannot be used on a field with primitive type");
        return null;
      } else if ((javaType instanceof IJavaSourceRefType) && 
          (getNotContainable(((IJavaSourceRefType) javaType).getDeclaration()) != null)) {
        context.reportModelingProblem(a, "Cannot be used when the type of field is explicitly @NotContainable");
        return null;
      }
      break;
    case Immutable:
      if (javaType instanceof IJavaPrimitiveType) {
        context.reportModelingProblem(a, "Cannot be used on a field with primitive type");
        return null;
      } else if ((javaType instanceof IJavaSourceRefType) && 
          (getMutable(((IJavaSourceRefType) javaType).getDeclaration()) != null)) {
        context.reportModelingProblem(a, "Cannot be used when the type of field is explicitly @Mutable");
        return null;
      }
      break;
    case AnnotationBounds:
      break;
      /*
    case NonNull:
    case Nullable:
      boolean isBad = false;
      final IRNode gp = JJNode.tree.getParent(JJNode.tree.getParent(promisedFor));
      if (!DeclStatement.prototype.includes(gp)) {
        context.reportError(a, "May only be used on a local variable declaration");
        isBad = true;
      } else {
        final boolean isFinal = JavaNode.getModifier(gp, JavaNode.FINAL);
        if (!isFinal) {
          context.reportError(a, "Variable must be declared to be final");
          isBad = true;
        }
        if (javaType instanceof IJavaPrimitiveType) {
          context.reportError(a, "Cannot be used on a variable with primitive type");
          isBad = true;
        }
      }
      if (isBad) return null;
      */
      default:
    	context.reportModelingProblem(a, "Unexpected kind of Vouch: "+a.getKind());
    	return null;
    }
    return new VouchFieldIsPromiseDrop(a);
  }
  
  /**
	 * Scrubbing code for the Lock annotation
	 * 
	 * @param context
	 *            The context to use to report errors
	 * @param a
	 *            The LockDeclarationNode
	 * @return A scrubbed LockModel
	 * @throws Exception
	 */
	private static <T extends AbstractLockDeclarationNode> LockModel scrubAbstractLock(
	    final AnnotationScrubberContext context,
	    final IProtectedRegions protectedRegions,
			final T lockDecl,
			final LockScrubContinuation<T> continuation) {
    boolean declIsGood = true; // assume the best

    final IBinder binder = context.getBinder(lockDecl.getPromisedFor());
		final IJavaDeclaredType promisedForType =
		  (IJavaDeclaredType) JavaTypeFactory.convertNodeTypeToIJavaType(
		      lockDecl.getPromisedFor(), binder);
		
    /*
     * >> LocksOnce << --- Check for unique lock names State and policy
     * locks share the same namespace within the class hierarchy. Lock
     * name needs to be unique among the state and policy locks declared
     * within the class and its ancestors.
     */

    // Get the name of the current lock declaration
    final String currentName = lockDecl.getId();

    // Prime the set of lock names from the ancestor classes
		final Map<String, String> inheritedLockNames =
		  getInheritedLockNames(promisedForType, binder.getTypeEnvironment());
    
    // Did we inherit a lock with the same name?
    final String ancestorTypeName = inheritedLockNames.get(currentName);
    if (ancestorTypeName != null) {
      context.reportError("Lock \"" + currentName
          + "\" already declared in ancestor class "
          + ancestorTypeName, lockDecl);
      declIsGood = false;
    }

    // Did we already declare a lock with the same name in this class?
    for (LockModel existingLock : getModels(lockDecl.getPromisedFor())) {
      final String existingName = existingLock.getSimpleName();
      if (currentName.equals(existingName)) {
        context.reportError("Lock \""
            + currentName
            + "\" already declared in class "
            + JavaNames.getRelativeTypeNameDotSep(lockDecl.getPromisedFor()),
            lockDecl);
        declIsGood = false;
        break;
      }
    }

		/*
		 * >> WFLockDefs << and >> WFPolicyLockDefs << (1) The associated
		 * field must exist (2) Field must be "this", "class", or a final
		 * field (2a) Field must not be an instance field from a
		 * non-ancestor class (2b) Field must not be a primitive type.
		 * 
		 * With the change to the new promise representation in summer '07, 
		 * we no longer even get here if (1) is not satisfied.  The Promise 
		 * parsing and binding mechanism will reject the annotation if 
		 * the field is nonexistent.
		 */
		final ExpressionNode field = lockDecl.getField();
		final boolean fieldIsThis = (field instanceof ThisExpressionNode)
        || (field instanceof QualifiedThisExpressionNode);
		final boolean fieldIsStatic;
		final IRNode lockFieldNode;
		
		if (!fieldIsThis) {
			if (!(field instanceof ClassLockExpressionNode)) {
				// Have real field, check that it is final...
				final FieldRefNode fieldRefNode = (FieldRefNode) field;
				final IVariableBinding varBinding = fieldRefNode.resolveBinding();
				lockFieldNode = varBinding.getNode();
				
        final int mods = VariableDeclarator.getMods(lockFieldNode);
				if (!TypeUtil.isJSureFinal(lockFieldNode)) {
					context.reportError("Field \"" + field //$NON-NLS-1$
							+ "\" is not final", lockDecl); //$NON-NLS-1$
					declIsGood = false;
				}

				// ...has primitive type...
				// TODO check this line
				final IJavaType fieldTypeNode = varBinding.getJavaType();

				if (fieldTypeNode instanceof IJavaPrimitiveType) {
					context.reportError(
					    "Field \"" + field //$NON-NLS-1$
							+ "\" has a primitive type", lockDecl); //$NON-NLS-1$
					declIsGood = false;
				}

				/*
				 * ...and, if non-static and not a reference to a field in qualified receiver,
				 * that it is from an ancestor class or the current class.
				 */
				fieldIsStatic = JavaNode.getModifier(mods, JavaNode.STATIC);
				final boolean fieldIsQualifiedReceiver =
				  fieldRefNode.getObject() instanceof QualifiedThisExpressionNode;
				if (!fieldIsStatic && !fieldIsQualifiedReceiver) {
					final IJavaType fieldIsFromClass = getEnclosingType(varBinding.getNode(), binder);
					if (!binder.getTypeEnvironment().isRawSubType(
							promisedForType, fieldIsFromClass)) {
						context.reportError(
						    "Field \"" + field + //$NON-NLS-1$
								"\" is not from an ancestor of the annotated class.", //$NON-NLS-1$
								lockDecl);
						declIsGood = false;
					}
				}
			}	else {
				fieldIsStatic = true;
				lockFieldNode = null;
			}
		} else {
			fieldIsStatic = false;
			lockFieldNode = null;
		}

		return continuation.continueScrubbing(
		    context, protectedRegions, promisedForType, lockDecl, declIsGood, fieldIsStatic, lockFieldNode);
	}

	/**
	 * Get the lock names inherited from super classes. Returns a map from lock
	 * name to the name of the class in which it is declared.
	 * 
	 * TODO: Should probably find a way to cache the results
	 * 
	 * @param type
	 *            The class whose ancestors should be inspected. The class
	 *            itself should not be inspected.
	 * @param typeEnv
	 *            The type environment to use
	 */
	private static Map<String, String> getInheritedLockNames(
			final IJavaDeclaredType type, final ITypeEnvironment typeEnv) {
		final Map<String, String> names = new HashMap<String, String>();
		IJavaDeclaredType current = type.getSuperclass(typeEnv);
		while (current != null) {
			final IRNode classDecl = current.getDeclaration();
			final String typeName = JavaNames.getRelativeTypeNameDotSep(classDecl);
			for (LockModel lockModel : getModels(classDecl)) {
				final String name = lockModel.getSimpleName();
				if (name != null) {
					/*
					 * We may see duplicates because the ancestors may have bad
					 * lock declarations. But it doesn't matter because we only
					 * need a reference to one of the earlier declarations.
					 */
					names.put(name, typeName);
				}
			}
			current = current.getSuperclass(typeEnv);
		}
		return names;
	}

	/**
	 * Utility method from LockAnnotation
	 * 
	 * @param field
	 * @return
	 */
	private static IJavaType getEnclosingType(
	    final IRNode field, final IBinder binder) {
		IRNode decl = VisitUtil.getEnclosingType(field);
		return JavaTypeFactory.convertNodeTypeToIJavaType(decl, binder);
	}

	/**
	 * Get the visibility of a lock's representation. The visibility of a lock
	 * representation is <code>public</code> if the lock representation is
	 * <code>this</code> or <code>.class</code>. Otherwise the visibility
	 * is determined by the visibility of the associated field. In addition,
	 * visibility is influenced by any lock getter method that returns the lock
	 * in question: getter methods can be used to increase the visibility of a
	 * lock.
	 * 
	 * @param lockDeclNode
	 *            A LockDeclaration node
	 */
	public static Visibility getLockFieldVisibility(
			final AbstractLockDeclarationNode lockDeclNode, final IBinder binder) {
		Visibility maxViz = getLockFieldVisibilityRaw(lockDeclNode, binder);

		/*
		 * Get all the methods of the class and see if any of them declare that
		 * they return the given lock. See if the method increases the lock
		 * field's visibility
		 */
		final IRNode clazz = lockDeclNode.getPromisedFor();
		final IRNode body  = VisitUtil.getClassBody(clazz);
		final Iterator<IRNode> decls = ClassBody.getDeclIterator(body);
		while (decls.hasNext()) {
			final IRNode member = decls.next();
			if (MethodDeclaration.prototype.includes(tree.getOperator(member))) {
				final IRNode returnNode = JavaPromise.getReturnNodeOrNull(member);
				if (returnNode != null) {
				  /* No returns lock means it is not committed to returning any 
				   * particular value.
				   */
					final ReturnsLockPromiseDrop returnedLock = getReturnsLock(returnNode);
					if (returnedLock != null) {
						final LockModel returnedLockModel = isLockNameOkay(
						    TypeUtil.isStatic(member), returnedLock.getAAST().getLock(),
						    null, true);
						// Check for null because the requiresLock annotation
						// could be bad
						if (returnedLockModel != null
								&& returnedLockModel.getAAST().equals(lockDeclNode)) {
							// check the viz of the method
							final Visibility methodViz = Visibility.getVisibilityOf(member);
							if (methodViz.atLeastAsVisibleAs(maxViz)) {
							  maxViz = methodViz;
							}
						}
					}
				}
			}
		}
		return maxViz;
	}

	/**
	 * Get the raw visibility of a lock's representation&mdash;raw visibility
	 * never takes into account lock-getter methods. The raw visibility of a
	 * lock representation is <code>public</code> if the lock representation
	 * is <code>this</code> or <code>.class</code>. Otherwise the raw
	 * visibility is determined by the visibility of the associated field.
	 * 
	 * @param lockDeclNode
	 *            A LockDeclaration node
	 */
	public static Visibility getLockFieldVisibilityRaw(
			final AbstractLockDeclarationNode lockDeclNode, final IBinder binder) {
		final ExpressionNode field = lockDeclNode.getField();
		if (field instanceof ThisExpressionNode) {
			return Visibility.PUBLIC;
		} else if (field instanceof QualifiedThisExpressionNode) {
		  return Visibility.PUBLIC;
		} else if (field instanceof ClassLockExpressionNode) {
			return Visibility.PUBLIC;
		} else {
      FieldRefNode ref = (FieldRefNode) field;
			final IRNode boundField = ref.resolveBinding().getNode();
			if (TypeUtil.isInterface(VisitUtil.getEnclosingType(boundField))) {
				return Visibility.PUBLIC;
			}
			else {
			  return Visibility.getVisibilityOf(
			      tree.getParent(tree.getParent(boundField)));
			}
		}
	}

	/**
	 * Parse rule for the
	 * 
	 * @PolicyLock annotation
	 */
	public static class PolicyLock_ParseRule extends
			DefaultSLAnnotationParseRule<PolicyLockDeclarationNode, LockModel> {
		protected PolicyLock_ParseRule() {
			super(POLICY_LOCK, typeDeclOps, PolicyLockDeclarationNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws RecognitionException {
			return parser.policyLock().getTree();
		}

		// No storage: shared with @Lock

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<PolicyLockDeclarationNode, LockModel>(name(),
          PolicyLockDeclarationNode.class, lockRule.getStorage(),
					ScrubberType.BY_HIERARCHY, VOUCH_FIELD_IS) {
				@Override
        protected PromiseDrop<AbstractLockDeclarationNode> makePromiseDrop(
						PolicyLockDeclarationNode a) {
					return AnnotationRules.storeDropIfNotNull(lockRule.getStorage(),
							scrubPolicyLock(getContext(), a));
				}
			};
		}

	}

	/**
	 * Scrubbing code for the PolicyLock annotation
	 * 
	 * @param context
	 * @param policyLockDecl
	 * @return
	 */
	private static LockModel scrubPolicyLock(
			final AnnotationScrubberContext context,
			final PolicyLockDeclarationNode policyLockDecl) {
	  // XXX: This is sleazy, passing a null reference to the ProtectedRegions parameter 
    return scrubAbstractLock(context, null, policyLockDecl, POLICY_LOCK_DECLARATION_CONTINUATION);
	}

	/**
	 * Test whether a lock name is syntactically correct given the context.
	 * Returns the bound lock object if it is okay, or <code>null</code> if
	 * any errors were reported. Enforces the following constraints
	 * <ul>
	 * <li>If the lock id is qualified by a parameter name (including
	 * <tt>this</tt>), the parameter must be a parameter of the annotated
	 * method/constructor, the lock id must be declared in the type of the
	 * parameter, the parameter must be final, and the lock must be an instance
	 * lock.
	 * <li>If the lock is qualified by a qualified receiver, the qualified
	 * receiver must exist, the lock id must be declared in the type of the
	 * qualified receiver, and the lock must be an instance lock.
	 * <li>If the lock id is qualified by a class name, then the class must
	 * exist, the lock id must exist in the named class, and the lock id must
	 * refer to a static lock.
	 * <li>If the lock id is unqualified, then the lock must exist in the
	 * current class, and be static if the method is static.
	 * </ul>
	 * 
	 * @param isStatic
	 *            Whether the annotated method is <code>static</code> or not.
	 *            (If <tt>true</tt>, then <tt>isConstructor</tt> must be
	 *            <tt>false</tt>).
	 * @param isConstructor
	 *            Whether the annotation is on a constructor. If <tt>true</tt>
	 *            then <tt>isStatic</tt> must be <tt>false</tt>.
	 * @param lockName
	 *            A QualifiedLockName or SimpleLockName expression node taken
	 *            from a requiresLock or returnsLock annotation.
	 * @param binder
	 *            The name binder to use
	 * @param report
	 *            The error reporter to use
	 * @return The bound lock object if it is okay, or <code>null</code> if
	 *         any errors were reported
	 */
  private static LockModel isLockNameOkay(final boolean isStatic,
      final LockNameNode lockName, final AnnotationScrubberContext report,
      final boolean isBinary) {
    // Default to assuming we should not get the binding
    boolean getBinding = false;
    boolean deferCheckForStaticUseOfThis = false;
    boolean staticUseOfThis = false;
    boolean checkForTypeQualifiedInstance = false;
    boolean checkForInstanceQualifiedStatic = false;
    boolean isBad = false;

    if (lockName instanceof QualifiedLockNameNode) {
      // Check that the qualifying variable exists
      final ExpressionNode base = ((QualifiedLockNameNode) lockName).getBase();
      if (base instanceof VariableUseExpressionNode) {
        VariableUseExpressionNode v = (VariableUseExpressionNode) base;
        final IVariableBinding var = v.resolveBinding();
        if (var == null) {
          report.reportError("Parameter \"" + v //$NON-NLS-1$
              + "\" does not exist", v); //$NON-NLS-1$
          isBad = true;
        } else {
          final IRNode n = var.getNode();
          /* Bind to either a ParameterDeclaration or a VariableDeclarator.
           * If VariableDeclarator, we have to check that the field is 
           * a final instance field. whose type is a user-declared type.
           */
          if (VariableDeclarator.prototype.includes(n)) {
            if (TypeUtil.isStatic(n)) {
              report.reportError("Field cannot be static", base);
              isBad = true;
            }
            if (!TypeUtil.isJSureFinal(n) ) {
              report.reportError("Field must be final", base);
              isBad = true;
            }
            
            /* Should test the type of the field.  But, the binding of the
             * lock name already fails before we get here if the type of the
             * field is primitive, or if the lock name doesn't exist.
             */
            /*
            if (report.getBinder(n).getJavaType(n) instanceof IJavaPrimitiveType) {
              report.reportError("Field must have a non-primitive type", base);
              isBad = true;
            }
            */
          }
          getBinding = true;
          checkForInstanceQualifiedStatic = true;
        }
      } else if (base instanceof TypeExpressionNode) {
        final IType type = ((TypeExpressionNode) base).resolveType();
        if (type == null) {
          report.reportError("Class \"" + base //$NON-NLS-1$
              + "\" does not exist", lockName); //$NON-NLS-1$
          isBad = true;
        } else {
          getBinding = true;
          checkForTypeQualifiedInstance = true;
        }
      } else if (base instanceof ThisExpressionNode) {
        if (!isStatic) {
          getBinding = true;
          checkForInstanceQualifiedStatic = true;
        } else {
          staticUseOfThis = true;
        }
      } else if (base instanceof QualifiedThisExpressionNode) {
        /*
         * No longer needed final IRNode rcvr = ((QualifiedThisExpressionNode)
         * base); if (rcvr == null) { report.reportError("Qualified receiver \""
         * + base + "\" does not exist", lockName); } else {
         */
        getBinding = true;
        checkForInstanceQualifiedStatic = true;
        // }
      }
    } else if (lockName instanceof SimpleLockNameNode) {
      getBinding = true;
      if (isStatic) {
        deferCheckForStaticUseOfThis = true;
      }
    } else {
      // defensive programming!
      throw new IllegalArgumentException("Don't know what to do with a \""
          + lockName.getClass().getSimpleName() + "\"");
    }

    ILockBinding boundLock = null;
    if (getBinding) {
      boundLock = lockName.resolveBinding();
      if (boundLock == null) {
        report.reportError("Lock \"" + lockName.getId() + "\" does not exist", //$NON-NLS-1$ //$NON-NLS-2$
            lockName);
        isBad = true;
      } else {
        final boolean lockIsStatic = boundLock.getModel().getAAST()
            .isLockStatic();
        if (deferCheckForStaticUseOfThis) {
          staticUseOfThis = !lockIsStatic;
        }
        if (checkForInstanceQualifiedStatic) {
          if (lockIsStatic) {
            report
                .reportError(
                    "Cannot qualify a static lock with a receiver or a method parameter",
                    lockName);
            isBad = true;
          }
        }
        if (checkForTypeQualifiedInstance) {
          if (!lockIsStatic) {
            report
                .reportError("Cannot type-qualify an instance lock", lockName);
            isBad = true;
          }
        }
      }
    }

    if (staticUseOfThis) {
      report.reportError("Cannot reference \"this\" from a static method",
          lockName);
      isBad = true;
    }

    return isBad ? null : boundLock.getModel();
  }

	/**
	 * Does the given region have any field members in the given class. Copied
	 * from {@link LockAnnotation}
	 */
	private static boolean regionContainsFields(IJavaDeclaredType type,
			RegionModel region, IBinder binder) {
		if (type == null) {
			return false;
		}
		else {
			final IRNode clazz = type.getDeclaration();

			// For each field in the class, see if the given region is an
			// ancestor
			final IRNode body = ClassDeclaration.getBody(clazz);
			final Iterator<IRNode> decls = ClassBody.getDeclIterator(body);
			while (decls.hasNext()) {
				final IRNode decl = decls.next();
				if (FieldDeclaration.prototype.includes(JJNode.tree
						.getOperator(decl))) {
					IRNode vars = FieldDeclaration.getVars(decl);
					for(IRNode var : VariableDeclarators.getVarIterator(vars)) {					
					  // TODO This creates a LOT of RegionModel objects so this
					  // may need to be changed
					  final IRegion fieldAsRegion = RegionModel.getInstance(var);
					  if (region.ancestorOf(fieldAsRegion)) {
					    return true;
					  }
          }
				}
			}
			// Region is empty in the class, but what about the superclass?
			return regionContainsFields(
			    type.getSuperclass(binder.getTypeEnvironment()), region, binder);
		}
	}
	
	private static abstract class TypeAnnotationScrubber<
	    A extends AbstractModifiedBooleanNode,
	    P extends ModifiedBooleanPromiseDrop<A>,
	    NP extends BooleanPromiseDrop<? extends AbstractBooleanNode>>
	extends AbstractAASTScrubber<A, P> {
    protected final String name;
    protected final String notName;
	  private final boolean allowAnnotationDeclarations;
	  
	  public TypeAnnotationScrubber(
	      final SimpleBooleanAnnotationParseRule<A, P> rule,
	      final boolean allowAD,
	      final String n, final String notN, final String... deps) {
	    super(rule, ScrubberType.INCLUDE_SUBTYPES_BY_HIERARCHY, deps);
	    allowAnnotationDeclarations = allowAD;
	    name = n;
	    notName = notN;
	  }

	  @Override
	  protected final P makePromiseDrop(IAnnotationTraversalCallback<A> cb, A a, boolean isAssumption) {
	    final P originalPromiseDrop = makePromiseDrop(a, isAssumption);
	    if (originalPromiseDrop != null) {
        final IRNode promisedFor = a.getPromisedFor();
        final boolean implementationOnly = a.isImplementationOnly();
        /* Add derived annotations to any AnonClassExpression or 
         * EnumClassConstantDeclaration that extends from this class,
         * but only if the annotation is not implementationOnly.
         */
        if (!implementationOnly) {
          for (final IRNode sub : getContext().getBinder(promisedFor).getTypeEnvironment().getRawSubclasses(promisedFor)) {
            final Operator subOp = JJNode.tree.getOperator(sub);
            if (AnonClassExpression.prototype.includes(subOp) ||
                EnumConstantClassDeclaration.prototype.includes(subOp)) { // Bug 1705: Not being returned at the moment
              // Add derived annotation
              final boolean verify = a.verify();
              final A derived = makeDerivedAnnotation(verify ? 0 : JavaNode.NO_VERIFY, a);
              derived.copyPromisedForContext(sub, a, AnnotationOrigin.SCOPED_ON_TYPE);
              cb.addDerived(derived, originalPromiseDrop);
            }
          }
        }        
	    }
	    return originalPromiseDrop;
	  }

	  @Override
	  protected final P makePromiseDrop(final A a, boolean isAssumption) {
	    return storeDropIfNotNull(a, scrubAnnotated(a, isAssumption));          
	  }
	  
	  @Override
    protected final P makePromiseDrop(final A a) {
	    throw new IllegalStateException("Should not get to here");       
	  }
	  
	  private P scrubAnnotated(final A node, final boolean isAssumption) {
	    final AnnotationScrubberContext context = getContext();
	    final IRNode promisedFor = node.getPromisedFor();
      final Operator op = JJNode.tree.getOperator(promisedFor);
	    final boolean implementationOnly = node.isImplementationOnly();
	    boolean bad = false;
	    
	    if (!allowAnnotationDeclarations && AnnotationDeclaration.prototype.includes(op)) {
	      bad = true;
	      context.reportModelingProblem(node, "Annotation declarations may not be annotated @{0}", name);
	    } else if (InterfaceDeclaration.prototype.includes(op)) {
	      // the verify attribute is non-sense on interfaces
	      if (!node.verify()) {
	        bad = true;
	        context.reportModelingProblem(node, "An interface may not be @{0}(verify=false)", name);
	      }
	      // The implemenationOnly attribute must be false on interfaces
	      if (implementationOnly) {
	        bad = true;
	        context.reportModelingProblem(node, "An Interface may not be @{0}(implementationOnly=true)", name);
	      }
	      
	      // Scan each extended interface for incompatibility
	      final IRNode extensions = InterfaceDeclaration.getExtensions(promisedFor);
	      if (extensions != null) {
	        for (final IRNode superDecl : Extensions.getSuperInterfaceIterator(extensions)) {
	          final IRNode bound = context.getBinder(superDecl).getBinding(superDecl);
	          bad |= !checkAnnotatedInterfaceSuperInterface(node, promisedFor, bound);
	        }
	      }
	    } else if (AnnotationDeclaration.prototype.includes(op)) { 
	      // Annotation declaration is like an interface, but cannot extend anything
	      
        // the verify attribute is non-sense on interfaces
        if (!node.verify()) {
          bad = true;
          context.reportModelingProblem(node, "An interface may not be @{0}(verify=false)", name);
        }
        // The implemenationOnly attribute must be false on interfaces
        if (implementationOnly) {
          bad = true;
          context.reportModelingProblem(node, "An Interface may not be @{0}(implementationOnly=true)", name);
        }
        // Applies to can not be exclusively "instance"
        if (node.getAppliesTo() == Part.Instance) {
          bad = true;
          context.reportModelingProblem(node, "Annotations never have any instance state");
        }
	    } else { // class
	      final IRNode superDecl;
	      final Iterable<IRNode> interfaces;
	      if (EnumDeclaration.prototype.includes(op)) {
	        superDecl = context.getBinder(promisedFor).getTypeEnvironment().findNamedType(
	            JAVA_LANG_ENUM);
	        interfaces = Implements.getIntfIterator(EnumDeclaration.getImpls(promisedFor));
	        if (superDecl == null) {
	        	context.getBinder(promisedFor).getTypeEnvironment().findNamedType(JAVA_LANG_ENUM);
	        }
	      } else if (EnumConstantClassDeclaration.prototype.includes(op)) {
	        // Get the enclosing EnumDeclaration
	        superDecl = JJNode.tree.getParent(JJNode.tree.getParent(promisedFor));
	        interfaces = null;
	      } else if (AnonClassExpression.prototype.includes(op)) {
	        final IRNode superTypeName = AnonClassExpression.getType(promisedFor);
          final IRNode superType = context.getBinder(promisedFor).getBinding(superTypeName);
	        if (TypeUtil.isInterface(superType)) {
	          superDecl = context.getBinder(promisedFor).getTypeEnvironment().getObjectType().getDeclaration();
	          interfaces = new Iterable<IRNode>() {
              @Override
              public Iterator<IRNode> iterator() {
                return new SingletonIterator<IRNode>(superTypeName);
              }	            
	          };
	        } else {
	          superDecl = superType;
	          interfaces = null;
	        }
	      } else {
	        superDecl = context.getBinder(promisedFor).getBinding(
	            ClassDeclaration.getExtension(promisedFor));
	        interfaces = Implements.getIntfIterator(ClassDeclaration.getImpls(promisedFor));
	      }

        // Scan each implemented interface for incompatibility
        if (interfaces != null) {
          for (final IRNode intfName : interfaces) {
            final IRNode intfDecl = context.getBinder(intfName).getBinding(intfName);
            final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> iDrop =
                getAnnotation(intfDecl);
            if (implementationOnly) {
              if (iDrop != null && !isStaticOnly(iDrop)) {
                bad = true;
                context.reportModelingProblem(node,
                    "Class may not be @{0}(implementationOnly=true) because it implements the @{0} interface {1}",
                    name, JavaNames.getRelativeTypeNameDotSep(intfDecl));
              }
            }
            
            bad |= !checkAnnotatedClassSuperInterface(node, promisedFor, intfDecl, iDrop);
          }
        }

        // java.lang.Object doesn't have a superclass
        if (superDecl != promisedFor) {
          if (implementationOnly) {
	          final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> superAnno = getAnnotation(superDecl);
	          if (!isLessSpecific(superDecl)) {
  	          if (superAnno == null) {
    	        	if (!isAssumption) {
    	        		bad = true;
    	        		context.reportModelingProblem(node,
    	        				"Class may not be @{0}(implementationOnly=true) because it extends the non-@{0} class {1}",
    	        				name, JavaNames.getRelativeTypeNameDotSep(superDecl));
    	        	}
  	          } else if (!superAnno.isImplementationOnly()) {
  	            bad = true;
  	            context.reportModelingProblem(node,
  	                "Class may not be @{0}(implementationOnly=true) because it extends the @{0} class {1}",
  	                name, JavaNames.getRelativeTypeNameDotSep(superDecl));
  	          }
	          }
          } else { // implementationOnly == false
            if (!isAssumption) {
              final ModifiedBooleanPromiseDrop<?> d = getAnnotation(superDecl);
  	          if (!isStaticOnly(node) && !isLessSpecific(superDecl) && (d == null || isStaticOnly(d))) {
  	            bad = true;
  	            context.reportModelingProblem(node,
  	                "Class may not be @{0} because it extends the non-@{0} class {1}",
  	                name, JavaNames.getRelativeTypeNameDotSep(superDecl));
  	          }
            }
          }

  	      // Scan superclass for incompatibility
  	      bad |= !checkAnnotatedClassSuperClass(node, promisedFor, superDecl);
        }
	    }
	    
	    // Cannot be both T and not T
      final NP notDrop = getNotAnnotation(promisedFor);
	    if (isTandNotT(node, notDrop, context)) {
	      bad = true;
	    }
	          
	    if (bad) {
	      return null;
	    } else {
	      return moreChecks(node, promisedFor) ? createDrop(node) : null;
	    }
	  }

	  protected boolean isTandNotT(
	      final A node, final NP notDrop, final AnnotationScrubberContext context) {
      if (notDrop != null && !node.isImplementationOnly()) {
        notDrop.invalidate();
        context.reportModelingProblem(
            node, "Cannot be both @{0} and @{1}", name, notName);
        return true;
      }
      return false;
	  }
	  
    protected boolean moreChecks(A node, IRNode promisedFor) {
	    return true;
	  }
    
    @Override 
    protected final boolean processUnannotatedType(final IJavaSourceRefType dt) {
      final AnnotationScrubberContext context = getContext();
      final IRNode typeDecl = dt.getDeclaration();
      final boolean isInterface = TypeUtil.isInterface(typeDecl);
      final Iterable<IJavaType> supers = 
        dt.getSupertypes(context.getBinder(typeDecl).getTypeEnvironment()) ;
      
      // Are we actually annotated with the NOT form of the annotation?
      final boolean isNOT = getNotAnnotation(typeDecl) != null;
      // Are we annotated with a more specific annotation
      final boolean isMoreSpecific = isMoreSpecific(typeDecl);
      
      boolean result = true;      
      if (isInterface) { // unannotated interface
        // If any superinterface is T on the instance state we have an error
        for (final IJavaType zuper : supers) {
          final IRNode zuperDecl = ((IJavaDeclaredType) zuper).getDeclaration();
          // ignore CLASS java.lang.Object (which is a super if the interface doesn't extend anything)
          if (TypeUtil.isInterface(zuperDecl)) {
            final ModifiedBooleanPromiseDrop<?> zAnno = getAnnotation(zuperDecl);
            if (zAnno != null && !isStaticOnly(zAnno)) {
              if (isNOT) {
                context.reportModelingProblem(typeDecl,
                    "Interface may not be @{0} because it extends the @{1} interface {2}",
                    notName, name, JavaNames.getRelativeTypeNameDotSep(zuperDecl));
                result = false;
              } else if (!isMoreSpecific && !zAnno.isAssumed()) {
                context.reportModelingProblemAndProposal(new Builder(name, typeDecl, zuperDecl).setOrigin(Origin.PROBLEM).build(),
                    "Interface must be annotated @{0} because it extends the @{0} interface {1}",
                    name, JavaNames.getRelativeTypeNameDotSep(zuperDecl));
                result = false;
              }              
            }
          }
        }
      } else { // unannotated class
        for (final IJavaType zuper : supers) {
          final IRNode zuperDecl = ((IJavaDeclaredType) zuper).getDeclaration();
          final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> anno = getAnnotation(zuperDecl);
          if (anno != null && !isStaticOnly(anno)) {
            if (TypeUtil.isInterface(zuperDecl)) {
              if (isNOT) {
                context.reportModelingProblem(typeDecl,
                    "Class may not be @{0} because it implements a @{1} interface {2}",
                    notName, name, JavaNames.getRelativeTypeNameDotSep(zuperDecl));
                result = false;
              } else if (!isMoreSpecific && !anno.isAssumed()) {
                context.reportModelingProblemAndProposal(new Builder(name, typeDecl, zuperDecl).setOrigin(Origin.PROBLEM).build(),
                    "Class must be annotated @{0} because it implements a @{0} interface {1}",
                    name, JavaNames.getRelativeTypeNameDotSep(zuperDecl));
                result = false;
              }            
            } else if (!anno.isImplementationOnly()) {
              if (isNOT) {
                context.reportModelingProblem(typeDecl,
                    "Class may not be @{0} because it extends a @{1} class {2}",
                    notName, name, JavaNames.getRelativeTypeNameDotSep(zuperDecl));
                result = false;
              } else if (!anno.isAssumed()) {
                context.reportModelingProblemAndProposal(new Builder(name, typeDecl, zuperDecl).setOrigin(Origin.PROBLEM).build(),
                    "Class must be annotated @{0} because it extends a @{0} class {1}",
                    name, JavaNames.getRelativeTypeNameDotSep(zuperDecl));
                result = false;
              }
            }
          }
        }
      }
      return result;
    }

    protected abstract ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> getAnnotation(IRNode typeDecl);
    
    protected abstract NP getNotAnnotation(IRNode typeDecl);
    
    /* typeDecl is an unannotated class.  We want to know if it is actually
     * annotated with an annotation more specific than the one we are checking.
     */
    protected boolean isMoreSpecific(final IRNode typeDecl) {
      return false;
    }
    
    /* classDecl is the superclass of an annotated class.  We want to know if it
     * is actually annotated with an annotation less specific than the one we are
     * checking.
     */
    protected boolean isLessSpecific(final IRNode classDecl) {
      return false;
    }
    
    protected final boolean isStaticOnly(final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> drop) {
      return isStaticOnly(drop.getAAST());
    }
    
    protected final boolean isStaticOnly(final AbstractModifiedBooleanNode n) {
      return n.getAppliesTo() == Part.Static;
    }
    
    /**
     * TODO Note that these currently don't look for supertypes that are missing annotations
     */

    /* If I apply to static only, and the super interfaces is annotated with
     * a compatible annotation it must apply to the static state only.
     * 
     * If I apply to instance, then the super interfaces can apply to anything
     * because if they apply to static only, it's like they are unannotated.
     * But the super interface must have a compatible annotation.
     */
    // Return true if no errors detected
    protected abstract boolean checkAnnotatedInterfaceSuperInterface(A node, IRNode iDecl, IRNode sDecl);

    protected abstract boolean checkAnnotatedClassSuperInterface(A node, IRNode cDecl, IRNode iDecl, ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> iDrop);
    
    protected abstract boolean checkAnnotatedClassSuperClass(A node, IRNode cDecl, IRNode sDecl);

	  protected abstract P createDrop(A node);
	  
	  protected abstract A makeDerivedAnnotation(int mods, A orig);
	}
  
  private static abstract class TypeAnnotationWithAppliesToScrubber<
      A extends AbstractModifiedBooleanNode,
      P extends ModifiedBooleanPromiseDrop<A>,
      NP extends ModifiedBooleanPromiseDrop<? extends AbstractBooleanNode>>
  extends TypeAnnotationScrubber<A, P, NP> {
    public TypeAnnotationWithAppliesToScrubber(
        final SimpleBooleanAnnotationParseRule<A, P> rule,
        final boolean allowAD,
        final String n, final String notN, final String... deps) {
      super(rule, allowAD, n, notN, deps);
    }

    @Override
    protected boolean isTandNotT(
        final A node, final NP notDrop, final AnnotationScrubberContext context) {
      /*
	     * Cannot be both T and not T unless they apply to different parts of 
	     * the class.  Specifically, one must apply to only the instance part
	     * and the other must apply to only the static part.
	     */
      if (notDrop != null) {
        if (!node.isImplementationOnly()) {
          final Part appliesTo = node.getAppliesTo();
          final Part notAppliesTo = notDrop.getAppliesTo();
          
          /* Ideally we would have a comparison method in the Part enumeration,
           * but because the enumeration is part of the annotation language,
           * I don't want to put executable code into it.
           */
          boolean okay = true;
          if (appliesTo == Part.InstanceAndStatic) {
            okay = false;
          } else if (appliesTo == Part.Instance) {
            okay = notAppliesTo == Part.Static;
          } else if (appliesTo == Part.Static) {
            okay = notAppliesTo == Part.Instance;
          }
          
          if (!okay) {
            notDrop.invalidate();
            context.reportModelingProblem(node,
                "Cannot apply opposite annotations to the same part of a class: @{0}(appliesTo=Part.{1}) and @{2}(appliesTo=Part.{3})",
                name, appliesTo.name(), notName, notAppliesTo.name());
            return true;
          }
        }
      }
      
      return false;
    }
  }
  
	public static class AnnotationBounds_ParseRule
	extends SimpleBooleanAnnotationParseRule<AnnotationBoundsNode,AnnotationBoundsPromiseDrop> {
		public AnnotationBounds_ParseRule() {
			super(ANNO_BOUNDS, typeDeclOps, AnnotationBoundsNode.class);
		}
		
		@Override
		protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
			return new AnnotationBoundsNode(
					createNamedType(offset, context.getProperty(CONTAINABLE_PROP)),
					createNamedType(offset, context.getProperty(IMMUTABLE_PROP)),
					createNamedType(offset, context.getProperty(REFERENCE_PROP)),
					createNamedType(offset, context.getProperty(THREAD_SAFE_PROP)),
					createNamedType(offset, context.getProperty(VALUE_PROP)));
		}
		
		@Override
		protected IPromiseDropStorage<AnnotationBoundsPromiseDrop> makeStorage() {
			return BooleanPromiseDropStorage.create(name(), AnnotationBoundsPromiseDrop.class);
		}
		
		@Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<AnnotationBoundsNode, AnnotationBoundsPromiseDrop>(
          this, ScrubberType.UNORDERED) {
        @Override
        protected AnnotationBoundsPromiseDrop makePromiseDrop(final AnnotationBoundsNode a) {
          return storeDropIfNotNull(a, scrubAnnotationBounds(a));
        }

        private AnnotationBoundsPromiseDrop scrubAnnotationBounds(
            final AnnotationBoundsNode a) {
          final AnnotationScrubberContext context = getContext();
          final IRNode promisedFor = a.getPromisedFor();

          /* Check that all the formal type parameters named in the annotation
           * bounds exist.
           */
          class Visitor implements BoundsVisitor {
            private boolean good = true;
            private final Set<String> names = new HashSet<String>();
            
            @Override
            public void visitWhenType(final NamedTypeNode namedType) {
              // Check for duplicates
              final String name = namedType.getType();
              if (!names.add(name)) {
                context.reportModelingProblem(
                    namedType, "Type formal {0} named more than once", name);
              }
                
              /* Named type must exist, and be associated with the class being
               * annotated.  (No fair naming type formals of outer classes!)
               */
              final ISourceRefType resolvedType = namedType.resolveType();
              if (resolvedType == null) {
                good = false;
                context.reportModelingProblem(namedType,
                    "No type formal parameter named {0}", name);
              } else {
                final IRNode t = resolvedType.getNode();
                final IRNode c = JJNode.tree.getParent(JJNode.tree.getParent(t));
                if (!c.equals(promisedFor)) {
                  good = false;
                  context.reportModelingProblem(namedType,
                      "Type formal {0} is from a surrounding type", name);
                }
              }
            }
            
            public boolean isGood() {
              return good;
            }
          }

          final Visitor cVisitor = new Visitor();
          final Visitor iVisitor = new Visitor();
          final Visitor rVisitor = new Visitor();
          final Visitor tsVisitor = new Visitor();
          final Visitor vVisitor = new Visitor();
          a.visitImmutableBounds(iVisitor);
          a.visitContainableBounds(cVisitor);
          a.visitReferenceBounds(rVisitor);
          a.visitThreadSafeBounds(tsVisitor);
          a.visitValueBounds(vVisitor);

          if (cVisitor.isGood() && iVisitor.isGood() && rVisitor.isGood() &&
              tsVisitor.isGood() && vVisitor.isGood()) {
            return new AnnotationBoundsPromiseDrop(a);
          } else {
            return null;
          }
        }
      };
		}
	}
	
  public static class Containable_ParseRule 
  extends SimpleBooleanAnnotationParseRule<ContainableNode,ContainablePromiseDrop> {
    public Containable_ParseRule() {
      super(CONTAINABLE, typeDeclOps, ContainableNode.class);
    }
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {      
      return new ContainableNode(mods);
    }
    @Override
    protected IPromiseDropStorage<ContainablePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), ContainablePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new TypeAnnotationScrubber<ContainableNode, ContainablePromiseDrop, NotContainablePromiseDrop>(this, false, "Containable", "NotContainable", NOT_CONTAINABLE) {
        @Override
        protected ContainablePromiseDrop getAnnotation(final IRNode superDecl) {
          return getContainableImplementation(superDecl);
        }
        
        @Override
        protected ContainablePromiseDrop createDrop(final ContainableNode node) {
          return new ContainablePromiseDrop(node);
        }

        @Override
        protected NotContainablePromiseDrop getNotAnnotation(final IRNode typeDecl) {
          return getNotContainable(typeDecl);
        }
        
        @Override
        protected ContainableNode makeDerivedAnnotation(
            final int mods, ContainableNode orig) {
          return new ContainableNode(mods);
        }

        /* If I apply to static only, and the super interfaces is annotated with
         * a compatible annotation it must apply to the static state only.
         * 
         * If I apply to instance, then the super interfaces can apply to anything
         * because if they apply to static only, it's like they are unannotated.
         * But the super interface must have a compatible annotation.
         */
        @Override
        protected boolean checkAnnotatedInterfaceSuperInterface(
            final ContainableNode node, final IRNode iDecl, final IRNode sDecl) {
          // Nothing to check: never applies to the static part only
          return true;
        }

        @Override
        protected boolean checkAnnotatedClassSuperInterface(
            final ContainableNode node, final IRNode cDecl, final IRNode iDecl,
            final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> iDrop) {
          // Nothing to check
          return true;
        }

        @Override
        protected boolean checkAnnotatedClassSuperClass(
            final ContainableNode node, final IRNode iDecl, final IRNode sDecl) {
          // Nothing to check
          return true;
        }
      };
    }    
  }  
  
  static Part computeAppliesTo(IAnnotationParsingContext context, int offset) {
  	final String raw = context.getProperty(AbstractModifiedBooleanNode.APPLIES_TO);
	if (raw == null) {
		return Part.InstanceAndStatic;
	} 
	try {
		return Part.valueOf(raw);
	} catch(IllegalArgumentException e) {
		context.reportError(offset, "Unknown Part specified: "+raw);
		return null;
	}
  }
  
  public static class ThreadSafe_ParseRule 
  extends SimpleBooleanAnnotationParseRule<ThreadSafeNode,ThreadSafePromiseDrop> {
    public ThreadSafe_ParseRule() {
      super(THREAD_SAFE, typeDeclOps, ThreadSafeNode.class);
    }
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
    	Part part = computeAppliesTo(context, offset);
    	if (part == null) {
    		return null;
    	}
    	return new ThreadSafeNode(mods, part);
    }
    @Override
    protected IPromiseDropStorage<ThreadSafePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), ThreadSafePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new TypeAnnotationWithAppliesToScrubber<ThreadSafeNode, ThreadSafePromiseDrop, NotThreadSafePromiseDrop>(this, true, "ThreadSafe", "NotThreadSafe", IMMUTABLE, NOT_THREAD_SAFE) {
        @Override
        protected ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> getAnnotation(final IRNode superDecl) {
          return getThreadSafeImplementation(superDecl);
        }

        @Override
        protected ThreadSafePromiseDrop createDrop(final ThreadSafeNode node) {
          return new ThreadSafePromiseDrop(node);
        }

        @Override
        protected NotThreadSafePromiseDrop getNotAnnotation(final IRNode typeDecl) {
          return getNotThreadSafe(typeDecl);
        }

        /* typeDecl is a class not annotated with ThreadSafe.  We want to know
         * if is actually annotated with Immutable, which is more specific
         * than ThreadSafe.
         */
        @Override
        protected boolean isMoreSpecific(final IRNode typeDecl) {
          final ImmutablePromiseDrop iDrop = getImmutableImplementation(typeDecl);
          return iDrop != null && !isStaticOnly(iDrop);
        }
        
        /* classDecl is the superclass of a class annotated with ThreasSafe.
         * We want to know if it is actually annotated with
         * @Immutable(implementationOnly=true), which is less specific than 
         * @ThreadSafe.
         */
        @Override
        protected boolean isLessSpecific(IRNode classDecl) {
          final ImmutablePromiseDrop iDrop = getImmutableImplementation(classDecl);
          return (iDrop != null) && iDrop.isImplementationOnly() && !isStaticOnly(iDrop);
        }
        
        @Override
        protected ThreadSafeNode makeDerivedAnnotation(
            final int mods, ThreadSafeNode orig) {
          return new ThreadSafeNode(mods, orig.getAppliesTo());
        }

        /* If I apply to static only, and the super interfaces is annotated with
         * a compatible annotation it must apply to the static state only.
         * 
         * If I apply to instance, then the super interfaces can apply to anything
         * because if they apply to static only, it's like they are unannotated.
         * But the super interface must have a compatible annotation.
         */
        @Override
        protected boolean checkAnnotatedInterfaceSuperInterface(
            final ThreadSafeNode node, final IRNode iDecl, final IRNode sDecl) {
          if (isStaticOnly(node)) {
            final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> sDrop = getThreadSafeImplPromise(sDecl);
            if (sDrop != null && !isStaticOnly(sDrop)) {
              getContext().reportModelingProblem(node,
                  "Interface may not apply @ThreadSafe to the static part only because super interface {0} applies @{1} to the instance state",
                  JavaNames.getRelativeTypeNameDotSep(sDecl), sDrop.getToken());
              return false;
            } else {
              return true;
            }
          } else {
            /* Check that the super interface is not @Immutable, unless it 
             * applies to the static state only 
             */
            final ImmutablePromiseDrop iDrop = getImmutableImplementation(sDecl);
            if (iDrop != null && !isStaticOnly(iDrop)) {
              getContext().reportModelingProblem(node, 
                  "Interface may not be @ThreadSafe because it extends the @Immutable interface {0}",
                  JavaNames.getRelativeTypeNameDotSep(sDecl));
              return false;
            } else {
              return true;
            }
          }
        }

        /* We have a class annotated with @ThreadSafe.  If the class applies
         * the annotation to the static part only, and the super interface
         * is annotated with a compatible annotation, then it must apply to the
         * static state only.
         * 
         * If the class applies the annotation to the instance state, then 
         * the super interface may be unannotated, or annotated with a compatible
         * annotation and apply to anything.  (Nothing to check in this case
         * for Immutable) 
         */
        @Override
        protected boolean checkAnnotatedClassSuperInterface(
            final ThreadSafeNode node, final IRNode cDecl, final IRNode iDecl,
            final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> iDrop) {
          if (isStaticOnly(node)) {
            if (iDrop != null && !isStaticOnly(iDrop)) {
              getContext().reportModelingProblem(node,
                  "Class may not apply @ThreadSafe to the static part only because super interface {0} applies @{1} to the instance state",
                  JavaNames.getRelativeTypeNameDotSep(iDecl), iDrop.getToken());
              return false;
            } else {
              return true;
            }
          } else {
            /* super interface may not be immutable, unless it applies
             * to the state state only.
             */
            final ImmutablePromiseDrop immutableDrop = getImmutableImplementation(iDecl);
            if (immutableDrop != null && !isStaticOnly(immutableDrop)) {
              getContext().reportModelingProblem(node, 
                  "Class may not be @ThreadSafe because it implements the @Immutable interface {0}",
                  JavaNames.getRelativeTypeNameDotSep(iDecl));
              return false;
            } else {
              return true;
            }
          }
        }

        /* We have a class annotated with @ThreadSafe.  If the class applies
         * the annotation to the static part only, the super class must be
         * unannotated or @ThreadSafe/@Immutable and implementationOnly, or @ThreadSafe
         * and only apply to the static part.
         * 
         * If the class applies to the instance state, then the super class
         * must be @ThreadSafe and apply to the instance state.
         */
        @Override
        protected boolean checkAnnotatedClassSuperClass(
            final ThreadSafeNode node, final IRNode iDecl, final IRNode sDecl) {
          if (isStaticOnly(node)) {
            final ModifiedBooleanPromiseDrop<?> iDrop = getThreadSafeImplPromise(sDecl);
            if (iDrop != null) {
              if (!iDrop.isImplementationOnly()) {
                if (!isStaticOnly(iDrop)) {
                  getContext().reportModelingProblem(node,
                      "Class may not apply @ThreadSafe to the static part only because the super class {0} applies @{1} to the instance state",
                          JavaNames.getRelativeTypeNameDotSep(sDecl), iDrop.getToken());
                  return false;
                } else {
                  return true;
                }
              } else {
                return true;
              }
            } else {
              return true;
            }
          } else {
            final ImmutablePromiseDrop iDrop = getImmutableImplementation(sDecl);
            if (iDrop != null && !iDrop.isImplementationOnly()) {
              getContext().reportModelingProblem(node, 
                  "Class may not be @ThreadSafe because it extends the @Immutable(implementationOnly=false) class {0}",
                  JavaNames.getRelativeTypeNameDotSep(sDecl));
              return false;
            } else {
              return true;
            }
          }
        }
      };
    }    
  }
  
//  public static class NotThreadSafe_ParseRule 
//  extends MarkerAnnotationParseRule<NotThreadSafeNode,NotThreadSafePromiseDrop> {
//    public NotThreadSafe_ParseRule() {
//      super(NOT_THREAD_SAFE, typeDeclOps, NotThreadSafeNode.class);
//    }
//    @Override
//    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
//      Part part = computeAppliesTo(context, offset);
//      if (part == null) {
//    	  return null;
//      }    	
//      return new NotThreadSafeNode(part);
//    }
//    @Override
//    protected IPromiseDropStorage<NotThreadSafePromiseDrop> makeStorage() {
//      return BooleanPromiseDropStorage.create(name(), NotThreadSafePromiseDrop.class);
//    }
//	@Override
//	protected NotThreadSafePromiseDrop createDrop(NotThreadSafeNode a) {
//		return new NotThreadSafePromiseDrop(a);
//	}    
//  }

  public static class NotThreadSafe_ParseRule
  extends SimpleBooleanAnnotationParseRule<NotThreadSafeNode, NotThreadSafePromiseDrop> {
    public NotThreadSafe_ParseRule() {
      super(NOT_THREAD_SAFE, typeDeclOps, NotThreadSafeNode.class);
    }
    
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      Part part = computeAppliesTo(context, offset);
      if (part == null) {
        return null;
      }
      return new NotThreadSafeNode(part);
    }
    
    @Override
    protected IPromiseDropStorage<NotThreadSafePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), NotThreadSafePromiseDrop.class);
    }

    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<NotThreadSafeNode, NotThreadSafePromiseDrop>(this) {
        @Override
        protected PromiseDrop<NotThreadSafeNode> makePromiseDrop(
            NotThreadSafeNode a, boolean isAssumption) {
          return storeDropIfNotNull(a, scrubNotThreadSafe(getContext(), a));
        }
        
        private NotThreadSafePromiseDrop scrubNotThreadSafe(
            final AnnotationScrubberContext context, final NotThreadSafeNode a) {
          final IRNode promisedFor = a.getPromisedFor();
          if (AnnotationDeclaration.prototype.includes(promisedFor)) {
            // Applies to can not be exclusively "instance"
            if (a.getAppliesTo() == Part.Instance) {
              context.reportModelingProblem(a, "Annotations never have any instance state");
              return null;
            }
          }
          return new NotThreadSafePromiseDrop(a);
        }
      };
    }
  }

  public static class NotContainable_ParseRule 
  extends MarkerAnnotationParseRule<NotContainableNode,NotContainablePromiseDrop> {
    public NotContainable_ParseRule() {
      super(NOT_CONTAINABLE, typeDeclOps, NotContainableNode.class);
    }
    
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      return new NotContainableNode();
    }
    
    @Override
    protected IPromiseDropStorage<NotContainablePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), NotContainablePromiseDrop.class);
    }

    @Override
  	protected NotContainablePromiseDrop createDrop(NotContainableNode a) {
  		return new NotContainablePromiseDrop(a);
  	}    
  }
  
//  public static class Mutable_ParseRule 
//  extends MarkerAnnotationParseRule<MutableNode,MutablePromiseDrop> {
//    public Mutable_ParseRule() {
//      super(MUTABLE, typeDeclOps, MutableNode.class);
//    }
//    @Override
//    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
//      Part part = computeAppliesTo(context, offset);
//      if (part == null) {
//    	  return null;
//      }
//      return new MutableNode(mods, part);
//    }
//    @Override
//    protected IPromiseDropStorage<MutablePromiseDrop> makeStorage() {
//      return BooleanPromiseDropStorage.create(name(), MutablePromiseDrop.class);
//    }
//	@Override
//	protected MutablePromiseDrop createDrop(MutableNode a) {
//		return new MutablePromiseDrop(a);
//	}    
//  }

  public static class Mutable_ParseRule
  extends SimpleBooleanAnnotationParseRule<MutableNode, MutablePromiseDrop> {
    public Mutable_ParseRule() {
      super(MUTABLE, typeDeclOps, MutableNode.class);
    }
    
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      Part part = computeAppliesTo(context, offset);
      if (part == null) {
        return null;
      }
      return new MutableNode(mods, part);
    }
    
    @Override
    protected IPromiseDropStorage<MutablePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), MutablePromiseDrop.class);
    }

    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new AbstractAASTScrubber<MutableNode, MutablePromiseDrop>(this) {
        @Override
        protected PromiseDrop<MutableNode> makePromiseDrop(
            MutableNode a, boolean isAssumption) {
          return storeDropIfNotNull(a, scrubMutable(getContext(), a));
        }
        
        private MutablePromiseDrop scrubMutable(
            final AnnotationScrubberContext context, final MutableNode a) {
          final IRNode promisedFor = a.getPromisedFor();
          if (AnnotationDeclaration.prototype.includes(promisedFor)) {
            // Applies to can not be exclusively "instance"
            if (a.getAppliesTo() == Part.Instance) {
              context.reportModelingProblem(a, "Annotations never have any instance state");
              return null;
            }
          }
          return new MutablePromiseDrop(a);
        }
      };
    }
  }
  
  public static class ImmutableParseRule 
  extends SimpleBooleanAnnotationParseRule<ImmutableNode,ImmutablePromiseDrop> {
    public ImmutableParseRule() {
      // Was typeDeclOps
      super(IMMUTABLE, typeFuncFieldParamDeclOps, ImmutableNode.class);
    }
    @Override
    protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
      Part part = computeAppliesTo(context, offset);
      if (part == null) {
    	return null;
      }
      return new ImmutableNode(mods, part);
    }
    @Override
    protected IPromiseDropStorage<ImmutablePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), ImmutablePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber makeScrubber() {
      return new TypeAnnotationWithAppliesToScrubber<ImmutableNode,ImmutablePromiseDrop, MutablePromiseDrop>(this, true, "Immutable", "Mutable", MUTABLE, NOT_THREAD_SAFE) {
        @Override
        protected ImmutablePromiseDrop getAnnotation(final IRNode superDecl) {
          return getImmutableImplementation(superDecl);
        }
        
        @Override
        protected ImmutablePromiseDrop createDrop(final ImmutableNode node) {
          return new ImmutablePromiseDrop(node);
        }

        @Override
        protected MutablePromiseDrop getNotAnnotation(final IRNode typeDecl) {
          return getMutable(typeDecl);
        }
        
        @Override
        protected ImmutableNode makeDerivedAnnotation(
            final int mods, ImmutableNode orig) {
          return new ImmutableNode(mods, orig.getAppliesTo());
        }

        /* If I apply to static only, and the super interfaces is annotated with
         * a compatible annotation it must apply to the static state only.
         * 
         * If I apply to instance, then the super interfaces can apply to anything
         * because if they apply to static only, it's like they are unannotated.
         * But the super interface must have a compatible annotation.
         */
        @Override
        protected boolean checkAnnotatedInterfaceSuperInterface(
            final ImmutableNode node, final IRNode iDecl, final IRNode sDecl) {
          if (isStaticOnly(node)) {
            final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> sDrop = getThreadSafeImplPromise(sDecl);
            if (sDrop != null && !isStaticOnly(sDrop)) {
              getContext().reportModelingProblem(node,
                  "Interface may not apply @Immutable to the static part only because super interface {0} applies @{1} to the instance state",
                  JavaNames.getRelativeTypeNameDotSep(sDecl), sDrop.getToken());
              return false;
            } else {
              return true;
            }
          } else {
            // Nothing to check: Super interface may be ThreadSafe
            return true;
          }
        }

        /* We have a class annotated with @Immutable.  If the class applies
         * the annotation to the static part only, and the super interface
         * is annotated with a compatible annotation, then it must apply to the
         * static state only.
         * 
         * If the class applies the annotation to the instance state, then 
         * the superface may be unannotated, or annotated with a compatible
         * annotation and apply to anything.  (Nothing to check in this case
         * for Immutable) 
         */
        @Override
        protected boolean checkAnnotatedClassSuperInterface(
            final ImmutableNode node, final IRNode cDecl, final IRNode iDecl,
            final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> iDrop) {
          if (isStaticOnly(node)) {
            if (iDrop != null && !isStaticOnly(iDrop)) {
              getContext().reportModelingProblem(node,
                  "Class may not apply @Immutable to the static part only because super interface {0} applies @{1} to the instance state",
                  JavaNames.getRelativeTypeNameDotSep(iDecl), iDrop.getToken());
              return false;
            } else {
              return true;
            }
          } else {
            // Nothing to check: Implemented interfaces may be ThreadSafe
            return true;
          }
        }

        /* We have a class annotated with @Immutable.  If the class applies
         * the annotation to the static part only, the super class must be
         * unannotated, implementationOnly, or also only apply to the static part.
         * 
         * If the class applies to the instance state, then the super class
         * must be immutable and apply to the instance state.
         */
        @Override
        protected boolean checkAnnotatedClassSuperClass(
            final ImmutableNode node, final IRNode iDecl, final IRNode sDecl) {
          final ImmutablePromiseDrop iDrop = getImmutableImplementation(sDecl);
          if (isStaticOnly(node)) {
            if (iDrop != null && !iDrop.isImplementationOnly() && !isStaticOnly(iDrop)) {
              getContext().reportModelingProblem(node,
                  "Class may not apply @Immutable to the static part only because the super class {0} applies @Immutable to the instance state",
                  JavaNames.getRelativeTypeNameDotSep(sDecl));
              return false;
            } else {
              return true;
            }
          } else {
            return true;
          }
        }
        
        @Override
        protected boolean moreChecks(final ImmutableNode p, final IRNode promisedFor) {
          if (getNotThreadSafe(promisedFor) != null) {
            getContext().reportModelingProblem(p, "Cannot be @Immutable and @NotThreadSafe");
            return false;
          }
          
          return true;
        }
      };
    }    
  }
  
  private static final class LockFieldVisibilityScrubber extends SimpleScrubber {
    public LockFieldVisibilityScrubber() {
      super(LOCK_FIELD_VISIBILITY, LOCK, RETURNS_LOCK);
    }
    
    @Override
    protected void scrub() {
      for (LockDeclarationNode lockDecl : AASTStore.getASTsByClass(LockDeclarationNode.class)) {
        if (lockDecl.getStatus() == AASTStatus.VALID) { 
          checkVisibilityOfField(getContext(), lockDecl);
        }
      }
    }

    private void checkVisibilityOfField(
        final AnnotationScrubberContext context, final LockDeclarationNode lockDecl) {
      final ExpressionNode field = lockDecl.getField();
      final RegionNameNode region = lockDecl.getRegion();
      final IRegionBinding regionBinding = region.resolveBinding();
      final Visibility lockViz = getLockFieldVisibility(lockDecl, context.getBinder(lockDecl.getPromisedFor()));
      final Visibility regionViz = regionBinding.getModel().getVisibility();
      if (!lockViz.atLeastAsVisibleAs(regionViz)) { // (5)
        /* We create this as a warning drop instead of a modeling error because
         * it doesn't break anything, it only means that the lock model isn't
         * as useful as they probably mean it to be.
         */
        final String qualifiedName = computeQualifiedName(lockDecl);
        final LockModel model = LockModel.getInstance(qualifiedName, lockDecl.getPromisedFor()); 
        final ModelingProblemDrop wd = new ModelingProblemDrop(lockDecl.getPromisedFor());
        wd.setMessage(com.surelogic.analysis.concurrency.driver.Messages.LockAnalysis_ds_LockViz, field, lockViz.nameLowerCase(), region, regionViz.nameLowerCase());
        wd.setCategorizingMessage(com.surelogic.analysis.concurrency.driver.Messages.DSC_LOCK_VIZ);
        model.addDependent(wd);
      }
    }
  }

  private static final class InitRegionSet extends SimpleScrubber {
    private final IProtectedRegions protectedRegions;
    
    public InitRegionSet(final IProtectedRegions pr) {
      super(REGION_INITIALIZER);
      protectedRegions = pr;
    }
  
    @Override
    protected void scrub() {
      // All we do is clear the set of protected regions
      protectedRegions.clear();
    }
  }

  /**
   * Do a tree-based comparison of two lock specifications to determine if they are
   * structurally equal, that is, name the same lock. Really this should be a
   * feature of the AASTNodes, but no one else needs it yet.
   * 
   * @param spec1
   *          The first LockSpecificationNode.
   * @param spec2
   *          The Second LockSpecificationNode.
   * @return <code>true</code> iff the two trees are identical.
   */
  private static boolean areLockSpecificationsEqual(
      final LockSpecificationNode spec1, final LockSpecificationNode spec2) {
    if (((spec1 instanceof ReadLockNode) && (spec2 instanceof ReadLockNode)) ||
        ((spec1 instanceof WriteLockNode) && (spec2 instanceof WriteLockNode))) {
      return areLockNamesEqual(spec1.getLock(), spec2.getLock());      
    } else if ((spec1 instanceof LockNameNode) && (spec2 instanceof LockNameNode)) {
      return areLockNamesEqual((LockNameNode) spec1, (LockNameNode) spec2);
    }
    return false;
  }

  /**
   * Do a tree-based comparison of two lock names to determine if they are
   * structurally equal, that is, name the same lock. Really this should be a
   * feature of the AASTNodes, but no one else needs it yet.
   * 
   * @param name1
   *          The first LockNameNode.
   * @param name2
   *          The Second LockNameNode.
   * @return <code>true</code> iff the two trees are identical.
   */
  private static boolean areLockNamesEqual(
      final LockNameNode name1, final LockNameNode name2) {
    final String id1 = name1.getId();
    final String id2 = name2.getId();
    if (id1.equals(id2)) {
      if (name1 instanceof SimpleLockNameNode && name2 instanceof SimpleLockNameNode) {
        return true;
      } else if (name1 instanceof QualifiedLockNameNode && name2 instanceof QualifiedLockNameNode) {
        final ExpressionNode base1 = ((QualifiedLockNameNode) name1).getBase();
        final ExpressionNode base2 = ((QualifiedLockNameNode) name2).getBase();
        if ((base1 instanceof ThisExpressionNode) && (base2 instanceof ThisExpressionNode)) {
          return true;
        } else if ((base1 instanceof VariableUseExpressionNode) && (base2 instanceof VariableUseExpressionNode)) {   
          final String var1 = ((VariableUseExpressionNode) base1).getId();
          final String var2 = ((VariableUseExpressionNode) base2).getId();
          return var1.equals(var2);
        } else if ((base1 instanceof TypeExpressionNode) && (base2 instanceof TypeExpressionNode)) {
          final NamedTypeNode namedType1 = (NamedTypeNode) ((TypeExpressionNode) base1).getType();
          final NamedTypeNode namedType2 = (NamedTypeNode) ((TypeExpressionNode) base2).getType();
          return namedType1.getType().equals(namedType2.getType());
        } else if ((base1 instanceof QualifiedThisExpressionNode) && (base2 instanceof QualifiedThisExpressionNode)) {
          final NamedTypeNode namedType1 = (NamedTypeNode) ((QualifiedThisExpressionNode) base1).getType();
          final NamedTypeNode namedType2 = (NamedTypeNode) ((QualifiedThisExpressionNode) base2).getType();
          return namedType1.getType().equals(namedType2.getType());
        } else {
          // incomparable subtrees
          return false;
        }
      } else {
        /* One of the locks is a SimpleLockName, and the other is a
         * QualifiedLockName. The problem is that the simple name could be the
         * name of an instance lock, in which case the qualified name needs to
         * contain a ThisExpression, or it can be the name of a static lock, in
         * which case the qualified name needs to be a TypeExpression that names
         * the type of the class being annotated. I don't know of any way of
         * determining whether the lock is static or not other than to bind the
         * lock. We only need to check whether the qualified lock name refers to
         * a this expression or a type expression and both lock names
         * resolve to the same LockModel.
         */
        final ExpressionNode base;
        if (name1 instanceof QualifiedLockNameNode) {
          base = ((QualifiedLockNameNode) name1).getBase();
        } else {
          base = ((QualifiedLockNameNode) name2).getBase();
        }
        if ((base instanceof ThisExpressionNode) || (base instanceof TypeExpressionNode)) {
          final LockModel model1 = name1.resolveBinding().getModel(); 
          final LockModel model2 = name2.resolveBinding().getModel(); 
          return model1.equals(model2);
        } else {
          return false;
        }
      }
    } else {
      // Names don't match, cannot be the same lock
      return false;
    }
  } 
  
  static final Object DEFAULT_TO_NORMAL_VOUCH = null;
  
	static class VouchFieldIs_ParseRule extends DefaultSLAnnotationParseRule<VouchFieldIsNode,VouchFieldIsPromiseDrop> {
	  VouchFieldIs_ParseRule() {
			super(VOUCH_FIELD_IS, varDeclOps, VouchFieldIsNode.class);
		}

		// Should only get called by the Vouch parse rule
		@Override
		public Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) 
		throws Exception {
			final String id = context.getAllText().trim();
			final FieldKind kind;
			final Operator op = context.getOp();
			if (op instanceof ParameterDeclaration) {
				if (!ANNO_BOUNDS.equals(id)) {
					context.reportError(0, "Illegal vouch for parameter: "+id);
					return ParseResult.FAIL;
				}
				kind = FieldKind.AnnotationBounds;
			} else if (op instanceof DeclStatement) {
				// AnnoBounds, NonNull or Nullable
				try {
					kind = FieldKind.valueOf(id);				
				} catch(IllegalArgumentException e) {					
					context.reportError(0, "Unknown vouch for parameter: "+id);
					return ParseResult.FAIL;
				}
				if (kind.ordinal() < FieldKind.AnnotationBounds.ordinal()) {
					context.reportError(0, "Illegal vouch for parameter: "+id);
					return ParseResult.FAIL;
				}
			} else if ("final".equals(id)) {
				kind = FieldKind.Final;
			} else {				
				try {
					kind = FieldKind.valueOf(id);				
				} catch(IllegalArgumentException e) {					
					return DEFAULT_TO_NORMAL_VOUCH;
				}
			}
			return new VouchFieldIsNode(context.mapToSource(0), kind, context.getProperty(VouchFieldIsNode.REASON));
		}
		
		@Override
		protected IPromiseDropStorage<VouchFieldIsPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(), VouchFieldIsPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber makeScrubber() {
			return new AbstractAASTScrubber<VouchFieldIsNode, VouchFieldIsPromiseDrop>(
			    this, ScrubberType.UNORDERED, NOT_THREAD_SAFE, NOT_CONTAINABLE, 
			    MUTABLE) {
				@Override
				protected PromiseDrop<VouchFieldIsNode> makePromiseDrop(VouchFieldIsNode a) {
					return storeDropIfNotNull(a, scrubVouchFieldIs(getContext(), a));
				}
			};
		}
	}

	public static class ThreadConfined_ParseRule 
	extends MarkerAnnotationParseRule<ThreadConfinedNode,ThreadConfinedPromiseDrop> {
		public ThreadConfined_ParseRule() {
			super(THREAD_CONFINED, fieldDeclOp, ThreadConfinedNode.class);
		}
		@Override
		protected IAASTRootNode makeAAST(IAnnotationParsingContext context, int offset, int mods) {
			return new ThreadConfinedNode();
		}
		@Override
		protected IPromiseDropStorage<ThreadConfinedPromiseDrop> makeStorage() {
			return BooleanPromiseDropStorage.create(name(), ThreadConfinedPromiseDrop.class);
		}
		@Override
		protected ThreadConfinedPromiseDrop createDrop(ThreadConfinedNode a) {
			return new ThreadConfinedPromiseDrop(a);
		}    
	}
}
