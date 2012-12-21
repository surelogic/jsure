package com.surelogic.analysis.nullable;

import java.util.Map;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis;
import com.surelogic.analysis.nullable.DefinitelyAssignedAnalysis.AllResultsQuery;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;

public final class NullableModule extends AbstractWholeIRAnalysis<DefinitelyAssignedAnalysis, Unused>{
  private static final int DEFINITELY_ASSIGNED = 900;
  private static final int NOT_DEFINITELY_ASSIGNED = 901;
  
  
  
  public NullableModule() {
    super("Definitely Assigned");
  }

  @Override
  protected DefinitelyAssignedAnalysis constructIRAnalysis(final IBinder binder) {
    return new DefinitelyAssignedAnalysis(binder, true);
  }

  @Override
  protected boolean doAnalysisOnAFile(final IIRAnalysisEnvironment env,
      final CUDrop cud, final IRNode compUnit) {
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      @Override
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
  
  private final class DefinitelyAssignedVisitor extends AbstractJavaAnalysisDriver<AllResultsQuery> {
    @Override
    protected AllResultsQuery createNewQuery(final IRNode decl) {
      return getAnalysis().getAllResultsQuery(decl);
    }

    @Override
    protected AllResultsQuery createSubQuery(final IRNode caller) {
      return currentQuery().getSubAnalysisQuery(caller);
    }


    
    @Override
    protected void handleConstructorDeclaration(final IRNode cdecl) {
      final Map<IRNode, Boolean> fieldsStatus = 
          currentQuery().getResultFor(ConstructorDeclaration.getBody(cdecl));
      for (final Map.Entry<IRNode, Boolean> e : fieldsStatus.entrySet()) {
        final IRNode fieldDecl = e.getKey();
        final NonNullPromiseDrop pd = NonNullRules.getNonNull(fieldDecl);
        if (pd != null) {
          final boolean isDefinitelyAssigned = e.getValue().booleanValue();
          ResultsBuilder.createResult(cdecl, pd, isDefinitelyAssigned,
              DEFINITELY_ASSIGNED, NOT_DEFINITELY_ASSIGNED,
              JavaNames.genSimpleMethodConstructorName(cdecl));
        }
      }
      
      doAcceptForChildren(cdecl);
    }
  }

  @Override
  protected void clearCaches() {
    // Nothing to do
  }
}
