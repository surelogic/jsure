package com.surelogic.analysis.testing;

import java.util.Map;
import java.util.Map.Entry;

import com.surelogic.analysis.*;
import com.surelogic.analysis.nullable.DefinitelyAssignedFieldAnalysis;
import com.surelogic.analysis.nullable.DefinitelyAssignedFieldAnalysis.AllResultsQuery;
import com.surelogic.analysis.visitors.AbstractJavaAnalysisDriver;
import com.surelogic.dropsea.ir.HintDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;

public final class DefinitelyAssignedModule extends AbstractWholeIRAnalysis<DefinitelyAssignedFieldAnalysis, CUDrop>{
  public DefinitelyAssignedModule() {
    super("Definitely Assigned");
  }

  @Override
  protected DefinitelyAssignedFieldAnalysis constructIRAnalysis(final IBinder binder) {
    return new DefinitelyAssignedFieldAnalysis(binder, true);
  }

  @Override
  protected boolean doAnalysisOnAFile(final IIRAnalysisEnvironment env,
      final CUDrop cud, final IRNode compUnit) {
	  checkDefinitelyAssignedsForFile(compUnit);
    return true;
  }

  protected void checkDefinitelyAssignedsForFile(final IRNode compUnit) {
    final DefinitelyAssignedVisitor v = new DefinitelyAssignedVisitor();
    v.doAccept(compUnit);
    getAnalysis().clear();
  }
  
  private final class DefinitelyAssignedVisitor extends AbstractJavaAnalysisDriver<AllResultsQuery> {
    public DefinitelyAssignedVisitor() {
      super(true, false);
    }
    
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
      final Map<IRNode, Boolean> fieldStatus = 
          currentQuery().getResultFor(ConstructorDeclaration.getBody(cdecl));
      for (final Entry<IRNode, Boolean> e : fieldStatus.entrySet()) {
        if (!TypeUtil.isStatic(e.getKey())) {
          final HintDrop drop = HintDrop.newInformation(cdecl);
          drop.setCategorizingMessage(Messages.DSC_NON_NULL);
          if (e.getValue().booleanValue()) {
            drop.setMessage(Messages.ASSIGNED, VariableDeclarator.getId(e.getKey()));
          } else {
            drop.setMessage(Messages.NOT_ASSIGNED, VariableDeclarator.getId(e.getKey()));
          }
        }
      }
      
      doAcceptForChildren(cdecl);
    }


    
    @Override
    protected void handleClassInitDeclaration(final IRNode classBody, final IRNode node) {
      final Map<IRNode, Boolean> fieldStatus = currentQuery().getResultFor(classBody);
      for (final Entry<IRNode, Boolean> e : fieldStatus.entrySet()) {
        if (TypeUtil.isStatic(e.getKey())) {
          final HintDrop drop = HintDrop.newInformation(classBody);
          drop.setCategorizingMessage(Messages.DSC_NON_NULL);
          if (e.getValue().booleanValue()) {
            drop.setMessage(Messages.ASSIGNED, VariableDeclarator.getId(e.getKey()));
          } else {
            drop.setMessage(Messages.NOT_ASSIGNED, VariableDeclarator.getId(e.getKey()));
          }
        }
      }
    }
  }

  @Override
  protected void clearCaches() {
    // Nothing to do
  }
}
