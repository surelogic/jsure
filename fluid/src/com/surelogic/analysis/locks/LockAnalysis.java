package com.surelogic.analysis.locks;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.aast.java.NamedTypeNode;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.TopLevelAnalysisVisitor.TypeBodyPair;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaCaptureType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaIntersectionType;
import edu.cmu.cs.fluid.java.bind.IJavaNullType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.IJavaTypeFormal;
import edu.cmu.cs.fluid.java.bind.IJavaUnionType;
import edu.cmu.cs.fluid.java.bind.IJavaVoidType;
import edu.cmu.cs.fluid.java.bind.IJavaWildcardType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.TypeFormal;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.ModifiedBooleanPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ContainablePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.IUniquePromise;
import edu.cmu.cs.fluid.sea.drops.promises.ImmutablePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.drops.promises.ThreadSafePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.UniquePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.VouchFieldIsPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ProposedPromiseBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.SingletonIterator;

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

//	private UpperBoundGetter upperBoundGetter = null;
	
	
	
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
      new ThreadSafeProcessor(threadSafeDrop, typeDecl, typeBody).processType();
    }
    
    final ContainablePromiseDrop containableDrop = 
      LockRules.getContainableImplementation(typeDecl);
    // no @Containable annotation --> Default "annotation" of not containable
    // Also check for verify=false
    if (containableDrop != null && containableDrop.verify()) {
      new ContainableProcessor(containableDrop, typeDecl, typeBody).processType();
    }

		final ImmutablePromiseDrop immutableDrop = LockRules
				.getImmutableImplementation(typeDecl);
		// no @Immutable annotation --> Default "annotation" of mutable
		// Also check for verify=false
		if (immutableDrop != null && immutableDrop.verify()) {
			new ImmutableProcessor(immutableDrop, typeDecl, typeBody)
					.processType();
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

	private final class ThreadSafeProcessor extends TypeImplementationProcessor<ThreadSafePromiseDrop> {
		private final Set<RegionLockRecord> lockDeclarations;
		private boolean hasFields = false;

		public ThreadSafeProcessor(
				final ThreadSafePromiseDrop tsDrop,
				final IRNode typeDecl, final IRNode typeBody) {
			super(LockAnalysis.this, tsDrop, typeDecl, typeBody);
			lockDeclarations = lockModelHandle.get().getRegionLocksInClass(
					JavaTypeFactory.getMyThisType(typeDecl));
		}

		private RegionLockRecord getLockForRegion(final IRegion r) {
			for (final RegionLockRecord lr : lockDeclarations) {
				if (lr.region.ancestorOf(r)) {
					return lr;
				}
			}
			return null;
		}

		@Override
		protected String message2string(final int msg) {
			return Messages.toString(msg);
		}

    @Override
    protected void processSuperType(final IRNode tdecl) {
      final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> pDrop =
          LockRules.getThreadSafeImplPromise(tdecl);
      if (pDrop != null) {
        final ResultDropBuilder result = createResult(tdecl, true,
            Messages.THREAD_SAFE_SUPERTYPE,
            JavaNames.getQualifiedTypeName(tdecl));
        result.addTrustedPromise(pDrop);
      }
    }

		@Override
		protected void postProcess() {
			if (!hasFields) {
				createResult(typeBody, true, Messages.TRIVIALLY_THREADSAFE);
			}
		}

		@Override
		protected void processVariableDeclarator(final IRNode fieldDecl,
				final IRNode varDecl, final boolean isStatic) {
			// we have a field
			hasFields = true;

			/*
			 * Field needs to be: (1) Volatile and thread safe (2) Final and
			 * thread safe (3) Protected by a lock and thread safe
			 * 
			 * Where "thread safe" means (1) The declared type of the field is
			 * primitive (2) The declared type of the field is annotated
			 * @ThreadSafe (3) The declared type of the field is annotated
			 * @Containable and the field is also annotated @Unique, and the
			 * referenced object is aggregated into lock-protected regions.
			 */
			final String id = VariableDeclarator.getId(varDecl);

			// Check for vouch
			final VouchFieldIsPromiseDrop vouchDrop = 
			    LockRules.getVouchFieldIs(varDecl);
			if (vouchDrop != null && (vouchDrop.isThreadSafe() || vouchDrop.isImmutable())) {
				final String reason = vouchDrop.getReason();
				final ResultDropBuilder result = 
				    reason == VouchFieldIsNode.NO_REASON ? createResult(
				        varDecl, true, Messages.VOUCHED_THREADSAFE, id)
				        : createResult(varDecl, true,
				            Messages.VOUCHED_THREADSAFE_WITH_REASON, id, reason);
				result.addTrustedPromise(vouchDrop);
			} else {
				/*
				 * First check if the field is volatile, final, or
				 * lock-protected
				 */
				final boolean isFinal = TypeUtil.isFinal(varDecl);
				final boolean isVolatile = TypeUtil.isVolatile(varDecl);
				final RegionLockRecord fieldLock =
				    getLockForRegion(RegionModel.getInstance(varDecl));

				if (isFinal || isVolatile || fieldLock != null) {
				  /* Check if the declared type of the field is thread-safe or
				   * containable.
				   */
          final IJavaType type = getBinder().getJavaType(varDecl);
          final boolean isPrimitive = type instanceof IJavaPrimitiveType;
          final boolean isArray = type instanceof IJavaArrayType;
          final boolean testedType;
          final boolean usingImplDrop;
          final boolean isThreadSafe;
          final Iterable<ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>> tsDrops;
          final Iterable<IRNode> notThreadSafe;
          final boolean isDeclaredContainable;
          final ContainableAnnotationTester cTester =
              new ContainableAnnotationTester(
                  getBinder(), promiseDrop.getAST().getWhenContainable());

          if (!isPrimitive && !isArray) { // type formal or declared type
            final ThreadSafeAnnotationTester tsTester =
                new ThreadSafeAnnotationTester(getBinder(),
                    promiseDrop.getAST().getWhenImmutable(),
                    promiseDrop.getAST().getWhenThreadSafe());
            final boolean isTS = tsTester.testType(type);
            testedType = true;
            /*
             * If the type is not thread safe, we can check to see
             * if the implementation assigned to the field is thread
             * safe, but only if the field is final.
             */
            if (!isTS && isFinal) {
              final IRNode init = VariableDeclarator.getInit(varDecl);
              if (Initialization.prototype.includes(init)) {
                final IRNode initExpr = Initialization.getValue(init);
                if (NewExpression.prototype.includes(initExpr)) {
                  final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> implTypeTSDrop =
                      LockRules.getThreadSafeImplPromise(
                          ((IJavaDeclaredType) getBinder().getJavaType(initExpr)).getDeclaration());
                  usingImplDrop = true;
                  if (implTypeTSDrop != null) {
                    isThreadSafe = true;
                    tsDrops = new SingletonIterator<ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>>(implTypeTSDrop);
                    notThreadSafe = new EmptyIterator<IRNode>();
                  } else {
                    isThreadSafe = false;
                    tsDrops = new EmptyIterator<ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>>();
                    notThreadSafe = tsTester.getFailed();
                  }
                } else {
                  usingImplDrop = false;
                  isThreadSafe = false;
                  tsDrops = new EmptyIterator<ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>>();
                  notThreadSafe = tsTester.getFailed();
                }                
              } else {
                usingImplDrop = false;
                isThreadSafe = false;
                tsDrops = new EmptyIterator<ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>>();
                notThreadSafe = tsTester.getFailed();
              }
            } else {
              usingImplDrop = false;
              isThreadSafe = isTS;
              tsDrops = tsTester.getDrops();
              notThreadSafe = tsTester.getFailed();
            }
            
            isDeclaredContainable = cTester.testType(type);
          } else {
            testedType = false;
            usingImplDrop = false;
            isThreadSafe = false;
            tsDrops = new EmptyIterator<ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>>();
            notThreadSafe = new EmptyIterator<IRNode>();
            isDeclaredContainable = false;
          }
          
          final boolean isContainable =
              isDeclaredContainable || (isArray && cTester.testType(type));

					/*
					 * @ThreadSafe takes priority over @Containable: If the type
					 * is threadsafe don't check the aggregation status
					 */
					final IUniquePromise uDrop = UniquenessUtils.getUnique(varDecl);
					final Map<IRegion, IRegion> aggMap;
					boolean isContained = false;
					if (!isThreadSafe && isContainable) {
						if (uDrop != null) {
							aggMap = UniquenessUtils.constructRegionMapping(varDecl);
							isContained = true;
							for (final IRegion destRegion : aggMap.values()) {
								isContained &= (getLockForRegion(destRegion) != null);
							}
						} else {
							aggMap = null;
						}
					} else {
						aggMap = null;
						// no @Containable annotation --> Default "annotation"
						// of not containable
						isContained = false;
					}

					final String typeString = type.toString();
					if (isPrimitive || isThreadSafe || isContained) {
						final ResultDropBuilder result;
						if (isFinal) {
							result = createResult(
							    varDecl, true, Messages.FINAL_AND_THREADSAFE, id);
						} else if (isVolatile) {
							result = createResult(
							    varDecl, true, Messages.VOLATILE_AND_THREADSAFE, id);
						} else { // lock protected
							result = createResult(
							    varDecl, true, Messages.PROTECTED_AND_THREADSAFE, id,
									fieldLock.name);
							result.addTrustedPromise(fieldLock.lockDecl);
						}

						if (isPrimitive) {
							result.addSupportingInformation(
							    varDecl, Messages.PRIMITIVE_TYPE, typeString);
						} else if (isThreadSafe) {
							result.addSupportingInformation(
							    varDecl, Messages.DECLARED_TYPE_IS_THREAD_SAFE,	typeString);
							for (final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> p : tsDrops) {
							  result.addTrustedPromise(p);
							}
							if (usingImplDrop) {
								result.addSupportingInformation(
								    varDecl, Messages.THREAD_SAFE_IMPL);
							}
						} else { // contained
							result.addSupportingInformation(
							    varDecl, Messages.DECLARED_TYPE_IS_CONTAINABLE,	typeString);
							for (final ContainablePromiseDrop p : cTester.getDrops()) {
								result.addTrustedPromise(p);
							}
							result.addTrustedPromise(uDrop.getDrop());
							for (final IRegion destRegion : aggMap.values()) {
								result.addTrustedPromise(getLockForRegion(destRegion).lockDecl);
							}
						}
					} else {
						final ResultDropBuilder result = createResult(
						    varDecl, false, Messages.UNSAFE_REFERENCE, id);
						// type could be a non-declared, non-primitive type,
						// that is, an array
						if (testedType) {
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_IS_NOT_THREAD_SAFE,
									typeString);
							for (final IRNode n : notThreadSafe) {
								result.addProposal(new ProposedPromiseBuilder(
										"ThreadSafe", null, n, varDecl,	Origin.MODEL));
							}
							for (final IRNode n : cTester.getFailed()) {
								result.addProposal(new ProposedPromiseBuilder(
										"Containable", null, n, varDecl, Origin.MODEL));
							}
						}

						if (isContainable) {
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_IS_CONTAINABLE,
									typeString);
							if (!isContained) {
								result.addSupportingInformation(varDecl,
										Messages.NOT_AGGREGATED);
							}
						} else {
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_NOT_CONTAINABLE,
									typeString);
						}

						if (uDrop == null) {
							result.addProposal(new ProposedPromiseBuilder(
									"Unique", null, varDecl, varDecl,
									Origin.MODEL));
						}
					}
				} else {
					createResult(varDecl, false, Messages.UNSAFE_FIELD, id);
				}
			}
		}
	}

	private final class ContainableProcessor extends
			TypeImplementationProcessor<ContainablePromiseDrop> {
		public ContainableProcessor(
				final ContainablePromiseDrop cDrop,
				final IRNode typeDecl, final IRNode typeBody) {
			super(LockAnalysis.this, cDrop, typeDecl, typeBody);
		}

		@Override
		protected String message2string(final int msg) {
			return Messages.toString(msg);
		}

		@Override
		protected void processSuperType(final IRNode tdecl) {
		  final ContainablePromiseDrop pDrop =
		      LockRules.getContainableImplementation(tdecl);
		  if (pDrop != null) {
		    final ResultDropBuilder result = createResult(tdecl, true,
		        Messages.CONTAINABLE_SUPERTYPE,
		        JavaNames.getQualifiedTypeName(tdecl));
		    result.addTrustedPromise(pDrop);
		  }
		}
		
		@Override
		protected void processConstructorDeclaration(final IRNode cdecl) {
			final IRNode rcvrDecl = JavaPromise.getReceiverNodeOrNull(cdecl);
			final BorrowedPromiseDrop bpd = UniquenessRules
					.getBorrowed(rcvrDecl);

			final IRNode returnDecl = JavaPromise.getReturnNodeOrNull(cdecl);
			final UniquePromiseDrop upd = UniquenessRules.getUnique(returnDecl);

			// Prefer unique return over borrowed receiver
			final String id = JavaNames.genSimpleMethodConstructorName(cdecl);
			if (upd != null) {
				final ResultDropBuilder result = createResult(cdecl, true,
						Messages.CONSTRUCTOR_UNIQUE_RETURN, id);
				result.addTrustedPromise(upd);
			} else if (bpd != null) {
				final ResultDropBuilder result = createResult(cdecl, true,
						Messages.CONSTRUCTOR_BORROWED_RECEVIER, id);
				result.addTrustedPromise(bpd);
			} else {
				final ResultDropBuilder result = createResult(cdecl, false,
						Messages.CONSTRUCTOR_BAD, id);
				result.addProposal(new ProposedPromiseBuilder("Unique",
						"return", cdecl, cdecl, Origin.MODEL));
			}
		}

		@Override
		protected void processMethodDeclaration(final IRNode mdecl) {
			// Must borrow the receiver if the method is not static
			if (!TypeUtil.isStatic(mdecl)) {
				final String id = JavaNames
						.genSimpleMethodConstructorName(mdecl);
				final IRNode rcvrDecl = JavaPromise
						.getReceiverNodeOrNull(mdecl);
				final BorrowedPromiseDrop bpd = UniquenessRules
						.getBorrowed(rcvrDecl);
				if (bpd == null) {
					final ResultDropBuilder result = createResult(mdecl, false,
							Messages.METHOD_BAD, id);
					result.addProposal(new ProposedPromiseBuilder("Borrowed",
							"this", mdecl, mdecl, Origin.MODEL));
				} else {
					final ResultDropBuilder result = createResult(mdecl, true,
							Messages.METHOD_BORROWED_RECEIVER, id);
					result.addTrustedPromise(bpd);
				}
			}
		}

		@Override
		protected void processVariableDeclarator(final IRNode fieldDecl,
				final IRNode varDecl, final boolean isStatic) {
			final String id = VariableDeclarator.getId(varDecl);
			final IJavaType type = getBinder().getJavaType(varDecl);
			
			if (type instanceof IJavaPrimitiveType) {
				createResult(varDecl, true, Messages.FIELD_CONTAINED_PRIMITIVE,
						id);
			} else {
				final VouchFieldIsPromiseDrop vouchDrop = LockRules
						.getVouchFieldIs(varDecl);
				if (vouchDrop != null && vouchDrop.isContainable()) {
					final String reason = vouchDrop.getReason();
					final ResultDropBuilder result = reason == VouchFieldIsNode.NO_REASON ? createResult(
							varDecl, true, Messages.FIELD_CONTAINED_VOUCHED, id)
							: createResult(
									varDecl,
									true,
									Messages.FIELD_CONTAINED_VOUCHED_WITH_REASON,
									id, reason);
					result.addTrustedPromise(vouchDrop);
				} else {
					final IUniquePromise uniqueDrop = UniquenessUtils.getUnique(varDecl);
					final ContainableAnnotationTester tester =
					    new ContainableAnnotationTester(
					        getBinder(), promiseDrop.getAST().getWhenContainable());
          final boolean isContainable = tester.testType(type);
					  
					if (isContainable && uniqueDrop != null) {
						final ResultDropBuilder result = createResult(varDecl,
								true, Messages.FIELD_CONTAINED_OBJECT, id);
						result.addSupportingInformation(varDecl,
								Messages.DECLARED_TYPE_IS_CONTAINABLE,
								type.toString());
						for (final ContainablePromiseDrop p : tester.getDrops()) {
							result.addTrustedPromise(p);
						}
						result.addSupportingInformation(varDecl, Messages.FIELD_IS_UNIQUE);
						result.addTrustedPromise(uniqueDrop.getDrop());
					} else {
						final ResultDropBuilder result =
						    createResult(varDecl, false, Messages.FIELD_BAD, id);

						// Always suggest @Vouch("Containable")
						result.addProposal(new ProposedPromiseBuilder("Vouch",
								"Containable", varDecl, varDecl, Origin.MODEL));

						if (isContainable) {
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_IS_CONTAINABLE,
									type.toString());
	            for (final ContainablePromiseDrop p : tester.getDrops()) {
	              result.addTrustedPromise(p);
	            }
						} else {
							// no @Containable annotation --> Default
							// "annotation" of not containable
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_NOT_CONTAINABLE,
									type.toString());
							for (final IRNode t : tester.getTested()) {
								result.addProposal(new ProposedPromiseBuilder(
										"Containable", null, t, varDecl, Origin.MODEL));
							}
						}

						if (uniqueDrop != null) {
							result.addSupportingInformation(varDecl,
									Messages.FIELD_IS_UNIQUE);
							result.addTrustedPromise(uniqueDrop.getDrop());
						} else {
							result.addSupportingInformation(varDecl,
									Messages.FIELD_NOT_UNIQUE);
							result.addProposal(new ProposedPromiseBuilder(
									"Unique", null, varDecl, varDecl,
									Origin.MODEL));
						}
					}
				}
			}
		}
	}

	private final class ImmutableProcessor extends TypeImplementationProcessor<ImmutablePromiseDrop> {
		private boolean hasFields = false;
		
		public ImmutableProcessor(
				final ImmutablePromiseDrop iDrop,
				final IRNode typeDecl, final IRNode typeBody) {
			super(LockAnalysis.this, iDrop, typeDecl, typeBody);
		}

		@Override
		protected String message2string(final int msg) {
			return Messages.toString(msg);
		}

    @Override
    protected void processSuperType(final IRNode tdecl) {
      final ImmutablePromiseDrop pDrop =
          LockRules.getImmutableImplementation(tdecl);
      if (pDrop != null) {
        final ResultDropBuilder result = createResult(tdecl, true,
            Messages.IMMUTABLE_SUPERTYPE,
            JavaNames.getQualifiedTypeName(tdecl));
        result.addTrustedPromise(pDrop);
      }
    }

		@Override
		protected void postProcess() {
			// We are only called on classes annotated with @Immutable
			if (!hasFields) {
				createResult(typeBody, true, Messages.TRIVIALLY_IMMUTABLE);
			}
		}

		@Override
		protected void processVariableDeclarator(final IRNode fieldDecl,
				final IRNode varDecl, final boolean isStatic) {
			// We have a field
			hasFields = true;

			/*
			 * (1) Field must be final. (2) non-primitive fields must be
			 * @Immutable or Vouched for @Vouch("Immutable")
			 */
			final String id = VariableDeclarator.getId(varDecl);

			final VouchFieldIsPromiseDrop vouchDrop =
			    LockRules.getVouchFieldIs(varDecl);
			if (vouchDrop != null && vouchDrop.isImmutable()) {
				// VOUCHED
				final String reason = vouchDrop.getReason();
				final ResultDropBuilder result = reason == VouchFieldIsNode.NO_REASON ? createResult(
						varDecl, true, Messages.IMMUTABLE_VOUCHED, id)
						: createResult(varDecl, true,
								Messages.IMMUTABLE_VOUCHED_WITH_REASON, id,
								reason);
				result.addTrustedPromise(vouchDrop);
			} else {
				final boolean isFinal = TypeUtil.isFinal(varDecl);
				final IJavaType type = getBinder().getJavaType(varDecl);
				ResultDropBuilder result = null;
				boolean proposeVouch = false;

				if (type instanceof IJavaPrimitiveType) {
					// PRIMITIVELY TYPED
					if (isFinal) {
						result = createResult(varDecl, true,
								Messages.IMMUTABLE_FINAL_PRIMITIVE, id);
					} else {
						result = createResult(varDecl, false,
								Messages.IMMUTABLE_NOT_FINAL, id);
						// Cannot use vouch on primitive types
					}
				} else {
					// REFERENCE-TYPED
				  final ImmutableAnnotationTester tester = 
				      new ImmutableAnnotationTester(
				          getBinder(), promiseDrop.getAST().getWhenImmutable());
          final boolean isImmutable = tester.testType(type);
          
					if (isImmutable) {
						// IMMUTABLE REFERENCE TYPE
						if (isFinal) {
							result = createResult(varDecl, true,
									Messages.IMMUTABLE_FINAL_IMMUTABLE, id);
							for (final ImmutablePromiseDrop p : tester.getDrops()) {
							  result.addTrustedPromise(p);
							}
						} else {
							result = createResult(varDecl, false,
									Messages.IMMUTABLE_NOT_FINAL, id);
              for (final ImmutablePromiseDrop p : tester.getDrops()) {
                result.addTrustedPromise(p);
              }
							proposeVouch = true;
						}
					} else {
            /*
             * If the type is not immutable, we can check to see
             * if the implementation assigned to the field is immutable,
             * but only if the field is final.
             */
					  boolean stillBad = true;
            if (isFinal) {
              final IRNode init = VariableDeclarator.getInit(varDecl);
              if (Initialization.prototype.includes(init)) {
                final IRNode initExpr = Initialization.getValue(init);
                if (NewExpression.prototype.includes(initExpr)) {
                  final ImmutablePromiseDrop implTypeIDrop =
                      LockRules.getImmutableImplementation(
                          ((IJavaDeclaredType) getBinder().getJavaType(initExpr)).getDeclaration());
                  if (implTypeIDrop != null) {
                    // we have an instance of an immutable implementation
                    stillBad = false;
                    result = createResult(varDecl, true,
                        Messages.IMMUTABLE_FINAL_IMMUTABLE, id);
                    result.addTrustedPromise(implTypeIDrop);
                    result.addSupportingInformation(
                        varDecl, Messages.IMMUTABLE_IMPL);
                  }
                }
              }
            }
					  
            if (stillBad) {
  						// MUTABLE REFERENCE TYPE
  						proposeVouch = true;
  						if (isFinal) {
  							result = createResult(varDecl, false,
  									Messages.IMMUTABLE_FINAL_NOT_IMMUTABLE, id);
  						} else {
  							result = createResult(varDecl, false,
  									Messages.IMMUTABLE_NOT_FINAL_NOT_IMMUTABLE,
  									id);
  						}
  						for (final IRNode typeDecl : tester.getTested()) {
  							result.addProposal(new ProposedPromiseBuilder(
  									"Immutable", null, typeDecl, varDecl,
  									Origin.MODEL));
  						}
            }
					}
				}

				if (proposeVouch && result != null) {
					result.addProposal(new ProposedPromiseBuilder("Vouch",
							"Immutable", varDecl, varDecl, Origin.MODEL));
				}
			}
		}
	}
