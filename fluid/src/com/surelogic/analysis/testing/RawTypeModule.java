package com.surelogic.analysis.testing;

import com.surelogic.analysis.AbstractJavaAnalysisDriver;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IIRAnalysisEnvironment;
import com.surelogic.analysis.Unused;
import com.surelogic.analysis.nullable.RawTypeAnalysis;
import com.surelogic.analysis.nullable.RawTypeAnalysis.Query;
import com.surelogic.analysis.nullable.RawTypeLattice.Element;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.ConstructorCall;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.InfoDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;

public final class RawTypeModule extends AbstractWholeIRAnalysis<RawTypeAnalysis, Unused>{
  public RawTypeModule() {
    super("Raw Types");
  }

  @Override
  protected RawTypeAnalysis constructIRAnalysis(final IBinder binder) {
    return new RawTypeAnalysis(binder);
  }

  @Override
  protected boolean doAnalysisOnAFile(final IIRAnalysisEnvironment env,
      final CUDrop cud, final IRNode compUnit) {
    runInVersion(new edu.cmu.cs.fluid.util.AbstractRunner() {
      public void run() {
        checkRawTypesForFile(compUnit);
      }
    });
    return true;
  }

  protected void checkRawTypesForFile(final IRNode compUnit) {
    final RawTypeVisitor v = new RawTypeVisitor();
    v.doAccept(compUnit);
    getAnalysis().clear();
  }
  
  private final class RawTypeVisitor extends AbstractJavaAnalysisDriver<Query> {
    @Override
    protected Query createNewQuery(final IRNode decl) {
      return getAnalysis().getRawTypeQuery(decl);
    }

    @Override
    protected Query createSubQuery(final IRNode caller) {
      return currentQuery().getSubAnalysisQuery(caller);
    }

    
    
    @Override
    protected void enteringEnclosingType(final IRNode newType) {
      System.out.println(">>> Entering type " + JavaNames.getTypeName(newType));
    }
    
    @Override
    protected void leavingEnclosingType(final IRNode newType) {
      System.out.println("<<< Leaving type " + JavaNames.getTypeName(newType));
    }
    
    @Override
    protected void enteringEnclosingDeclPrefix(
        final IRNode newDecl, final IRNode anonClassDecl) {
      System.out.println("############################ Running raw types on " + JavaNames.genQualifiedMethodConstructorName(newDecl) + "############################");
    }
    
    
    
//    @Override
//    public Void visitVariableUseExpression(final IRNode use) {
//      // Ignore if we are the LHS of an assignment
//      final IRNode parent = JJNode.tree.getParent(use);
//      if (AssignExpression.prototype.includes(parent) &&
//          AssignExpression.getOp1(parent).equals(use)) {
//        return null;
//      }
//      
//      // See if the current variable is a primitive or not
//      final IJavaType type = getBinder().getJavaType(use);
//      if (type instanceof IJavaReferenceType) {
//         // See if the current variable is considered to be null or not
//        final Set<IRNode> nonNull = currentQuery().getResultFor(use);
//        final IRNode varDecl = getBinder().getBinding(use);
//        final InfoDrop drop = new InfoDrop(null);
//        setResultDependUponDrop(drop, use);
//        drop.setCategory(Messages.DSC_NON_NULL);
//        final String varName = VariableUseExpression.getId(use);
//        if (nonNull.contains(varDecl)) {
//          drop.setResultMessage(Messages.NOT_NULL, varName);
//        } else {
//          drop.setResultMessage(Messages.MAYBE_NULL, varName);
//        }
//      }
//      
//      return null;
//    }
    
    @Override
    public Void visitThisExpression(final IRNode expr) {
      // Ignore if ConstructorCall is the super expression
      final IRNode parent = JJNode.tree.getParent(expr);
      if (ConstructorCall.prototype.includes(parent) &&
          ConstructorCall.getObject(parent) == expr) {
        return null;
      }

      final Element rawness = currentQuery().getResultFor(expr);
      final InfoDrop drop = new InfoDrop(expr);
      drop.setCategory(Messages.DSC_NON_NULL);
      drop.setResultMessage(Messages.RAWNESS, rawness);
      return null;
    }
    
    @Override
    public void handleConstructorCall(final IRNode expr) {
      final Element rawness = currentQuery().getResultFor(expr);
      final InfoDrop drop = new InfoDrop(expr);
      drop.setCategory(Messages.DSC_NON_NULL);
      drop.setResultMessage(Messages.RAWNESS, rawness);
      
      super.handleConstructorCall(expr);
    }
  }

  @Override
  protected void clearCaches() {
    // Nothing to do
  }
}
