package com.surelogic.analysis.locks;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.aast.promise.RegionMappingNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.alias.TypeBasedMayAlias;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.RegionRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaDeclaredType;
import edu.cmu.cs.fluid.java.bind.IJavaPrimitiveType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.JavaTypeFactory;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.DropPredicateFactory;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.Sea;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.BorrowedPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ContainablePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ExplicitUniqueInRegionPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.ImmutablePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.drops.promises.ThreadSafePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.UniquePromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.VouchFieldIsPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ProposedPromiseBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;

public class LockAnalysis extends AbstractAnalysisSharingAnalysis<BindingContextAnalysis,LockVisitor,LockAnalysis.Pair> {	
  /** Should we try to run things in parallel */
  private static boolean wantToRunInParallel = false;
  
  /**
   * Are we actually going to run things in parallel?  Not all JRE have the
   * libraries we need to actually run in parallel.
   */
  private static boolean willRunInParallel = wantToRunInParallel && !singleThreaded;
  
  /**
   * Use a work queue?  Only relevant if {@link #willRunInParallel} is 
   * <code>true</code>.  Otherwise it is <code>false</code>.
   */
	private static boolean queueWork = willRunInParallel && true;

  /**
   * Analyze compilation units in parallel?  Only relevant if {@link #willRunInParallel} is 
   * <code>true</code> and {@link #queueWork} is <code>true</code>.  Otherwise it is <code>false</code>.
   * When relevant, a <code>false</code> value means analyze by types, a
   * smaller granularity than compilation units.
   */
	private static boolean byCompUnit = queueWork && true; // otherwise by type
	
	
	
	private final AtomicReference<GlobalLockModel> lockModelHandle = 
		new AtomicReference<GlobalLockModel>(null);
	
	
	
	public LockAnalysis() {
		super(willRunInParallel, queueWork ? Pair.class : null, "LockAssurance", BindingContextAnalysis.factory);
		if (runInParallel()) {
			setWorkProcedure(new Procedure<Pair>() {
				public void op(Pair n) {
					if (byCompUnit) {
						//System.out.println("Parallel Lock: "+JavaNames.genPrimaryTypeName(n));
					  final TopLevelAnalysisVisitor topLevel = 
					    new TopLevelAnalysisVisitor(
					        new ClassProcessor(getAnalysis(), getResultDependUponDrop()));
					  // actually n.typeDecl is a CompilationUnit here!
						topLevel.doAccept(n.typeDecl);	
					} else {
						//System.out.println("Parallel Lock: "+JavaNames.getRelativeTypeName(n));
					  actuallyAnalyzeClassBody(
					      getAnalysis(),getResultDependUponDrop(),
					      n.typeDecl, n.classBody);
					}
				}
			});
		}      
	}
	
	
	private final void actuallyAnalyzeClassBody(
	    final LockVisitor lv, final Drop rd, 
	    final IRNode typeDecl, final IRNode classBody) {
	  lv.analyzeClass(classBody, rd);
	  
    final ThreadSafePromiseDrop threadSafeDrop =
      LockRules.getThreadSafeImplementation(typeDecl);
    // If null, assume it's not meant to be thread safe
    if (threadSafeDrop != null) {
      new ThreadSafeVisitor(typeDecl, threadSafeDrop).doAccept(classBody);
    }
    
    final ContainablePromiseDrop containableDrop = 
      LockRules.getContainableImplementation(typeDecl);
    // no @Containable annotation --> Default "annotation" of not containable
    // Also check for verify=false
    if (containableDrop != null && containableDrop.verify()) {
      new ContainableVisitor(typeDecl, containableDrop).doAccept(classBody);
    }

    final ImmutablePromiseDrop immutableDrop = 
      LockRules.getImmutableImplementation(typeDecl);
    // no @Immutable annotation --> Default "annotation" of mutable
    // Also check for verify=false
    if (immutableDrop != null && immutableDrop.verify()) {
      new ImmutableVisitor(typeDecl, immutableDrop).doAccept(classBody);
    }
	}
	
	
	@Override
	public void init(IIRAnalysisEnvironment env) {
		super.init(env);
		env.ensureClassIsLoaded(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_LOCK);
		env.ensureClassIsLoaded(LockUtils.JAVA_UTIL_CONCURRENT_LOCKS_READWRITELOCK);
	}
	
