package com.surelogic.analysis.concurrency.driver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.analysis.AbstractAnalysisSharingAnalysis;
import com.surelogic.analysis.ConcurrencyType;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.heldlocks.GlobalLockModel;
import com.surelogic.analysis.concurrency.heldlocks.LockUtils;
import com.surelogic.analysis.concurrency.heldlocks.LockVisitor;
import com.surelogic.analysis.concurrency.threadsafe.ContainableProcessor;
import com.surelogic.analysis.concurrency.threadsafe.ImmutableProcessor;
import com.surelogic.analysis.concurrency.threadsafe.ThreadSafeProcessor;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.visitors.TopLevelAnalysisVisitor;
import com.surelogic.analysis.visitors.TopLevelAnalysisVisitor.TypeBodyPair;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.DropPredicateFactory;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.locks.LockModel;
import com.surelogic.dropsea.ir.drops.type.constraints.ContainablePromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ImmutablePromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ThreadSafePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import extra166y.Ops.Procedure;

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

	private final AtomicReference<GlobalLockModel> lockModelHandle =
	    new AtomicReference<GlobalLockModel>(null);
	
	private final AtomicReference<ConcurrentStateMetrics> metrics = 
		new AtomicReference<ConcurrentStateMetrics>(null);
	
	public LockAnalysis() {
		super(willRunInParallel, queueWork ? TypeBodyPair.class : null,
				"LockAssurance", BindingContextAnalysis.factory);
		if (runInParallel() == ConcurrencyType.INTERNALLY) {
			setWorkProcedure(new Procedure<TypeBodyPair>() {
				@Override
        public void op(final TypeBodyPair n) {
					if (byCompUnit) {
						// System.out.println("Parallel Lock: "+JavaNames.genPrimaryTypeName(n));
						TopLevelAnalysisVisitor.processCompilationUnit(
								new ClassProcessor(getAnalysis()),
								// actually n.typeDecl is a CompilationUnit
								// here!
								n.getType());
					} else {
						// System.out.println("Parallel Lock: "+JavaNames.getRelativeTypeName(n));
						actuallyAnalyzeClassBody(getAnalysis(),
								n.getType(),	n.getClassBody());
					}
				}
			});
		}
	}
	
	private final void actuallyAnalyzeClassBody(
	    final LockVisitor lv, 
	    final IRNode typeDecl, final IRNode typeBody) {
	  lv.analyzeClass(typeBody);
	  
	  final ThreadSafePromiseDrop threadSafeDrop =
			  LockRules.getThreadSafeImplementation(typeDecl);
	  // If null, assume it's not meant to be thread safe
	  // Also check for verify=false
	  if (threadSafeDrop != null && threadSafeDrop.verify()) {
		  new ThreadSafeProcessor(getBinder(), threadSafeDrop, typeDecl, typeBody, lockModelHandle.get()).processType();
	  }

	  final ContainablePromiseDrop containableDrop = 
			  LockRules.getContainableImplementation(typeDecl);
	  // no @Containable annotation --> Default "annotation" of not containable
	  // Also check for verify=false
	  if (containableDrop != null && containableDrop.verify()) {
		  new ContainableProcessor(getBinder(), containableDrop, typeDecl, typeBody).processType();
	  }

	  final ImmutablePromiseDrop immutableDrop = LockRules
			  .getImmutableImplementation(typeDecl);
	  // no @Immutable annotation --> Default "annotation" of mutable
	  // Also check for verify=false
	  if (immutableDrop != null && immutableDrop.verify()) {
		  new ImmutableProcessor(getBinder(), immutableDrop, typeDecl, typeBody, lockModelHandle.get()).processType();
	  }	  
	  metrics.get().summarizeFieldInfo(typeDecl, typeBody, lv.getLockUtils());	  
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

		// Make sure that the metrics are initialized with the appropriate binder
		final ConcurrentStateMetrics metricsRef = new ConcurrentStateMetrics(binder);
		metrics.set(metricsRef);
		
		// Initialize the global lock model
		final GlobalLockModel globalLockModel = new GlobalLockModel(binder);

		/*
		 * This seems stupid to me. I feel like I should be able to get the
		 * LockModel object from the LockDeclarationDrop and
		 * PromiseLockDeclarationDrop objects. I shouldn't have to use the lock
		 * name as an intermediary. But the as far as I can tell, there is no
		 * back link from the drop to the LockModel.
		 */

		// Run through the LockModel and add them to the GlobalLockModel
    final List<LockModel> lockModelDrops = Sea.getDefault().getDropsOfType(LockModel.class);
		for (LockModel lockDrop : lockModelDrops) {
			if (lockDrop.getMessage().contains("MUTEX")) {
				System.err.println("Looking at @"+lockDrop.getMessage());
			}
			final IRNode classDecl = lockDrop.getNode();

			if (classDecl == null) {
				LOG.severe("TODO invalidate dependent drops");
				lockDrop.invalidate();
				continue;
			}
			if (lockDrop.getAAST() == null) {
				LOG.warning("No AST for " + lockDrop.getMessage());
				continue;
			}
			if (lockDrop.getAAST() instanceof LockDeclarationNode) {
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
		boolean flushed = false;
		if (byCompUnit) {
			flushed = queueWork(new TypeBodyPair(compUnit, null));
		} else {
			// FIX factor out?
			final ClassProcessor cp = new ClassProcessor(getAnalysis());
			TopLevelAnalysisVisitor.processCompilationUnit(cp, compUnit);
			if (runInParallel() == ConcurrencyType.INTERNALLY) {
				if (queueWork) {
					flushed = queueWork(cp.getTypeBodies());
				} else {
					runInParallel(TypeBodyPair.class, cp.getTypeBodies(),
							getWorkProcedure());
				}
			}
		}
		if (flushed) {
			JavaComponentFactory.clearCache();
		}
		return true;
	}

	@Override
	public IAnalysisGranulator<TypeBodyPair> getGranulator() {
		return TopLevelAnalysisVisitor.granulator;
	}
	
	@Override
	protected boolean doAnalysisOnGranule_wrapped(IIRAnalysisEnvironment env, TypeBodyPair n) {
		final LockVisitor v = getAnalysis();
		try {			
			actuallyAnalyzeClassBody(v, n.getType(), n.getClassBody());
		} finally {
			v.clearAfterEachGranule();
		}
		return true; 
	}
	
	@Override
	public Iterable<TypeBodyPair> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
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
		private final List<TypeBodyPair> types = new ArrayList<TypeBodyPair>();

		public ClassProcessor(final LockVisitor lv) {
			lockVisitor = lv;
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
				actuallyAnalyzeClassBody(lockVisitor, typeDecl, classBody);
			}
		}
	}
}
