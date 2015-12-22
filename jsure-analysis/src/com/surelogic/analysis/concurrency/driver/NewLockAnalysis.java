package com.surelogic.analysis.concurrency.driver;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.ImmutableList;
import com.surelogic.analysis.AbstractAnalysisSharingAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.concurrency.driver.LockModelBuilder;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.analysis.visitors.FlowUnitVisitor;
import com.surelogic.analysis.visitors.SuperVisitor;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.locks.LockModel;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.bind.IBinder;

public final class NewLockAnalysis
extends AbstractAnalysisSharingAnalysis<BindingContextAnalysis, NewLockVisitor, CUDrop> {
	public NewLockAnalysis() {
		super(false, "New Lock Analysis", BindingContextAnalysis.factory);
	}

	@Override
	protected NewLockVisitor constructIRAnalysis(final IBinder binder) {
	  final AtomicReference<AnalysisLockModel> lockModel =
	      LockModelBuilder.getLockModel();
	  
    // Make sure the MUTEX lock shows up in the viewer
    /* XXX: NullPointerException if the lock is not found. This is okay because
     * it is catastrophic if MUTEX is not declared
     */
	  final LockModel mutex = lockModel.get().getJavaLangObjectMutex();
    mutex.setFromSrc(true);
	  
    return new NewLockVisitor(binder, getSharedAnalysis(), lockModel);
	}

	@Override
	protected boolean doAnalysisOnAFile(
	    final IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
    final Driver driver = new Driver();
    driver.doAccept(compUnit);
//    getAnalysis().clear();
    return true;
	}
	
	@Override
	protected void clearCaches() {
		// Nothing to do
	}
  
  
  
  private final class Driver extends SuperVisitor {
    public Driver() {
      super(true);
    }
    
    @Override
    protected List<FlowUnitVisitor<?>> createSubVisitors() {
      return ImmutableList.<FlowUnitVisitor<?>>of(getAnalysis());
    }


    
    private JavaComponentFactory jcf = null;
    
    @Override
    protected void enteringEnclosingDecl(
        final IRNode newDecl, final IRNode anonClassDecl) {
      jcf = JavaComponentFactory.startUse();
    }
    
    @Override
    protected final void leavingEnclosingDecl(
        final IRNode oldDecl, final IRNode returningTo) {
      JavaComponentFactory.finishUse(jcf);
      jcf = null;
    }
  }
}