	@Override
	public void startAnalyzeBegin(IIRProject p, IBinder binder) {
		super.startAnalyzeBegin(p, binder);
		
		// Initialize the global lock model
		final GlobalLockModel globalLockModel = new GlobalLockModel(binder);
		LockModel.purgeUnusedLocks();
		
		/*
		 * This seems stupid to me. I feel like I should be able to get the
		 * LockModel object from the LockDeclarationDrop and
		 * PromiseLockDeclarationDrop objects. I shouldn't have to use the lock
		 * name as an intermediary. But the as far as I can tell, there is no
		 * back link from the drop to the LockModel.
		 */

		// Run through the LockModel and add them to the GlobalLockModel
		final Set<? extends LockModel> lockModelDrops = Sea.getDefault()
				.getDropsOfType(LockModel.class);
		for (LockModel lockDrop : lockModelDrops) {
			final IRNode classDecl = lockDrop.getNode();

			if (classDecl == null) {
				LOG.severe("TODO invalidate dependent drops");
				lockDrop.invalidate();
				continue;
			}
			if (lockDrop.getAST() == null) {
				LOG.warning("No AST for " + lockDrop.getMessage());
				continue;
			}						
			if (lockDrop.getAST() instanceof LockDeclarationNode) {
				if (!lockDrop.hasMatchingDependents(DropPredicateFactory.matchExactType(RegionModel.class))) {
					// This is not really valid, but properly invalidated due to the inversion of dependencies
					// between the LockModel and RegionModel (for UI purposes)
					continue;
				}
				globalLockModel.addRegionLockDeclaration(binder, lockDrop,
						JavaTypeFactory.getMyThisType(classDecl));
			} else {
				globalLockModel.addPolicyLockDeclaration(binder, lockDrop,
						JavaTypeFactory.getMyThisType(classDecl));
			}
		}

		// Share the new global lock model with the lock visitor, and other
		// helpers
		lockModelHandle.set(globalLockModel);
	}

	@Override
	protected boolean flushAnalysis() {
		return true;
	}
	
	@Override
	protected LockVisitor constructIRAnalysis(IBinder binder) {		
	  if (binder == null || binder.getTypeEnvironment() == null) {
		  return null;
	  }
    return new LockVisitor(this, binder, new Effects(binder),
        new TypeBasedMayAlias(binder), getSharedAnalysis(), lockModelHandle);
	}
	
	@Override
	protected void clearCaches() {
		if (!runInParallel()) {
			LockVisitor lv = getAnalysis();
			if (lv != null) {
				lv.clearCaches();
			}
		} else {
			analyses.clearCaches();
		}
		super.clearCaches();
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
		if (byCompUnit) {
			boolean flushed = queueWork(new Pair(compUnit, null));
			if (flushed) {
				JavaComponentFactory.clearCache();
			}
			return true;
		}
		// FIX factor out?
		final ClassProcessor cp = new ClassProcessor(getAnalysis(), getResultDependUponDrop());
		new TopLevelAnalysisVisitor(cp).doAccept(compUnit);
		if (runInParallel()) {
			if (queueWork) {
        boolean flushed = queueWork(cp.getTypeBodies());
				if (flushed) {
					JavaComponentFactory.clearCache();
				}
			} else {
        runInParallel(Pair.class, cp.getTypeBodies(), getWorkProcedure());
			}
		}
		return true;
	}
	
