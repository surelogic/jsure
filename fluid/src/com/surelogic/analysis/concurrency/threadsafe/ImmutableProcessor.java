package com.surelogic.analysis.concurrency.threadsafe;

import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.type.constraints.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.type.constraints.ImmutableAnnotationTester;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.drops.promises.VouchFieldIsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ImmutablePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;

public final class ImmutableProcessor extends TypeImplementationProcessor<ImmutablePromiseDrop> {
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
      final ResultDrop result = createResult(name, true,
          Messages.IMMUTABLE_SUPERTYPE,
          JavaNames.getQualifiedTypeName(tdecl));
      result.addTrustedPromise(pDrop);
    }
  }

  @Override
  protected void postProcess() {
    // We are only called on classes annotated with @Immutable
    if (!hasFields) {
      createResult(typeBody, true, Messages.TRIVIALLY_IMMUTABLE);
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
        result = createResult(varDecl, true, Messages.IMMUTABLE_VOUCHED, id);
      } else {
        result = createResult(
            varDecl, true, Messages.IMMUTABLE_VOUCHED_WITH_REASON, id, reason);
      }
      result.addTrustedPromise(vouchDrop);
    } else {
      /* Now we use a result folder because we are conjoining two results:
       * (1) the field is final
       * (2) the field's type is immutable or primitive
       */
      final ResultFolderDrop folder = createResultFolder(varDecl);
      boolean isGood;
      
      // (1) Check finality of the field
      final boolean isFinal = TypeUtil.isFinal(varDecl);
      if (isFinal) {
        final ResultDrop fDrop = createResultInFolder(
            folder, fieldDecl, true, Messages.IMMUTABLE_FINAL);
        isGood = true;
        
        // Get the @Vouch("final") annotation if there is one
        final VouchFieldIsPromiseDrop vouchFinal = LockRules.getVouchFieldIs(varDecl);
        if (vouchFinal != null && vouchFinal.isFinal()) {
          fDrop.addTrustedPromise(vouchFinal);
        }
      } else {
        createResultInFolder(
            folder, fieldDecl, false, Messages.IMMUTABLE_NOT_FINAL);
        isGood = false;
        folder.addProposal(new ProposedPromiseDrop("Vouch",
            "final", varDecl, varDecl, Origin.MODEL));
      }

      /* (2) Check the immutability fo the field's type.  Four cases:
       *   1. The type is primitive (GOOD)
       *   2. The type is @Immutable (GOOD)
       *   3. The field is final and the field initializer is of a type
       *      whose implementation is @Immutable (GOOD)
       *   4. All other cases (BAD) 
       */
      final IJavaType type = binder.getJavaType(varDecl);
      if (type instanceof IJavaPrimitiveType) { // PRIMITIVELY TYPED
        createResultInFolder(folder, FieldDeclaration.getType(fieldDecl), true,
            Messages.IMMUTABLE_PRIMITIVE, type.toSourceText());
        isGood &= true;
      } else { // REFERENCE-TYPED
        final ImmutableAnnotationTester tester = 
            new ImmutableAnnotationTester(
                binder, AnnotationBoundsTypeFormalEnv.INSTANCE, true); 
        final boolean isImmutable = tester.testType(type);
        
        if (isImmutable) { // IMMUTABLE REFERENCE TYPE
          final ResultDrop iResult = createResultInFolder(
              folder, FieldDeclaration.getType(fieldDecl), true,
              Messages.FIELD_TYPE_IMMUTABLE, type.toSourceText());
          iResult.addTrustedPromises(tester.getPromises());
          isGood &= true;
        } else {
          /*
           * If the type is not immutable, we can check to see
           * if the implementation assigned to the field is immutable,
           * but only if the field is final.
           */
          boolean stillBad = true;
          if (isFinal) {
            final IRNode init = VariableDeclarator.getInit(varDecl);
            if (Initialization.prototype.includes(init)) {
              final IRNode initExpr = Initialization.getValue(init);
              if (NewExpression.prototype.includes(initExpr)) {
                final ImmutablePromiseDrop implTypeIDrop =
                    LockRules.getImmutableImplementation(
                        ((IJavaDeclaredType) binder.getJavaType(initExpr)).getDeclaration());
                if (implTypeIDrop != null) {
                  // we have an instance of an immutable implementation
                  stillBad = false;
                  final ResultDrop iResult = createResultInFolder(
                      folder, initExpr, true, Messages.IMMUTABLE_IMPL); 
                  iResult.addTrustedPromise(implTypeIDrop);
                }
              }
            }
          }
          
          if (!stillBad) {
            isGood &= true;
          } else {
            final ResultDrop iResult = createResultInFolder(
                folder, FieldDeclaration.getType(fieldDecl), false,
                Messages.FIELD_TYPE_NOT_IMMUTABLE, type.toSourceText());
            for (final IRNode typeDecl : tester.getTested()) {
              iResult.addProposal(new ProposedPromiseDrop(
                  "Immutable", null, typeDecl, varDecl,
                  Origin.MODEL));
            }
            folder.addProposal(new ProposedPromiseDrop("Vouch",
                "Immutable", varDecl, varDecl, Origin.MODEL));
          }
        }
      }

      if (isGood) {
        folder.setMessage(Messages.FOLDER_IS_IMMUTABLE, id);
      } else {
        folder.setMessage(Messages.FOLDER_IS_NOT_IMMUTABLE, id);
      }
    }
  }
}