//
//	private static boolean isArrayTypeContainable(final IJavaArrayType aType,
//	    final ContainableAnnotationTester tester) {
//	  if (aType.getDimensions() == 1) {
//	    final IJavaType baseType = aType.getBaseType();
//	    if (baseType instanceof IJavaPrimitiveType) {
//	      return true;
//	    } else {
//	      return testDeclaredOrIntersectionType(baseType, tester);
//	    }
//	  } else {
//	    return false;
//	  }
//	}
	
	
	
	private static abstract class TypeDeclAnnotationTester<P extends ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>> {
	  private final ITypeEnvironment typeEnv;
	  private final IJavaDeclaredType javaLangObject;
	  private final Set<IRNode> tested = new HashSet<IRNode>();
	  private final Set<P> drops = new HashSet<P>();
	  private final Set<IRNode> failed = new HashSet<IRNode>();
	  
	  
	  
	  protected TypeDeclAnnotationTester(final IBinder binder) {
	    final ITypeEnvironment te = binder.getTypeEnvironment();
      typeEnv = te;
	    javaLangObject = te.getObjectType();
	  }
	  
	  
    
    public final Iterable<IRNode> getTested() {
      return tested;
    }
    
    public final Iterable<P> getDrops() {
      return drops;
    }
    
    public final Iterable<IRNode> getFailed() {
      return failed;
    }

	  
	  
	  public final boolean testType(final IJavaType type) {
	    if (type instanceof IJavaNullType) {
	      return false;
	    } else if (type instanceof IJavaPrimitiveType) {
	      return false;
	    } else if (type instanceof IJavaVoidType) {
	      return false;
	    } else if (type instanceof IJavaDeclaredType) {
	      return testDeclaredType(((IJavaDeclaredType) type).getDeclaration());
      } else if (type instanceof IJavaArrayType) {
	      return testArrayType((IJavaArrayType) type);
	    } else if (type instanceof IJavaCaptureType) {
        final IJavaType upper = ((IJavaCaptureType) type).getUpperBound();
        testType((upper == null) ? javaLangObject : upper);
	    } else if (type instanceof IJavaIntersectionType) {
	      final IJavaIntersectionType intType = (IJavaIntersectionType) type;
	      final boolean first = testType(intType.getPrimarySupertype());
	      final boolean second = testType(intType.getSecondarySupertype());
        /*
         * Intersection implies AND, so you would think that we should conjoin
         * the results below. But an interface that is not annotated with X may
         * have X-annotated implementations. So mixing an unannotated interface
         * into the intersection doesn't kill the possibility of X-ness. If the
         * class portion of the intersection is not-X, then really the whole
         * intersection should be false, but it's not possible to have a
         * implementation that is X where the class is not-X and an interface is
         * X, so it doesn't matter if we let this case through here.
         */
	      return first || second;
	    } else if (type instanceof IJavaTypeFormal) {
	      // First check the formal against a "when" attribute
	      if (testFormalAgainstAnnotationBounds(
	          TypeFormal.getId(((IJavaTypeFormal) type).getDeclaration()))) {
	        return true;
	      } else {
	        // Test the upperbound
          final IJavaType upper = ((IJavaTypeFormal) type).getSuperclass(typeEnv);
          return testType((upper == null) ? javaLangObject : upper);
	      }
	    } else if (type instanceof IJavaUnionType) {
	      // Can't get the least upper bound, use object instead
	      return testType(javaLangObject);
	    } else if (type instanceof IJavaWildcardType) {
        // dead case?  Turned into Capture types, I think
        final IJavaType upper = ((IJavaCaptureType) type).getUpperBound();
        testType((upper == null) ? javaLangObject : upper);
	    } 
	    // shouldn't get here?
	    return false;
	  }
	  
	  
	  
	  protected final boolean testDeclaredType(final IRNode type) {
	    tested.add(type);
	    final P drop = testTypeDeclaration(type);
	    if (drop != null) {
	      drops.add(drop);
	      return true;
	    } else {
	      failed.add(type);
	      return false;
	    }
	  }

    protected static boolean testFormalAgainstAnnotationBounds(
        final String formalName, final NamedTypeNode[] annotationBounds) {
      for (final NamedTypeNode namedType : annotationBounds) {
        if (namedType.getType().equals(formalName)) {
          return true;
        }
      }
      return false;
    }
	  
	  protected abstract boolean testArrayType(IJavaArrayType type);
	  
	  protected abstract P testTypeDeclaration(IRNode type);
	  
    protected abstract boolean testFormalAgainstAnnotationBounds(String formalName);
	}
  
  private static final class ThreadSafeAnnotationTester extends TypeDeclAnnotationTester<ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>> {
    private final NamedTypeNode[] whenImmutable;
    private final NamedTypeNode[] whenThreadSafe;
    
    public ThreadSafeAnnotationTester(final IBinder binder,
        final NamedTypeNode[] whenI, final NamedTypeNode[] whenTS) {
      super(binder);
      whenImmutable = whenI;
      whenThreadSafe = whenTS;
    }
    
    @Override
    protected ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> testTypeDeclaration(IRNode type) {
      return LockRules.getThreadSafeTypePromise(type);
    }
    
    @Override
    protected boolean testFormalAgainstAnnotationBounds(final String formalName) {
      if (testFormalAgainstAnnotationBounds(formalName, whenImmutable)) {
        return true;
      } else {
        return testFormalAgainstAnnotationBounds(formalName, whenThreadSafe);
      }
    }
    
    @Override
    protected boolean testArrayType(final IJavaArrayType type) {
      return false;
    }
  }
  
  private static final class ImmutableAnnotationTester extends TypeDeclAnnotationTester<ImmutablePromiseDrop> {
    private final NamedTypeNode[] whenImmutable;

    public ImmutableAnnotationTester(
        final IBinder binder, final NamedTypeNode[] annotationBounds) {
      super(binder);
      whenImmutable = annotationBounds;
    }
    
    @Override
    protected ImmutablePromiseDrop testTypeDeclaration(IRNode type) {
      return LockRules.getImmutableType(type);
    }           
    
    @Override
    protected boolean testFormalAgainstAnnotationBounds(final String formalName) {
      return testFormalAgainstAnnotationBounds(formalName, whenImmutable);
    }
    
    @Override
    protected boolean testArrayType(final IJavaArrayType type) {
      return false;
    }
  }

  private static final class ContainableAnnotationTester extends TypeDeclAnnotationTester<ContainablePromiseDrop> {
    private final NamedTypeNode[] whenContainable;

    public ContainableAnnotationTester(
        final IBinder binder, final NamedTypeNode[] annotationBounds) {
      super(binder);
      whenContainable = annotationBounds;
    }
    
    @Override
    protected ContainablePromiseDrop testTypeDeclaration(IRNode type) {
      return LockRules.getContainableType(type);
    }
    
    @Override
    protected boolean testFormalAgainstAnnotationBounds(final String formalName) {
      return testFormalAgainstAnnotationBounds(formalName, whenContainable);
    }
    
    @Override
    protected boolean testArrayType(final IJavaArrayType type) {
      if (type.getDimensions() == 1) {
        final IJavaType baseType = type.getBaseType();
        if (baseType instanceof IJavaPrimitiveType) {
          return true;
        } else {
          return testType(baseType);
        }
      } else {
        return false;
      }
    }
  }
  
  
	
