package com.surelogic.analysis.uniqueness.classic.sideeffecting;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.granules.AbstractGranulator;
import com.surelogic.analysis.granules.GranuleInType;
import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.analysis.uniqueness.classic.sideeffecting.store.StoreLattice;
import com.surelogic.analysis.visitors.JavaSemanticsVisitor;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.common.concurrent.Procedure;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.ResultFolderDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.IUniquePromise;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquePromiseDrop;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.Parameters;
import edu.cmu.cs.fluid.java.operator.SomeFunctionDeclaration;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.uwm.cs.fluid.control.FlowAnalysis;

public class UniquenessAnalysisModule extends AbstractAnalysisSharingAnalysis<BindingContextAnalysis, UniquenessAnalysis, UniquenessAnalysisModule.MethodRecord> {
  private static final long NANO_SECONDS_PER_SECOND = 1000000000L;

  
  
  private final Map<PromiseDrop<? extends IAASTRootNode>, Set<ResultFolderDrop>> uniqueDropsToUses = 
    new HashMap<PromiseDrop<? extends IAASTRootNode>, Set<ResultFolderDrop>>();
  
  
  
  public UniquenessAnalysisModule() {
		super(true , "UniqueAnalysis", BindingContextAnalysis.factory);
	}


  
	@Override
	protected UniquenessAnalysis constructIRAnalysis(IBinder binder) {
    final boolean shouldTimeOut = IDE.getInstance().getBooleanPreference(
        IDEPreferences.TIMEOUT_FLAG);
		return new UniquenessAnalysis(
		    this, binder,	shouldTimeOut, getSharedAnalysis());
	}
	
	@Override
	protected void clearCaches() {
	  uniqueDropsToUses.clear();
    if (runInParallel() != ConcurrencyType.INTERNALLY) {
      getAnalysis().clearCaches();
    } else {
      analyses.clearCaches();
    }
	}
	
	@Override
	protected boolean doAnalysisOnAFile(IIRAnalysisEnvironment env, CUDrop cud, final IRNode compUnit) {
		return checkUniquenessForFile(compUnit, env.getMonitor());
	}

	@Override
	public Iterable<MethodRecord> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
    /* Link control flow PROMISE drops to unique fields.  We do this here,
     * and not in StoreLattice.makeResultDrops() because we only want one
     * result drop between the unique promise drop and the control flow drops.
     * If we did it in the lattice, we would have to make one result drop for 
     * each control flow drop, and that would be dumb.
     */
    for (final Map.Entry<PromiseDrop<? extends IAASTRootNode>, Set<ResultFolderDrop>> entry : uniqueDropsToUses.entrySet()) {
      final ResultDrop middleDrop = new ResultDrop(entry.getKey().getNode());
      middleDrop.addChecked(entry.getKey());
      middleDrop.setConsistent();
      middleDrop.setMessage(Messages.CONTROL_FLOW_ROOT, p.getName());
      for (final ResultFolderDrop cfDrop : entry.getValue()) {
        middleDrop.addTrusted(cfDrop);
      }
    }
    
    // Create the drops from the drop builders
    finishBuild();
		
