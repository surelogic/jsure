package com.surelogic.analysis.concurrency.threadsafe;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.concurrency.util.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.concurrency.util.ImmutableAnnotationTester;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;
import edu.cmu.cs.fluid.sea.drops.promises.ImmutablePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.VouchFieldIsPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ProposedPromiseBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;

public final class ImmutableProcessor extends TypeImplementationProcessor<ImmutablePromiseDrop> {
  private boolean hasFields = false;
  
  public ImmutableProcessor(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
      final ImmutablePromiseDrop iDrop,
      final IRNode typeDecl, final IRNode typeBody) {
    super(a, iDrop, typeDecl, typeBody);
  }

  @Override
  protected String message2string(final int msg) {
    return Messages.toString(msg);
  }

  @Override
  protected void processSuperType(final IRNode tdecl) {
    final ImmutablePromiseDrop pDrop =
        LockRules.getImmutableImplementation(tdecl);
    if (pDrop != null) {
      final ResultDropBuilder result = createResult(tdecl, true,
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
    // We have a field
    hasFields = true;

    /*
     * (1) Field must be final. (2) non-primitive fields must be
     * @Immutable or Vouched for @Vouch("Immutable")
     */
    final String id = VariableDeclarator.getId(varDecl);

    final VouchFieldIsPromiseDrop vouchDrop =
        LockRules.getVouchFieldIs(varDecl);
    if (vouchDrop != null && vouchDrop.isImmutable()) {
      // VOUCHED
      final String reason = vouchDrop.getReason();
      final ResultDropBuilder result = reason == VouchFieldIsNode.NO_REASON ? createResult(
          varDecl, true, Messages.IMMUTABLE_VOUCHED, id)
          : createResult(varDecl, true,
              Messages.IMMUTABLE_VOUCHED_WITH_REASON, id,
              reason);
      result.addTrustedPromise(vouchDrop);
    } else {
      final boolean isFinal = TypeUtil.isFinal(varDecl);
      final IJavaType type = binder.getJavaType(varDecl);
      ResultDropBuilder result = null;
      boolean proposeVouch = false;

      if (type instanceof IJavaPrimitiveType) {
        // PRIMITIVELY TYPED
        if (isFinal) {
          result = createResult(varDecl, true,
              Messages.IMMUTABLE_FINAL_PRIMITIVE, id);
        } else {
          result = createResult(varDecl, false,
              Messages.IMMUTABLE_NOT_FINAL, id);
          // Cannot use vouch on primitive types
        }
      } else {
        // REFERENCE-TYPED
        final ImmutableAnnotationTester tester = 
            new ImmutableAnnotationTester(
                binder, AnnotationBoundsTypeFormalEnv.INSTANCE); 
        final boolean isImmutable = tester.testType(type);
        
        if (isImmutable) {
          // IMMUTABLE REFERENCE TYPE
          if (isFinal) {
            result = createResult(varDecl, true,
                Messages.IMMUTABLE_FINAL_IMMUTABLE, id);
            for (final PromiseDrop<? extends IAASTRootNode> p : tester.getPromises()) {
              result.addTrustedPromise(p);
            }
          } else {
            result = createResult(varDecl, false,
                Messages.IMMUTABLE_NOT_FINAL, id);
            for (final PromiseDrop<? extends IAASTRootNode> p : tester.getPromises()) {
              result.addTrustedPromise(p);
            }
            proposeVouch = true;
          }
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
                  result = createResult(varDecl, true,
                      Messages.IMMUTABLE_FINAL_IMMUTABLE, id);
                  result.addTrustedPromise(implTypeIDrop);
                  result.addSupportingInformation(
                      varDecl, Messages.IMMUTABLE_IMPL);
                }
              }
            }
          }
          
          if (stillBad) {
            // MUTABLE REFERENCE TYPE
            proposeVouch = true;
            if (isFinal) {
              result = createResult(varDecl, false,
                  Messages.IMMUTABLE_FINAL_NOT_IMMUTABLE, id);
            } else {
              result = createResult(varDecl, false,
                  Messages.IMMUTABLE_NOT_FINAL_NOT_IMMUTABLE,
                  id);
            }
            for (final IRNode typeDecl : tester.getTested()) {
              result.addProposal(new ProposedPromiseBuilder(
                  "Immutable", null, typeDecl, varDecl,
                  Origin.MODEL));
            }
          }
        }
      }

      if (proposeVouch && result != null) {
        result.addProposal(new ProposedPromiseBuilder("Vouch",
            "Immutable", varDecl, varDecl, Origin.MODEL));
      }
    }
  }
}
