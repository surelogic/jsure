package com.surelogic.analysis.concurrency.threadsafe;

import java.util.Map;
import java.util.Set;

import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode.State;
import com.surelogic.analysis.ResultsBuilder;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.analysis.annotationbounds.ParameterizedTypeAnalysis;
import com.surelogic.analysis.concurrency.heldlocks.GlobalLockModel;
import com.surelogic.analysis.concurrency.heldlocks.RegionLockRecord;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.type.constraints.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.type.constraints.ContainableAnnotationTester;
import com.surelogic.analysis.type.constraints.ThreadSafeAnnotationTester;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.IProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
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
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;

public final class ThreadSafeProcessor extends TypeImplementationProcessor {
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
  private static final int FIELD_IS_PROTECTED = 412;
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
  private static final int DEST_REGION_PROTECTED = 427;
  private static final int DEST_REGION_UNPROTECTED = 428;
  
  
  
  private final ResultsBuilder builder;
  private final Set<RegionLockRecord> lockDeclarations;
  private boolean hasFields = false;
  private final State staticPart;

  
  
  public ThreadSafeProcessor(final IBinder b,
      final ThreadSafePromiseDrop tsDrop,
      final IRNode typeDecl, final IRNode typeBody,
      final GlobalLockModel globalLockModel) {
    super(b, typeDecl, typeBody);
    builder = new ResultsBuilder(tsDrop);
    staticPart = tsDrop.staticPart();
    lockDeclarations = globalLockModel.getRegionLocksInClass(
        JavaTypeFactory.getMyThisType(typeDecl));
  }

  private static RegionLockRecord getLockForRegion(
      final Set<RegionLockRecord> lockDeclarations, final IRegion r) {
    for (final RegionLockRecord lr : lockDeclarations) {
      if (lr.region.ancestorOf(r)) {
        return lr;
      }
    }
    return null;
  }

  @Override
  protected void processSuperType(final IRNode name, final IRNode tdecl) {
    final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> pDrop =
        LockRules.getThreadSafeImplPromise(tdecl);
    if (pDrop != null) {
      final ResultDrop result = builder.createRootResult(
          true, name, THREAD_SAFE_SUPERTYPE, JavaNames.getQualifiedTypeName(tdecl));
      result.addTrusted(pDrop);
    }
  }

  @Override
  protected void postProcess() {
    if (!hasFields) {
      builder.createRootResult(true, JJNode.tree.getParent(typeBody), TRIVIALLY_THREADSAFE);    
    }
  }

  @Override
  protected void processVariableDeclarator(final IRNode fieldDecl,
      final IRNode varDecl, final boolean isStatic) {
    // we have a field
    hasFields = true;
    
    
    if (isStatic) {
      if (staticPart == State.Immutable) {
        ImmutableProcessor.assureFieldIsImmutable(builder, binder, fieldDecl, varDecl);
      } else if (staticPart == State.ThreadSafe) {
        assureFieldIsThreadSafe(builder, binder, lockDeclarations, fieldDecl, varDecl);
      }
    } else {
      assureFieldIsThreadSafe(builder, binder, lockDeclarations, fieldDecl, varDecl);
    }
  }

  static void assureFieldIsThreadSafe(
      final ResultsBuilder builder, final IBinder binder,
      final Set<RegionLockRecord> lockDeclarations,
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
      
      final boolean isFinal = TypeUtil.isFinal(varDecl);
      ResultsBuilder.createResult(part1Folder, fieldDecl, isFinal,
          FIELD_IS_FINAL, FIELD_IS_NOT_FINAL);
  
      final boolean isVolatile = TypeUtil.isVolatile(varDecl);
      ResultsBuilder.createResult(part1Folder, fieldDecl, isVolatile,
          FIELD_IS_VOLATILE, FIELD_IS_NOT_VOLATILE);
  
      final RegionLockRecord fieldLock =
          getLockForRegion(lockDeclarations, RegionModel.getInstance(varDecl));
      if (fieldLock != null) {
        final ResultDrop result = ResultsBuilder.createResult(
            true, part1Folder, varDecl, FIELD_IS_PROTECTED, fieldLock.name);
        result.addTrusted(fieldLock.lockDecl);
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
      final ThreadSafeAnnotationTester tsTester =
          new ThreadSafeAnnotationTester(
              binder, AnnotationBoundsTypeFormalEnv.INSTANCE,
              ParameterizedTypeAnalysis.getFolders(), true, false);
      final boolean isTS = tsTester.testType(type);
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
            final ThreadSafeAnnotationTester tsTester2 =
                new ThreadSafeAnnotationTester(
                    binder, AnnotationBoundsTypeFormalEnv.INSTANCE,
                    ParameterizedTypeAnalysis.getFolders(), true, true);
            if (tsTester2.testType(binder.getJavaType(initExpr))) {
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
          tsResult.addProposal(new ProposedPromiseDrop(
              "ThreadSafe", null, n, varDecl, Origin.MODEL));
        }
      }
        
      final ResultFolderDrop containableFolder = ResultsBuilder.createAndFolder(
          part2folder, fieldDecl, OBJECT_IS_CONTAINED, OBJECT_IS_NOT_CONTAINED);
      final ContainableAnnotationTester cTester =
          new ContainableAnnotationTester(
              binder, AnnotationBoundsTypeFormalEnv.INSTANCE,
              ParameterizedTypeAnalysis.getFolders(), true, false);
      final boolean isContainable = cTester.testType(type);
      final ResultDrop cResult = ResultsBuilder.createResult(
          containableFolder, fieldTypeNode, isContainable,
          TYPE_IS_CONTAINABLE, TYPE_IS_NOT_CONTAINABLE, type.toSourceText());
      cResult.addTrusted(cTester.getTrusts());
      if (!isContainable) {
        for (final IRNode n : tsTester.getFailed()) {
          cResult.addProposal(new ProposedPromiseDrop(
              "Containable", null, n, varDecl, Origin.MODEL));
        }
      }
            
      // check aggregation
      final IUniquePromise uDrop = UniquenessUtils.getUnique(varDecl);
      if (uDrop == null) {
        final ResultDrop uResult =
            ResultsBuilder.createResult(false, containableFolder, fieldDecl, FIELD_IS_NOT_UNIQUE);
        uResult.addProposal(new ProposedPromiseDrop(
            "Unique", null, varDecl, varDecl,  Origin.MODEL));
      } else {
        final ResultDrop uResult =
            ResultsBuilder.createResult(true, containableFolder, fieldDecl, FIELD_IS_UNIQUE);
        uResult.addTrusted(uDrop.getDrop());
  
        // Check that the destination regions are lock protected
        final Map<IRegion, IRegion> aggMap =
            UniquenessUtils.constructRegionMapping(varDecl);
        for (final IRegion destRegion : aggMap.values()) {
          final RegionLockRecord lock = getLockForRegion(lockDeclarations, destRegion);
          if (lock != null) {
            final ResultDrop aggResult = ResultsBuilder.createResult(
                true, containableFolder, varDecl, DEST_REGION_PROTECTED,
                destRegion.getName(), lock.name);
            aggResult.addTrusted(lock.lockDecl);
          } else {
            ResultsBuilder.createResult(false, containableFolder, varDecl,
                DEST_REGION_UNPROTECTED, destRegion.getName());
          }
        }
      }
  
      if (!isPrimitive) {
        // Propose that the field be vouched threadsafe
        folder.addProposalNotProvedConsistent(new ProposedPromiseDrop(
            "Vouch", "ThreadSafe", varDecl, varDecl, Origin.MODEL));
      }
    }
  }
}
