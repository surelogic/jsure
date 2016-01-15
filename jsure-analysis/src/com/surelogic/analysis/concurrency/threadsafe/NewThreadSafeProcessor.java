package com.surelogic.analysis.concurrency.threadsafe;

import java.util.Map;
import java.util.Set;

import com.surelogic.Containable;
import com.surelogic.Part;
import com.surelogic.ThreadSafe;
import com.surelogic.Unique;
import com.surelogic.Vouch;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.annotationbounds.ParameterizedTypeAnalysis;
import com.surelogic.analysis.concurrency.model.declared.StateLock;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.type.constraints.TypeAnnotationTester;
import com.surelogic.analysis.type.constraints.TypeAnnotations;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.analysis.visitors.TypeImplementationProcessor;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Builder;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.VouchFieldIsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ThreadSafePromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.IUniquePromise;

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

public final class NewThreadSafeProcessor extends TypeImplementationProcessor {
  private static final int THREAD_SAFE_SUPERTYPE = 400;
  private static final int TRIVIALLY_THREADSAFE = 401;
  private static final int VOUCHED_THREADSAFE = 402;
  private static final int VOUCHED_THREADSAFE_WITH_REASON = 403;
  private static final int FIELD_IS_THREADSAFE = 404;
  private static final int FIELD_IS_NOT_THREADSAFE = 405;
  private static final int FIELD_DECL_IS_SAFE = 406;
  private static final int FIELD_DECL_IS_NOT_SAFE = 407;
  private static final int FIELD_IS_FINAL = 408;
  private static final int FIELD_IS_NOT_FINAL = 409;
  private static final int FIELD_IS_VOLATILE = 410;
  private static final int FIELD_IS_NOT_VOLATILE = 411;
  private static final int FIELD_IS_PROTECTED = 440;
  private static final int FIELD_IS_NOT_PROTECTED = 413;
  private static final int OBJECT_IS_PROTECTED = 414;
  private static final int OBJECT_IS_NOT_PROTECTED = 415;
  private static final int TYPE_IS_PRIMITIVE = 416;
  private static final int TYPE_IS_NOT_PRIMITIVE = 417;
  private static final int TYPE_IS_THREADSAFE = 418;
  private static final int TYPE_IS_NOT_THREADSAFE = 419;
  private static final int THREADSAFE_IMPL = 420;
  private static final int OBJECT_IS_CONTAINED = 421;
  private static final int OBJECT_IS_NOT_CONTAINED = 422;
  private static final int TYPE_IS_CONTAINABLE = 423;
  private static final int TYPE_IS_NOT_CONTAINABLE = 424;
  private static final int FIELD_IS_UNIQUE = 425;
  private static final int FIELD_IS_NOT_UNIQUE = 426;
  private static final int DEST_REGION_PROTECTED = 441;
  private static final int DEST_REGION_UNPROTECTED = 428;
  private static final int CONTAINABLE_IMPL = 429;
  private static final int TRIVIALLY_THREADSAFE_STATIC_ONLY = 430;
  private static final int TRIVIALLY_THREADSAFE_NO_STATIC = 431;
  private static final int TYPE_IS_VOUCHED_CONTAINABLE = 432;
  private static final int TYPE_IS_VOUCHED_CONTAINABLE_WITH_REASON = 433;
  private static final int IMPLICITLY_FINAL = 477;
  private static final int CONSTANT_IS_THREADSAFE = 434;
  private static final int CONSTANT_IS_NOT_THREADSAFE = 435;

  
  
  private final ResultsBuilder builder;
  private final Set<StateLock<?, ?>> lockDeclarations;
  private final boolean isInterface;
  private boolean hasStaticFields = false;
  private final boolean verifyStaticState;
  private final boolean verifyInstanceState;
  
  
  
  public NewThreadSafeProcessor(final IBinder b,
      final ThreadSafePromiseDrop tsDrop,
      final IRNode typeDecl, final IRNode typeBody,
      Set<StateLock<?, ?>> lockDecls) {
    super(b, typeDecl, typeBody);
    isInterface = TypeUtil.isInterface(typeDecl);
    builder = new ResultsBuilder(tsDrop);
    final Part appliesTo = tsDrop.getAppliesTo();
    verifyInstanceState = appliesTo != Part.Static;
    verifyStaticState = appliesTo != Part.Instance;
    lockDeclarations = lockDecls;
  }

