package com.surelogic.analysis.locks;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.aast.IAASTRootNode;
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
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaIntersectionType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.TypeUtil.UpperBoundGetter;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.ModifiedBooleanPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ContainablePromiseDrop;
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

	private UpperBoundGetter upperBoundGetter = null;
	
	
	
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

		upperBoundGetter = new UpperBoundGetter(binder.getTypeEnvironment());
		
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
						JavaTypeFactory.getMyThisType(classDecl));
			} else {
				globalLockModel.addPolicyLockDeclaration(binder, lockDrop,
						JavaTypeFactory.getMyThisType(classDecl));
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

	private final class ThreadSafeProcessor extends TypeImplementationProcessor {
		private final Set<RegionLockRecord> lockDeclarations;
		private boolean hasFields = false;

		public ThreadSafeProcessor(
				final PromiseDrop<? extends IAASTRootNode> tsDrop,
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
			if (vouchDrop != null && vouchDrop.isThreadSafe()) {
				final String reason = vouchDrop.getReason();
				final ResultDropBuilder result = reason == VouchFieldIsNode.NO_REASON ? createResult(
						varDecl, true, Messages.VOUCHED_THREADSAFE, id)
						: createResult(varDecl, true,
								Messages.VOUCHED_THREADSAFE_WITH_REASON, id,
								reason);
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
          final Iterable<ContainablePromiseDrop> cDrops;
          final Iterable<IRNode> notContainable;
          
          if (!isPrimitive && !isArray) { // type formal or declared type
            final TrackingAnnotationTester<ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>> tsTester =
                new TrackingAnnotationTester<ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>>() {
                  @Override
                  protected ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> testTypeDeclImpl(IRNode type) {
                    return LockRules.getThreadSafeTypePromise(type);
                  }
                };
            final boolean isTS = testFieldType(type, tsTester);
            testedType = true;
            /*
             * If the type is not thread safe, we can check to see
             * if the implementation assigned to the field is thread
             * safe, but only if the field is final.
             */
            if (!isTS && isFinal) {
              final IRNode init = VariableDeclarator.getInit(varDecl);
              // XXX: This is not right: Should specifically check for NewExpression!
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
                    notThreadSafe = EmptyIterator.prototype();
                  } else {
                    isThreadSafe = false;
                    tsDrops = EmptyIterator.prototype();
                    notThreadSafe = tsTester.getFailed();
                  }
                } else {
                  usingImplDrop = false;
                  isThreadSafe = false;
                  tsDrops = EmptyIterator.prototype();
                  notThreadSafe = tsTester.getFailed();
                }                
              } else {
                usingImplDrop = false;
                isThreadSafe = false;
                tsDrops = EmptyIterator.prototype();
                notThreadSafe = tsTester.getFailed();
              }
            } else {
              usingImplDrop = false;
              isThreadSafe = isTS;
              tsDrops = tsTester.getDrops();
              notThreadSafe = tsTester.getFailed();
            }
            
            final TrackingAnnotationTester<ContainablePromiseDrop> cTester =
                new TrackingAnnotationTester<ContainablePromiseDrop>() {
                  @Override
                  protected ContainablePromiseDrop testTypeDeclImpl(IRNode type) {
                    return LockRules.getContainableType(type);
                  }
                };
            isDeclaredContainable = testFieldType(type, cTester);
            cDrops = cTester.getDrops();
            notContainable = cTester.getFailed();
          } else {
            testedType = false;
            usingImplDrop = false;
            isThreadSafe = false;
            tsDrops = EmptyIterator.prototype();
            notThreadSafe = EmptyIterator.prototype();
            isDeclaredContainable = false;
            cDrops = EmptyIterator.prototype();
            notContainable = EmptyIterator.prototype();
          }
          
          final boolean isContainable = isDeclaredContainable 
              || (isArray && isArrayTypeContainable((IJavaArrayType) type));

					/*
					 * @ThreadSafe takes priority over @Containable: If the type
					 * is threadsafe don't check the aggregation status
					 */
					final PromiseDrop<? extends IAASTRootNode> uDrop =
					    UniquenessUtils.getFieldUnique(varDecl);
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
							for (final ContainablePromiseDrop p : cDrops) {
								result.addTrustedPromise(p);
							}
							result.addTrustedPromise(uDrop);
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
							for (final IRNode n : notContainable) {
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
			TypeImplementationProcessor {
		public ContainableProcessor(
				final PromiseDrop<? extends IAASTRootNode> cDrop,
				final IRNode typeDecl, final IRNode typeBody) {
			super(LockAnalysis.this, cDrop, typeDecl, typeBody);
		}

		@Override
		protected String message2string(final int msg) {
			return Messages.toString(msg);
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
			final boolean isPrimitive = type instanceof IJavaPrimitiveType;
			final boolean isArray = type instanceof IJavaArrayType;
			if (isPrimitive) {
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
					final PromiseDrop<? extends IAASTRootNode> uniqueDrop = UniquenessUtils
							.getFieldUnique(varDecl);

					final boolean isContainable;
					final Iterable<IRNode> types;
					final Iterable<ContainablePromiseDrop> drops;
					if (isArray) {
            isContainable = isArrayTypeContainable((IJavaArrayType) type);
            types = EmptyIterator.prototype();
            drops = EmptyIterator.prototype();
					} else { // formal type variable or declared type
	          final TrackingAnnotationTester<ContainablePromiseDrop> tester =
	              new TrackingAnnotationTester<ContainablePromiseDrop>() {
	                @Override
	                protected ContainablePromiseDrop testTypeDeclImpl(IRNode type) {
	                  return LockRules.getContainableType(type);
	                }
    	          };
	          
	          isContainable = testFieldType(type, tester);
	          types = tester.getTested();
	          drops = tester.getDrops();
	        }
					  
					if (isContainable && uniqueDrop != null) {
						final ResultDropBuilder result = createResult(varDecl,
								true, Messages.FIELD_CONTAINED_OBJECT, id);
						result.addSupportingInformation(varDecl,
								Messages.DECLARED_TYPE_IS_CONTAINABLE,
								type.toString());
						for (final ContainablePromiseDrop p : drops) {
							result.addTrustedPromise(p);
						}
						result.addSupportingInformation(varDecl, Messages.FIELD_IS_UNIQUE);
						result.addTrustedPromise(uniqueDrop);
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
	            for (final ContainablePromiseDrop p : drops) {
	              result.addTrustedPromise(p);
	            }
						} else {
							// no @Containable annotation --> Default
							// "annotation" of not containable
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_NOT_CONTAINABLE,
									type.toString());
							for (final IRNode t : types) {
								result.addProposal(new ProposedPromiseBuilder(
										"Containable", null, t, varDecl, Origin.MODEL));
							}
						}

						if (uniqueDrop != null) {
							result.addSupportingInformation(varDecl,
									Messages.FIELD_IS_UNIQUE);
							result.addTrustedPromise(uniqueDrop);
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

	private final class ImmutableProcessor extends TypeImplementationProcessor {
		private boolean hasFields = false;
		
		public ImmutableProcessor(
				final PromiseDrop<? extends IAASTRootNode> iDrop,
				final IRNode typeDecl, final IRNode typeBody) {
			super(LockAnalysis.this, iDrop, typeDecl, typeBody);
		}

		@Override
		protected String message2string(final int msg) {
			return Messages.toString(msg);
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

			final VouchFieldIsPromiseDrop vouchDrop = LockRules
					.getVouchFieldIs(varDecl);
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
				final boolean isPrimitive = (type instanceof IJavaPrimitiveType);
				ResultDropBuilder result = null;
				boolean proposeVouch = false;

				if (isPrimitive) {
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
				  final TrackingAnnotationTester<ImmutablePromiseDrop> tester = new TrackingAnnotationTester<ImmutablePromiseDrop>() {
            @Override
            protected ImmutablePromiseDrop testTypeDeclImpl(IRNode type) {
              return LockRules.getImmutableType(type);
            }				    
          };
          
          final boolean isImmutable = testFieldType(type, tester);
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

				if (proposeVouch) {
					result.addProposal(new ProposedPromiseBuilder("Vouch",
							"Immutable", varDecl, varDecl, Origin.MODEL));
				}
			}
		}
	}

	private static boolean isArrayTypeContainable(final IJavaArrayType aType) {
		return (aType.getBaseType() instanceof IJavaPrimitiveType)
				&& (aType.getDimensions() == 1);
	}
	
	
	
	private interface TypeDeclAnnotationTester {
	  public boolean testTypeDecl(final IRNode typeDecl);
	}
	
	private abstract class TrackingAnnotationTester<P extends ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode>> implements TypeDeclAnnotationTester {
	  private final Set<IRNode> tested = new HashSet<IRNode>();
	  private final Set<P> drops = new HashSet<P>();
	  private final Set<IRNode> failed = new HashSet<IRNode>();
	  
	  public final boolean testTypeDecl(final IRNode type) {
	    tested.add(type);
	    final P drop = testTypeDeclImpl(type);
	    if (drop != null) {
	      drops.add(drop);
	      return true;
	    } else {
	      failed.add(type);
	      return false;
	    }
	  }
	  
	  protected abstract P testTypeDeclImpl(IRNode type);
	  
	  public Iterable<IRNode> getTested() {
	    return tested;
	  }
	  
	  public Iterable<P> getDrops() {
	    return drops;
	  }
	  
	  public Iterable<IRNode> getFailed() {
	    return failed;
	  }
	}
	
	private boolean testFieldType(
	    final IJavaType type, final TypeDeclAnnotationTester tester) {
    /* We assume the type is a reference type. The upper bound of the field's
     * type is thus an IJavaArrayType, IJavaIntersectionType, or
     * IJavaDeclaredType. (Cannot be null or void in a field declaration).
     * 
     * Array type is not immutable.
     * 
     * Intersection type is immutable if any one of the conjoined types is
     * declared to be immutable.
     * 
     * Declared type is immutable if the type is declared to be immutable.
     */
    final IJavaType upperBound = upperBoundGetter.getUpperBound(type);
    
    if (upperBound instanceof IJavaArrayType) {
      return false;
    } else { // Declared or intersection type
      return testDeclaredOrIntersectionType(upperBound, tester);
    }
	}
	
	private static boolean testDeclaredOrIntersectionType(
	    final IJavaType type, final TypeDeclAnnotationTester tester) {
	  // type is IJavaDeclaredType or IJavaIntersectionType
	  if (type instanceof IJavaDeclaredType) {
	    return tester.testTypeDecl(((IJavaDeclaredType) type).getDeclaration());
	  } else {
	    final IJavaIntersectionType iType = (IJavaIntersectionType) type;
	    /* Avoid short-circuit evaluation of the disjunciton because we want 
	     * to visit all the types in the intersection. 
	     */
	    final boolean first = 
	        testDeclaredOrIntersectionType(iType.getPrimarySupertype(), tester);
	    final boolean second =
	        testDeclaredOrIntersectionType(iType.getSecondarySupertype(), tester); 
	    return first || second;
	  }
	}
}
