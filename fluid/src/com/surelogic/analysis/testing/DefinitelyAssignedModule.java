package com.surelogic.analysis.testing;

import java.util.Set;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.nullable.Assigned;
import com.surelogic.analysis.nullable.AssignedVars;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis.Query;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public final class DefinitelyAssignedModule extends AbstractWholeIRAnalysis<DefinitelyAssignedAnalysis, Unused>{
  public DefinitelyAssignedModule() {
    super("Definitely Assigned");
  }

  @Override
  protected DefinitelyAssignedAnalysis constructIRAnalysis(final IBinder binder) {
    return new DefinitelyAssignedAnalysis(binder);
  }

  @Override
  protected boolean doAnalysisOnAFile(final IIRAnalysisEnvironment env,
      final CUDrop cud, final IRNode compUnit) {
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      public void run() {
        checkDefinitelyAssignedsForFile(compUnit);
      }
    });
    return true;
  }

  protected void checkDefinitelyAssignedsForFile(final IRNode compUnit) {
    final DefinitelyAssignedVisitor v = new DefinitelyAssignedVisitor();
    v.doAccept(compUnit);
    getAnalysis().clear();
  }
  
  private final class DefinitelyAssignedVisitor extends AbstractJavaAnalysisDriver<Query> {
    @Override
    protected Query createNewQuery(final IRNode decl) {
      return getAnalysis().getAssignedVarsQuery(decl);
    }

    @Override
    protected Query createSubQuery(final IRNode caller) {
      return currentQuery().getSubAnalysisQuery(caller);
    }


    
    @Override
    protected void handleConstructorDeclaration(final IRNode cdecl) {
      final Set<IRNode> notAssigned =
          currentQuery().getResultFor(ConstructorDeclaration.getBody(cdecl));
      
      for (final IRNode vd : notAssigned) {
        final InfoDrop drop = new InfoDrop(null);
        setResultDependUponDrop(drop, cdecl);
        drop.setCategory(Messages.DSC_NON_NULL);
        drop.setResultMessage(Messages.NOT_ASSIGNED, VariableDeclarator.getId(vd));
      }
      
      doAcceptForChildren(cdecl);
    }
  }

  @Override
  protected void clearCaches() {
    // Nothing to do
  }
}
