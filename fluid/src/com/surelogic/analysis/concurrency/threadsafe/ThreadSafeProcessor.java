package com.surelogic.analysis.concurrency.threadsafe;

import java.util.Map;
import java.util.Set;

import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.concurrency.heldlocks.GlobalLockModel;
import com.surelogic.analysis.concurrency.heldlocks.RegionLockRecord;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.type.constraints.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.type.constraints.ContainableAnnotationTester;
import com.surelogic.analysis.type.constraints.ThreadSafeAnnotationTester;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.dropsea.ir.ProposedPromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.ProposedPromiseDrop.Origin;
import com.surelogic.dropsea.ir.drops.ModifiedBooleanPromiseDrop;
import com.surelogic.dropsea.ir.drops.RegionModel;
import com.surelogic.dropsea.ir.drops.VouchFieldIsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ThreadSafePromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.IUniquePromise;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;

public final class ThreadSafeProcessor extends TypeImplementationProcessor<ThreadSafePromiseDrop> {
  private final Set<RegionLockRecord> lockDeclarations;
  private boolean hasFields = false;

  public ThreadSafeProcessor(
      final AbstractWholeIRAnalysis<? extends IBinderClient, ?> a,
      final ThreadSafePromiseDrop tsDrop,
      final IRNode typeDecl, final IRNode typeBody,
      final GlobalLockModel globalLockModel) {
    super(a, tsDrop, typeDecl, typeBody);
    lockDeclarations = globalLockModel.getRegionLocksInClass(
        JavaTypeFactory.getMyThisType(typeDecl));
  }

