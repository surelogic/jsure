package com.surelogic.analysis.concurrency.driver;

import java.util.Set;

import com.surelogic.analysis.AbstractAnalysisSharingAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.driver.LockModelBuilder;
import com.surelogic.analysis.effects.Effect;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.visitors.JavaSemanticsVisitor;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;

public final class NewLockAnalysis
extends AbstractAnalysisSharingAnalysis<BindingContextAnalysis, Effects, CUDrop> {
	public NewLockAnalysis() {
		super(false, "New Lock Analysis", BindingContextAnalysis.factory);
	}

	@Override
	protected Effects constructIRAnalysis(IBinder binder) {
    return new Effects(binder, LockModelBuilder.getLockModel());
	}

	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
		runOverFile(compUnit);
		return true;
	}

	protected void runOverFile(final IRNode compUnit) {
	  final LV_Visitor v = new LV_Visitor();
	  v.doAccept(compUnit);
	}	
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
	
	private final class LV_Visitor extends JavaSemanticsVisitor {
		public LV_Visitor() {
			super(true, true);
		}
		
		private void reportEffects(final IRNode mdecl) {
		  final Set<Effect> effects = 
		      getAnalysis().getImplementationEffects(mdecl, getSharedAnalysis());
		  for (final Effect e : effects) {
		    final HintDrop drop = HintDrop.newInformation(e.getSource());
//		    drop.setCategorizingMessage(Messages.DSC_EFFECTS);
//		    drop.setMessage(Messages.EFFECT, e.toString());
		  }
		}

		@Override
		protected void handleConstructorDeclaration(final IRNode cdecl) {
		  reportEffects(cdecl);
			super.handleConstructorDeclaration(cdecl);
		}

		@Override
		protected void handleMethodDeclaration(final IRNode mdecl) {
		  reportEffects(mdecl);
			super.handleMethodDeclaration(mdecl);
		}

		@Override
		protected void handleNonAnnotationTypeDeclaration(final IRNode tdecl) {
			final IRNode clinit = JavaPromise.getClassInitOrNull(tdecl);
			if (clinit != null) {
			  reportEffects(clinit);
			}
			super.handleNonAnnotationTypeDeclaration(tdecl);
		}
	}
}
