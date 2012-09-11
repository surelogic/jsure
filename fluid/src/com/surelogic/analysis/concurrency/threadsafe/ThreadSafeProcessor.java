package com.surelogic.analysis.concurrency.threadsafe;

import java.util.Map;
import java.util.Set;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.analysis.TypeImplementationProcessor;
import com.surelogic.analysis.concurrency.driver.Messages;
import com.surelogic.analysis.concurrency.heldlocks.GlobalLockModel;
import com.surelogic.analysis.concurrency.heldlocks.RegionLockRecord;
import com.surelogic.analysis.concurrency.util.AnnotationBoundsTypeFormalEnv;
import com.surelogic.analysis.concurrency.util.ContainableAnnotationTester;
import com.surelogic.analysis.concurrency.util.ThreadSafeAnnotationTester;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.IJavaArrayType;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.Initialization;
import edu.cmu.cs.fluid.java.operator.NewExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;
import edu.cmu.cs.fluid.sea.drops.ModifiedBooleanPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.IUniquePromise;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.drops.promises.ThreadSafePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.VouchFieldIsPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ProposedPromiseBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.util.EmptyIterator;
import edu.cmu.cs.fluid.util.SingletonIterator;

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
  protected void processSuperType(final IRNode tdecl) {
    final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> pDrop =
        LockRules.getThreadSafeImplPromise(tdecl);
    if (pDrop != null) {
      final ResultDropBuilder result = createResultBuilder(tdecl, true,
          Messages.THREAD_SAFE_SUPERTYPE,
          JavaNames.getQualifiedTypeName(tdecl));
      result.addTrustedPromise(pDrop);
    }
  }

  @Override
  protected void postProcess() {
    if (!hasFields) {
      createResultBuilder(typeBody, true, Messages.TRIVIALLY_THREADSAFE);
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
      final ResultDropBuilder result = 
          reason == VouchFieldIsNode.NO_REASON ? createResultBuilder(
              varDecl, true, Messages.VOUCHED_THREADSAFE, id)
              : createResultBuilder(varDecl, true,
                  Messages.VOUCHED_THREADSAFE_WITH_REASON, id, reason);
      result.addTrustedPromise(vouchDrop);
    } else {
      /*
       * First check if the field is volatile, final, or
       * lock-protected
       */
      final boolean isFinal = TypeUtil.isFinal(varDecl);
      final boolean isVolatile = TypeUtil.isVolatile(varDecl);
      final RegionLockRecord fieldLock =
          getLockForRegion(RegionModel.getInstance(varDecl));

      if (isFinal || isVolatile || fieldLock != null) {
        /* Check if the declared type of the field is thread-safe or
         * containable.
         */
        final IJavaType type = binder.getJavaType(varDecl);
        final boolean isPrimitive = type instanceof IJavaPrimitiveType;
        final boolean isArray = type instanceof IJavaArrayType;
        final boolean testedType;
        final boolean usingImplDrop;
        final boolean isThreadSafe;
        final Iterable<PromiseDrop<? extends IAASTRootNode>> tsDrops;
        final Iterable<IRNode> notThreadSafe;
        final boolean isDeclaredContainable;
        final ContainableAnnotationTester cTester =
            new ContainableAnnotationTester(
                binder, AnnotationBoundsTypeFormalEnv.INSTANCE, true);

        if (!isPrimitive && !isArray) { // type formal or declared type
          final ThreadSafeAnnotationTester tsTester =
              new ThreadSafeAnnotationTester(binder, AnnotationBoundsTypeFormalEnv.INSTANCE, true);
          final boolean isTS = tsTester.testType(type);
          testedType = true;
          /*
           * If the type is not thread safe, we can check to see
           * if the implementation assigned to the field is thread
           * safe, but only if the field is final.
           */
          if (!isTS && isFinal) {
            final IRNode init = VariableDeclarator.getInit(varDecl);
            if (Initialization.prototype.includes(init)) {
              final IRNode initExpr = Initialization.getValue(init);
              if (NewExpression.prototype.includes(initExpr)) {
                final ModifiedBooleanPromiseDrop<? extends AbstractModifiedBooleanNode> implTypeTSDrop =
                    LockRules.getThreadSafeImplPromise(
                        ((IJavaDeclaredType) binder.getJavaType(initExpr)).getDeclaration());
                usingImplDrop = true;
                if (implTypeTSDrop != null) {
                  isThreadSafe = true;
                  tsDrops = new SingletonIterator<PromiseDrop<? extends IAASTRootNode>>(implTypeTSDrop);
                  notThreadSafe = new EmptyIterator<IRNode>();
                } else {
                  isThreadSafe = false;
                  tsDrops = new EmptyIterator<PromiseDrop<? extends IAASTRootNode>>();
                  notThreadSafe = tsTester.getFailed();
                }
              } else {
                usingImplDrop = false;
                isThreadSafe = false;
                tsDrops = new EmptyIterator<PromiseDrop<? extends IAASTRootNode>>();
                notThreadSafe = tsTester.getFailed();
              }                
            } else {
              usingImplDrop = false;
              isThreadSafe = false;
              tsDrops = new EmptyIterator<PromiseDrop<? extends IAASTRootNode>>();
              notThreadSafe = tsTester.getFailed();
            }
          } else {
            usingImplDrop = false;
            isThreadSafe = isTS;
            tsDrops = tsTester.getPromises();
            notThreadSafe = tsTester.getFailed();
          }
          
          isDeclaredContainable = cTester.testType(type);
        } else {
          testedType = false;
          usingImplDrop = false;
          isThreadSafe = false;
          tsDrops = new EmptyIterator<PromiseDrop<? extends IAASTRootNode>>();
          notThreadSafe = new EmptyIterator<IRNode>();
          isDeclaredContainable = false;
        }
        
        final boolean isContainable =
            isDeclaredContainable || (isArray && cTester.testType(type));

        /*
         * @ThreadSafe takes priority over @Containable: If the type
         * is threadsafe don't check the aggregation status
         */
        final IUniquePromise uDrop = UniquenessUtils.getUnique(varDecl);
        final Map<IRegion, IRegion> aggMap;
        boolean isContained = false;
        if (!isThreadSafe && isContainable) {
          if (uDrop != null) {
            aggMap = UniquenessUtils.constructRegionMapping(varDecl);
            isContained = true;
            for (final IRegion destRegion : aggMap.values()) {
              isContained &= (getLockForRegion(destRegion) != null);
            }
          } else {
            aggMap = null;
          }
        } else {
          aggMap = null;
          // no @Containable annotation --> Default "annotation"
          // of not containable
          isContained = false;
        }

        final String typeString = type.toSourceText();
        if (isPrimitive || isThreadSafe || isContained) {
          final ResultDropBuilder result;
          if (isFinal) {
            result = createResultBuilder(
                varDecl, true, Messages.FINAL_AND_THREADSAFE, id);
          } else if (isVolatile) {
            result = createResultBuilder(
                varDecl, true, Messages.VOLATILE_AND_THREADSAFE, id);
          } else { // lock protected
            result = createResultBuilder(
                varDecl, true, Messages.PROTECTED_AND_THREADSAFE, id,
                fieldLock.name);
            result.addTrustedPromise(fieldLock.lockDecl);
          }

          if (isPrimitive) {
            result.addSupportingInformation(
                varDecl, Messages.PRIMITIVE_TYPE, typeString);
          } else if (isThreadSafe) {
            result.addSupportingInformation(
                varDecl, Messages.DECLARED_TYPE_IS_THREAD_SAFE, typeString);
            for (final PromiseDrop<? extends IAASTRootNode> p : tsDrops) {
              result.addTrustedPromise(p);
            }
            if (usingImplDrop) {
              result.addSupportingInformation(
                  varDecl, Messages.THREAD_SAFE_IMPL);
            }
          } else { // contained
            result.addSupportingInformation(
                varDecl, Messages.DECLARED_TYPE_IS_CONTAINABLE, typeString);
            for (final PromiseDrop<? extends IAASTRootNode> p : cTester.getPromises()) {
              result.addTrustedPromise(p);
            }
            result.addTrustedPromise(uDrop.getDrop());
            for (final IRegion destRegion : aggMap.values()) {
              result.addTrustedPromise(getLockForRegion(destRegion).lockDecl);
            }
          }
        } else {
          final ResultDropBuilder result = createResultBuilder(
              varDecl, false, Messages.UNSAFE_REFERENCE, id);
          // type could be a non-declared, non-primitive type,
          // that is, an array
          if (testedType) {
            result.addSupportingInformation(varDecl,
                Messages.DECLARED_TYPE_IS_NOT_THREAD_SAFE,
                typeString);
            for (final IRNode n : notThreadSafe) {
              result.addProposal(new ProposedPromiseBuilder(
                  "ThreadSafe", null, n, varDecl, Origin.MODEL));
            }
            for (final IRNode n : cTester.getFailed()) {
              result.addProposal(new ProposedPromiseBuilder(
                  "Containable", null, n, varDecl, Origin.MODEL));
            }
          }

          if (isContainable) {
            result.addSupportingInformation(varDecl,
                Messages.DECLARED_TYPE_IS_CONTAINABLE,
                typeString);
            if (!isContained) {
              result.addSupportingInformation(varDecl,
                  Messages.NOT_AGGREGATED);
            }
          } else {
            result.addSupportingInformation(varDecl,
                Messages.DECLARED_TYPE_NOT_CONTAINABLE,
                typeString);
          }

          if (uDrop == null) {
            result.addProposal(new ProposedPromiseBuilder(
                "Unique", null, varDecl, varDecl,
                Origin.MODEL));
          }
        }
      } else {
        createResultBuilder(varDecl, false, Messages.UNSAFE_FIELD, id);
      }
    }
  }
}
