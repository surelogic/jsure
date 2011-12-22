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
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
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
	public void startAnalyzeBegin(IIRProject p, IBinder binder) {
		super.startAnalyzeBegin(p, binder);

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
			final VouchFieldIsPromiseDrop vouchDrop = LockRules
					.getVouchFieldIs(varDecl);
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
				final RegionLockRecord fieldLock = getLockForRegion(RegionModel
						.getInstance(varDecl));

				if (isFinal || isVolatile || fieldLock != null) {
					/* Now check if the referenced object is thread safe */
					final IJavaType type = getBinder().getJavaType(varDecl);
					final IRNode typeDecl;
					final boolean isPrimitive = type instanceof IJavaPrimitiveType;
					final boolean isArray = type instanceof IJavaArrayType;
					final PromiseDrop<? extends AbstractModifiedBooleanNode> declTSDrop;
					final boolean usingImplDrop;
					final ContainablePromiseDrop declContainableDrop;

					if (type instanceof IJavaDeclaredType) {
						typeDecl = ((IJavaDeclaredType) type).getDeclaration();
						// Null if no @ThreadSafe ==> not thread safe
						final PromiseDrop<? extends AbstractModifiedBooleanNode> typePromise = LockRules
								.getThreadSafeTypePromise(typeDecl);
						/*
						 * If the type is not thread safe, we can check to see
						 * if the implementation assigned to the field is thread
						 * safe, but only if the field is final.
						 */
						if (typePromise == null && isFinal) {
							final IRNode init = VariableDeclarator
									.getInit(varDecl);
							if (Initialization.prototype.includes(init)) {
								declTSDrop = LockRules
										.getThreadSafeImplPromise(((IJavaDeclaredType) getBinder()
												.getJavaType(init))
												.getDeclaration());
								usingImplDrop = true;
							} else {
								declTSDrop = typePromise;
								usingImplDrop = false;
							}
						} else {
							declTSDrop = typePromise;
							usingImplDrop = false;
						}
						// Null if no @Containable ==> Default annotation of not
						// containable
						declContainableDrop = LockRules
								.getContainableType(typeDecl);
					} else {
						typeDecl = null;
						declTSDrop = null;
						declContainableDrop = null;
						usingImplDrop = false;
					}

					final boolean isContainable = (declContainableDrop != null)
							|| (isArray && isArrayTypeContainable((IJavaArrayType) type));

					/*
					 * @ThreadSafe takes priority over @Containable: If the type
					 * is threadsafe don't check the aggregation status
					 */
					final PromiseDrop<? extends IAASTRootNode> uDrop = UniquenessUtils
							.getFieldUnique(varDecl);
					final Map<IRegion, IRegion> aggMap;
					boolean isContained = false;
					if (declTSDrop == null && isContainable) {
						if (uDrop != null) {
							aggMap = UniquenessUtils
									.constructRegionMapping(varDecl);
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
					if (isPrimitive || declTSDrop != null || isContained) {
						final ResultDropBuilder result;
						if (isFinal) {
							result = createResult(varDecl, true,
									Messages.FINAL_AND_THREADSAFE, id);
						} else if (isVolatile) {
							result = createResult(varDecl, true,
									Messages.VOLATILE_AND_THREADSAFE, id);
						} else { // lock protected
							result = createResult(varDecl, true,
									Messages.PROTECTED_AND_THREADSAFE, id,
									fieldLock.name);
							result.addTrustedPromise(fieldLock.lockDecl);
						}

						if (isPrimitive) {
							result.addSupportingInformation(varDecl,
									Messages.PRIMITIVE_TYPE, typeString);
						} else if (declTSDrop != null) {
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_IS_THREAD_SAFE,
									typeString);
							result.addTrustedPromise(declTSDrop);
							if (usingImplDrop) {
								result.addSupportingInformation(varDecl,
										Messages.THREAD_SAFE_IMPL);
							}
						} else { // contained
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_IS_CONTAINABLE,
									typeString);
							if (declContainableDrop != null) {
								result.addTrustedPromise(declContainableDrop);
							}
							result.addTrustedPromise(uDrop);
							for (final IRegion destRegion : aggMap.values()) {
								result.addTrustedPromise(getLockForRegion(destRegion).lockDecl);
							}
						}
					} else {
						final ResultDropBuilder result = createResult(varDecl,
								false, Messages.UNSAFE_REFERENCE, id);
						// type could be a non-declared, non-primitive type,
						// that is, an array
						if (typeDecl != null) {
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_IS_NOT_THREAD_SAFE,
									typeString);
							if (declTSDrop == null) {
								result.addProposal(new ProposedPromiseBuilder(
										"ThreadSafe", null, typeDecl, varDecl,
										Origin.MODEL));
							}
							if (declContainableDrop == null) {
								result.addProposal(new ProposedPromiseBuilder(
										"Containable", null, typeDecl, varDecl,
										Origin.MODEL));
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
					final ContainablePromiseDrop declContainableDrop;
					final IRNode typeDecl;
					if (isArray) {
						typeDecl = null;
						declContainableDrop = null;
						isContainable = isArrayTypeContainable((IJavaArrayType) type);
					} else if (type instanceof IJavaDeclaredType) {
						typeDecl = ((IJavaDeclaredType) type).getDeclaration();
						declContainableDrop = LockRules
								.getContainableType(typeDecl);
						isContainable = declContainableDrop != null;
					} else {
						typeDecl = null;
						declContainableDrop = null;
						isContainable = false;
					}

					if (isContainable && uniqueDrop != null) {
						final ResultDropBuilder result = createResult(varDecl,
								true, Messages.FIELD_CONTAINED_OBJECT, id);
						result.addSupportingInformation(varDecl,
								Messages.DECLARED_TYPE_IS_CONTAINABLE,
								type.toString());
						if (declContainableDrop != null) {
							result.addTrustedPromise(declContainableDrop);
						}
						result.addSupportingInformation(varDecl,
								Messages.FIELD_IS_UNIQUE);
						result.addTrustedPromise(uniqueDrop);
					} else {
						final ResultDropBuilder result = createResult(varDecl,
								false, Messages.FIELD_BAD, id);

						// Always suggest @Vouch("Containable")
						result.addProposal(new ProposedPromiseBuilder("Vouch",
								"Containable", varDecl, varDecl, Origin.MODEL));

						if (isContainable) {
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_IS_CONTAINABLE,
									type.toString());
							if (declContainableDrop != null) {
								result.addTrustedPromise(declContainableDrop);
							}
						} else {
							// no @Containable annotation --> Default
							// "annotation" of not containable
							result.addSupportingInformation(varDecl,
									Messages.DECLARED_TYPE_NOT_CONTAINABLE,
									type.toString());
							if (typeDecl != null) {
								result.addProposal(new ProposedPromiseBuilder(
										"Containable", null, typeDecl, varDecl,
										Origin.MODEL));
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
					final IRNode typeDecl = (type instanceof IJavaDeclaredType) ? ((IJavaDeclaredType) type)
							.getDeclaration() : null;
					// no @Immutable annotation --> Default "annotation" of
					// mutable
					final ImmutablePromiseDrop declImmutableDrop = LockRules
							.getImmutableType(typeDecl);

					if (declImmutableDrop != null) {
						// IMMUTABLE REFERENCE TYPE
						if (isFinal) {
							result = createResult(varDecl, true,
									Messages.IMMUTABLE_FINAL_IMMUTABLE, id);
							result.addTrustedPromise(declImmutableDrop);
						} else {
							result = createResult(varDecl, false,
									Messages.IMMUTABLE_NOT_FINAL, id);
							result.addTrustedPromise(declImmutableDrop);
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
						if (typeDecl != null) {
							result.addProposal(new ProposedPromiseBuilder(
									"Immutable", null, typeDecl, varDecl,
									Origin.MODEL));
						} else {
							// TODO what if this is a type formal, or something
							// else?
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
}