  private static StateLock<?, ?> getLockForRegion(
      final Set<StateLock<?, ?>> stateLocks, final IRegion r) {
    for (final StateLock<?, ?> lock : stateLocks) {
      if (lock.getRegion().ancestorOf(r)) {
        return lock;
      }
    }
    return null;
  }

  @Override
  protected void processSuperType(/*final IRNode name,*/ final IRNode tdecl) {
    // Super type is only interesting if we care about instance state
    if (verifyInstanceState) {
      final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> pDrop =
          LockRules.getThreadSafeImplPromise(tdecl);
      if (pDrop != null) {
        final ResultDrop result = builder.createRootResult(
            true, tdecl, THREAD_SAFE_SUPERTYPE, JavaNames.getQualifiedTypeName(tdecl));
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
        builder.createRootResult(true, typeDecl, TRIVIALLY_THREADSAFE);
      } else if (!verifyStaticState) {
        builder.createRootResult(true, typeDecl, TRIVIALLY_THREADSAFE_STATIC_ONLY);
      }
    } else {
      /* If we are verifying instance state, there will always be a drop 
       * about the super type. 
       */
      if (!verifyInstanceState) { // implies we are verifying static state
        /* trivially verified if we don't have static fields.
         */
        if (!hasStaticFields) {
          builder.createRootResult(true, typeDecl, TRIVIALLY_THREADSAFE_NO_STATIC);
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
        assureFieldIsThreadSafe(builder, binder, lockDeclarations, fieldDecl, varDecl);
      }
    } else if (verifyInstanceState) {
      assureFieldIsThreadSafe(builder, binder, lockDeclarations, fieldDecl, varDecl);
    }
  }

  static void assureFieldIsThreadSafe(
      final ResultsBuilder builder, final IBinder binder,
      final Set<StateLock<?, ?>> stateLocks,
      final IRNode fieldDecl, final IRNode varDecl) {
    /*
     * Field needs to be: (1) Volatile and thread safe (2) Final and
     * thread safe (3) Protected by a lock and thread safe
     * 
     * Where "thread safe" means (1) The declared type of the field is
     * primitive (2) The declared type of the field is annotated
     * @ThreadSafe (3) The declared type of the field is annotated
     * @Containable and the field is also annotated @Unique, and the
     * referenced object is aggregated into lock-protected regions.
     */
    final String id = VariableDeclarator.getId(varDecl);
  
    // Check for vouch
    final VouchFieldIsPromiseDrop vouchDrop = 
        LockRules.getVouchFieldIs(varDecl);
    if (vouchDrop != null && (vouchDrop.isThreadSafe() || vouchDrop.isImmutable())) {
      final String reason = vouchDrop.getReason();
      final ResultDrop result = 
          reason == VouchFieldIsNode.NO_REASON
            ? builder.createRootResult(true, varDecl, VOUCHED_THREADSAFE, id)
            : builder.createRootResult(true, varDecl, VOUCHED_THREADSAFE_WITH_REASON, id, reason);
      result.addTrusted(vouchDrop);
    } else {
      /* Create an AND folder for the field: We conjoin two things:
       * (1) That the field is protected, and (2) that the object referenced
       * by the field is protected.
       */
      final ResultFolderDrop folder = builder.createRootAndFolder(
          varDecl, FIELD_IS_THREADSAFE, FIELD_IS_NOT_THREADSAFE, id);
      
      /*
       * Part 1: Check if the field is volatile, final, or
       * lock-protected
       */
      final ResultFolderDrop part1Folder = ResultsBuilder.createOrFolder(
          folder, varDecl, FIELD_DECL_IS_SAFE, FIELD_DECL_IS_NOT_SAFE);
      
      final boolean isFinal = TypeUtil.isJSureFinal(varDecl);
      ResultsBuilder.createResult(part1Folder, varDecl, isFinal,
          FIELD_IS_FINAL, FIELD_IS_NOT_FINAL);
  
      final boolean isVolatile = TypeUtil.isVolatile(varDecl);
      ResultsBuilder.createResult(part1Folder, varDecl, isVolatile,
          FIELD_IS_VOLATILE, FIELD_IS_NOT_VOLATILE);
  
      final StateLock<?, ?> fieldLock =
          getLockForRegion(stateLocks, RegionModel.getInstance(varDecl));
      if (fieldLock != null) {
        final ResultDrop result = ResultsBuilder.createResult(
            true, part1Folder, varDecl, FIELD_IS_PROTECTED);
        result.addTrusted(fieldLock.getSourceAnnotation());
      } else {
        ResultsBuilder.createResult(false, part1Folder, varDecl, FIELD_IS_NOT_PROTECTED);
      }
      
      /*
       * Part2: Check that the field's type is thread safe or contained.
       */
      final IRNode fieldTypeNode = FieldDeclaration.getType(fieldDecl);
      final ResultFolderDrop part2folder = ResultsBuilder.createOrFolder(
          folder, fieldTypeNode, OBJECT_IS_PROTECTED, OBJECT_IS_NOT_PROTECTED);
      
      // Test if the type is primitive
      final IJavaType type = binder.getJavaType(varDecl);
      final boolean isPrimitive = type instanceof IJavaPrimitiveType;
      ResultsBuilder.createResult(part2folder, fieldTypeNode, isPrimitive, 
          TYPE_IS_PRIMITIVE, TYPE_IS_NOT_PRIMITIVE, type.toSourceText());
  
      // Test if the type of the field is thread safe (or immutable)
      final TypeAnnotationTester tsTester =
          new TypeAnnotationTester(TypeAnnotations.THREAD_SAFE, binder,
              ParameterizedTypeAnalysis.getFolders());
      final boolean isTS = tsTester.testFieldDeclarationType(fieldTypeNode);
      final ResultDrop tsResult = ResultsBuilder.createResult(part2folder, fieldTypeNode, isTS,
          TYPE_IS_THREADSAFE, TYPE_IS_NOT_THREADSAFE, type.toSourceText());
      tsResult.addTrusted(tsTester.getTrusts());
  
      boolean proposeThreadSafe = !isTS;
      if (!isTS && isFinal) {
        /*
         * If the type is not thread safe, we can check to see
         * if the implementation assigned to the field is thread
         * safe, but only if the field is final.
         */
        final IRNode init = VariableDeclarator.getInit(varDecl);
        if (Initialization.prototype.includes(init)) {
          final IRNode initExpr = Initialization.getValue(init);
          if (NewExpression.prototype.includes(initExpr)) {
            final TypeAnnotationTester tsTester2 =
                new TypeAnnotationTester(TypeAnnotations.THREAD_SAFE, binder,
                    ParameterizedTypeAnalysis.getFolders());
            if (tsTester2.testFinalObjectType(NewExpression.getType(initExpr))) {
              proposeThreadSafe = false;
              final ResultDrop result =
                  ResultsBuilder.createResult(true, part2folder, initExpr, THREADSAFE_IMPL);
              result.addTrusted(tsTester2.getTrusts());
            }
          }
        }
      }
      if (proposeThreadSafe) {
        for (final IRNode n : tsTester.getFailed()) {
          tsResult.addProposal(new Builder(ThreadSafe.class, n, varDecl).build());
        }
      }
        
      final ResultFolderDrop containableFolder = ResultsBuilder.createAndFolder(
          part2folder, varDecl, OBJECT_IS_CONTAINED, OBJECT_IS_NOT_CONTAINED);
      final TypeAnnotationTester cTester =
          new TypeAnnotationTester(TypeAnnotations.CONTAINABLE, binder,
              ParameterizedTypeAnalysis.getFolders());
      final boolean fieldTypeIsNotSingleton = !isSingletonType(binder.getJavaType(varDecl));
      
      if (vouchDrop != null && vouchDrop.isContainable()) {
        final String reason = vouchDrop.getReason();
        final ResultDrop result = 
            reason == VouchFieldIsNode.NO_REASON
              ? ResultsBuilder.createResult(true, containableFolder, varDecl, TYPE_IS_VOUCHED_CONTAINABLE)
              : ResultsBuilder.createResult(true, containableFolder, varDecl, TYPE_IS_VOUCHED_CONTAINABLE_WITH_REASON, reason);
        result.addTrusted(vouchDrop);
      } else {
        final boolean isContainable = cTester.testFieldDeclarationType(fieldTypeNode);
        boolean haveInitializerResult = false;
        boolean proposeContainable = !isContainable;
        if (TypeUtil.isJSureFinal(varDecl) && !isContainable) {
          /*
           * If the type is not containable, we can check to see
           * if the implementation assigned to the field is containable,
           * but only if the field is final.
           */
          final IRNode init = VariableDeclarator.getInit(varDecl);
          if (Initialization.prototype.includes(init)) {
            final IRNode initExpr = Initialization.getValue(init);
            if (NewExpression.prototype.includes(initExpr)) {
              final TypeAnnotationTester cTester2 =
                  new TypeAnnotationTester(TypeAnnotations.CONTAINABLE, binder,
                      ParameterizedTypeAnalysis.getFolders());
              if (cTester2.testFinalObjectType(NewExpression.getType(initExpr))) {
                // we have an instance of an immutable implementation
                haveInitializerResult = true;
                proposeContainable = false;
                final ResultDrop result = ResultsBuilder.createResult(
                    true, containableFolder, initExpr, CONTAINABLE_IMPL);
                result.addTrusted(cTester2.getTrusts());
              }
            }
          }
        }
        if (isContainable || !haveInitializerResult) {
          final ResultDrop cResult = ResultsBuilder.createResult(
              containableFolder, fieldTypeNode, isContainable,
              TYPE_IS_CONTAINABLE, TYPE_IS_NOT_CONTAINABLE, type.toSourceText());
          cResult.addTrusted(cTester.getTrusts());

          if (proposeContainable && fieldTypeIsNotSingleton) {
            for (final IRNode n : tsTester.getFailed()) {
              cResult.addProposal(new Builder(Containable.class, n, varDecl).build());
            }
          }
        }
      }
            
      // check aggregation
      final IUniquePromise uDrop = UniquenessUtils.getUnique(varDecl);
      if (uDrop == null) {
        final ResultDrop uResult =
            ResultsBuilder.createResult(false, containableFolder, varDecl, FIELD_IS_NOT_UNIQUE);
        if (!isPrimitive && fieldTypeIsNotSingleton) {
          uResult.addProposal(new Builder(Unique.class, varDecl, varDecl).build());
        }
      } else {
        final ResultDrop uResult =
            ResultsBuilder.createResult(true, containableFolder, varDecl, FIELD_IS_UNIQUE);
        uResult.addTrusted(uDrop.getDrop());
  
        // Check that the destination regions are lock protected
        final Map<IRegion, IRegion> aggMap =
            UniquenessUtils.constructRegionMapping(varDecl);
        for (final IRegion destRegion : aggMap.values()) {
          final StateLock<?, ?> lock = getLockForRegion(stateLocks, destRegion);
          if (lock != null) {
            final ResultDrop aggResult = ResultsBuilder.createResult(
                true, containableFolder, varDecl, DEST_REGION_PROTECTED,
                destRegion.getName());
            aggResult.addTrusted(lock.getSourceAnnotation());
          } else {
            ResultsBuilder.createResult(false, containableFolder, varDecl,
                DEST_REGION_UNPROTECTED, destRegion.getName());
          }
        }
      }
      
      if (!isPrimitive) {
        // Propose that the field be vouched threadsafe
        folder.addProposalNotProvedConsistent(new Builder(Vouch.class, varDecl, varDecl).setValue("ThreadSafe").build());
      }
    }
  }


  @Override
  protected void processEnumConstantDeclaration(final IRNode constDecl) {
    /* 
     * An enum constant declaration is a static final field.
     */
    hasStaticFields = true;
    if (verifyStaticState) {
      /*
       * Declaration is always final, so it needs to be thread safe.
       * The declared type is always E (for Enum<E>) and it is pointless to
       * have a @Unique constant reference (makes it useless), so we have 
       * to check if the declared type is declared ThreadSafe, which it will be,
       * otherwise
       * we wouldn't be checking this to begin with.  (Type may FAIL to be
       * ThreadSafe if it declares additional instance fields.)
       */
      final String id = EnumConstantDeclaration.getId(constDecl);
      final ResultFolderDrop folder = builder.createRootAndFolder(
          constDecl, CONSTANT_IS_THREADSAFE, CONSTANT_IS_NOT_THREADSAFE, id);
      ResultsBuilder.createResult(true, folder, constDecl, IMPLICITLY_FINAL);
      final IJavaType constType =
          binder.getTypeEnvironment().convertNodeTypeToIJavaType(typeDecl);
      final ResultDrop iResult = ResultsBuilder.createResult(
          folder, typeDecl, true, TYPE_IS_THREADSAFE, TYPE_IS_NOT_THREADSAFE,
          constType.toSourceText());  
      iResult.addTrusted(LockRules.getThreadSafeImplPromise(typeDecl));
    }
  }
}
