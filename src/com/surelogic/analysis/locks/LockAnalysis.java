/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/LockAnalysis.java,v 1.5 2008/09/08 19:12:16 chance Exp $*/
package com.surelogic.analysis.locks;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.uwm.BindingContextAnalysis;
import com.surelogic.analysis.effects.Effects;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.analysis.TypeBasedAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

public class LockAnalysis extends AbstractWholeIRAnalysis<LockVisitor,IRNode> {	
  /** Should we try to run things in parallel */
  private static boolean wantToRunInParallel = false;
  
  /**
   * Are we actually going to run things in parallel?  Not all JRE have the
   * libraries we need to actually run in parallel.
   */
  private static boolean willRunInParallel = wantToRunInParallel && !singleThreaded;
  
  /**
   * Use a work queue?  Only relevant if {@link #willRunInParallel} is 
   * <code>true</code>.  Otherwise it is <code>false</code>.
   */
	private static boolean queueWork = willRunInParallel && true;

  /**
   * Analyze compilation units in parallel?  Only relevant if {@link #willRunInParallel} is 
   * <code>true</code> and {@link #queueWork} is <code>true</code>.  Otherwise it is <code>false</code>.
   * When relevant, a <code>false</code> value means analyze by types, a
   * smaller granularity than compilation units.
   */
	private static boolean byCompUnit = queueWork && true; // otherwise by type
	
	
	
	private final AtomicReference<GlobalLockModel> lockModelHandle = 
		new AtomicReference<GlobalLockModel>(null);
	private BindingContextAnalysis bca;
	
	
	
	public LockAnalysis() {
		super(willRunInParallel, queueWork ? IRNode.class : null, "LockAssurance");
		if (runInParallel()) {
			setWorkProcedure(new Procedure<IRNode>() {
				public void op(IRNode n) {
					if (byCompUnit) {
						//System.out.println("Parallel Lock: "+JavaNames.genPrimaryTypeName(n));
						final TopLevelVisitor topLevel = 
							new TopLevelVisitor(getAnalysis(), getResultDependUponDrop());
						topLevel.doAccept(n);	
					} else {
						//System.out.println("Parallel Lock: "+JavaNames.getRelativeTypeName(n));
						getAnalysis().analyzeClass(n, getResultDependUponDrop());
					}
				}
			});
		}      
	}
	
	public void init(IIRAnalysisEnvironment env) {
		env.ensureClassIsLoaded(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_LOCK);
		env.ensureClassIsLoaded(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK);
	}
	
	@Override
	public void startAnalyzeBegin(IIRProject p, IBinder binder) {
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
	  bca = new BindingContextAnalysis(binder, true);
    return new LockVisitor(this, binder, new Effects(binder),
        new TypeBasedAliasAnalysis(binder), bca, lockModelHandle);
	}
	
	@Override
	protected void clearCaches() {
		if (!runInParallel()) {
			getAnalysis().clearCaches();
		} else {
			analyses.clearCaches();
		}
		if (bca != null) {
			bca.clear();
		}
	}
	
	@Override
	public boolean doAnalysisOnAFile(CUDrop cud, final IRNode compUnit, IAnalysisMonitor monitor) {
		if (byCompUnit) {
			boolean flushed = queueWork(compUnit);
			if (flushed) {
				JavaComponentFactory.clearCache();
			}
			return true;
		}
		// FIX factor out?
		final TopLevelVisitor topLevel = new TopLevelVisitor(getAnalysis(),
				getResultDependUponDrop());
		topLevel.doAccept(compUnit);	
		if (runInParallel()) {
			if (queueWork) {
				boolean flushed = queueWork(topLevel.getTypeBodies());
				if (flushed) {
					JavaComponentFactory.clearCache();
				}
			} else {
				runInParallel(IRNode.class, topLevel.getTypeBodies(), getWorkProcedure());
			}
		}
		return true;
	}
	
	@Override
	public void postAnalysis(IIRProject p) {
		finishBuild();
		
		super.postAnalysis(p);
		/* Have to do this afterwards, because postAnalysis can cause
		 * a LockVisitor to be created (which seems wrong) in the
		 * non parallel case.
		 */
    lockModelHandle.set(null);
	}
	
	private final class TopLevelVisitor extends VoidTreeWalkVisitor {
		private final LockVisitor lockVisitor;
		private final Drop resultsDependUpon;
		private final List<IRNode> types = new ArrayList<IRNode>();

		public TopLevelVisitor(final LockVisitor lv, final Drop rd) {
			lockVisitor = lv;
			resultsDependUpon = rd;
		}

		public Collection<IRNode> getTypeBodies() {
			return types;
		}
		
		private void analyzeClass(IRNode cbody) {
			if (runInParallel() && !byCompUnit) {
				types.add(cbody);
			} else {
				lockVisitor.analyzeClass(cbody,	resultsDependUpon);
			}
		}
		
		@Override
		public Void visitAnonClassExpression(final IRNode node) {
			analyzeClass(AnonClassExpression.getBody(node));
			doAcceptForChildren(node);
			return null;
		}

		@Override
		public Void visitClassDeclaration(final IRNode node) {
			analyzeClass(ClassDeclaration.getBody(node));
			doAcceptForChildren(node);
			return null;
		}

		@Override
		public Void visitEnumConstantClassDeclaration(final IRNode node) {
      analyzeClass(EnumConstantClassDeclaration.getBody(node));
      doAcceptForChildren(node);
      return null;
		}
		
		@Override
		public Void visitEnumDeclaration(final IRNode node) {
			analyzeClass(EnumDeclaration.getBody(node));
			doAcceptForChildren(node);
			return null;
		}

		@Override
		public Void visitInterfaceDeclaration(final IRNode node) {
			analyzeClass(InterfaceDeclaration.getBody(node));
			doAcceptForChildren(node);
			return null;
		}
	}
}
