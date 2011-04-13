package com.surelogic.annotation.rules;

import java.util.*;

import org.antlr.runtime.*;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.*;
import com.surelogic.aast.java.*;
import com.surelogic.aast.promise.*;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.JavaProjects;
import com.surelogic.analysis.locks.FieldKind;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.*;
import com.surelogic.annotation.parse.*;
import com.surelogic.annotation.scrub.*;
import com.surelogic.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.ModifiedBooleanPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

public class LockRules extends AnnotationRules {
  public static final String LOCK = "RegionLock";
	private static final String IS_LOCK = "IsLock";
	private static final String REQUIRES_LOCK = "RequiresLock";
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
  
	private static final AnnotationRules instance = new LockRules();

	private static final IProtectedRegions protectedRegions = new ProtectedRegions();
	
	private static final InitRegionSet initRegionSet = new InitRegionSet(protectedRegions);
  private static final Lock_ParseRule lockRule = new Lock_ParseRule(protectedRegions);
	private static final PolicyLock_ParseRule policyRule = new PolicyLock_ParseRule();
	private static final IsLock_ParseRule isLockRule = new IsLock_ParseRule();
	private static final RequiresLock_ParseRule requiresLockRule = new RequiresLock_ParseRule();
	private static final ReturnsLock_ParseRule returnsLockRule = new ReturnsLock_ParseRule();
	//private static final ProhibitsLock_ParseRule prohibitsLockRule = new ProhibitsLock_ParseRule();
  private static final Containable_ParseRule containableRule = new Containable_ParseRule();
  private static final ThreadSafe_ParseRule threadSafeRule = new ThreadSafe_ParseRule();
  private static final NotThreadSafe_ParseRule notThreadSafeRule = new NotThreadSafe_ParseRule();
  private static final ImmutableParseRule immutableRule = new ImmutableParseRule();
  private static final Mutable_ParseRule mutableRule = new Mutable_ParseRule();
  private static final NotContainable_ParseRule notContainableRule = new NotContainable_ParseRule();
  private static final VouchFieldIs_ParseRule vouchFieldIsRule = new VouchFieldIs_ParseRule();
  
  private interface IProtectedRegions {
	  void clear();
	  boolean addIfNotAlreadyProtected(ITypeEnvironment tenv, String qualifiedRegionName,
		        final IJavaDeclaredType clazz);
  }
  
  private static class ProtectedRegions implements IProtectedRegions {
	private final Map<String,IProtectedRegions> projects = 
		new HashMap<String,IProtectedRegions>();
	  
	public boolean addIfNotAlreadyProtected(ITypeEnvironment tenv, 
			String qualifiedRegionName, IJavaDeclaredType clazz) {
		final IIRProject p = JavaProjects.getEnclosingProject(clazz.getDeclaration());
		IProtectedRegions state = projects.get(p.getName());
		if (state == null) {
			state = new Project_ProtectedRegions();
			projects.put(p.getName(), state);
		}
		return state.addIfNotAlreadyProtected(tenv, qualifiedRegionName, clazz);
	}

	public void clear() {
		projects.clear();
	}
  }
  
  private static class Project_ProtectedRegions implements IProtectedRegions {
    private final Map<String, Set<IJavaType>> protectedRegions = new HashMap<String, Set<IJavaType>>();
    
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

	public static IsLockPromiseDrop getIsLock(IRNode vdecl) {
		return getDrop(isLockRule.getStorage(), vdecl);
	}
	  
  public static boolean isContainableType(final IRNode cdecl) {
    return getContainableType(cdecl) != null;
  }
  
