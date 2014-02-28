package com.surelogic.analysis.concurrency.threadsafe;

import com.surelogic.Immutable;
import com.surelogic.Part;
import com.surelogic.Vouch;
import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.annotationbounds.ParameterizedTypeAnalysis;
import com.surelogic.analysis.concurrency.heldlocks.GlobalLockModel;
import com.surelogic.analysis.type.constraints.TypeAnnotationTester;
import com.surelogic.analysis.type.constraints.TypeAnnotations;
import com.surelogic.analysis.visitors.TypeImplementationProcessor;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;
import com.surelogic.dropsea.ir.drops.VouchFieldIsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ImmutablePromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.operator.EnumConstantDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;

public final class ImmutableProcessor extends TypeImplementationProcessor {
  private static final int CONSTANT_IS_IMMUTABLE = 475;
  private static final int CONSTANT_IS_NOT_IMMUTABLE = 476;
  private static final int IMPLICITLY_FINAL = 477;
  private static final int TRIVIALLY_IMMUTABLE_NO_STATIC = 478;
  private static final int TRIVIALLY_IMMUTABLE_STATIC_ONLY = 479;
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
  
  
  
  private final boolean isInterface;
  private final ResultsBuilder builder;
  private final boolean verifyInstanceState;
  private final boolean verifyStaticState;
  private boolean hasStaticFields = false;
  
  
  
  public ImmutableProcessor(final IBinder b,
      final ImmutablePromiseDrop iDrop,
      final IRNode typeDecl, final IRNode typeBody,
      final GlobalLockModel globalLockModel) {
    super(b, typeDecl, typeBody);
    isInterface = TypeUtil.isInterface(typeDecl);
    builder = new ResultsBuilder(iDrop);
    final Part appliesTo = iDrop.getAppliesTo();
    verifyInstanceState = appliesTo != Part.Static;
    verifyStaticState = appliesTo != Part.Instance;
  }

  @Override
  protected void processSuperType(/*final IRNode name,*/ final IRNode tdecl) {
    // Super type is only interesting if we care about instance state
    if (verifyInstanceState) {
      final ImmutablePromiseDrop pDrop =
          LockRules.getImmutableImplementation(tdecl);
      if (pDrop != null) {
        final ResultDrop result = builder.createRootResult(
            true, tdecl, IMMUTABLE_SUPERTYPE,
            JavaNames.getQualifiedTypeName(tdecl));
        result.addTrusted(pDrop);
      }
    }
  }

  @Override
  protected void postProcess() {
    if (isInterface) {
      /* Interfaces cannot have instance state.  Two cases for trivial
       * verification: (1) The interface is empty, or (2) there are static fields
       * but they are not subject to verification.
       */
      if (!hasStaticFields) { // interfaces never have instance fields
        builder.createRootResult(true, typeDecl, TRIVIALLY_IMMUTABLE);
      } else if (!verifyStaticState) {
        builder.createRootResult(true, typeDecl, TRIVIALLY_IMMUTABLE_STATIC_ONLY);
      }
    } else {
      /* If we are verifying instance state, there will always be a drop 
       * about the super type. 
       */
      if (!verifyInstanceState) { // implies we are verifying static state
        /* trivially verified if we don't have static fields.
         */
        if (!hasStaticFields) {
          builder.createRootResult(true, typeDecl, TRIVIALLY_IMMUTABLE_NO_STATIC);
        }
      }
    }    
  }

  @Override
  protected void processVariableDeclarator(final IRNode fieldDecl,
      final IRNode varDecl, final boolean isStatic) {
    if (isStatic) {
      hasStaticFields = true;
      if (verifyStaticState) {
        assureFieldIsImmutable(builder, binder, fieldDecl, varDecl);
      }
    } else if (verifyInstanceState) {
      assureFieldIsImmutable(builder, binder, fieldDecl, varDecl);
    }
  }


