package com.surelogic.analysis.concurrency.driver;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.concurrency.model.AnalysisLockModel;
import com.surelogic.analysis.concurrency.model.declared.StateLock;
import com.surelogic.analysis.concurrency.threadsafe.ContainableProcessor;
import com.surelogic.analysis.concurrency.threadsafe.ImmutableProcessor;
import com.surelogic.analysis.concurrency.threadsafe.NewThreadSafeProcessor;
import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.visitors.TopLevelAnalysisVisitor;
import com.surelogic.analysis.visitors.TopLevelAnalysisVisitor.TypeBodyPair;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.drops.type.constraints.ContainablePromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ImmutablePromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ThreadSafePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;

public class ThreadSafeAnalysis extends AbstractWholeIRAnalysis<IBinderClient, TypeBodyPair> {
	public ThreadSafeAnalysis() {
		super("ThreadSafeAssurance");
	}
	
	private final void actuallyAnalyzeClassBody(
	    final IRNode typeDecl, final IRNode typeBody) {
	  final ThreadSafePromiseDrop threadSafeDrop =
			  LockRules.getThreadSafeImplementation(typeDecl);
	  // If null, assume it's not meant to be thread safe
	  // Also check for verify=false
	  if (threadSafeDrop != null && threadSafeDrop.verify()) {
	    final AtomicReference<AnalysisLockModel> m = LockModelBuilder.getLockModel();
	    final Set<StateLock<?, ?>> stateLocks =
	        m.get().getAllStateLocksIn(
	            JavaTypeFactory.getMyThisType(typeDecl));
	    
		  new NewThreadSafeProcessor(getBinder(), threadSafeDrop, typeDecl, typeBody,
		      stateLocks).processType();
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
		  new ImmutableProcessor(getBinder(), immutableDrop, typeDecl, typeBody).processType();
	  }	  
	}

	@Override
	protected IBinderClient constructIRAnalysis(final IBinder binder) {
	  return null;
	}

	@Override
	protected void clearCaches() {
	  // do nothing
	}

	@Override
	public IAnalysisGranulator<TypeBodyPair> getGranulator() {
		return TopLevelAnalysisVisitor.granulator;
	}
	
	@Override
	protected boolean doAnalysisOnGranule_wrapped(
	    final IIRAnalysisEnvironment env, final TypeBodyPair n) {
	  actuallyAnalyzeClassBody(n.getType(), n.getClassBody());
		return true; 
	}
}