  private RegionLockRecord getLockForRegion(final IRegion r) {
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
      final ResultDrop result = createResult(name, true,
          Messages.THREAD_SAFE_SUPERTYPE,
          JavaNames.getQualifiedTypeName(tdecl));
      result.addTrusted(pDrop);
    }
  }

  @Override
  protected void postProcess() {
    if (!hasFields) {
      createResult(typeBody, true, Messages.TRIVIALLY_THREADSAFE);
    }
  }

  @Override
  protected void processVariableDeclarator(final IRNode fieldDecl,
      final IRNode varDecl, final boolean isStatic) {
    // we have a field
    hasFields = true;

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
          reason == VouchFieldIsNode.NO_REASON ? createResult(
              varDecl, true, Messages.VOUCHED_THREADSAFE, id)
              : createResult(varDecl, true,
                  Messages.VOUCHED_THREADSAFE_WITH_REASON, id, reason);
      result.addTrusted(vouchDrop);
    } else {
      /* Create a Results Folder for the field.  We are going to AND together
       * a bunch of results.  Keep track of the overall correctness though
       * so we can set the correct message on the Result Folder.
       */
      final ResultFolderDrop folder = createResultFolder(varDecl);
      
      /*
       * Part 1: Check if the field is volatile, final, or
       * lock-protected
       */
      boolean passesPart1 = false;
      final boolean isFinal = TypeUtil.isFinal(varDecl);
      if (isFinal) {
        createResultInFolder(folder, fieldDecl, true, Messages.THREADSAFE_FINAL);
        passesPart1 = true;
      }
      
      final boolean isVolatile = TypeUtil.isVolatile(varDecl);
      if (isVolatile) {
        createResultInFolder(folder, fieldDecl, true, Messages.THEADSAFE_VOLATILE);
        passesPart1 = true;
      }
      
      final RegionLockRecord fieldLock =
          getLockForRegion(RegionModel.getInstance(varDecl));
      if (fieldLock != null) {
        final ResultDrop result = createResultInFolder(
            folder, varDecl, true, Messages.LOCK_PROTECTED, fieldLock.name);
        result.addTrusted(fieldLock.lockDecl);
        passesPart1 = true;
      }
      
      if (!passesPart1) {
        createResultInFolder(folder, varDecl, false, Messages.UNPROTECTED_FIELD);
      }
      
      /*
       * Part2: Check that the field's type is thread safe or contained.
       */
      boolean passesPart2 = false;
      final IJavaType type = binder.getJavaType(varDecl);

      // Test if the type of the field is primitive
      final boolean isPrimitive = type instanceof IJavaPrimitiveType;
      final IRNode fieldTypeNode = FieldDeclaration.getType(fieldDecl);
      if (isPrimitive) {
        createResultInFolder(folder, fieldTypeNode,
            true, Messages.THREADSAFE_PRIMITIVE, type.toSourceText());
        passesPart2 = true;
      } else { // REFERENCE TYPE
        // Test if the type of the field is thread safe (or immutable)
        final ThreadSafeAnnotationTester tsTester =
            new ThreadSafeAnnotationTester(binder, AnnotationBoundsTypeFormalEnv.INSTANCE, true);
        final boolean isTS = tsTester.testType(type);
        if (isTS) {
          final ResultDrop result = createResultInFolder(
              folder, fieldTypeNode, true,
              Messages.THREADSAFE_THREADSAFE, type.toSourceText());
          result.addTrusted(tsTester.getPromises());
//          final ResultFolderDrop annoFolder = 
//              ParameterizedTypeAnalysis.getFolderForTypeNode(fieldTypeNode);
//          if (annoFolder != null) result.addTrustedResultFolder(annoFolder);
          passesPart2 = true;
        } else {
          /*
           * If the type is not thread safe, we can check to see
           * if the implementation assigned to the field is thread
           * safe, but only if the field is final.
           */
          boolean stillBad = true;
          if (isFinal) {
            final IRNode init = VariableDeclarator.getInit(varDecl);
            if (Initialization.prototype.includes(init)) {
              final IRNode initExpr = Initialization.getValue(init);
              if (NewExpression.prototype.includes(initExpr)) {
                final IRNode declaredTypeOfNewExpr =
                    ((IJavaDeclaredType) binder.getJavaType(initExpr)).getDeclaration();
                final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> implTypeTSDrop =
                    LockRules.getThreadSafeImplPromise(
                        declaredTypeOfNewExpr);
                if (implTypeTSDrop != null) {
                  stillBad = false;
                  passesPart2 = true;
                  final ResultDrop result = createResultInFolder(
                      folder, initExpr, true, Messages.THREADSAFE_IMPL); 
                  result.addTrusted(implTypeTSDrop);
//                  final IRNode xx = NewExpression.getType(initExpr);
//                  final ResultFolderDrop annoFolder = 
//                      ParameterizedTypeAnalysis.getFolderForTypeNode(xx);
//                  if (annoFolder != null) result.addTrustedResultFolder(annoFolder);
                }
              }
            }
          }
          
          if (stillBad) {
            // only check containability if the type is not threadsafe 
            final ContainableAnnotationTester cTester =
                new ContainableAnnotationTester(
                    binder, AnnotationBoundsTypeFormalEnv.INSTANCE, true);
            if (cTester.testType(type)) {
              passesPart2 = true; // may be made false again, below, if the field is not aggregated
              final ResultDrop result = createResultInFolder(
                  folder, fieldTypeNode, true,
                  Messages.THREADSAFE_CONTAINABLE, type.toSourceText());
              result.addTrusted(cTester.getPromises());
            } else { // NEITHER THREAD SAFE NOR CONTAINABLE
              // Propose to make the type @ThreadSafe
              for (final IRNode n : tsTester.getFailed()) {
                folder.addProposal(new ProposedPromiseDrop(
                    "ThreadSafe", null, n, varDecl, Origin.MODEL));
              }
              
              // Propose to make the type @Containable
              for (final IRNode n : tsTester.getFailed()) {
                folder.addProposal(new ProposedPromiseDrop(
                    "Containable", null, n, varDecl, Origin.MODEL));
              }
            }
            
            // check aggregation
            final IUniquePromise uDrop = UniquenessUtils.getUnique(varDecl);
            if (uDrop != null) {
              final ResultDrop result = createResultInFolder(
                  folder, varDecl, true, Messages.THREADSAFE_UNIQUE);
              result.addTrusted(uDrop.getDrop());
              passesPart2 &= true; // might still be made false if the aggregation isn't lock protected

              // Check that the destination regions are lock protected
              final ResultFolderDrop subFolder = createSubFolder(folder, varDecl);
              final Map<IRegion, IRegion> aggMap =
                  UniquenessUtils.constructRegionMapping(varDecl);
              boolean protectedRegions = true;
              for (final IRegion destRegion : aggMap.values()) {
                final RegionLockRecord lock = getLockForRegion(destRegion);
                if (lock != null) {
                  final ResultDrop result2 = createResultInFolder(
                      subFolder, varDecl, true,Messages.DEST_REGION_PROTECTED,
                      destRegion.getName(), lock.name);
                  result2.addTrusted(lock.lockDecl);
                } else {
                  protectedRegions = false;
                  createResultInFolder(subFolder, varDecl, false,
                      Messages.DEST_REGION_UNPROTECTED, destRegion.getName());
                }
              }

              if (protectedRegions) {
                subFolder.setMessage(Messages.FOLDER_AGGREGATION_IS_PROTECTED);
              } else {
                passesPart2 = false;
                subFolder.setMessage(Messages.FOLDER_AGGREGATION_IS_NOT_PROTECTED);
              }
            } else {
              // Need to do this because it may be been made temporarily true above
              passesPart2 &= false;
              folder.addProposal(new ProposedPromiseDrop(
                  "Unique", null, varDecl, varDecl,  Origin.MODEL));
            }
          }
        }        
      }
      
      if (!passesPart2) { // && !hasAggregationSubFolder) {
        createResultInFolder(folder, varDecl, false, Messages.UNPROTECTED_REFERENCE);
      }
      
      if (passesPart1 && passesPart2) {
        folder.setMessage(Messages.FOLDER_IS_THREADSAFE, id);
      } else {
        folder.setMessage(Messages.FOLDER_IS_NOT_THREADSAFE, id);
        if (!isPrimitive) {
          // Propose that the field be vouched threadsafe
          folder.addProposal(new ProposedPromiseDrop(
              "Vouch", "ThreadSafe", varDecl, varDecl, Origin.MODEL));
        }
      }
    }
  }
}