	@Override
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
		finishBuild();
		return super.analyzeEnd(env, p);
	}
	
	@Override
	public void postAnalysis(IIRProject p) {		
		super.postAnalysis(p);
		/* Have to do this afterwards, because postAnalysis can cause
		 * a LockVisitor to be created---which seems wrong---in the
		 * non parallel case.
		 */
    lockModelHandle.set(null);
	}
	
	
	
	protected final class Pair {
	  public final IRNode typeDecl;
	  public final IRNode classBody;
	  
	  public Pair(final IRNode td, final IRNode cb) {
	    typeDecl = td;
	    classBody = cb;
	  }
	}
	
	
	
	private final class ClassProcessor extends TopLevelAnalysisVisitor.SimpleClassProcessor {
    private final LockVisitor lockVisitor;
    private final Drop resultsDependUpon;
    private final List<Pair> types = new ArrayList<Pair>();
    
    public ClassProcessor(final LockVisitor lv, final Drop rd) {
      lockVisitor = lv;
      resultsDependUpon = rd;
    }

    public Collection<Pair> getTypeBodies() {
      return types;
    }
    
    @Override
    protected void visitTypeDecl(final IRNode typeDecl, final IRNode classBody) {
      if (runInParallel() && !byCompUnit) {
        types.add(new Pair(typeDecl, classBody));
      } else {
        actuallyAnalyzeClassBody(
            lockVisitor, resultsDependUpon, typeDecl, classBody);
      }
    }
	}



  private final class ThreadSafeVisitor extends JavaSemanticsVisitor {
    private final Set<IRNode> varDecls = new HashSet<IRNode>();
    private final PromiseDrop<? extends IAASTRootNode> threadSafeDrop;
    private final Set<RegionLockRecord> lockDeclarations;
    
    public ThreadSafeVisitor(
        final IRNode classDecl, final PromiseDrop<? extends IAASTRootNode> tsDrop) {
      super(classDecl, false);
      threadSafeDrop = tsDrop;
      lockDeclarations = lockModelHandle.get().getRegionLocksInClass(
          JavaTypeFactory.getMyThisType(classDecl));
    }

    
    
    private RegionLockRecord getLockForRegion(final IRegion r) {
      for (final RegionLockRecord lr : lockDeclarations) {
        if (lr.region.ancestorOf(r)) {
          return lr;
        }
      }
      return null;
    }
    
    
    
    private final ResultDropBuilder createResult(
        final IRNode varDecl, final boolean isConsistent, 
        final int msg, final Object... args) {
      final ResultDropBuilder result =
        ResultDropBuilder.create(LockAnalysis.this, Messages.toString(msg));
      setResultDependUponDrop(result, varDecl);
      result.addCheckedPromise(threadSafeDrop);
      result.setConsistent(isConsistent);
      result.setResultMessage(msg, args);
      return result;
    }
    
    
    
    @Override
    protected void handleFieldInitialization(
        final IRNode varDecl, final boolean isStatic) {
      /*
       * Field needs to be:
       * (1) Volatile and thread safe
       * (2) Final and thread safe
       * (3) Protected by a lock and thread safe
       * 
       * Where "thread safe" means
       * (1) The declared type of the field is primitive
       * (2) The declared type of the field is annotated @ThreadSafe
       * (3) The declared type of the field is annotated @Containable and the
       *     field is also annotated @Unique, and the referenced object is
       *     aggregated into lock-protected regions. 
       */
      /* Make sure we only visit each variable declaration once.  This is a 
       * stupid way of doing this, but it's good enough for now.  SHould make 
       * a new visitor type probably.
       * 
       * XXX: Why might we get visited more than once?  Must have happened or
       * I wouldn't have added this, but I don't remember how this happens.
       */
      if (varDecls.add(varDecl)) {
        final String id = VariableDeclarator.getId(varDecl);
        
        /* First check if the field is volatile, final, or lock-protected */
        final boolean isFinal = TypeUtil.isFinal(varDecl);
        final boolean isVolatile = TypeUtil.isVolatile(varDecl);
        final RegionLockRecord fieldLock = getLockForRegion(RegionModel.getInstance(varDecl));
        
        if (isFinal || isVolatile || fieldLock != null) {
          /* Now check if the referenced object is thread safe */
          final IJavaType type = getBinder().getJavaType(varDecl);
          final IRNode typeDecl;
          final boolean isPrimitive = type instanceof IJavaPrimitiveType;
          final ThreadSafePromiseDrop declTSDrop;
          final ContainablePromiseDrop declContainableDrop;
          if (type instanceof IJavaDeclaredType) {
            typeDecl = ((IJavaDeclaredType) type).getDeclaration();
            // Null if no @ThreadSafe ==> not thread safe
            declTSDrop = LockRules.getThreadSafeType(typeDecl);
            // Null if no @Containable ==> Default annotation of not containable
            declContainableDrop = LockRules.getContainableType(typeDecl);
          } else {
            typeDecl = null;
            declTSDrop = null;
            declContainableDrop = null;
          }
          
          /* @ThreadSafe takes priority over @Containable: If the type is
           * threadsafe don't check the aggregation status
           */
          final UniquePromiseDrop uDrop;
          final ExplicitUniqueInRegionPromiseDrop aggDrop;
          boolean isContained = false;
          if (declTSDrop == null && declContainableDrop != null) {
            uDrop = UniquenessRules.getUniqueDrop(varDecl);
            aggDrop = RegionRules.getAggregate(varDecl);
            if (uDrop != null && aggDrop != null) {
              isContained = true;
              for (final RegionMappingNode mapping : aggDrop.getAST().getMapping().getMappingList()) {
                final IRegion destRegion = mapping.getTo().resolveBinding().getRegion();
                isContained &= (getLockForRegion(destRegion) != null);
              }
            }
          } else {
            uDrop = null;
            aggDrop = null;
            // no @Containable annotation --> Default "annotation" of not containable
            isContained = false;
          }
          
          if (isPrimitive || declTSDrop != null || isContained) {
            final ResultDropBuilder result;
            if (isFinal) {
              result = createResult(varDecl, true, Messages.FINAL_AND_THREADSAFE, id);
            } else if(isVolatile) {
              result = createResult(varDecl, true, Messages.VOLATILE_AND_THREADSAFE, id);
            } else { // lock protected 
              result = createResult(varDecl, true, Messages.PROTECTED_AND_THREADSAFE, id, fieldLock.name);
              result.addTrustedPromise(fieldLock.lockDecl);
            }
            
            if (isPrimitive) {
              result.addSupportingInformation(varDecl, Messages.PRIMITIVE_TYPE);
            } else if (declTSDrop != null) {
              result.addTrustedPromise(declTSDrop);
            } else { // contained
              result.addTrustedPromise(declContainableDrop);
              result.addTrustedPromise(uDrop);
              result.addTrustedPromise(aggDrop);
              for (final RegionMappingNode mapping : aggDrop.getAST().getMapping().getMappingList()) {
                final IRegion destRegion = mapping.getTo().resolveBinding().getRegion();
                result.addTrustedPromise(getLockForRegion(destRegion).lockDecl);
              }
            }
          } else {
            final ResultDropBuilder result = 
              createResult(varDecl, false, Messages.UNSAFE_REFERENCE, id);
            // type could be a non-declared, non-primitive type, that is, an array
            if (typeDecl != null) {
              if (declTSDrop == null) {
                result.addProposal(new ProposedPromiseBuilder(
                    "ThreadSafe", null, typeDecl, varDecl));
              }
              if (declContainableDrop == null) {
                result.addProposal(new ProposedPromiseBuilder(
                    "Containable", null, typeDecl, varDecl));
              }
            }
            if (uDrop == null) {
              result.addProposal(new ProposedPromiseBuilder(
                  "Unique", null, varDecl, varDecl));
            }
            if (aggDrop == null) {
              result.addProposal(new ProposedPromiseBuilder(
                  "Aggregate", null, varDecl, varDecl));
            }
          }
        } else {
          createResult(varDecl, false, Messages.UNSAFE_FIELD, id);
        }
      }
    }
  }
  


  private final class ContainableVisitor extends JavaSemanticsVisitor {
    private final Set<IRNode> varDecls = new HashSet<IRNode>();
    private final PromiseDrop<? extends IAASTRootNode> containableDrop;
    
    
    
    public ContainableVisitor(
        final IRNode classDecl, final PromiseDrop<? extends IAASTRootNode> cDrop) {
      super(classDecl, false);
      containableDrop = cDrop;
    }
    
    
    
    private final ResultDropBuilder createResult(
        final IRNode decl, final boolean isConsistent, 
        final int msg, final Object... args) {
      final ResultDropBuilder result =
        ResultDropBuilder.create(LockAnalysis.this, Messages.toString(msg));
      setResultDependUponDrop(result, decl);
      result.addCheckedPromise(containableDrop);
      result.setConsistent(isConsistent);
      result.setResultMessage(msg, args);
      return result;
    }
    

    
    @Override
    protected void handleConstructorDeclaration(final IRNode cdecl) {
      final IRNode rcvrDecl = JavaPromise.getReceiverNodeOrNull(cdecl);
      final BorrowedPromiseDrop bpd = UniquenessRules.getBorrowedDrop(rcvrDecl);

      final IRNode returnDecl = JavaPromise.getReturnNodeOrNull(cdecl);
      final UniquePromiseDrop upd = UniquenessRules.getUniqueDrop(returnDecl);
      
      // Prefer unique return over borrowed receiver
      final String id = JavaNames.genMethodConstructorName(cdecl);
      if (upd != null) {
        final ResultDropBuilder result =
          createResult(cdecl, true, Messages.CONSTRUCTOR_UNIQUE_RETURN, id);
        result.addTrustedPromise(upd);
      } else if (bpd != null) {
        final ResultDropBuilder result =
          createResult(cdecl, true, Messages.CONSTRUCTOR_BORROWED_RECEVIER, id);
        result.addTrustedPromise(bpd);
      } else {
        final ResultDropBuilder result =
          createResult(cdecl, false, Messages.CONSTRUCTOR_BAD, id);
        result.addProposal(
            new ProposedPromiseBuilder("Unique", "return", cdecl, cdecl));
      }
      doAcceptForChildren(cdecl);
    }

    @Override
    protected void handleMethodDeclaration(final IRNode mdecl) {
      // Must borrow the receiver if the method is not static
      if (!TypeUtil.isStatic(mdecl)) {
        final String id = JavaNames.genMethodConstructorName(mdecl);
        final IRNode rcvrDecl = JavaPromise.getReceiverNodeOrNull(mdecl);
        final BorrowedPromiseDrop bpd = UniquenessRules.getBorrowedDrop(rcvrDecl);
        if (bpd == null) {
          final ResultDropBuilder result =
            createResult(mdecl, false, Messages.METHOD_BAD, id);
          result.addProposal(
              new ProposedPromiseBuilder("Borrowed", "this", mdecl, mdecl));
        } else {
          final ResultDropBuilder result =
            createResult(mdecl, true, Messages.METHOD_BORROWED_RECEIVER, id);
          result.addTrustedPromise(bpd);
        }
      }
      doAcceptForChildren(mdecl);
    }
    
    
    
    @Override
    protected void handleFieldInitialization(
        final IRNode varDecl, final boolean isStatic) {
      /* Make sure we only visit each variable declaration once.  This is a 
       * stupid way of doing this, but it's good enough for now.  SHould make 
       * a new visitor type probably.
       * 
       * XXX: Why might we get visited more than once?  Must have happened or
       * I wouldn't have added this, but I don't remember how this happens.
       */
      if (varDecls.add(varDecl)) {
        final String id = VariableDeclarator.getId(varDecl);
        final IJavaType type = getBinder().getJavaType(varDecl);
        final boolean isPrimitive = type instanceof IJavaPrimitiveType;
        if (isPrimitive) {
          createResult(varDecl, true, Messages.FIELD_CONTAINED_PRIMITIVE, id);
        } else {
          final VouchFieldIsPromiseDrop vouchDrop = LockRules.getVouchFieldIs(varDecl);
          if (vouchDrop != null && vouchDrop.isContainable()) {
            final ResultDropBuilder result =
              createResult(varDecl, true, Messages.FIELD_CONTAINED_VOUCHED, id);
            result.addTrustedPromise(vouchDrop);
          } else {
            final UniquePromiseDrop uniqueDrop = UniquenessRules.getUniqueDrop(varDecl);
            final ExplicitUniqueInRegionPromiseDrop aggDrop = RegionRules.getAggregate(varDecl);
            final IRNode typeDecl = (type instanceof IJavaDeclaredType) ? ((IJavaDeclaredType) type).getDeclaration() : null;
            // no @Containable annotation --> Default "annotation" of not containable
            final ContainablePromiseDrop declContainableDrop = LockRules.getContainableType(typeDecl);
            if (declContainableDrop != null && uniqueDrop != null && aggDrop != null) {
              final ResultDropBuilder result =
                createResult(varDecl, true, Messages.FIELD_CONTAINED_OBJECT, id);
              result.addTrustedPromise(declContainableDrop);
              result.addTrustedPromise(uniqueDrop);
              result.addTrustedPromise(aggDrop);
            } else {
              final ResultDropBuilder result =
                createResult(varDecl, false, Messages.FIELD_BAD, id);
              
              // Always suggest @Vouch("Containable")
              result.addProposal(new ProposedPromiseBuilder(
                      "Vouch", "Containable", varDecl, varDecl));
              
              if (declContainableDrop != null) {
                result.addTrustedPromise(declContainableDrop);              
              } else {
                // no @Containable annotation --> Default "annotation" of not containable
                result.addSupportingInformation(varDecl, Messages.FIELD_NOT_CONTAINABLE);
                if (type instanceof IJavaDeclaredType) {
                  result.addProposal(new ProposedPromiseBuilder(
                      "Containable", null, typeDecl, varDecl));
                }
              }
  
              if (uniqueDrop != null) {
                result.addTrustedPromise(uniqueDrop);
              } else {
                result.addSupportingInformation(varDecl, Messages.FIELD_NOT_UNIQUE);
                result.addProposal(new ProposedPromiseBuilder("Unique", null, varDecl, varDecl));
                result.addProposal(new ProposedPromiseBuilder("Aggregate", null, varDecl, varDecl));
              }
              
              if (aggDrop != null) {
                result.addTrustedPromise(aggDrop);
              } else { 
                result.addSupportingInformation(varDecl, Messages.FIELD_NOT_AGGREGATED);
                result.addProposal(new ProposedPromiseBuilder("Aggregate", null, varDecl, varDecl));
              }
            }
          } 
        }
      }
    }
  }
  
  
  
  private final class ImmutableVisitor extends JavaSemanticsVisitor {
    private final Set<IRNode> varDecls = new HashSet<IRNode>();
    private final PromiseDrop<? extends IAASTRootNode> immutableDrop;
    
    
    
    public ImmutableVisitor(
        final IRNode classDecl, final PromiseDrop<? extends IAASTRootNode> cDrop) {
      super(classDecl, false);
      immutableDrop = cDrop;
    }
    
    
    
    private final ResultDropBuilder createResult(
        final IRNode decl, final boolean isConsistent, 
        final int msg, final Object... args) {
      final ResultDropBuilder result =
        ResultDropBuilder.create(LockAnalysis.this, Messages.toString(msg));
      setResultDependUponDrop(result, decl);
      result.addCheckedPromise(immutableDrop);
      result.setConsistent(isConsistent);
      result.setResultMessage(msg, args);
      return result;
    }


    
    @Override
    protected void handleFieldInitialization(
        final IRNode varDecl, final boolean isStatic) {
      /* Make sure we only visit each variable declaration once.  This is a 
       * stupid way of doing this, but it's good enough for now.  SHould make 
       * a new visitor type probably.
       * 
       * XXX: Why might we get visited more than once?  Must have happened or
       * I wouldn't have added this, but I don't remember how this happens.
       */
      if (varDecls.add(varDecl)) {
        /* (1) Field must be final.
         * (2) non-primitive fields must be @Immutable
         * or
         * Vouched for @Vouch("Immutable")
         */
        final String id = VariableDeclarator.getId(varDecl);

        final VouchFieldIsPromiseDrop vouchDrop = LockRules.getVouchFieldIs(varDecl);
        if (vouchDrop != null && vouchDrop.isImmutable()) {
          // VOUCHED OR
          final ResultDropBuilder result =
            createResult(varDecl, true, Messages.IMMUTABLE_VOUCHED, id);
          result.addTrustedPromise(vouchDrop);
        } else {
          final boolean isFinal = TypeUtil.isFinal(varDecl);
          final IJavaType type = getBinder().getJavaType(varDecl);
          final boolean isPrimitive = (type instanceof IJavaPrimitiveType);
          ResultDropBuilder result = null;
          boolean proposeVouch = false;
          
          if (isPrimitive) {
            // PRIMITIVELY TYPED
            if (isFinal) {
              result = createResult(varDecl, true, Messages.IMMUTABLE_FINAL_PRIMITIVE, id);
            } else {
              result = createResult(varDecl, false, Messages.IMMUTABLE_NOT_FINAL, id);
              proposeVouch = true;
            }
          } else {
            // REFERENCE-TYPED
            final IRNode typeDecl = (type instanceof IJavaDeclaredType) ? ((IJavaDeclaredType) type).getDeclaration() : null;
            // no @Immutable annotation --> Default "annotation" of mutable
            final ImmutablePromiseDrop declImmutableDrop = LockRules.getImmutableType(typeDecl);
            
            if (declImmutableDrop != null) {
              // IMMUTABLE REFERENCE TYPE
              if (isFinal) {
                result = createResult(varDecl, true, Messages.IMMUTABLE_FINAL_IMMUTABLE, id);
                result.addTrustedPromise(declImmutableDrop);
              } else {
                result = createResult(varDecl, false, Messages.IMMUTABLE_NOT_FINAL, id);
                result.addTrustedPromise(declImmutableDrop);
                proposeVouch = true;
              }
            } else {
              // MUTABLE REFERENCE TYPE
              proposeVouch = true;
              if (isFinal) {
                result = createResult(varDecl, false, Messages.IMMUTABLE_FINAL_NOT_IMMUTABLE, id);
              } else {
                result = createResult(varDecl, false, Messages.IMMUTABLE_NOT_FINAL_NOT_IMMUTABLE, id);
              }
              if (typeDecl != null) {
            	  result.addProposal(new ProposedPromiseBuilder(
            			  "Immutable", null, typeDecl, varDecl));
              } else {
            	  // TODO what if this is a type formal, or something else?
              }
            }
          }
          
          if (proposeVouch) {
            result.addProposal(new ProposedPromiseBuilder(
                "Vouch", "Immutable", varDecl, varDecl));
          }
        }
      }
    }
  }  
}