  static void assureFieldIsImmutable(
      final ResultsBuilder builder, final IBinder binder,
      final IRNode fieldDecl, final IRNode varDecl) {
    final String id = VariableDeclarator.getId(varDecl);

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
        result = builder.createRootResult(true, varDecl, VOUCHED_IMMUTABLE, id);
      } else {
        result = builder.createRootResult(
            true, varDecl, VOUCHED_IMMUTABLE_WITH_REASON, id, reason);
      }
      result.addTrusted(vouchDrop);
    } else {
      /* Now we use a result folder because we are conjoining two results:
       * (1) the field is final
       * (2) the field's type is immutable or primitive
       */
      final ResultFolderDrop folder = builder.createRootAndFolder(
          varDecl, FIELD_IS_IMMUTABLE, FIELD_IS_NOT_IMMUTABLE, id);
      
      // (1) Check finality of the field
      final boolean isFinal = TypeUtil.isFinal(varDecl);
      final ResultDrop fDrop = ResultsBuilder.createResult(folder, varDecl,
          isFinal, FIELD_IS_FINAL, FIELD_IS_NOT_FINAL);
      if (isFinal) {
        // Get the @Vouch("final") annotation if there is one
        final VouchFieldIsPromiseDrop vouchFinal = LockRules.getVouchFieldIs(varDecl);
        if (vouchFinal != null && vouchFinal.isFinal()) {
          fDrop.addTrusted(vouchFinal);
        }
      } else {
        fDrop.addProposal(new Builder(Vouch.class, varDecl, varDecl).setValue("final").build());
      }

      /* (2) Check the immutability fo the field's type.  Four cases:
       *   1. The type is primitive (GOOD)
       *   2. The type is @Immutable (GOOD)
       *   3. The field is final and the field initializer is of a type
       *      whose implementation is @Immutable (GOOD)
       *   4. All other cases (BAD) 
       */
      final ResultFolderDrop typeFolder = ResultsBuilder.createOrFolder(
          folder, varDecl, OBJECT_IS_IMMUTABLE, OBJECT_IS_NOT_IMMUTABLE);
      final IJavaType type = binder.getJavaType(varDecl);
      final IRNode typeDeclNode = FieldDeclaration.getType(fieldDecl);
      
      // primitive
      final boolean isPrimitive = type instanceof IJavaPrimitiveType;
      ResultsBuilder.createResult(typeFolder, typeDeclNode, isPrimitive,
          TYPE_IS_PRIMITIVE, TYPE_IS_NOT_PRIMITIVE, type.toSourceText());
      
      // immutable
      final TypeAnnotationTester tester =
          new TypeAnnotationTester(TypeAnnotations.IMMUTABLE, binder,
              ParameterizedTypeAnalysis.getFolders());
      final boolean isImmutable = tester.testFieldDeclarationType(typeDeclNode);
      final ResultDrop iResult = ResultsBuilder.createResult(
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
            final TypeAnnotationTester tester2 =
                new TypeAnnotationTester(TypeAnnotations.IMMUTABLE, binder,
                    ParameterizedTypeAnalysis.getFolders());
            if (tester2.testFinalObjectType(NewExpression.getType(initExpr))) {
              // we have an instance of an immutable implementation
              proposeImmutable = false;
              final ResultDrop result = ResultsBuilder.createResult(
                  true, typeFolder, initExpr, IMMUTABLE_IMPL);
              result.addTrusted(tester2.getTrusts());
            }
          }
        }
      }
      
      if (proposeImmutable) {
        for (final IRNode typeDecl : tester.getTested()) {
          iResult.addProposal(new Builder(Immutable.class, typeDecl, varDecl).build());
        }
      }        

      if (!isPrimitive) {
        folder.addProposalNotProvedConsistent(new Builder(Vouch.class, varDecl, varDecl).setValue("Immutable").build());
      }
    }
  }

  @Override
  protected void processEnumConstantDeclaration(final IRNode constDecl) {
    /* 
     * An enum constant declaration is a static final field of type E.
     */
    hasStaticFields = true;
    if (verifyStaticState) {
      final String id = EnumConstantDeclaration.getId(constDecl);
      
      final ResultFolderDrop folder = builder.createRootAndFolder(
          constDecl, CONSTANT_IS_IMMUTABLE, CONSTANT_IS_NOT_IMMUTABLE, id);
      
      // (1) The field is final
      ResultsBuilder.createResult(true, folder, constDecl, IMPLICITLY_FINAL);
  
      /* Check the type for immutability.  Might get a red-X here because we 
       * have instance fields that not immutable.
       */
      final IJavaType constType =
          binder.getTypeEnvironment().convertNodeTypeToIJavaType(typeDecl);
      final ResultDrop iResult = ResultsBuilder.createResult(
          folder, typeDecl, true,
          TYPE_IS_IMMUTABLE, TYPE_IS_NOT_IMMUTABLE, constType.toSourceText());  
      iResult.addTrusted(LockRules.getImmutableImplementation(typeDecl));
    }
  }
}
