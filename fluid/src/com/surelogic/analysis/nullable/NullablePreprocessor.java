package com.surelogic.analysis.nullable;

import com.surelogic.NonNull;
import com.surelogic.aast.promise.NonNullNode;
import com.surelogic.analysis.visitors.JavaSemanticsVisitor;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.operator.AllocationExpression;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;

final class NullablePreprocessor extends JavaSemanticsVisitor {
  public NullablePreprocessor() {
    super(true, true);
  }

  
  
  @Override
  protected void handleFieldInitialization(final IRNode varDecl, final boolean isStatic) {
    doAcceptForChildren(varDecl);
    
    /*
     * If the field is UNANNOTATED, final, and initialized to a new object then
     * we add a virtual @NonNull annotation. This corresponds to the actions in
     * NonNullRawTypeAnalysis.Transfer.transferUseField(). Don't add
     * 
     * @NonNull proposal here because that will be added later in
     * NullableModule2.postAnalysis().
     */
    if (NonNullRules.getNonNull(varDecl) == null &&
        NonNullRules.getNullable(varDecl) == null) {
      final IRNode init = VariableDeclarator.getInit(varDecl);
      if (TypeUtil.isJSureFinal(varDecl) &&
          Initialization.prototype.includes(init) &&
          AllocationExpression.prototype.includes(Initialization.getValue(init))) {
        final NonNullNode nnn = new NonNullNode(0);
        nnn.setPromisedFor(varDecl, null);
        final NonNullPromiseDrop pd = new NonNullPromiseDrop(nnn);
        AnnotationRules.attachAsVirtual(NonNullRules.getNonNullStorage(), pd);
        
        // immediately propose that a read @NonNull be placed here
        pd.addProposal(new Builder(NonNull.class, varDecl, varDecl).build());
      }
    }
  }
}