		// FIX only clearing some of the threads?
		if (getAnalysis() != null) {
			getAnalysis().clear();
		}
		return Collections.emptyList();
	}

	protected boolean checkUniquenessForFile(IRNode compUnit, final IAnalysisMonitor monitor) {
		try {
			final Set<MethodRecord> methods = shouldAnalyzeCompilationUnit(compUnit);

      if (runInParallel() == ConcurrencyType.INTERNALLY) {
        runInParallel(MethodRecord.class, methods, new Procedure<MethodRecord>() {
          @Override
          public void op(MethodRecord mr) {
            final String methodName = JavaNames.genRelativeFunctionName(mr.getMethod());
            if (monitor != null) {
              monitor.subTask("Checking [ Uniqueness Assurance ] " + methodName, false);
            }
            analzyePseudoMethodDeclaration(mr); 
            if (monitor != null) {
              monitor.subTaskDone(0);
            }
            ImmutableHashOrderSet.clearCaches();
          }
        });
        return !methods.isEmpty();
      } else {
        // Analyze the given nodes
        for (MethodRecord mr : methods) {
          final String methodName = JavaNames.genQualifiedMethodConstructorName(mr.getMethod());
          if (monitor != null) {
            monitor.subTask("Checking [ Uniqueness Assurance ] " + methodName, false);
          }
          System.out.println("Sequential: " + methodName);
          analzyePseudoMethodDeclaration(mr);
          if (monitor != null) {
              monitor.subTaskDone(0);
          }
        }
        ImmutableHashOrderSet.clearCaches();
        return !methods.isEmpty();
      }
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Exception in unique assurance", e); //$NON-NLS-1$
		}
		return false;
	}

	final IAnalysisGranulator<MethodRecord> granulator = new AbstractGranulator<MethodRecord>(MethodRecord.class) {
		@Override
		protected void extractGranules(List<MethodRecord> granules,	ITypeEnvironment tEnv, IRNode cu) {
			granules.addAll(shouldAnalyzeCompilationUnit(tEnv.getBinder(), cu));
		}
	};
	
	@Override
	public IAnalysisGranulator<MethodRecord> getGranulator() {
		return granulator;
	}

	@Override
	protected boolean doAnalysisOnGranule_wrapped(IIRAnalysisEnvironment env, MethodRecord mr) {
		analzyePseudoMethodDeclaration(mr);
		return true;
	}
	
	/**
	 * @param decl A constructor declaration, method declaration, init declaration,
	 * or class init declaration node.
	 */
	void analzyePseudoMethodDeclaration(final MethodRecord mr) {
	  /* Invoking getResultFor() on the query will kick off the control-flow
	   * analysis of the method.  We use the results of the query later on
	   * to link the control-flow result drop of this method to the 
	   * unique fields read by the method body.
	   */
	  StoreLattice sl = null;
    final String methodName = JavaNames.genQualifiedMethodConstructorName(mr.getMethod());
    // Prepare for 'too long' warning
    final long tooLongDuration = IDE.getInstance().getIntPreference(
        IDEPreferences.TIMEOUT_WARNING_SEC) * NANO_SECONDS_PER_SECOND;
    final long startTime = System.nanoTime();
	  try {
  	  // Get the analysis object, which triggers the control flow analysis
      sl = getAnalysis().getAnalysis(mr.getMethod()).getLattice();
      
      // Did we take too long?
      final long endTime = System.nanoTime();
      final long duration = endTime - startTime;
      if (duration > tooLongDuration) {
        sl.getCFDrop().addWarningHintWithCategory(mr.getMethod(), Messages.DSC_UNIQUENESS_LONG_RUNNING,
            Messages.TOO_LONG, tooLongDuration / NANO_SECONDS_PER_SECOND,
            methodName, duration / NANO_SECONDS_PER_SECOND);
      }
	  } catch (final FlowAnalysis.AnalysisGaveUp e) { // Analysis self-aborted
      final long endTime = System.nanoTime();
      final long duration = endTime - startTime;
      sl = ((UniquenessAnalysis.Uniqueness) e.fa).getLattice();
      
      // kill any partial results
      sl.cancelResults();
      
      /* Add "time out" result to (1) the flow unit's control flow drop, 
       * (2) Borrowed promises of the method's
       * parameters, and (3) Unique promise on the method's return node,
       */
      final ResultDrop timeOutResult = new ResultDrop(mr.getMethod());
      timeOutResult.setTimeout();
      timeOutResult.setCategorizingMessage(Messages.DSC_UNIQUENESS_TIMEOUT);
      timeOutResult.setMessage(Messages.TIMEOUT,
          e.timeOut / NANO_SECONDS_PER_SECOND,
          methodName, duration / NANO_SECONDS_PER_SECOND);
      
      // (1)
      sl.getCFDrop().addTrusted(timeOutResult);
      
      if (!ClassInitDeclaration.prototype.includes(mr.getMethod())) {
        final IRNode formals = SomeFunctionDeclaration.getParams(mr.getMethod());
        for (final IRNode p : Parameters.getFormalIterator(formals)) {
          final BorrowedPromiseDrop pd = UniquenessRules.getBorrowed(p);
          if (pd != null) timeOutResult.addChecked(pd);
        }
        
        final IRNode rcvr = JavaPromise.getReceiverNodeOrNull(mr.getMethod());
        if (rcvr != null) {
          final BorrowedPromiseDrop pd = UniquenessRules.getBorrowed(rcvr);
          if (pd != null) timeOutResult.addChecked(pd);
        }      
        
        final IRNode ret = JavaPromise.getReturnNodeOrNull(mr.getMethod());
        if (ret != null) {
          /* Covers both Unique return from a method and Unique return as
           * borrowed receiver for a constructor. 
           */
          final UniquePromiseDrop pd = UniquenessRules.getUnique(ret);
          if (pd != null) timeOutResult.addChecked(pd);
        }
      }
	  }
	  
	  /* Add the control flow drop to the set of users for each unique field 
	   * accessed in the method.
	   */
    for (final PromiseDrop<? extends IAASTRootNode> pd : mr.usedUniqueFields) {
      Set<ResultFolderDrop> value = uniqueDropsToUses.get(pd);
      if (value == null) {
        value = new HashSet<ResultFolderDrop>();
        uniqueDropsToUses.put(pd, value);
      }
      value.add(sl.getCFDrop());
    }
	}

	/**
   * Returns the method/constructor/initializer declarations contained in the
   * given compilation that need to be analyzed because they contain structures
   * that depend on unique annotations.
   * 
   * @param compUnit
   *          A compilation unit node.
   * @return A set of method/constructor declarations. May include
   *         InitDeclaration nodes in the case of anonymous class expressions
   *         and ClassInitDeclaration nodes in the case of static initializers.
   */
	Set<MethodRecord> shouldAnalyzeCompilationUnit(final IRNode compUnit) {
		return shouldAnalyzeCompilationUnit(getBinder(), compUnit);
	}
	
	Set<MethodRecord> shouldAnalyzeCompilationUnit(IBinder binder, final IRNode compUnit) {
    final NewShouldAnalyzeVisitor visitor = new NewShouldAnalyzeVisitor(binder);
	  visitor.doAccept(compUnit);
	  return visitor.getResults();
	}

	static final class MethodRecord extends GranuleInType {
	  private final IRNode methodDecl;
		public final Set<PromiseDrop<? extends IAASTRootNode>> usedUniqueFields;

		public MethodRecord(final IRNode m) {
			super(VisitUtil.getEnclosingType(m));
			methodDecl = m;			
			usedUniqueFields = new HashSet<PromiseDrop<? extends IAASTRootNode>>();
		}

		public void addField(final PromiseDrop<? extends IAASTRootNode> uDrop) {
			usedUniqueFields.add(uDrop);
		}

	  public IRNode getMethod() {
	    return methodDecl;
	  }
	  
	  @Override
	  public IRNode getNode() {
	    return methodDecl;
	  }
	  
	  @Override
	  public String getLabel() {
	    return JavaNames.getFullName(methodDecl);
	  }
	  
	  public IRNode getClassBody() {
	    return VisitUtil.getClassBody(typeDecl);
	  }

	  @Override
	  public boolean equals(final Object other) {
	    if (other instanceof MethodRecord) {
	      final MethodRecord tan = (MethodRecord) other;
	      return typeDecl == tan.typeDecl && methodDecl == tan.methodDecl;
	    } else {
	      return false;
	    }
	  }

	  @Override
	  public int hashCode() {
	    int result = 17;
	    result = 31 * result + typeDecl.hashCode();
	    if (methodDecl != null) {
	      result = 31 * result + methodDecl.hashCode();
	    }
	    return result;
	  }
	}
	
	
	
  /*
   * We are searching for (1) declarations of a unique fields, (2) uses of
   * unique fields, (3) methods that have borrowed parameters or a unique return
   * values, or (4) invocations of a method that has unique parameter
   * requirements.
   */
	private static final class NewShouldAnalyzeVisitor extends JavaSemanticsVisitor {
    private final IBinder binder;
    
    /**
     * The output of the visitation: the set of method/constructor declarations
     * that should receive additional scrutiny by the uniqueness analysis.
     */
    private final Set<MethodRecord> results = new HashSet<MethodRecord>();
    
    private final Map<IRNode, MethodRecord> map = new HashMap<IRNode, MethodRecord>();
    
    
    
    public NewShouldAnalyzeVisitor(final IBinder binder) {
      super(VisitInsideTypes.YES, SkipAnnotations.NO);
      this.binder = binder;
    }

    
    
    public Set<MethodRecord> getResults() {
      return results;
    }
    
    private MethodRecord addMethod(final IRNode mdecl) {
      MethodRecord mr = map.get(mdecl);
      if (mr == null) {
        mr = new MethodRecord(mdecl);
        results.add(mr);
        map.put(mdecl, mr);
      }
      return mr;
    }
    
    private void addField(
        final IRNode mdecl, final PromiseDrop<? extends IAASTRootNode> uDrop) {
      final MethodRecord mr = addMethod(mdecl);
      mr.addField(uDrop);
    }
    
    
    // Don't go inside annotations, they aren't interesting, and cause problems.
    @Override
    public Void visitAnnotation(final IRNode annotation) {
      return null;
    }

    
    
    /* Case 4: invoking method with unique parameter.
     */
    @Override
    protected void handleAsMethodCall(final IRNode call) {
      final IRNode declNode = binder.getBinding(call);

      if (declNode != null) {
        final Operator declOp = JJNode.tree.getOperator(declNode);
        IRNode formals = null;
        boolean hasUnique = false;
        if (declOp instanceof ConstructorDeclaration) {
          formals = ConstructorDeclaration.getParams(declNode);
        } else if (declOp instanceof MethodDeclaration) {
          formals = MethodDeclaration.getParams(declNode);
          if (!TypeUtil.isStatic(declNode)) {
            final IRNode self = JavaPromise.getReceiverNode(declNode);
            hasUnique = UniquenessRules.isUnique(self);
          }
        }
        if (formals != null) {
          for (int i = 0; !hasUnique && (i < JJNode.tree.numChildren(formals)); i++) {
            final IRNode param = JJNode.tree.getChild(formals, i);
            hasUnique = UniquenessRules.isUnique(param);
          }
          if (hasUnique) {
            addMethod(getEnclosingDecl());
          }
        }
      }
    }

    @Override
    protected void handleConstructorDeclaration(final IRNode cdecl) {
      // Case 3b: borrowed/unique parameter
      boolean hasBorrowedParam = false;
      hasBorrowedParam |= UniquenessRules.isBorrowedReceiver(cdecl);
      // Cannot have a unique receiver

      final IRNode formals = ConstructorDeclaration.getParams(cdecl);
      for (int i = 0; i < JJNode.tree.numChildren(formals); i++) {
        final IRNode param = JJNode.tree.getChild(formals, i);
        hasBorrowedParam |= UniquenessRules.isBorrowed(param);
      }
      if (hasBorrowedParam) {
        addMethod(cdecl);
      }
            
      // Check the rest of the constructor
      doAcceptForChildren(cdecl);
    }

    @Override
    public Void visitFieldRef(final IRNode fieldRef) {
      /* Case (2): A use of a unique field. */
      final IRNode fdecl = binder.getBinding(fieldRef);
      final IUniquePromise uDrop = UniquenessUtils.getUnique(fdecl);
      if (uDrop != null) {
        addField(getEnclosingDecl(), uDrop.getDrop());
      }
      doAcceptForChildren(fieldRef);
      return null;
    }
    
    @Override
    protected void handleMethodDeclaration(final IRNode mdecl) {
      // Case 3a: returns unique
      final IRNode retDecl = JavaPromise.getReturnNodeOrNull(mdecl);
      final boolean returnsUnique =
        (retDecl == null) ? false : UniquenessRules.isUnique(retDecl);

      // Case 3b: borrowed/unique parameter
      boolean hasBorrowedParam = false;
      if (!TypeUtil.isStatic(mdecl)) { // non-static method
        final IRNode self = JavaPromise.getReceiverNode(mdecl);
        hasBorrowedParam |= UniquenessRules.isBorrowed(self);
      }
      final IRNode formals = MethodDeclaration.getParams(mdecl);
      for (int i = 0; i < JJNode.tree.numChildren(formals); i++) {
        final IRNode param = JJNode.tree.getChild(formals, i);
        hasBorrowedParam |= UniquenessRules.isBorrowed(param);
      }
      if (returnsUnique || hasBorrowedParam) {
        addMethod(mdecl);
      }
    
      doAcceptForChildren(mdecl);
    }
    
    @Override
    protected void handleFieldInitialization(final IRNode varDecl, final boolean isStatic) {
      /* CASE (1): If the field is UNIQUE then we
       * add the current enclosing declaration to the results.
       */
      final IUniquePromise uDrop = UniquenessUtils.getUnique(varDecl);
      if (uDrop != null) {
        addField(getEnclosingDecl(), uDrop.getDrop());
      }
      // analyze the the RHS of the initialization
      doAcceptForChildren(varDecl);
    }
	}
}
