package com.surelogic.analysis.concurrency.threadsafe;

import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.analysis.annotationbounds.ParameterizedTypeAnalysis;
import com.surelogic.analysis.type.constraints.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.type.constraints.ImmutableAnnotationTester;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.VouchFieldIsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ImmutablePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;

public final class ImmutableProcessor extends TypeImplementationProcessor<ImmutablePromiseDrop> {
  private static final int IMMUTABLE_SUPERTYPE = 480;
  private static final int TRIVIALLY_IMMUTABLE = 481;
  private static final int VOUCHED_IMMUTABLE = 482;
  private static final int VOUCHED_IMMUTABLE_WITH_REASON = 483;
  private static final int FIELD_IS_IMMUTABLE = 484;
  private static final int FIELD_IS_NOT_IMMUTABLE = 485;
  private static final int FIELD_IS_FINAL = 486;
  private static final int FIELD_IS_NOT_FINAL = 487;
  private static final int OBJECT_IS_IMMUTABLE = 488;
  private static final int OBJECT_IS_NOT_IMMUTABLE = 489;
  private static final int TYPE_IS_PRIMITIVE = 490;
  private static final int TYPE_IS_NOT_PRIMITIVE = 491;
  private static final int TYPE_IS_IMMUTABLE = 492;
  private static final int TYPE_IS_NOT_IMMUTABLE = 493;
  private static final int IMMUTABLE_IMPL = 494;

  
  
  private boolean hasFields = false;
  
  public ImmutableProcessor(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
      final ImmutablePromiseDrop iDrop,
      final IRNode typeDecl, final IRNode typeBody) {
    super(a, iDrop, typeDecl, typeBody);
  }

  @Override
  protected void processSuperType(final IRNode name, final IRNode tdecl) {
    final ImmutablePromiseDrop pDrop =
        LockRules.getImmutableImplementation(tdecl);
    if (pDrop != null) {
      final ResultDrop result = createRootResult(
          true, name, IMMUTABLE_SUPERTYPE,
          JavaNames.getQualifiedTypeName(tdecl));
      result.addTrusted(pDrop);
    }
  }

  @Override
  protected void postProcess() {
    // We are only called on classes annotated with @Immutable
    if (!hasFields) {
      createRootResult(true, typeBody, TRIVIALLY_IMMUTABLE);
    }
  }

  @Override
  protected void processVariableDeclarator(final IRNode fieldDecl,
      final IRNode varDecl, final boolean isStatic) {
    final String id = VariableDeclarator.getId(varDecl);

    // We have a field
    hasFields = true;

    /*
     * (1) Field must be final. (2) non-primitive fields must be
     * @Immutable or Vouched for @Vouch("Immutable")
     */
    final VouchFieldIsPromiseDrop vouchDrop =
        LockRules.getVouchFieldIs(varDecl);
    if (vouchDrop != null && vouchDrop.isImmutable()) {
      // VOUCHED
      final String reason = vouchDrop.getReason();
      final ResultDrop result; 
      if (reason == VouchFieldIsNode.NO_REASON) {
        result = createRootResult(true, varDecl, VOUCHED_IMMUTABLE, id);
      } else {
        result = createRootResult(
            true, varDecl, VOUCHED_IMMUTABLE_WITH_REASON, id, reason);
      }
      result.addTrusted(vouchDrop);
    } else {
      /* Now we use a result folder because we are conjoining two results:
       * (1) the field is final
       * (2) the field's type is immutable or primitive
       */
      final ResultFolderDrop folder = createRootAndFolder(
          varDecl, FIELD_IS_IMMUTABLE, FIELD_IS_NOT_IMMUTABLE, id);
      
      // (1) Check finality of the field
      final boolean isFinal = TypeUtil.isFinal(varDecl);
      final ResultDrop fDrop = createResult(folder, fieldDecl,
          isFinal, FIELD_IS_FINAL, FIELD_IS_NOT_FINAL);
      if (isFinal) {
        // Get the @Vouch("final") annotation if there is one
        final VouchFieldIsPromiseDrop vouchFinal = LockRules.getVouchFieldIs(varDecl);
        if (vouchFinal != null && vouchFinal.isFinal()) {
          fDrop.addTrusted(vouchFinal);
        }
      } else {
        fDrop.addProposal(new ProposedPromiseDrop("Vouch",
            "final", varDecl, varDecl, Origin.MODEL));
      }

      /* (2) Check the immutability fo the field's type.  Four cases:
       *   1. The type is primitive (GOOD)
       *   2. The type is @Immutable (GOOD)
       *   3. The field is final and the field initializer is of a type
       *      whose implementation is @Immutable (GOOD)
       *   4. All other cases (BAD) 
       */
      final ResultFolderDrop typeFolder = createOrFolder(
          folder, varDecl, OBJECT_IS_IMMUTABLE, OBJECT_IS_NOT_IMMUTABLE);
      final IJavaType type = binder.getJavaType(varDecl);
      final IRNode typeDeclNode = FieldDeclaration.getType(fieldDecl);
      
      // primitive
      final boolean isPrimitive = type instanceof IJavaPrimitiveType;
      createResult(typeFolder, typeDeclNode, isPrimitive,
          TYPE_IS_PRIMITIVE, TYPE_IS_NOT_PRIMITIVE, type.toSourceText());
      
      // immutable
      final ImmutableAnnotationTester tester = new ImmutableAnnotationTester(
              binder, AnnotationBoundsTypeFormalEnv.INSTANCE,
              ParameterizedTypeAnalysis.getFolders(), true, false); 
      final boolean isImmutable = tester.testType(type);
      final ResultDrop iResult = createResult(
          typeFolder, typeDeclNode, isImmutable,
          TYPE_IS_IMMUTABLE, TYPE_IS_NOT_IMMUTABLE, type.toSourceText());  
      iResult.addTrusted(tester.getTrusts());
      
      boolean proposeImmutable = !isImmutable;
      if (isFinal && !isImmutable) {
        /*
         * If the type is not immutable, we can check to see
         * if the implementation assigned to the field is immutable,
         * but only if the field is final.
         */
        final IRNode init = VariableDeclarator.getInit(varDecl);
        if (Initialization.prototype.includes(init)) {
          final IRNode initExpr = Initialization.getValue(init);
          if (NewExpression.prototype.includes(initExpr)) {
            final ImmutableAnnotationTester tester2 =
                new ImmutableAnnotationTester(
                    binder, AnnotationBoundsTypeFormalEnv.INSTANCE,
                    ParameterizedTypeAnalysis.getFolders(), true, true); 
            if (tester2.testType(binder.getJavaType(initExpr))) {
              // we have an instance of an immutable implementation
              proposeImmutable = false;
              final ResultDrop result = createResult(
                  true, typeFolder, initExpr, IMMUTABLE_IMPL);
              result.addTrusted(tester2.getTrusts());
            }
          }
        }
      }
      
      if (proposeImmutable) {
        for (final IRNode typeDecl : tester.getTested()) {
          iResult.addProposal(new ProposedPromiseDrop(
              "Immutable", null, typeDecl, varDecl,
              Origin.MODEL));
        }
      }        

      if (!isPrimitive) {
        folder.addProposalNotProvedConsistent(new ProposedPromiseDrop("Vouch",
            "Immutable", varDecl, varDecl, Origin.MODEL));
      }
    }
  }
}
