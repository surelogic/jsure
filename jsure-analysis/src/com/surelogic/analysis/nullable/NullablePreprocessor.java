package com.surelogic.analysis.nullable;

import com.surelogic.NonNull;
import com.surelogic.TrackPartiallyInitialized;
import com.surelogic.aast.promise.NonNullNode;
import com.surelogic.aast.promise.TrackPartiallyInitializedNode;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.visitors.JavaSemanticsVisitor;
import com.surelogic.annotation.rules.AnnotationRules;
import com.surelogic.annotation.rules.NonNullRules;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.nullable.NonNullPromiseDrop;
import com.surelogic.dropsea.ir.drops.nullable.TrackPartiallyInitializedPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AllocationExpression;
import edu.cmu.cs.fluid.java.operator.AnonClassExpression;
import edu.cmu.cs.fluid.java.operator.ClassDeclaration;
import edu.cmu.cs.fluid.java.operator.EnumConstantClassDeclaration;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.StringConcat;
import edu.cmu.cs.fluid.java.operator.StringLiteral;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;

final class NullablePreprocessor extends JavaSemanticsVisitor implements IBinderClient {
  private static final String JAVA_LANG_ENUM = "java.lang.Enum";
  
  private static final int TPI_TRACKED = 985;
  private static final int TPI_NOT_TRACKED = 986;
  private static final int SUPER_IS_TPI = 987;
  private static final int SUPER_NOT_TPI = 988;
  
  
  
  private final IBinder binder;
  private final IRNode javaLangEnum;
  
  
  
  public NullablePreprocessor(final IBinder b) {
    super(VisitInsideTypes.YES, SkipAnnotations.YES);
    binder = b;
    javaLangEnum = b.getTypeEnvironment().findNamedType(JAVA_LANG_ENUM);
  }

  
  
  @Override
  public IBinder getBinder() {
    return binder;
  }

  @Override
  public void clearCaches() {
    // nothing to do yet
  }
  
  
  
  @Override
  protected void handleClassDeclaration(final IRNode classDecl) {
    /* If the class has a @TrackPartiallyInitalized annotation, then the
     * super class needs to have one too.
     */
    final TrackPartiallyInitializedPromiseDrop tpi = 
        NonNullRules.getTrackPartiallyInitialized(classDecl);
    if (tpi != null) {
      if (tpi.verifyParent()) {
        final IRNode superClass = binder.getBinding(ClassDeclaration.getExtension(classDecl));
        assureTrackPartiallyInitialized(tpi, classDecl, superClass);
      }
    }
    
    super.handleClassDeclaration(classDecl);
  }
  
  @Override
  protected void handleEnumDeclaration(final IRNode enumDecl) {
    /* If the enum has a @TrackPartiallyInitalized annotation, then the
     * super class needs to have one too.
     */
    final TrackPartiallyInitializedPromiseDrop tpi = 
        NonNullRules.getTrackPartiallyInitialized(enumDecl);
    if (tpi != null) {
      if (tpi.verifyParent()) {
        assureTrackPartiallyInitialized(tpi, enumDecl, javaLangEnum);
      }
    }
    
    super.handleEnumDeclaration(enumDecl);
  }

  
  
  @Override
  protected void handleFieldInitialization(
      final IRNode varDecl, final boolean isStatic) {
    doAcceptForChildren(varDecl);
    
    /* This section is the original reason for the NullablePreprocessor.  We
     * add some virtual annotations that we need for the type checking pass.
     */
    
    /*
     * If the field is UNANNOTATED, final, and initialized to a new object or a String literal then
     * we add a virtual @NonNull annotation. This corresponds to the actions in
     * NonNullRawTypeAnalysis.Transfer.transferUseField(). Don't add
     * 
     * @NonNull proposal here because that will be added later in
     * NullableModule2.postAnalysis().
     */
    final NonNullPromiseDrop nonNull = NonNullRules.getNonNull(varDecl);
    if (nonNull == null &&
        NonNullRules.getNullable(varDecl) == null) {
      final IRNode init = VariableDeclarator.getInit(varDecl);
      if (TypeUtil.isJSureFinal(varDecl) &&
          Initialization.prototype.includes(init) &&
          (AllocationExpression.prototype.includes(Initialization.getValue(init)) ||
              StringLiteral.prototype.includes(Initialization.getValue(init)) ||
              StringConcat.prototype.includes(Initialization.getValue(init)))) {
        final NonNullNode nnn = new NonNullNode(0);
        nnn.setPromisedFor(varDecl, null);
        final NonNullPromiseDrop pd = new NonNullPromiseDrop(nnn);
        AnnotationRules.attachAsVirtual(NonNullRules.getNonNullStorage(), pd);
        
        // immediately propose that a real @NonNull be placed here
        NullableUtils.createCodeProposal(
            new Builder(NonNull.class, varDecl, varDecl));
      }
    }
    
    // ==== Set up the proof tree ====
    
    if (!isStatic && nonNull != null) {
      final IRNode enclosingType = getEnclosingType();
      final Operator op = JJNode.tree.getOperator(enclosingType);
      if (AnonClassExpression.prototype.includes(op) ||
          EnumConstantClassDeclaration.prototype.includes(op)) {
        // Put a virtual @TPI on the class
        final TrackPartiallyInitializedNode tpin = new TrackPartiallyInitializedNode(0, true);
        tpin.setPromisedFor(enclosingType, null);
        final TrackPartiallyInitializedPromiseDrop tpipd = new TrackPartiallyInitializedPromiseDrop(tpin);
        AnnotationRules.attachAsVirtual(NonNullRules.getTrackPartiallyInitalizedStorage(), tpipd);
        final IRNode superClass = 
            AnonClassExpression.prototype.includes(op) ?
                binder.getBinding(AnonClassExpression.getType(enclosingType)) :
                  JJNode.tree.getParent(JJNode.tree.getParent(enclosingType));
        assureTrackPartiallyInitialized(
            tpipd, enclosingType, superClass);
      }
      
      final TrackPartiallyInitializedPromiseDrop tpi =
          NonNullRules.getTrackPartiallyInitialized(enclosingType);
      final ResultDrop result = ResultsBuilder.createResult(
          varDecl, nonNull, tpi != null, TPI_TRACKED, TPI_NOT_TRACKED, JavaNames.genSimpleMethodConstructorName(getEnclosingDecl()));
      if (tpi != null) {
        result.addTrusted(tpi);
      } else {
        result.addProposal(
            new Builder(TrackPartiallyInitialized.class, enclosingType, varDecl).build());
      }
    }
  }



  private void assureTrackPartiallyInitialized(
      final TrackPartiallyInitializedPromiseDrop tpi,
      final IRNode classDecl, final IRNode superClass) {
    final TrackPartiallyInitializedPromiseDrop superTPI = NonNullRules.getTrackPartiallyInitialized(superClass);
    
    final ResultsBuilder builder = new ResultsBuilder(tpi);
    final ResultDrop tpiResult = builder.createRootResult(
        superClass, superTPI != null, SUPER_IS_TPI, SUPER_NOT_TPI);
    if (superTPI != null) {
      tpiResult.addTrusted(superTPI);
    } else {
      tpiResult.addProposal(
          new Builder(TrackPartiallyInitialized.class, superClass, classDecl).build());
    }
  }
}
