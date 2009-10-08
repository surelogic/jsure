/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/locks/LockAnalysis.java,v 1.5 2008/09/08 19:12:16 chance Exp $*/
package com.surelogic.analysis.locks;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.EffectsVisitor;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.analysis.TypeBasedAliasAnalysis;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;

public class LockAnalysis extends AbstractWholeIRAnalysis<LockVisitor> {	
	private final AtomicReference<GlobalLockModel> lockModelHandle = 
		new AtomicReference<GlobalLockModel>(null);
	
	public LockAnalysis() {
		super("LockAssurance");
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
		try {
			final BindingContextAnalysis bca = new BindingContextAnalysis(binder);
			return new LockVisitor(binder, 
					                      new EffectsVisitor(binder, bca), 
					                      new TypeBasedAliasAnalysis(binder),
					                      bca, lockModelHandle);
		} catch (SlotAlreadyRegisteredException e) {
			LOG.log(Level.SEVERE, "Couldn't create lock visitor!", e);
			return null;
		}
	}
	
	@Override
	public void doAnalysisOnAFile(CUDrop cud, final IRNode compUnit) {
		// FIX factor out?
		final Visitor<Void> topLevel = new TopLevelVisitor(getAnalysis(),
				                                           getResultDependUponDrop());
		topLevel.doAccept(compUnit);	
	}

	@Override
	public void postAnalysis(IIRProject p) {
		lockModelHandle.set(null);
		// FIX only clearing some of the threads?
		if (getAnalysis() != null) {
			getAnalysis().clearCaches();
		}
	}
	
	private static final class TopLevelVisitor extends VoidTreeWalkVisitor {
		private final LockVisitor lockVisitor;
		private final Drop resultsDependUpon;

		public TopLevelVisitor(final LockVisitor lv, final Drop rd) {
			lockVisitor = lv;
			resultsDependUpon = rd;
		}

		@Override
		public Void visitAnonClassExpression(final IRNode node) {
			lockVisitor.analyzeClass(AnonClassExpression.getBody(node),
					resultsDependUpon);
			doAcceptForChildren(node);
			return null;
		}

		@Override
		public Void visitClassDeclaration(final IRNode node) {
			lockVisitor.analyzeClass(ClassDeclaration.getBody(node),
					resultsDependUpon);
			doAcceptForChildren(node);
			return null;
		}

		@Override
		public Void visitEnumDeclaration(final IRNode node) {
			lockVisitor.analyzeClass(EnumDeclaration.getBody(node),
					resultsDependUpon);
			doAcceptForChildren(node);
			return null;
		}

		@Override
		public Void visitInterfaceDeclaration(final IRNode node) {
			lockVisitor.analyzeClass(InterfaceDeclaration.getBody(node),
					resultsDependUpon);
			doAcceptForChildren(node);
			return null;
		}
	}
}
