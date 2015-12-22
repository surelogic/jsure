package com.surelogic.analysis.concurrency.driver;

import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.IIRProject;
import com.surelogic.analysis.concurrency.heldlocks.LockUtils;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.dropsea.ir.DropPredicateFactory;
import com.surelogic.dropsea.ir.Sea;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.locks.GuardedByPromiseDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;

public class LockModelBuilder
extends AbstractWholeIRAnalysis<IBinderClient, CUDrop> {
  private static final AtomicReference<AnalysisLockModel> newLockModelHandle =
      new AtomicReference<AnalysisLockModel>(null);
	
	public LockModelBuilder() {
		super("Lock Model Builder");
	}
	
	public static AtomicReference<AnalysisLockModel> getLockModel() {
	  return newLockModelHandle;
	}

	@Override
	public void init(IIRAnalysisEnvironment env) {
		super.init(env);
		env.ensureClassIsLoaded(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_LOCK);
		env.ensureClassIsLoaded(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK);
	}

  @Override
  protected IBinderClient constructIRAnalysis(final IBinder binder) {
    return null;
  }

	@Override
	public void startAnalyzeBegin(final IIRProject p, final IBinder binder) {
		super.startAnalyzeBegin(p, binder);

		final AnalysisLockModel newLockModel = new AnalysisLockModel(binder);
		
		// Run through the LockModel and add them to the GlobalLockModel
    for (LockModel lockDrop : Sea.getDefault().getDropsOfType(LockModel.class)) {
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
				newLockModel.addLockDeclaration(lockDrop);
			} else {
        newLockModel.addLockDeclaration(lockDrop);
			}
		}

		// Get all the GuardedBy annotations on fields
    for (GuardedByPromiseDrop guardedByDrop : Sea.getDefault().getDropsOfType(GuardedByPromiseDrop.class)) {
      final IRNode decl = guardedByDrop.getNode();
      if (!SomeFunctionDeclaration.prototype.includes(decl)) { // Ignore method/constructor annotations
        newLockModel.addGuardedByDelaration(guardedByDrop);
      }
    }		
//
//    try {
//      final PrintWriter pw = new PrintWriter("/Users/aarong/model.txt");
//      newLockModel.dumpModel(pw);
//      pw.close();
//    } catch (IOException e) {
//      // eat it
//    }
    
		// Share the new global lock model with the lock visitor, and other
		// helpers
		newLockModelHandle.set(newLockModel);
	}

	@Override
	protected boolean flushAnalysis() {
		return true;
	}

	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud,
			final IRNode compUnit) {
	  // do nothing
	  return true;
	}

  @Override
  protected void clearCaches() {
    // Nothing to do
  }
}
