package com.surelogic.analysis.concurrency.driver;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.aast.promise.ThreadSafeNode;
import com.surelogic.analysis.AbstractAnalysisSharingAnalysis;
import com.surelogic.analysis.ConcurrencyType;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.TopLevelAnalysisVisitor;
import com.surelogic.analysis.TopLevelAnalysisVisitor.TypeBodyPair;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.heldlocks.GlobalLockModel;
import com.surelogic.analysis.concurrency.heldlocks.LockUtils;
import com.surelogic.analysis.concurrency.heldlocks.LockVisitor;
import com.surelogic.analysis.concurrency.threadsafe.ContainableProcessor;
import com.surelogic.analysis.concurrency.threadsafe.ImmutableProcessor;
import com.surelogic.analysis.concurrency.threadsafe.ThreadSafeProcessor;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaCaptureType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaIntersectionType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.bind.IJavaWildcardType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.InterfaceDeclaration;
import edu.cmu.cs.fluid.java.operator.ParameterizedType;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.VoidTreeWalkVisitor;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ContainablePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ImmutablePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.drops.promises.ThreadSafePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.TypeInstantiationDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;

public class LockAnalysis
		extends
		AbstractAnalysisSharingAnalysis<BindingContextAnalysis, LockVisitor, TypeBodyPair> {
	/** Should we try to run things in parallel */
	private static boolean wantToRunInParallel = false;

	/**
	 * Are we actually going to run things in parallel? Not all JRE have the
	 * libraries we need to actually run in parallel.
	 */
	private static boolean willRunInParallel = wantToRunInParallel
			&& !singleThreaded;

	/**
	 * Use a work queue? Only relevant if {@link #willRunInParallel} is
	 * <code>true</code>. Otherwise it is <code>false</code>.
	 */
	private static boolean queueWork = willRunInParallel && true;

	/**
	 * Analyze compilation units in parallel? Only relevant if
	 * {@link #willRunInParallel} is <code>true</code> and {@link #queueWork} is
	 * <code>true</code>. Otherwise it is <code>false</code>. When relevant, a
	 * <code>false</code> value means analyze by types, a smaller granularity
	 * than compilation units.
	 */
	private static boolean byCompUnit = queueWork && true; // otherwise by type

	private final AtomicReference<GlobalLockModel> lockModelHandle = new AtomicReference<GlobalLockModel>(
			null);

	private final Map<IRNode, TypeInstantiationDrop> dropCache =
	    new HashMap<IRNode, TypeInstantiationDrop>();
  
  private final Map<IRNode, List<Set<AnnotationBounds>>> cachedBounds =
      new HashMap<IRNode, List<Set<AnnotationBounds>>>();
	
	
	
	public LockAnalysis() {
		super(willRunInParallel, queueWork ? TypeBodyPair.class : null,
				"LockAssurance", BindingContextAnalysis.factory);
		if (runInParallel() == ConcurrencyType.INTERNALLY) {
			setWorkProcedure(new Procedure<TypeBodyPair>() {
				public void op(TypeBodyPair n) {
					if (byCompUnit) {
						// System.out.println("Parallel Lock: "+JavaNames.genPrimaryTypeName(n));
						TopLevelAnalysisVisitor.processCompilationUnit(
								new ClassProcessor(getAnalysis(),
										getResultDependUponDrop()),
								// actually n.typeDecl is a CompilationUnit
								// here!
								n.typeDecl());
					} else {
						// System.out.println("Parallel Lock: "+JavaNames.getRelativeTypeName(n));
						actuallyAnalyzeClassBody(getAnalysis(),
								getResultDependUponDrop(), n.typeDecl(),
								n.classBody());
					}
				}
			});
		}
	}
	
	private final void actuallyAnalyzeClassBody(
	    final LockVisitor lv, final Drop rd, 
	    final IRNode typeDecl, final IRNode typeBody) {
	  lv.analyzeClass(typeBody, rd);
	  
    final ThreadSafePromiseDrop threadSafeDrop =
      LockRules.getThreadSafeImplementation(typeDecl);
    // If null, assume it's not meant to be thread safe
    // Also check for verify=false
    if (threadSafeDrop != null && threadSafeDrop.verify()) {
      new ThreadSafeProcessor(this, threadSafeDrop, typeDecl, typeBody, lockModelHandle.get()).processType();
    }
    
    final ContainablePromiseDrop containableDrop = 
      LockRules.getContainableImplementation(typeDecl);
    // no @Containable annotation --> Default "annotation" of not containable
    // Also check for verify=false
    if (containableDrop != null && containableDrop.verify()) {
      new ContainableProcessor(this, containableDrop, typeDecl, typeBody).processType();
    }

		final ImmutablePromiseDrop immutableDrop = LockRules
				.getImmutableImplementation(typeDecl);
		// no @Immutable annotation --> Default "annotation" of mutable
		// Also check for verify=false
		if (immutableDrop != null && immutableDrop.verify()) {
			new ImmutableProcessor(this, immutableDrop, typeDecl, typeBody).processType();
		}
	}

	@Override
	public void init(IIRAnalysisEnvironment env) {
		super.init(env);
		env.ensureClassIsLoaded(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_LOCK);
		env.ensureClassIsLoaded(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK);
	}

	@Override
	public void startAnalyzeBegin(final IIRProject p, final IBinder binder) {
		super.startAnalyzeBegin(p, binder);

//		upperBoundGetter = new UpperBoundGetter(binder.getTypeEnvironment());
		
		// Initialize the global lock model
		final GlobalLockModel globalLockModel = new GlobalLockModel(binder);
		LockModel.purgeUnusedLocks();

		/*
		 * This seems stupid to me. I feel like I should be able to get the
		 * LockModel object from the LockDeclarationDrop and
		 * PromiseLockDeclarationDrop objects. I shouldn't have to use the lock
		 * name as an intermediary. But the as far as I can tell, there is no
		 * back link from the drop to the LockModel.
		 */

		// Run through the LockModel and add them to the GlobalLockModel
		final Set<? extends LockModel> lockModelDrops = Sea.getDefault()
				.getDropsOfType(LockModel.class);
		for (LockModel lockDrop : lockModelDrops) {
			final IRNode classDecl = lockDrop.getNode();

			if (classDecl == null) {
				LOG.severe("TODO invalidate dependent drops");
				lockDrop.invalidate();
				continue;
			}
			if (lockDrop.getAST() == null) {
				LOG.warning("No AST for " + lockDrop.getMessage());
				continue;
			}
			if (lockDrop.getAST() instanceof LockDeclarationNode) {
				if (!lockDrop.hasMatchingDependents(DropPredicateFactory
						.matchExactType(RegionModel.class))) {
					// This is not really valid, but properly invalidated due to
					// the inversion of dependencies
					// between the LockModel and RegionModel (for UI purposes)
					continue;
				}
				globalLockModel.addRegionLockDeclaration(binder, lockDrop,
						(IJavaDeclaredType) JavaTypeFactory.getMyThisType(classDecl));
			} else {
				globalLockModel.addPolicyLockDeclaration(binder, lockDrop,
						(IJavaDeclaredType) JavaTypeFactory.getMyThisType(classDecl));
			}
		}

		// Share the new global lock model with the lock visitor, and other
		// helpers
		lockModelHandle.set(globalLockModel);
	}

	@Override
	protected boolean flushAnalysis() {
		return true;
	}

	@Override
	protected LockVisitor constructIRAnalysis(IBinder binder) {
		if (binder == null || binder.getTypeEnvironment() == null) {
			return null;
		}
		return new LockVisitor(this, binder, new Effects(binder),
				new TypeBasedMayAlias(binder), getSharedAnalysis(),
				lockModelHandle);
	}

	@Override
	protected void clearCaches() {
		if (runInParallel() != ConcurrencyType.INTERNALLY) {
			LockVisitor lv = getAnalysis();
			if (lv != null) {
				lv.clearCaches();
			}
		} else {
			analyses.clearCaches();
		}
		dropCache.clear();
		cachedBounds.clear();
		super.clearCaches();
	}

	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud,
			final IRNode compUnit) {
		if (byCompUnit) {
			boolean flushed = queueWork(new TypeBodyPair(compUnit, null));
			if (flushed) {
				JavaComponentFactory.clearCache();
			}
			return true;
		}
		// FIX factor out?
		final ClassProcessor cp = new ClassProcessor(getAnalysis(),
				getResultDependUponDrop());
		TopLevelAnalysisVisitor.processCompilationUnit(cp, compUnit);
		if (runInParallel() == ConcurrencyType.INTERNALLY) {
			if (queueWork) {
				boolean flushed = queueWork(cp.getTypeBodies());
				if (flushed) {
					JavaComponentFactory.clearCache();
				}
			} else {
				runInParallel(TypeBodyPair.class, cp.getTypeBodies(),
						getWorkProcedure());
			}
		}
//		
//		final ParameterizedTypeVisitor tVisitor = new ParameterizedTypeVisitor(getBinder());
//		tVisitor.doAccept(compUnit);
		return true;
	}

	@Override
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		finishBuild();
		return super.analyzeEnd(env, p);
	}

	@Override
	public void postAnalysis(IIRProject p) {
		super.postAnalysis(p);
		/*
		 * Have to do this afterwards, because postAnalysis can cause a
		 * LockVisitor to be created---which seems wrong---in the non parallel
		 * case.
		 */
		lockModelHandle.set(null);
	}

	private final class ClassProcessor extends
			TopLevelAnalysisVisitor.SimpleClassProcessor {
		private final LockVisitor lockVisitor;
		private final Drop resultsDependUpon;
		private final List<TypeBodyPair> types = new ArrayList<TypeBodyPair>();

		public ClassProcessor(final LockVisitor lv, final Drop rd) {
			lockVisitor = lv;
			resultsDependUpon = rd;
		}

		public Collection<TypeBodyPair> getTypeBodies() {
			return types;
		}

		@Override
		protected void visitTypeDecl(final IRNode typeDecl,
				final IRNode classBody) {
			if (runInParallel() == ConcurrencyType.INTERNALLY && !byCompUnit) {
				types.add(new TypeBodyPair(typeDecl, classBody));
			} else {
				actuallyAnalyzeClassBody(lockVisitor, resultsDependUpon,
						typeDecl, classBody);
			}
		}
	}

  
  
  public TypeInstantiationDrop getDropForType(final IRNode typeDecl) {
    TypeInstantiationDrop drop = dropCache.get(typeDecl);
    if (drop == null) {
      drop = new TypeInstantiationDrop(typeDecl);
      dropCache.put(typeDecl, drop);
    }
    return drop;
  }
  
  private final class ParameterizedTypeVisitor extends VoidTreeWalkVisitor {
    private final IBinder binder;
    
    private final IJavaDeclaredType javaLangObject;
    
    
    
    public ParameterizedTypeVisitor(final IBinder b) {
      binder = b;
      javaLangObject = b.getTypeEnvironment().getObjectType();
    }

    
    
    @Override
    public Void visitParameterizedType(final IRNode pType) {
      final IRNode baseTypeDecl =
          binder.getBinding(ParameterizedType.getBase(pType));
      final List<Set<AnnotationBounds>> bounds = getBounds(baseTypeDecl);
      if (bounds != null) {
        final TypeInstantiationDrop pDrop = getDropForType(baseTypeDecl);
        
        // Should be true: if not, why not?
        final IJavaDeclaredType jType =
            (IJavaDeclaredType) binder.getJavaType(pType);
        final List<IJavaType> actuals = jType.getTypeParameters();
        for (int i = 0; i < actuals.size(); i++) {
          checkActualAgainstBounds(pDrop, pType, bounds.get(i), actuals.get(i));
        }        
      }
      
      doAcceptForChildren(pType);
      return null;
    }


    
    private void checkActualAgainstBounds(final TypeInstantiationDrop pDrop,
        final IRNode parameterizedType,
        final Set<AnnotationBounds> bounds, final IJavaType actual) {
      final IJavaDeclaredType declaredType = convertToDeclaredType(actual);
      final IRNode typeDecl = declaredType.getDeclaration();
      
      for (final AnnotationBounds bound : bounds) {
        if (bound.test(typeDecl)) {
          final ResultDropBuilder result = 
              ResultDropBuilder.create(LockAnalysis.this,
                  Messages.toString(Messages.ANNOTATION_BOUNDS_SATISFIED));
          LockAnalysis.this.setResultDependUponDrop(result, parameterizedType);
          result.addCheckedPromise(pDrop);
          result.setConsistent(true);
          result.setResultMessage(Messages.ANNOTATION_BOUNDS_SATISFIED,
              DebugUnparser.toString(parameterizedType));
          System.out.println("good");
        } else {
          final ResultDropBuilder result = 
              ResultDropBuilder.create(LockAnalysis.this,
                  Messages.toString(Messages.ANNOTATION_BOUNDS_NOT_SATISFIED));
          LockAnalysis.this.setResultDependUponDrop(result, parameterizedType);
          result.addCheckedPromise(pDrop);
          result.setConsistent(false);
          result.setResultMessage(Messages.ANNOTATION_BOUNDS_NOT_SATISFIED,
              DebugUnparser.toString(parameterizedType));
          System.out.println("bad");
        }
      }
    }
    
    private IJavaDeclaredType convertToDeclaredType(IJavaType ty) {
      while (!(ty instanceof IJavaDeclaredType)) {
        if (ty instanceof IJavaCaptureType) {
          final IJavaType upper = ((IJavaCaptureType) ty).getUpperBound();
          ty = (upper == null) ? javaLangObject : upper;
        } else if (ty instanceof IJavaWildcardType) {
          // dead case?  Turned into Capture types, I think
          final IJavaType upper = ((IJavaWildcardType) ty).getUpperBound();
          ty = (upper == null) ? javaLangObject : upper;
        } else if (ty instanceof IJavaTypeFormal) {
          final IJavaType upper = ((IJavaTypeFormal) ty).getSuperclass(binder.getTypeEnvironment());
          ty = (upper == null) ? javaLangObject : upper;
        } else if (ty instanceof IJavaArrayType) {
          // not presently supported in region annotations, convert to
          // any(Object):Instance
          ty = javaLangObject;
        } else if (ty instanceof IJavaIntersectionType) {
          ty = javaLangObject;
        } else {
          throw new IllegalStateException("Unexpected type: " + ty);
        }
      }
      return (IJavaDeclaredType) ty;
    }
    
    
    
    private List<Set<AnnotationBounds>> getBounds(final IRNode type) {
      final Operator op = JJNode.tree.getOperator(type);
      if (ClassDeclaration.prototype.includes(op)) {
        final List<Set<AnnotationBounds>> bounds = cachedBounds.get(type);
        if (bounds == null) {
          return computeBounds(type, ClassDeclaration.getTypes(type));
        } else {
          return bounds;
        }
      } else if (InterfaceDeclaration.prototype.includes(op)) {
        final List<Set<AnnotationBounds>> bounds = cachedBounds.get(type);
        if (bounds == null) {
          return computeBounds(type, InterfaceDeclaration.getTypes(type));
        } else {
          return bounds;
        }
      } else {
        return null;
      }
    }
    
    private List<Set<AnnotationBounds>> computeBounds(
        final IRNode type, final IRNode typeFormalsNode) {
      if (typeFormalsNode == null || !JJNode.tree.hasChildren(typeFormalsNode)) {
        cachedBounds.put(type, null);
        return null;
      }
      
      /* If we get here we 'type' is a class or interface declaration with
       * at least 1 type formal.
       */
      
      final List<IRNode> formalDecls = JJNode.tree.childList(typeFormalsNode);
      final int numFormals = formalDecls.size();
      final String[] formalIDs = new String[numFormals];
      final List<Set<AnnotationBounds>> bounds = 
          new ArrayList<Set<AnnotationBounds>>(numFormals);
      for (int i = 0; i < numFormals; i++) {
        bounds.add(EnumSet.noneOf(AnnotationBounds.class));
        formalIDs[i] = TypeFormal.getId(formalDecls.get(i));
      }
      boolean added = false;
      
      final ThreadSafePromiseDrop tsDrop = LockRules.getThreadSafeType(type);
      if (tsDrop != null) {
        final ThreadSafeNode ast = tsDrop.getAST();
        added |= addToBounds(bounds, formalIDs,
            ast.getWhenContainable(), AnnotationBounds.CONTAINABLE);
        added |= addToBounds(bounds, formalIDs,
            ast.getWhenImmutable(), AnnotationBounds.IMMUTABLE);
        added |= addToBounds(bounds, formalIDs,
            ast.getWhenThreadSafe(), AnnotationBounds.THREADSAFE);
      }
      
      final ImmutablePromiseDrop iDrop = LockRules.getImmutableType(type);
      if (iDrop != null) {
        added |= addToBounds(bounds, formalIDs,
            iDrop.getAST().getWhenImmutable(), AnnotationBounds.IMMUTABLE);
      }
      
      final ContainablePromiseDrop cDrop = LockRules.getContainableType(type);
      if (cDrop != null) {
        added |= addToBounds(bounds, formalIDs,
            cDrop.getAST().getWhenContainable(), AnnotationBounds.CONTAINABLE);
      }
      
      if (added) {
        cachedBounds.put(type, bounds);
        return bounds;
      } else {
        cachedBounds.put(type, null);
        return null;
      }
    }
    
    private boolean addToBounds(
        final List<Set<AnnotationBounds>> bounds, final String[] formalIDs,
        final NamedTypeNode[] names, final AnnotationBounds bound) {
      boolean added = false;
      for (final NamedTypeNode name : names) {
        final String id = name.getType();
        for (int i = 0; i < formalIDs.length; i++) {
          if (formalIDs[i].equals(id)) {
            bounds.get(i).add(bound);
            added = true;
            break;
          }
        }
      }
      return added;
    }
  }
  
  
  
  private enum AnnotationBounds {
    CONTAINABLE {
      @Override
      public boolean test(final IRNode typeDecl) {
        return LockRules.isContainableType(typeDecl);
      }
    },
    
    IMMUTABLE {
      @Override
      public boolean test(final IRNode typeDecl) {
        return LockRules.isImmutableType(typeDecl);
      }
    },
    
    THREADSAFE {
      @Override
      public boolean test(final IRNode typeDecl) {
        return LockRules.getThreadSafeTypePromise(typeDecl) != null;
      }
    };
    
    public abstract boolean test(IRNode typeDecl);
  }
}