  /**
   * Return whether the type is thread safe.  Only returns the promise drop if
   * the drop is not implementation only.
   * @param cdecl
   * @return
   */
  public static ContainablePromiseDrop getContainableType(final IRNode cdecl) {
    final ContainablePromiseDrop drop = getBooleanDrop(containableRule.getStorage(), cdecl);
    if (drop == null) {
      return null;
    } else {
      return drop.isImplementationOnly() ? null : drop;
    }
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
  
  public static boolean isThreadSafeType(final IRNode cdecl) {
    return getThreadSafeType(cdecl) != null;
  }
  
  /**
   * Return whether the type is thread safe.  Only returns the promise drop if
   * the drop is not implementation only.
   * @param cdecl
   * @return
   */
  public static ThreadSafePromiseDrop getThreadSafeType(final IRNode cdecl) {
    final ThreadSafePromiseDrop drop = getBooleanDrop(threadSafeRule.getStorage(), cdecl);
    if (drop == null) {
      return null;
    } else {
      return drop.isImplementationOnly() ? null : drop;
    }
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

  public static boolean isImmutableType(final IRNode cdecl) {
    return getImmutableType(cdecl) != null;
  }
  
  /**
   * Return whether the type is immutable.  Only returns the promise drop if
   * the drop is not implementation only.
   * @param cdecl
   * @return
   */
  public static ImmutablePromiseDrop getImmutableType(final IRNode cdecl) {
    final ImmutablePromiseDrop drop = getBooleanDrop(immutableRule.getStorage(), cdecl);
    if (drop == null) {
      return null;
    } else {
      return drop.isImplementationOnly() ? null : drop;
    }
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
  
  public static VouchFieldIsPromiseDrop getVouchFieldIs(final IRNode fieldDecl) {
    return getDrop(vouchFieldIsRule.getStorage(), fieldDecl);
  }
  
  @Override
  public void register(PromiseFramework fw) {
    registerScrubber(fw, initRegionSet);
		registerParseRuleStorage(fw, policyRule);
		registerParseRuleStorage(fw, lockRule);
		registerParseRuleStorage(fw, isLockRule);
		registerParseRuleStorage(fw, requiresLockRule);
		registerParseRuleStorage(fw, returnsLockRule);
		//registerParseRuleStorage(fw, prohibitsLockRule);
    registerParseRuleStorage(fw, containableRule);
    registerParseRuleStorage(fw, threadSafeRule);
    registerParseRuleStorage(fw, notThreadSafeRule);
    registerParseRuleStorage(fw, immutableRule);
    registerParseRuleStorage(fw, mutableRule);
    registerParseRuleStorage(fw, notContainableRule);
    registerParseRuleStorage(fw, vouchFieldIsRule);
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
		protected IAnnotationScrubber<ReturnsLockNode> makeScrubber() {
			return new AbstractAASTScrubber<ReturnsLockNode, ReturnsLockPromiseDrop>(this,
					ScrubberType.UNORDERED, LOCK, POLICY_LOCK) {
				@Override
        protected PromiseDrop<ReturnsLockNode> makePromiseDrop(
						ReturnsLockNode a) {
					return storeDropIfNotNull(a, scrubReturnsLock(getContext(), a));
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
			IAnnotationScrubberContext context, ReturnsLockNode node) {

		ReturnsLockPromiseDrop returnDrop = null;

		final IRNode returnNode = node.getPromisedFor();
		if (returnNode != null) {
		  // Get the method that the returnNode is associated with
		  final IRNode annotatedMethod = JavaPromise.getParentOrPromisedFor(returnNode);
      final LockNameNode lockName = node.getLock();
			if (lockName != null) {
				/*
				 * >> LocksOK << --- Lock name is bindable
				 */
				// Check if the lock name is good; if not we cannot continue
				final LockModel lockDecl = isLockNameOkay(JavaNode.getModifier(
				    annotatedMethod, JavaNode.STATIC), lockName, context);
				if (lockDecl != null) {
					returnDrop = new ReturnsLockPromiseDrop(node);
					lockDecl.addDependent(returnDrop);
				}
			}
		}
		return returnDrop;
	}

	public static class ProhibitsLock_ParseRule
	extends
	DefaultSLAnnotationParseRule<ProhibitsLockNode, ProhibitsLockPromiseDrop> {
		public ProhibitsLock_ParseRule() {
			super(PROHIBITS_LOCK, methodDeclOps, ProhibitsLockNode.class);
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
		protected IAnnotationScrubber<ProhibitsLockNode> makeScrubber() {
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
			IAnnotationScrubberContext context, ProhibitsLockNode node) {
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
			super(REQUIRES_LOCK, methodDeclOps, RequiresLockNode.class);
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
		protected IAnnotationScrubber<RequiresLockNode> makeScrubber() {
		  /* Order by hierarchy so that we know that any ancestral RequiresLock
		   * annotations are scrubbed.
		   */
			return new AbstractAASTScrubber<RequiresLockNode, RequiresLockPromiseDrop>(this,
					ScrubberType.BY_HIERARCHY, LOCK,
					POLICY_LOCK, RETURNS_LOCK) {
				@Override
        protected PromiseDrop<RequiresLockNode> makePromiseDrop(
						RequiresLockNode a) {
					return storeDropIfNotNull(a, scrubRequiresLock(getContext(), a));

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
			IAnnotationScrubberContext context, RequiresLockNode node) {
		final IRNode promisedFor = node.getPromisedFor();
		final Operator promisedForOp = JJNode.tree.getOperator(promisedFor);

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
			  isLockNameOkay(TypeUtil.isStatic(promisedFor), lockName, context);
			if (lockModel != null) {
				lockDecls.add(lockModel);

				if (namesReadWriteLock) {
	        /*
	         * We can only name a read lock or a write lock if the underlying
	         * lock is a ReadWriteLock.
	         */
				  if (!lockModel.getAST().isReadWriteLock()) {
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
  				if (lockModel.getAST().isReadWriteLock()) {
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
				}
				else if (lockName instanceof SimpleLockNameNode) {
					lockRefsThis = !lockModel.getAST().isLockStatic();
				}
				else {
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
				if (lockModel.getAST().isLockStatic()) {
					final IRNode lockDeclClass =
					  VisitUtil.getClosestType(lockModel.getNode());
					final IRNode methodDeclClass =
					  VisitUtil.getEnclosingType(promisedFor);
					staticLockFromSameClass =
					  lockDeclClass.equals(methodDeclClass);
				}
				if (lockRefsThis || staticLockFromSameClass) {
					final Visibility lockViz =
					  getLockFieldVisibility(lockModel.getAST(), context.getBinder());
					final Visibility methodViz = Visibility.getVisibilityOf(promisedFor);
					if (!lockViz.atLeastAsVisibleAs(methodViz)) {
						context.reportError(
						    "lock \"" + lockSpec.getLock().getId() +
						    "\" is less visible than requiring method/constructor", //$NON-NLS-1$
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
    RequiresLockPromiseDrop drop = getRequiresLock(promisedFor);
    if (drop != null) {
      drop.invalidate();
    }
    
    // Check for consistency with ancestors
    if (allGood) {
//    for (final IBinding context : scrubberContext.getBinder().findOverriddenParentMethods(promisedFor)) {
//    final IRNode overriddenMethod = context.getNode();
      
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
		protected IAnnotationScrubber<LockDeclarationNode> makeScrubber() {
			return new AbstractAASTScrubber<LockDeclarationNode, LockModel>(this,
					ScrubberType.BY_HIERARCHY, LockRules.REGION_INITIALIZER,
					RegionRules.REGIONS_DONE, VOUCH_FIELD_IS) {
				@Override
        protected PromiseDrop<AbstractLockDeclarationNode> makePromiseDrop(
						LockDeclarationNode a) {
					return AnnotationRules.storeDropIfNotNull(lockRule.getStorage(), a,
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
      final IAnnotationScrubberContext context,
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
          lockModel.addDependent(vouchFieldIs);
        }
      }
    }
    
    public abstract LockModel continueScrubbing(
        IAnnotationScrubberContext context,
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
        final IAnnotationScrubberContext context,
        final IProtectedRegions protectedRegions,
        final IJavaDeclaredType promisedForType,
        final LockDeclarationNode lockDecl,
        final boolean declIsGoodIn, final boolean fieldIsStatic,
        final IRNode lockFieldNode) {
      boolean declIsGood = declIsGoodIn;
      final IBinder binder = context.getBinder();
      final ExpressionNode field = lockDecl.getField();
      final RegionNameNode region = lockDecl.getRegion();
      final IRegionBinding regionBinding = region.resolveBinding();
      
      // Check that the region isn't already associated with a lock
      final String regionName = regionBinding.getModel().regionName;
      if (!protectedRegions.addIfNotAlreadyProtected(
          context.getBinder().getTypeEnvironment(), regionName, promisedForType)) {
        context.reportError(lockDecl, "Region \"{0}\" is already protected by a lock", regionName);
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
              + "\" should be protected by an instance field; consider including "+region+
              " in a static region", lockDecl); //$NON-NLS-1$
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
        final LockModel model = LockModel.getInstance(qualifiedName, lockDecl.getPromisedFor()); 
        model.setAST(lockDecl);
        model.setResultMessage(Messages.LockAnnotation_lockModel,
            model.getQualifiedName(), field, region,
            JavaNames.getTypeName(lockDecl.getPromisedFor()));
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
          final PromiseWarningDrop wd = new PromiseWarningDrop(com.surelogic.analysis.locks.Messages.toString(com.surelogic.analysis.locks.Messages.LockAnalysis_ds_UnsupportedModel));
          wd.setResultMessage(com.surelogic.analysis.locks.Messages.LockAnalysis_ds_UnsupportedModel);
          wd.setNodeAndCompilationUnitDependency(lockDecl.getPromisedFor());
          wd.setCategory(com.surelogic.analysis.locks.Messages.DSC_UNSUPPORTED_MODEL);
          model.addDependent(wd);
        }
                
        return model;
      }
      return null;
    }
  };
  
  private static final LockScrubContinuation<PolicyLockDeclarationNode> POLICY_LOCK_DECLARATION_CONTINUATION = new LockScrubContinuation<PolicyLockDeclarationNode>() {
    @Override
    public LockModel continueScrubbing(
        final IAnnotationScrubberContext context,
        final IProtectedRegions protectedRegions,
        final IJavaDeclaredType promisedForType,
        final PolicyLockDeclarationNode lockDecl, final boolean declIsGood,
        final boolean fieldIsStatic, final IRNode lockFieldNode) {
      final String qualifiedName = computeQualifiedName(lockDecl);     
      if (declIsGood) {
        final LockModel model = LockModel.getInstance(qualifiedName, lockDecl.getPromisedFor());
        model.setAST(lockDecl);
        model.setResultMessage(Messages.LockAnnotation_policyLockModel,
            model.getQualifiedName(), lockDecl.getField(), JavaNames.getTypeName(lockDecl
                .getPromisedFor()));
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
          final PromiseWarningDrop wd = new PromiseWarningDrop(com.surelogic.analysis.locks.Messages.toString(com.surelogic.analysis.locks.Messages.LockAnalysis_ds_UnsupportedModel));
          wd.setResultMessage(com.surelogic.analysis.locks.Messages.LockAnalysis_ds_UnsupportedModel);
          wd.setNodeAndCompilationUnitDependency(lockDecl.getPromisedFor());
          wd.setCategory(com.surelogic.analysis.locks.Messages.DSC_UNSUPPORTED_MODEL);
          model.addDependent(wd);
        }

        return model;
      }
      return null;
    }
  };
  
  private static VouchFieldIsPromiseDrop scrubVouchFieldIs(
      final IAnnotationScrubberContext context, final VouchFieldIsNode a) {
    final IRNode promisedFor = a.getPromisedFor();
    final IJavaType javaType = context.getBinder().getJavaType(promisedFor);
    switch (a.getKind()) {
    case Final:
      // Final: Must make sure the field is not actually declared final
      // Cannot use TypeUtils.isFinal() because that checks for @Vouch
      boolean isAlreadyFinal = false;
      if (TypeUtil.isInterface(VisitUtil.getEnclosingType(promisedFor))) {
        isAlreadyFinal = true; // declared in an interface
      } else {
        isAlreadyFinal = JavaNode.getModifier(JJNode.tree.getParent(
            JJNode.tree.getParent(promisedFor)), JavaNode.FINAL);
      }
      if (isAlreadyFinal) {
        context.reportError("Field is already declared to be final; no need to vouch it", a);
        return null;
      }
      break;
    case ThreadSafe:
      if (javaType instanceof IJavaPrimitiveType) {
        context.reportError(a, "Cannot be used on primitively typed field");
        return null;
      } else if ((javaType instanceof IJavaSourceRefType) && 
          (getNotThreadSafe(((IJavaSourceRefType) javaType).getDeclaration()) != null)) {
        context.reportError(a, "Cannot be used when the type of field is explicitly @NotThreadSafe");
        return null;
      }
      break;
    case Containable:
      if (javaType instanceof IJavaPrimitiveType) {
        context.reportError(a, "Cannot be used on a field with primitive type");
        return null;
      } else if ((javaType instanceof IJavaSourceRefType) && 
          (getNotContainable(((IJavaSourceRefType) javaType).getDeclaration()) != null)) {
        context.reportError(a, "Cannot be used when the type of field is explicitly @NotContainable");
        return null;
      }
      break;
    case Immutable:
      if (javaType instanceof IJavaPrimitiveType) {
        context.reportError(a, "Cannot be used on a field with primitive type");
        return null;
      } else if ((javaType instanceof IJavaSourceRefType) && 
          (getMutable(((IJavaSourceRefType) javaType).getDeclaration()) != null)) {
        context.reportError(a, "Cannot be used when the type of field is explicitly @Mutable");
        return null;
      }
      break;
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
	    final IAnnotationScrubberContext context,
	    final IProtectedRegions protectedRegions,
			final T lockDecl,
			final LockScrubContinuation<T> continuation) {
    boolean declIsGood = true; // assume the best

    final IBinder binder = context.getBinder();
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
            + JavaNames.getQualifiedTypeName(lockDecl
                .getPromisedFor()), lockDecl); //$NON-NLS-1$ //$NON-NLS-2$
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
				if (!TypeUtil.isFinal(lockFieldNode)) {
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
			final String typeName = JavaNames.getQualifiedTypeName(current);
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
					final ReturnsLockPromiseDrop returnedLock = getReturnsLock(returnNode);
					if (returnedLock != null) {
						final LockModel returnedLockModel = isLockNameOkay(
								TypeUtil.isStatic(member), returnedLock
										.getAST().getLock(), null);
						// Check for null because the requiresLock annotation
						// could be bad
						if (returnedLockModel != null
								&& returnedLockModel.getAST().equals(lockDeclNode)) {
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
		protected IAnnotationScrubber<PolicyLockDeclarationNode> makeScrubber() {
			return new AbstractAASTScrubber<PolicyLockDeclarationNode, LockModel>(name(),
          PolicyLockDeclarationNode.class, lockRule.getStorage(),
					ScrubberType.BY_HIERARCHY, VOUCH_FIELD_IS) {
				@Override
        protected PromiseDrop<AbstractLockDeclarationNode> makePromiseDrop(
						PolicyLockDeclarationNode a) {
					return AnnotationRules.storeDropIfNotNull(lockRule.getStorage(), a,
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
			final IAnnotationScrubberContext context,
			final PolicyLockDeclarationNode policyLockDecl) {
	  // XXX: This is sleazy, passing a null reference to the ProtectedRegions parameter 
    return scrubAbstractLock(context, null, policyLockDecl, POLICY_LOCK_DECLARATION_CONTINUATION);
	}

	/**
	 * Parse rule for the IsLock annotation
	 */
	public static class IsLock_ParseRule extends
			DefaultSLAnnotationParseRule<IsLockNode, IsLockPromiseDrop> {

		protected IsLock_ParseRule() {
			super(IS_LOCK, methodDeclOps, IsLockNode.class);
		}

		@Override
		protected Object parse(IAnnotationParsingContext context,
				SLAnnotationsParser parser) throws RecognitionException {
			return parser.isLock().getTree();
		}

		@Override
		protected IPromiseDropStorage<IsLockPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage
					.create(name(), IsLockPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber<IsLockNode> makeScrubber() {
			return new AbstractAASTScrubber<IsLockNode, IsLockPromiseDrop>(this,
					ScrubberType.UNORDERED, LOCK, POLICY_LOCK) {
				@Override
        protected PromiseDrop<IsLockNode> makePromiseDrop(IsLockNode a) {
					return storeDropIfNotNull(a, scrubIsLock(getContext(), a));
				}

			};
		}

	}

	/**
	 * Scrubbing code for the
	 * 
	 * @IsLock annotation
	 * @param context
	 * @param a
	 * @return
	 */
	private static IsLockPromiseDrop scrubIsLock(
			IAnnotationScrubberContext context, IsLockNode node) {
		final LockNameNode lockName = node.getLock();
		IsLockPromiseDrop promiseDrop = null;

		if (lockName != null) {
			LockModel lockDecl = isLockNameOkay(JavaNode.getModifier(node
					.getPromisedFor(), JavaNode.STATIC), lockName, context);
			if (lockDecl != null) {
				promiseDrop = new IsLockPromiseDrop(node);
				lockDecl.addDependent(promiseDrop);
			}
		}
		return promiseDrop;
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
			final LockNameNode lockName, final IAnnotationScrubberContext report) {
		// Default to assuming we should not get the binding
		boolean getBinding = false;
		boolean deferCheckForStaticUseOfThis = false;
		boolean staticUseOfThis = false;
		boolean checkForTypeQualifiedInstance = false;
		boolean checkForInstanceQualifiedStatic = false;
		boolean isBad = false;
		
		if (lockName instanceof QualifiedLockNameNode) {
			// Check that the qualifying variable exists
			final ExpressionNode base = ((QualifiedLockNameNode) lockName)
					.getBase();
			if (base instanceof VariableUseExpressionNode) {
				VariableUseExpressionNode v = (VariableUseExpressionNode) base;
				final IVariableBinding var = v.resolveBinding();
				if (var == null) {
					report.reportError("Parameter \"" + v //$NON-NLS-1$
							+ "\" does not exist", v); //$NON-NLS-1$
					isBad = true;
				}
				else {
					// make sure the parameter is final
					if (JavaNode.getModifier(var.getNode(), JavaNode.FINAL)) {
						getBinding = true;
						checkForInstanceQualifiedStatic = true;
					}
					else {
						report
								.reportError(
										"Parameter \"" //$NON-NLS-1$
												+ v
												+ "\" is not final; can only require locks on final parameters", //$NON-NLS-1$
										v);
	          isBad = true;
					}
				}
			}
			else if (base instanceof TypeExpressionNode) {
				final IType type = ((TypeExpressionNode) base).resolveType();
				if (type == null) {
					report.reportError("Class \"" + base //$NON-NLS-1$
							+ "\" does not exist", lockName); //$NON-NLS-1$
          isBad = true;
				}
				else {
					getBinding = true;
					checkForTypeQualifiedInstance = true;
				}
			}
			else if (base instanceof ThisExpressionNode) {
				if (!isStatic) {
					getBinding = true;
					checkForInstanceQualifiedStatic = true;
				}
				else {
					staticUseOfThis = true;
				}
			}
			else if (base instanceof QualifiedThisExpressionNode) {
				/*
				 * No longer needed final IRNode rcvr =
				 * ((QualifiedThisExpressionNode) base); if (rcvr == null) {
				 * report.reportError("Qualified receiver \"" + base + "\" does
				 * not exist", lockName); } else {
				 */
				getBinding = true;
				checkForInstanceQualifiedStatic = true;
				// }
			}
		}
		else if (lockName instanceof SimpleLockNameNode) {
			getBinding = true;
			if (isStatic) {
				deferCheckForStaticUseOfThis = true;
			}
		}
		else {
			// defensive programming!
			throw new IllegalArgumentException(
					"Don't know what to do with a \""
							+ lockName.getClass().getSimpleName() + "\"");
		}

		ILockBinding boundLock = null;
		if (getBinding) {
			boundLock = lockName.resolveBinding();
			if (boundLock == null) {
				report.reportError(
						"Lock \"" + lockName.getId() + "\" does not exist", //$NON-NLS-1$ //$NON-NLS-2$
						lockName);
        isBad = true;
			}
			else {
				final boolean lockIsStatic = boundLock.getModel().getAST()
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
						report.reportError(
								"Cannot type-qualify an instance lock",
								lockName);
	          isBad = true;
					}
				}
			}
		}

		if (staticUseOfThis) {
			report.reportError(
					"Cannot reference \"this\" from a static method", lockName);
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
	  private final String name;
    private final String notName;
	  
	  public TypeAnnotationScrubber(
	      final SimpleBooleanAnnotationParseRule<A, P> rule,
	      final String n, final String notN, final String... deps) {
	    super(rule, ScrubberType.INCLUDE_SUBTYPES_BY_HIERARCHY, deps);
	    name = n;
	    notName = notN;
	  }
	  
	  @Override
	  protected final P makePromiseDrop(final A a) {
	    return storeDropIfNotNull(a, scrubAnnotated(a));          
	  }
	  
	  private P scrubAnnotated(final A node) {
	    final IAnnotationScrubberContext context = getContext();
	    final IRNode promisedFor = node.getPromisedFor();
	    final boolean isInterface = TypeUtil.isInterface(promisedFor);
	    final boolean implementationOnly = node.isImplementationOnly();
	    boolean bad = false;
	    
	    if (isInterface) {
	      // the verify attribute is non-sense on interfaces
	      if (!node.verify()) {
	        bad = true;
	        context.reportError(node, "An interface may not be @{0}(verify=false)", name);
	      }
	      // The implemenationOnly attribute must be false on interfaces
	      if (implementationOnly) {
	        bad = true;
	        context.reportError(node, "An Interface may not be @{0}(implementationOnly=true)", name);
	      }
	    } else { // class
	      final IRNode superDecl = context.getBinder().getBinding(
	          ClassDeclaration.getExtension(promisedFor));

	      /* A class annotated with implementationOnly=true, cannot implement an
	       * interface annotated with T
	       */
	      if (implementationOnly) {
	        final IRNode impls = ClassDeclaration.getImpls(promisedFor);
	        for (final IRNode intfName : Implements.getIntfIterator(impls)) {
	          final IRNode intfDecl = context.getBinder().getBinding(intfName);
	          if (getSuperTypeAnno(intfDecl) != null) {
	            bad = true;
	            context.reportError(node,
	                "Class may not be @{0}(implementationOnly=true) because it implements the @{0} interface {1}",
	                name, JavaNames.getQualifiedTypeName(intfDecl));
	          }
	        }
	        
	        // java.lang.Object doesn't have a superclass
	        if (superDecl != promisedFor) {
	          final P superAnno = getSuperTypeAnno(superDecl);
	          if (superAnno == null) {
	            bad = true;
	            context.reportError(node,
	                "Class may not be @{0}(implementationOnly=true) because it extends the non-@{0} class {1}",
	                name, JavaNames.getQualifiedTypeName(superDecl));
	          } else if(!superAnno.isImplementationOnly() ) {
	            bad = true;
	            context.reportError(node,
	                "Class may not be @{0}(implementationOnly=true) because it extends the @{0} class {1}",
	                name, JavaNames.getQualifiedTypeName(superDecl));
	          }
	        }
	      } else { // implementationOnly == false
	        // java.lang.Object doesn't have a superclass
	        if (superDecl != promisedFor) {
	          if (getSuperTypeAnno(superDecl) == null) {
	            bad = true;
	            context.reportError(node,
	                "Class may not be @{0} because it extends the non-@{0} class {1}",
	                name, JavaNames.getQualifiedTypeName(superDecl));
	          }
	        }
	      }
	    }

	    // Cannot be both T and not T
	    final NP notDrop = getNotAnnotation(promisedFor);
      if (notDrop != null && !node.isImplementationOnly()) {
        notDrop.invalidate();
        getContext().reportError(
            node, "Cannot be both @{0} and @{1}", name, notName);
        bad = true;
      }
	    
	    if (bad) {
	      return null;
	    } else {
	      return createDrop(node);
	    }
	  }
    
    @Override 
    protected final boolean processUnannotatedType(final IJavaDeclaredType dt) {
      final IAnnotationScrubberContext context = getContext();
      final IRNode typeDecl = dt.getDeclaration();
      final boolean isInterface = TypeUtil.isInterface(typeDecl);
      final Iterable<IJavaType> supers = 
        dt.getSupertypes(context.getBinder().getTypeEnvironment()) ;
      
      // Are we actually annotated with the NOT form of the annotation?
      final boolean isNOT = getNotAnnotation(typeDecl) != null;
      
      boolean result = true;      
      if (isInterface) { // unannotated interface
        // If any superinterface is T we have an error
        for (final IJavaType zuper : supers) {
          final IRNode zuperDecl = ((IJavaDeclaredType) zuper).getDeclaration();
          // ignore CLASS java.lang.Object (which is a super if the interface doesn't extend anything)
          if (TypeUtil.isInterface(zuperDecl)) {
            if (getSuperTypeAnno(zuperDecl) != null) {
              if (isNOT) {
                context.reportError(typeDecl,
                    "Interface may not be @{0} because it extends the @{1} interface {2}",
                    notName, name, JavaNames.getQualifiedTypeName(zuper));
              } else {
                context.reportError(typeDecl,
                    "Interface must be annotated @{0} because it extends the @{0} interface {1}",
                    name, JavaNames.getQualifiedTypeName(zuper));
              }
              result = false;
            }
          }
        }
      } else { // unannotated class
        for (final IJavaType zuper : supers) {
          final IRNode zuperDecl = ((IJavaDeclaredType) zuper).getDeclaration();
          final P anno = getSuperTypeAnno(zuperDecl);
          if (anno != null) {
            if (TypeUtil.isInterface(zuperDecl)) {
              if (isNOT) {
                context.reportError(typeDecl,
                    "Class may not be @{0} because it implements a @{1} interface {2}",
                    notName, name, JavaNames.getQualifiedTypeName(zuper));
              } else {
                context.reportError(typeDecl,
                    "Class must be annotated @{0} because it implements a @{0} interface {1}",
                    name, JavaNames.getQualifiedTypeName(zuper));
              }
              result = false;
            } else if (!anno.isImplementationOnly()) {
              if (isNOT) {
                context.reportError(typeDecl,
                    "Class may not be @{0} because it extends a @{1} class {2}",
                    notName, name, JavaNames.getQualifiedTypeName(zuper));
              } else {
                context.reportError(typeDecl,
                    "Class must be annotated @{0} because it extends a @{0} class {1}",
                    name, JavaNames.getQualifiedTypeName(zuper));
              }
              result = false;
            }
          }
        }
      }
      return result;
    }
	  
    protected abstract NP getNotAnnotation(IRNode typeDecl);
	  
	  protected abstract P getSuperTypeAnno(IRNode superDecl);
	  
	  protected abstract P createDrop(A node);
	}
  
  public static class Containable_ParseRule 
  extends SimpleBooleanAnnotationParseRule<ContainableNode,ContainablePromiseDrop> {
    public Containable_ParseRule() {
      super(CONTAINABLE, typeDeclOps, ContainableNode.class);
    }
    @Override
    protected IAASTRootNode makeAAST(int offset, int mods) {
      return new ContainableNode(offset, mods);
    }
    @Override
    protected IPromiseDropStorage<ContainablePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), ContainablePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<ContainableNode> makeScrubber() {
      return new TypeAnnotationScrubber<ContainableNode, ContainablePromiseDrop, NotContainablePromiseDrop>(this, "Containable", "NotContainable", NOT_CONTAINABLE) {
        @Override
        protected ContainablePromiseDrop getSuperTypeAnno(final IRNode superDecl) {
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
      };
    }    
  }  
  public static class ThreadSafe_ParseRule 
  extends SimpleBooleanAnnotationParseRule<ThreadSafeNode,ThreadSafePromiseDrop> {
    public ThreadSafe_ParseRule() {
      super(THREAD_SAFE, typeDeclOps, ThreadSafeNode.class);
    }
    @Override
    protected IAASTRootNode makeAAST(int offset, int mods) {
      return new ThreadSafeNode(offset, mods);
    }
    @Override
    protected IPromiseDropStorage<ThreadSafePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), ThreadSafePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<ThreadSafeNode> makeScrubber() {
      return new TypeAnnotationScrubber<ThreadSafeNode, ThreadSafePromiseDrop, NotThreadSafePromiseDrop>(this, "ThreadSafe", "NotThreadSafe", NOT_THREAD_SAFE) {
        @Override
        protected ThreadSafePromiseDrop getSuperTypeAnno(final IRNode superDecl) {
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
      };
    }    
  }
  
  public static class NotThreadSafe_ParseRule 
  extends MarkerAnnotationParseRule<NotThreadSafeNode,NotThreadSafePromiseDrop> {
    public NotThreadSafe_ParseRule() {
      super(NOT_THREAD_SAFE, typeDeclOps, NotThreadSafeNode.class);
    }
    @Override
    protected IAASTRootNode makeAAST(int offset, int mods) {
      return new NotThreadSafeNode(offset);
    }
    @Override
    protected IPromiseDropStorage<NotThreadSafePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), NotThreadSafePromiseDrop.class);
    }
	@Override
	protected NotThreadSafePromiseDrop createDrop(NotThreadSafeNode a) {
		return new NotThreadSafePromiseDrop(a);
	}    
  }
  
  public static class NotContainable_ParseRule 
  extends MarkerAnnotationParseRule<NotContainableNode,NotContainablePromiseDrop> {
    public NotContainable_ParseRule() {
      super(NOT_CONTAINABLE, typeDeclOps, NotContainableNode.class);
    }
    @Override
    protected IAASTRootNode makeAAST(int offset, int mods) {
      return new NotContainableNode(offset);
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
  
  public static class Mutable_ParseRule 
  extends MarkerAnnotationParseRule<MutableNode,MutablePromiseDrop> {
    public Mutable_ParseRule() {
      super(MUTABLE, typeDeclOps, MutableNode.class);
    }
    @Override
    protected IAASTRootNode makeAAST(int offset, int mods) {
      return new MutableNode(offset);
    }
    @Override
    protected IPromiseDropStorage<MutablePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), MutablePromiseDrop.class);
    }
	@Override
	protected MutablePromiseDrop createDrop(MutableNode a) {
		return new MutablePromiseDrop(a);
	}    
  }
  
  public static class ImmutableParseRule 
  extends SimpleBooleanAnnotationParseRule<ImmutableNode,ImmutablePromiseDrop> {
    public ImmutableParseRule() {
      super(IMMUTABLE, typeDeclOps, ImmutableNode.class);
    }
    @Override
    protected IAASTRootNode makeAAST(int offset, int mods) {
      return new ImmutableNode(offset, mods);
    }
    @Override
    protected IPromiseDropStorage<ImmutablePromiseDrop> makeStorage() {
      return BooleanPromiseDropStorage.create(name(), ImmutablePromiseDrop.class);
    }
    @Override
    protected IAnnotationScrubber<ImmutableNode> makeScrubber() {
      return new TypeAnnotationScrubber<ImmutableNode,ImmutablePromiseDrop, MutablePromiseDrop>(this, "Immutable", "Mutable", MUTABLE) {
        @Override
        protected ImmutablePromiseDrop getSuperTypeAnno(final IRNode superDecl) {
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
        final IAnnotationScrubberContext context, final LockDeclarationNode lockDecl) {
      final ExpressionNode field = lockDecl.getField();
      final RegionNameNode region = lockDecl.getRegion();
      final IRegionBinding regionBinding = region.resolveBinding();
      final Visibility lockViz = getLockFieldVisibility(lockDecl, context.getBinder());
      final Visibility regionViz = regionBinding.getModel().getVisibility();
      if (!lockViz.atLeastAsVisibleAs(regionViz)) { // (5)
        /* We create this as a warning drop instead of a modeling error because
         * it doesn't break anything, it only means that the lock model isn't
         * as useful as they probably mean it to be.
         */
        final String qualifiedName = computeQualifiedName(lockDecl);
        final LockModel model = LockModel.getInstance(qualifiedName, lockDecl.getPromisedFor()); 
        final PromiseWarningDrop wd = new PromiseWarningDrop(com.surelogic.analysis.locks.Messages.toString(com.surelogic.analysis.locks.Messages.LockAnalysis_ds_LockViz));
        wd.setResultMessage(com.surelogic.analysis.locks.Messages.LockAnalysis_ds_LockViz, field, region);
        wd.setNodeAndCompilationUnitDependency(lockDecl.getPromisedFor());
        wd.setCategory(com.surelogic.analysis.locks.Messages.DSC_LOCK_VIZ);
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
  
	static class VouchFieldIs_ParseRule extends DefaultSLAnnotationParseRule<VouchFieldIsNode,VouchFieldIsPromiseDrop> {
	  VouchFieldIs_ParseRule() {
			super(VOUCH_FIELD_IS, fieldDeclOp, VouchFieldIsNode.class);
		}

		// Should only get called by the Assume parse rule
		@Override
		protected Object parse(IAnnotationParsingContext context, SLAnnotationsParser parser) 
		throws Exception {
			final String id = context.getAllText().trim();
			final FieldKind kind;
			if ("final".equals(id)) {
				kind = FieldKind.Final;
			} else {				
				kind = FieldKind.valueOf(id);				
			}
			return new VouchFieldIsNode(context.mapToSource(0), kind);
		}
		
		@Override
		protected IPromiseDropStorage<VouchFieldIsPromiseDrop> makeStorage() {
			return SinglePromiseDropStorage.create(name(), VouchFieldIsPromiseDrop.class);
		}

		@Override
		protected IAnnotationScrubber<VouchFieldIsNode> makeScrubber() {
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
}