//	private <P extends ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>> boolean testFieldType(
//	    final IJavaType type, final TypeDeclAnnotationTester<P> tester) {
//    /* We assume the type is a reference type. The upper bound of the field's
//     * type is thus an IJavaArrayType, IJavaIntersectionType, or
//     * IJavaDeclaredType. (Cannot be null or void in a field declaration).
//     * 
//     * Array type is not immutable.
//     * 
//     * Intersection type is immutable if any one of the conjoined types is
//     * declared to be immutable.
//     * 
//     * Declared type is immutable if the type is declared to be immutable.
//     */
//    final IJavaType upperBound = upperBoundGetter.getUpperBound(type);
//    
//    if (upperBound instanceof IJavaArrayType) {
//      return false;
//    } else { // Declared or intersection type
//      return testDeclaredOrIntersectionType(upperBound, tester);
//    }
//	}
	
//	private static <P extends ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>> boolean testDeclaredOrIntersectionType(
//	    final IJavaType type, final TypeDeclAnnotationTester<P> tester) {
//	  // type is IJavaDeclaredType or IJavaIntersectionType
//	  if (type instanceof IJavaDeclaredType) {
//	    return tester.testDeclaredType(((IJavaDeclaredType) type).getDeclaration());
//	  } else {
//	    final IJavaIntersectionType iType = (IJavaIntersectionType) type;
//	    /* Avoid short-circuit evaluation of the disjunction because we want 
//	     * to visit all the types in the intersection. 
//	     */
//	    final boolean first = 
//	        testDeclaredOrIntersectionType(iType.getPrimarySupertype(), tester);
//	    final boolean second =
//	        testDeclaredOrIntersectionType(iType.getSecondarySupertype(), tester); 
//	    return first || second;
//	  }
//	}
}
