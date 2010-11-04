package com.surelogic.analysis.locks;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.promise.LockDeclarationNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.uwm.BindingContextAnalysis;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.analysis.messages.Messages;
import com.surelogic.analysis.regions.IRegion;
import com.surelogic.annotation.rules.LockRules;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.analysis.TypeBasedAliasAnalysis;
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
import edu.cmu.cs.fluid.sea.drops.promises.LockModel;
import edu.cmu.cs.fluid.sea.drops.promises.RegionModel;
import edu.cmu.cs.fluid.sea.drops.promises.SelfProtectedPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;

public class LockAnalysis extends AbstractWholeIRAnalysis<LockVisitor,LockAnalysis.Pair> {	
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
	private BindingContextAnalysis bca;
	
	
	
	public LockAnalysis() {
		super(willRunInParallel, queueWork ? Pair.class : null, "LockAssurance");
		if (runInParallel()) {
			setWorkProcedure(new Procedure<Pair>() {
				public void op(Pair n) {
					if (byCompUnit) {
						//System.out.println("Parallel Lock: "+JavaNames.genPrimaryTypeName(n));
					  final TopLevelAnalysisVisitor topLevel = 
					    new TopLevelAnalysisVisitor(
					        new ClassProcessor(getAnalysis(), getResultDependUponDrop()));
					  // actually n.classBody is a CompilationUnit here!
						topLevel.doAccept(n.classBody);	
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
	  
    final SelfProtectedPromiseDrop threadSafeDrop =
      LockRules.getSelfProtectedDrop(typeDecl);
    if (threadSafeDrop != null) {
      new ThreadSafeVisitor(typeDecl, threadSafeDrop).doAccept(classBody);
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
	  if (binder == null) {
		  return null;
	  }
	  bca = new BindingContextAnalysis(binder, true);
    return new LockVisitor(this, binder, new Effects(binder),
        new TypeBasedAliasAnalysis(binder), bca, lockModelHandle);
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
		if (bca != null) {
			bca.clear();
		}
	}
	
	@Override
	public boolean doAnalysisOnAFile(CUDrop cud, final IRNode compUnit, IAnalysisMonitor monitor) {
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
	public void postAnalysis(IIRProject p) {
		finishBuild();
		
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
    private final PromiseDrop<? extends IAASTRootNode> threadSafeDrop;
    private final Set<RegionLockRecord> lockDeclarations;
    
    public ThreadSafeVisitor(
        final IRNode classDecl, final PromiseDrop<? extends IAASTRootNode> tsDrop) {
      super(classDecl, false);
      threadSafeDrop = tsDrop;
      lockDeclarations = lockModelHandle.get().getRegionLocksInClass(
          JavaTypeFactory.getMyThisType(classDecl));
    }

    
    
    private final ResultDropBuilder reportConsistency(
        final int msg, final IRNode varDecl,
        final boolean isPrimitive, final SelfProtectedPromiseDrop declTSDrop,
        final String xtraArg) {
      final ResultDropBuilder result =
        ResultDropBuilder.create(LockAnalysis.this, Messages.toString(msg));
      setResultDependUponDrop(result, varDecl);
      result.addCheckedPromise(threadSafeDrop);
      result.setConsistent();
      result.setResultMessage(msg, VariableDeclarator.getId(varDecl), xtraArg);
      if (isPrimitive) {
        result.addSupportingInformation(varDecl, Messages.PRIMITIVE_TYPE);
      } else {
        result.addTrustedPromise(declTSDrop);
      }
      return result;
    }
    
    
    
    @Override
    protected void handleFieldInitialization(
        final IRNode varDecl, final boolean isStatic) {
      final IJavaType type = getBinder().getJavaType(varDecl);
      final boolean isPrimitive = type instanceof IJavaPrimitiveType;
      final SelfProtectedPromiseDrop declTSDrop;
      if (type instanceof IJavaDeclaredType) {
        declTSDrop = LockRules.getSelfProtectedDrop(
            ((IJavaDeclaredType) type).getDeclaration());
      } else {
        declTSDrop = null;
      }
      final boolean isThreadSafe = isPrimitive || (declTSDrop != null);
      
      final boolean isFinal = TypeUtil.isFinal(varDecl);
      final boolean isVolatile = TypeUtil.isVolatile(varDecl);
      
      if (isFinal && isThreadSafe) {
        reportConsistency(
            Messages.FINAL_AND_THREADSAFE, varDecl, isPrimitive, declTSDrop, null);
      } else if (isVolatile && isThreadSafe) {
        reportConsistency(
            Messages.VOLATILE_AND_THREADSAFE, varDecl, isPrimitive, declTSDrop, null);
      } else {
        final IRegion fieldAsRegion = RegionModel.getInstance(varDecl);
        RegionLockRecord lock = null;
        for (final RegionLockRecord lr : lockDeclarations) {
          if (lr.region.ancestorOf(fieldAsRegion)) {
            lock = lr;
            break;
          }
        }
        
        if (lock != null && isThreadSafe) {
          reportConsistency(
              Messages.PROTECTED_AND_THREADSAFE, varDecl, isPrimitive,
              declTSDrop, lock.name);
        } else {
          final ResultDropBuilder result =
            ResultDropBuilder.create(
                LockAnalysis.this, Messages.toString(Messages.UNSAFE_FIELD));
          setResultDependUponDrop(result, varDecl);
          result.addCheckedPromise(threadSafeDrop);
          result.setInconsistent();
          result.setResultMessage(
              Messages.UNSAFE_FIELD, VariableDeclarator.getId(varDecl));
        }
      }      
    }
  }
  
}
