package com.surelogic.analysis.uniqueness.plusFrom.sideeffecting;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.granules.AbstractGranulator;
import com.surelogic.analysis.granules.IAnalysisGranulator;
import com.surelogic.analysis.granules.TypeAndMethod;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.UniquenessAnalysis.AbruptErrorQuery;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.UniquenessAnalysis.IsInvalidQuery;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.UniquenessAnalysis.IsPositivelyAssuredQuery;
import com.surelogic.analysis.uniqueness.plusFrom.sideeffecting.UniquenessAnalysis.NormalErrorQuery;
import com.surelogic.analysis.visitors.JavaSemanticsVisitor;
import com.surelogic.annotation.rules.LockRules;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.dropsea.ir.PromiseDrop;
import com.surelogic.dropsea.ir.ResultDrop;
import com.surelogic.dropsea.ir.drops.CUDrop;
import com.surelogic.dropsea.ir.drops.method.constraints.RegionEffectsPromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ImmutablePromiseDrop;
import com.surelogic.dropsea.ir.drops.type.constraints.ImmutableRefPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.BorrowedPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.ReadOnlyPromiseDrop;
import com.surelogic.dropsea.ir.drops.uniqueness.UniquePromiseDrop;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ide.IDEPreferences;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.JavaPromise;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaSourceRefType;
import edu.cmu.cs.fluid.java.bind.IJavaType;
import edu.cmu.cs.fluid.java.bind.ITypeEnvironment;
import edu.cmu.cs.fluid.java.operator.Arguments;
import edu.cmu.cs.fluid.java.operator.CallInterface;
import edu.cmu.cs.fluid.java.operator.ClassBody;
import edu.cmu.cs.fluid.java.operator.ClassInitializer;
import edu.cmu.cs.fluid.java.operator.ConstructorDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldDeclaration;
import edu.cmu.cs.fluid.java.operator.FieldRef;
import edu.cmu.cs.fluid.java.operator.MethodCall;
import edu.cmu.cs.fluid.java.operator.MethodDeclaration;
import edu.cmu.cs.fluid.java.operator.QualifiedThisExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarators;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.promise.ReceiverDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import extra166y.Ops.Procedure;

public class UniquenessAnalysisModule extends AbstractWholeIRAnalysis<UniquenessAnalysis,TypeAndMethod> {
  /**
   * All the method control flow result drops we create.  We scan this at the
   * end to invalidate any drops that are not used.
   */
  private final Set<ResultDrop> controlFlowDrops = 
	  Collections.synchronizedSet(new HashSet<ResultDrop>());
  
  public UniquenessAnalysisModule() {
		super(true && !singleThreaded, null, "UniqueAnalysis");
	}

	@Override
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do
	}

	@Override
	protected UniquenessAnalysis constructIRAnalysis(IBinder binder) {
    final boolean shouldTimeOut = IDE.getInstance().getBooleanPreference(
        IDEPreferences.TIMEOUT_FLAG);
    return new UniquenessAnalysis(this, binder, shouldTimeOut);
	}
	
	@Override
	protected void clearCaches() {
	  cachedControlFlow.clear();
	  controlFlowDrops.clear();
	  
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
	public Iterable<TypeAndMethod> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {		
    // Remove any control flow drops that aren't used for anything
    for (final ResultDrop cfDrop : controlFlowDrops) {
      //System.out.println("Looking at control flow drop: "+cfDrop);
      if (cfDrop.getChecked().isEmpty()) {
        cfDrop.invalidate();
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
			final Set<TypeAndMethod> methods = shouldAnalyzeCompilationUnit(compUnit);

      if (runInParallel() == ConcurrencyType.INTERNALLY) {
        runInParallel(TypeAndMethod.class, methods, new Procedure<TypeAndMethod>() {
          @Override
          public void op(TypeAndMethod node) {
            final String methodName = JavaNames.genRelativeFunctionName(node.getMethod());
            if (monitor != null) {
              monitor.subTask("Checking [ Uniqueness Assurance ] " + methodName, false);
            }
            //System.out.println("Parallel: " + methodName);
            /*
        	final DebugUnparser unparser = new DebugUnparser(10, JJNode.tree);
            String s = unparser.unparseString(node.methodDecl);
            final long start = System.currentTimeMillis();
            */
            analzyePseudoMethodDeclaration(node);
            /*
            final long end = System.currentTimeMillis();
            System.out.println("Parallel: " + methodName + " -- "+(end-start)+" ms");
            */
            if (monitor != null) {
                monitor.subTaskDone(0);
            }
            ImmutableHashOrderSet.clearCaches();
          }
        });
        return !methods.isEmpty();
      } else {
        // Analyze the given nodes
        for (Iterator<TypeAndMethod> iter = methods.iterator(); iter.hasNext();) {
          final TypeAndMethod node = iter.next();
          final String methodName = JavaNames.genQualifiedMethodConstructorName(node.getMethod());
          if (monitor != null) {
            monitor.subTask("Checking [ Uniqueness Assurance ] " + methodName, false);
          }
          System.out.println("Sequential: " + methodName);
          System.out.flush();
          analzyePseudoMethodDeclaration(node);
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

	@Override
	public IAnalysisGranulator<TypeAndMethod> getGranulator() {
		// TODO factor out for reuse?
		return new AbstractGranulator<TypeAndMethod>(TypeAndMethod.class) {
			@Override
			protected void extractGranules(List<TypeAndMethod> granules, ITypeEnvironment tEnv, IRNode cu) {
				granules.addAll(shouldAnalyzeCompilationUnit(tEnv.getBinder(), cu));
			}
		};
	}
	
	@Override
	protected boolean doAnalysisOnGranule_wrapped(IIRAnalysisEnvironment env, TypeAndMethod node) {
		analzyePseudoMethodDeclaration(node);
		return true;
	}
	
	/**
	 * @param decl A constructor declaration, method declaration, init declaration,
	 * or class init declaration node.
	 */
	private void analzyePseudoMethodDeclaration(final TypeAndMethod node) {		
		final PromiseRecord pr = createPromiseRecordFor(node);
		final Operator blockOp = JJNode.tree.getOperator(node.getMethod());
		final boolean isInit = InitDeclaration.prototype.includes(blockOp);
		final boolean isClassInit = ClassInitDeclaration.prototype.includes(blockOp);
		final boolean isConstructorDecl = ConstructorDeclaration.prototype.includes(blockOp);
		final boolean isMethodDecl = MethodDeclaration.prototype.includes(blockOp);

		try {
			/* if decl is a constructor declaration or initializer declaration, we need to
			 * scan the containing type and process the field declarations and
			 * initializer blocks.
			 */
			if (isInit || isClassInit || isConstructorDecl) {
				for (final IRNode bodyDecl : ClassBody.getDeclIterator(node.getClassBody())) {
					if (FieldDeclaration.prototype.includes(bodyDecl) || ClassInitializer.prototype.includes(bodyDecl)) {
						if (isClassInit == TypeUtil.isStatic(bodyDecl)) {
							analyzeSubtree(node.getMethod(), pr, bodyDecl);
						}
					}
				}
			}

			if (isConstructorDecl || isMethodDecl) {
				analyzeSubtree(node.getMethod(), pr, node.getMethod());
			}
		} finally {
			ImmutableHashOrderSet.cleanupCaches();
		}
	}

	private void analyzeSubtree(
			final IRNode decl, final PromiseRecord pr, final IRNode root) {
		final Iterator<IRNode> nodes = JJNode.tree.topDown(root);
		while (nodes.hasNext()) {
			final IRNode currentNode = nodes.next();
			checkMethodCall(decl, currentNode, pr);
			checkForError(decl, currentNode, pr);
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
	Set<TypeAndMethod> shouldAnalyzeCompilationUnit(final IRNode compUnit) {
		return shouldAnalyzeCompilationUnit(getBinder(), compUnit);
	}
	
	Set<TypeAndMethod> shouldAnalyzeCompilationUnit(IBinder binder, final IRNode compUnit) {
    final NewShouldAnalyzeVisitor visitor = new NewShouldAnalyzeVisitor(binder);
	  visitor.doAccept(compUnit);
	  return visitor.getResults();
	}
	
	private void checkForError(
	    final IRNode insideDecl, final IRNode node, final PromiseRecord pr) {
		if (isInvalid(insideDecl, node)) {
		  final ResultDrop cfDrop = pr.controlFlow;
		  cfDrop.setInconsistent();
		  cfDrop.addInformationHint(node, getErrorMessage(insideDecl, node));
		}
	}

	public void checkMethodCall(
	    final IRNode insideDecl, final IRNode node, final PromiseRecord pr) {
		if (JJNode.tree.getOperator(node) instanceof CallInterface) {
			final Set<ResultDrop> callDrops = pr.callsToDrops.get(node);
			
			final IsPositivelyAssuredQuery q =
			  getAnalysis().getIsPositivelyAssuredQuery(insideDecl);
			if (q.getResultFor(node).booleanValue()) {
				for (ResultDrop callDrop : callDrops) {
					callDrop.setConsistent();
					if (pr.calledUniqueParams.contains(callDrop)) {
					  callDrop.setMessage(Messages.UNIQUE_PARAMETERS_SATISFIED, DebugUnparser.toString(node));
					  callDrop.setCategorizingMessage(Messages.DSC_UNIQUE_PARAMS_SATISFIED);
					}
				}
			} else {
				for (ResultDrop callDrop : callDrops) {
					callDrop.setInconsistent();
          callDrop.addInformationHint(node, getErrorMessage(insideDecl, node));
					if (pr.calledUniqueParams.contains(callDrop)) {
					  callDrop.setMessage(Messages.UNIQUE_PARAMETERS_UNSATISFIED, DebugUnparser.toString(node));
					  callDrop.setCategorizingMessage(Messages.DSC_UNIQUE_PARAMS_UNSATISFIED);
					}
				}
			}
		}
	}

	/**
	 * Is the node the source of a uniqueness problem?
	 */
	private boolean isInvalid(final IRNode insideDecl, final IRNode node) {
		final IsInvalidQuery q = getAnalysis().getIsInvalidQuery(insideDecl);
		
		/* Definitely not erroneous */
		if (!q.getResultFor(node).booleanValue()) {
			return false;
		}

		/* Node is erroneous, but does the error come from a child? */
		for (Iterator<IRNode> ch = JJNode.tree.children(node); ch.hasNext();) {
			final IRNode n = ch.next();
			/* Problem comes from a child, so parent is not to blame */
			if (q.getResultFor(n).booleanValue()) {
			  return false;
			}
		}
		/* Not a problem from a child. */
		return true;
	}

	/**
	 * Assumes that isInvalid( n ) is true
	 */
  private String getErrorMessage(final IRNode insideDecl, final IRNode n) {
    final NormalErrorQuery normal = getAnalysis().getNormalErrorQuery(insideDecl);
    final AbruptErrorQuery abrupt = getAnalysis().getAbruptErrorQuery(insideDecl);
    final String normErr = normal.getResultFor(n);
    final String abruptErr = abrupt.getResultFor(n);

		if (normErr != UniquenessAnalysis.NOT_AN_ERROR) {
			if (abruptErr != UniquenessAnalysis.NOT_AN_ERROR) {
				return "(" + normErr + ", " + abruptErr + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			return normErr;
		}
		if (abruptErr != UniquenessAnalysis.NOT_AN_ERROR) {
			return abruptErr + " (abrupt)"; //$NON-NLS-1$
		}
		// Shouldn't ever get here unless we are misused
		return UniquenessAnalysis.NOT_AN_ERROR;
	}



	/**
	 * A record of the annotations that are interesting for a particular
	 * MethodDeclaration, ConstructorDeclaration, ClassInitializer, or Initialzer.
	 */
	private class PromiseRecord {
    /**
     * The method/constructor declaration, or init declaration, or class init
     * declaration this information is for.
     */
	  public final IRNode methodDecl;
	  
		/** The unique parameters declared by this method/constructor */
		public final Set<UniquePromiseDrop> myUniqueParams;

		/** The borrowed parameters declared by this method/constructor */
		public final Set<BorrowedPromiseDrop> myBorrowedParams;

    /** The immutable and read-only parameters declared by this method/constructor */
		public final Set<ImmutableRefPromiseDrop> myImmutableParams;
		
    /** The immutable and read-only parameters declared by this method/constructor */
		public final Set<ReadOnlyPromiseDrop> myReadOnlyParams;
		
    /** The unique return value declared by this method */
    public final Set<UniquePromiseDrop> myUniqueReturn;

    /** The unique return value declared by this method */
    public final Set<ImmutableRefPromiseDrop> myImmutableReturn;

    /** The unique return value declared by this method */
    public final Set<ReadOnlyPromiseDrop> myReadOnlyReturn;

		/** Method call drops for each invoked method that has unique parameters */
		public final Set<ResultDrop> calledUniqueParams;

		/**
		 * Promise drops for each formal parameter of a called method that is 
		 * Borrowed(allowReturn=true).
		 */
		public final Set<BorrowedPromiseDrop> calledBorrowedAllowReturn;
		
		/**
		 * Promise drops for each formal parameter of a called method that is 
		 * ReadOnly.
		 */
		public final Set<ReadOnlyPromiseDrop> calledReadOnly;
		
    /**
     * Promise drops for each formal parameter of a called method that is 
     * Immutable.
     */
    public final Set<ImmutableRefPromiseDrop> calledImmutable;		
		
		/**
		 * Method call drops for each invoked method that has unique return;
		 * Map from the method call drop to the unique promise about the return value.
		 */
		public final Map<ResultDrop, UniquePromiseDrop> calledUniqueReturns;
		
		public final Set<ImmutableRefPromiseDrop> calledImmtuableReturns;
		public final Set<ReadOnlyPromiseDrop> calledReadOnlyReturns;
		
		/**
		 * Method call drops for each invoked constructor that has a borrowed
		 * receiver.  Map from the method call drop to the borrowed promise.
		 */
		public final Map<ResultDrop, BorrowedPromiseDrop> calledBorrowedConstructors;
		public final Map<ResultDrop, UniquePromiseDrop> calledUniqueConstructors;
		
		/** Method call drops for each invoked method that has borrowed parameters */
		public final Set<ResultDrop> calledBorrowedParams;
		public final Set<ResultDrop> calledBorrowedReceiverAsUniqueReturn;

		/** Method call drops for each invoked method that has effects */
		public final Set<ResultDrop> calledEffects;

		/** The unique and borrowed fields accessed */
		public final Set<PromiseDrop<? extends IAASTRootNode>> uniqueFields;
		
		public final Set<ImmutableRefPromiseDrop> usedImmutableFields;
		public final Set<ReadOnlyPromiseDrop> usedReadOnlyFields;
    
    /** Immutable types that have instances that are passed as actual arguments
     * to methods.
     */
    private final Set<ImmutablePromiseDrop> immutableActuals;

		/** Drop for control-flow within this block */
		public final ResultDrop controlFlow;
  
    /**
     * Result drop that aggregates together all the unique field promise drops.
     * Created lazily.
     */
    private ResultDrop aggregatedUniqueFields;
    
    /** Need a separate flag because aggregatedUniqueFields is allowed to be null;
     */
    private boolean isAggregatedUniqueFieldsSet = false;

    /**
     * Result drop that aggregates together all the unique parameter promise drops.
     * Created lazily.
     */
    private ResultDrop aggregatedUniqueParams;
    
    /** Need a separate flag because aggregatedUniqueParams is allowed to be null;
     */
    private boolean isAggregatedUniqueParamsSet = false;
    
		/**
		 * Map from method/constructor calls to the set of result drops that
		 * represent the calls.
		 */
		public final Map<IRNode, Set<ResultDrop>> callsToDrops;

		private String name;
		
		public PromiseRecord(final IRNode block) {
		  methodDecl = block;
			myUniqueParams = new HashSet<UniquePromiseDrop>();
			myBorrowedParams = new HashSet<BorrowedPromiseDrop>();
			myImmutableParams = new HashSet<ImmutableRefPromiseDrop>();
			myReadOnlyParams = new HashSet<ReadOnlyPromiseDrop>();
			myUniqueReturn = new HashSet<UniquePromiseDrop>();
			myImmutableReturn = new HashSet<ImmutableRefPromiseDrop>();
			myReadOnlyReturn = new HashSet<ReadOnlyPromiseDrop>();
      calledUniqueReturns = new HashMap<ResultDrop, UniquePromiseDrop>();
      calledImmtuableReturns = new HashSet<ImmutableRefPromiseDrop>();
      calledReadOnlyReturns = new HashSet<ReadOnlyPromiseDrop>();
      calledBorrowedConstructors = new HashMap<ResultDrop, BorrowedPromiseDrop>();
      calledUniqueConstructors = new HashMap<ResultDrop, UniquePromiseDrop>();
			calledUniqueParams = new HashSet<ResultDrop>();
      calledBorrowedParams = new HashSet<ResultDrop>();
      calledReadOnly = new HashSet<ReadOnlyPromiseDrop>();
      calledImmutable = new HashSet<ImmutableRefPromiseDrop>();
      calledBorrowedReceiverAsUniqueReturn = new HashSet<ResultDrop>();
			calledEffects = new HashSet<ResultDrop>();
			uniqueFields = new HashSet<PromiseDrop<? extends IAASTRootNode>>();
			usedImmutableFields = new HashSet<ImmutableRefPromiseDrop>();
			usedReadOnlyFields = new HashSet<ReadOnlyPromiseDrop>();
			immutableActuals = new HashSet<ImmutablePromiseDrop>();
			calledBorrowedAllowReturn = new HashSet<BorrowedPromiseDrop>();
			
			callsToDrops = new HashMap<IRNode, Set<ResultDrop>>();
			
			// Create the control flow drop for the block
			controlFlow = getMethodControlFlowDrop(block);
			name = DebugUnparser.toString(block);
		}
		
		@Override
		public String toString() {
			return name;
		}
		
		// Can return null
		public synchronized ResultDrop getAggregatedUniqueFields() {
		  if (!isAggregatedUniqueFieldsSet) {
		    if (uniqueFields.isEmpty()) {
		      aggregatedUniqueFields = null;
		    } else {
          final ResultDrop middleDrop = new ResultDrop(methodDecl);
          middleDrop.setConsistent();
          middleDrop.setMessage(Messages.AGGREGATED_UNIQUE_FIELDS, JavaNames.genQualifiedMethodConstructorName(methodDecl));
          for (final PromiseDrop<? extends IAASTRootNode> ud : uniqueFields) {
            middleDrop.addTrusted(ud);
          }       
          aggregatedUniqueFields = middleDrop;
		    }
		    isAggregatedUniqueFieldsSet = true;
		  }
		  return aggregatedUniqueFields;
		}
    
    // Can return null
    public synchronized ResultDrop getAggregatedUniqueParams() {
      if (!isAggregatedUniqueParamsSet) {
        if (myUniqueParams.isEmpty()) {
          aggregatedUniqueParams = null;
        } else {
          final ResultDrop middleDrop = new ResultDrop(methodDecl);
          middleDrop.setConsistent();
          middleDrop.setMessage(Messages.AGGREGATED_UNIQUE_PARAMS, JavaNames.genQualifiedMethodConstructorName(methodDecl));
          for (final UniquePromiseDrop ud : myUniqueParams) {
            middleDrop.addTrusted(ud);
          }       
          aggregatedUniqueParams = middleDrop;
        }
        isAggregatedUniqueParamsSet = true;
      }
      return aggregatedUniqueParams;
    }
	}

	private final Map<IRNode, ResultDrop> cachedControlFlow = new ConcurrentHashMap<IRNode, ResultDrop>();
	
	ResultDrop getMethodControlFlowDrop(final IRNode block) {
    ResultDrop drop = cachedControlFlow.get(block);
    if (drop == null || !drop.isValid()) {
      drop = new ResultDrop(block);
      drop.setConsistent();

      final String label, unparse;
      final Operator op = JJNode.tree.getOperator(block);
      if (ConstructorDeclaration.prototype.includes(op)) {
        label = "constructor";
        unparse = JavaNames.genMethodConstructorName(block);
      } else if (MethodDeclaration.prototype.includes(op)) {
    	  label = "method";
    	  unparse = JavaNames.genMethodConstructorName(block);
        /*
        if (message.endsWith("getMap()")) {
        	System.out.println("Found: "+message);
        }
        */
      } else if (ClassInitializer.prototype.includes(op)) {
    	  label = "initializer";
    	  unparse = DebugUnparser.toString(block);
      } else { // Field declaration
    	  label = "field initializer";
    	  unparse = DebugUnparser.toString(block);
      }
      drop.setMessage(Messages.METHOD_CONTROL_FLOW, label, unparse);
      cachedControlFlow.put(block, drop);
      controlFlowDrops.add(drop);
    }
    return drop;
	}

  /**
   * @param block
   *          A constructor declaration, method declaration, init declaration,
   *          or class init declaration node.
   */
  private PromiseRecord createPromiseRecordFor(final TypeAndMethod block) {
		final PromiseRecord pr = new PromiseRecord(block.getMethod());
		final Operator blockOp = JJNode.tree.getOperator(block.getMethod());
    final boolean isInit = InitDeclaration.prototype.includes(blockOp);
    final boolean isClassInit = ClassInitDeclaration.prototype.includes(blockOp);
    final boolean isConstructorDecl = ConstructorDeclaration.prototype.includes(blockOp);
    final boolean isMethodDecl = MethodDeclaration.prototype.includes(blockOp);

    /*
     * If the block is a constructor declaration or InitDeclaration or
     * ClassInitDeclaration we need to gather up the unique field declarations.
     */		
    if (isConstructorDecl || isInit || isClassInit) {
      // If constructor or init, add the IFQR if it exists and is borrowed
      if (!isClassInit) {
        final IRNode ifqr = JavaPromise.getQualifiedReceiverNodeOrNull(block.getType());
        if (UniquenessUtils.isFieldBorrowed(ifqr)) {
          pr.uniqueFields.add(UniquenessUtils.getFieldBorrowed(ifqr));
        }
      }
      
		  for (final IRNode bodyDecl : ClassBody.getDeclIterator(block.getClassBody())) {
		    if (FieldDeclaration.prototype.includes(bodyDecl)) {
		      if (isClassInit == TypeUtil.isStatic(bodyDecl)) {
  		      final IRNode variableDeclarators = FieldDeclaration.getVars(bodyDecl);
  		      for (IRNode varDecl : VariableDeclarators.getVarIterator(variableDeclarators)) {
  		        if (UniquenessUtils.isUnique(varDecl)) {
  		          pr.uniqueFields.add(UniquenessUtils.getUnique(varDecl).getDrop());
  		        }
  		        if (UniquenessUtils.isFieldBorrowed(varDecl)) {
  		          pr.uniqueFields.add(UniquenessUtils.getFieldBorrowed(varDecl));
  		        }
  		        final ImmutableRefPromiseDrop iDrop = LockRules.getImmutableRef(varDecl);
  		        final ReadOnlyPromiseDrop roDrop = UniquenessRules.getReadOnly(varDecl);
  		        if (iDrop != null) pr.usedImmutableFields.add(iDrop);
  		        if (roDrop != null) pr.usedReadOnlyFields.add(roDrop);
  		      }
  		      
  		      // Also check if the class has an IFQR that is borrowed
  		      final IRNode ifqr =
  		          JavaPromise.getQualifiedReceiverNodeOrNull(block.getClassBody());
  		      if (ifqr != null) {
  		        final BorrowedPromiseDrop bDrop = UniquenessRules.getBorrowed(ifqr);
  		        if (bDrop != null) pr.uniqueFields.add(bDrop);
  		      }
		      }
		    }
		  }
		}

		/* If the block is a method or constructor declaration, get promise
		 * information from it.
		 */
    if (isConstructorDecl || isMethodDecl) {
      // don't care about my effects, use a throw-away set here
      getPromisesFromMethodDecl(block.getMethod(),
          pr.myUniqueReturn, pr.myImmutableReturn, pr.myReadOnlyReturn,
          pr.myBorrowedParams, new HashSet<BorrowedPromiseDrop>(),
          pr.myImmutableParams, pr.myReadOnlyParams,
          pr.myUniqueParams, new HashSet<RegionEffectsPromiseDrop>());
    }

		/* Look at the guts of the method/constructor/initializer.  If the block
		 * is a constructor or instance init declaration, we also look at all the 
		 * field declarations and initializer blocks.
		 */
    if (isConstructorDecl || isMethodDecl) {
      populatePromiseRecord(pr, block.getMethod());
    }
    if (isInit || isClassInit || isConstructorDecl) {
      for (final IRNode bodyDecl : ClassBody.getDeclIterator(block.getClassBody())) {
        if (FieldDeclaration.prototype.includes(bodyDecl) || ClassInitializer.prototype.includes(bodyDecl)) {
          if (isClassInit == TypeUtil.isStatic(bodyDecl)) {
            populatePromiseRecord(pr, bodyDecl);
          }
        }
      }
    }

    final Set<ResultDrop> fooSet = Collections.singleton(pr.controlFlow);
    addDependencies(pr.myImmutableReturn, fooSet);
    addDependencies(pr.myReadOnlyReturn, fooSet);
    addDependencies(pr.myImmutableParams, fooSet);
    addDependencies(pr.myReadOnlyParams, fooSet);
    
    addDependencies(pr.calledImmtuableReturns, fooSet);
    addDependencies(pr.calledReadOnlyReturns, fooSet);
    
    addDependencies(pr.usedImmutableFields, fooSet);
    addDependencies(pr.usedReadOnlyFields, fooSet);
    
    addDependencies(pr.immutableActuals, fooSet);
    
    addDependencies(pr.calledBorrowedAllowReturn, fooSet);
    addDependencies(pr.calledImmutable, fooSet);
    addDependencies(pr.calledReadOnly, fooSet);
    
    /*
     * Set up the borrowed dependencies. Each parameter of the method that is
     * declared to be borrowed trusts the @borrowed annotations (including
     * @Unique("return") annotations on constructors) of any methods called by
     * the body of this method.
     */
		if (!pr.myBorrowedParams.isEmpty() ||
		    (isConstructorDecl && !pr.myUniqueReturn.isEmpty())) {
			final Set<ResultDrop> dependsOnResults = new HashSet<ResultDrop>(pr.calledBorrowedParams);
			dependsOnResults.addAll(pr.calledBorrowedReceiverAsUniqueReturn);
			dependsOnResults.add(pr.controlFlow);
      addDependencies(pr.myBorrowedParams, dependsOnResults);
      /* If we are a constructor, we treat unique("return") like @borrowed("this")
       */
      if (isConstructorDecl) {
        addDependencies(pr.myUniqueReturn, dependsOnResults);
      }
		}

		/*
		 * Set up the dependencies for this method's accessed unique fields. Depends
		 * on the unique parameters of the method, the unique return values of
		 * called methods, the borrowed parameters of called methods (including
     * @Unique("return") annotations on constructors), the unique
		 * fields accessed by this method, the effects of methods w/borrowed
		 * parameters, and the control-flow of the method itself.
		 */
		if (!pr.uniqueFields.isEmpty()) {
      final Set<ResultDrop> dependsOnResults = new HashSet<ResultDrop>();
      final ResultDrop aggregatedUniqueParams = pr.getAggregatedUniqueParams();
      if (aggregatedUniqueParams != null) dependsOnResults.add(aggregatedUniqueParams);
			final ResultDrop aggregatedUniqueFields = pr.getAggregatedUniqueFields();
			if (aggregatedUniqueFields != null) dependsOnResults.add(aggregatedUniqueFields);
			dependsOnResults.add(pr.controlFlow);
      dependsOnResults.addAll(pr.calledUniqueReturns.keySet());
      dependsOnResults.addAll(pr.calledBorrowedConstructors.keySet());
      dependsOnResults.addAll(pr.calledUniqueConstructors.keySet());
			dependsOnResults.addAll(pr.calledBorrowedParams);
			dependsOnResults.addAll(pr.calledBorrowedReceiverAsUniqueReturn);
			dependsOnResults.addAll(pr.calledEffects);
			addDependencies(pr.uniqueFields, dependsOnResults);
		}

		/*
		 * Set up the dependencies for this method's unique return value. Depends on
		 * the unique parameters of the method, the unique return values of called
		 * methods, the unique fields accessed by this method, and the control-flow
		 * of the method itself.
		 */
		if (!isConstructorDecl && !pr.myUniqueReturn.isEmpty()) {
      final Set<ResultDrop> dependsOnResults = new HashSet<ResultDrop>();
      final ResultDrop aggregatedUniqueParams = pr.getAggregatedUniqueParams();
      if (aggregatedUniqueParams != null) dependsOnResults.add(aggregatedUniqueParams);
      final ResultDrop aggregatedUniqueFields = pr.getAggregatedUniqueFields();
      if (aggregatedUniqueFields != null) dependsOnResults.add(aggregatedUniqueFields);
	    dependsOnResults.add(pr.controlFlow);
  		dependsOnResults.addAll(pr.calledUniqueReturns.keySet());
      dependsOnResults.addAll(pr.calledBorrowedConstructors.keySet());
      dependsOnResults.addAll(pr.calledUniqueConstructors.keySet());
      addDependencies(pr.myUniqueReturn, dependsOnResults);
		}

		/* My unique parameters depend on the control-flow results of my callers.
		 * This is set up in populatePromiseRecord().
		 * 
		 * We used to make them depend on my control flow, but that is wrong.
		 */
		
		/*
		 * Set up the dependencies for this method's called methods with unique
		 * parameters. Depends on the unique parameters of this method, the unique
		 * return values of called methods, read unique fields, and the control-flow
		 * of the method itself.
		 */
		{
			final Set<PromiseDrop<? extends IAASTRootNode>> dependsOnPromises =
				new HashSet<PromiseDrop<? extends IAASTRootNode>>();
			dependsOnPromises.addAll(pr.myUniqueParams);
			dependsOnPromises.addAll(pr.uniqueFields);
			if (!(dependsOnPromises.isEmpty() && pr.calledUniqueReturns.isEmpty()
			    && pr.calledImmtuableReturns.isEmpty()
			    && pr.calledReadOnlyReturns.isEmpty()
			    && pr.calledBorrowedConstructors.isEmpty()
			    && pr.calledUniqueConstructors.isEmpty())) {
				for (ResultDrop callToCheck : pr.calledUniqueParams) {
					// Add depended upon promises
					for (final PromiseDrop<? extends IAASTRootNode> trustedPD : dependsOnPromises) {
						if (trustedPD != null) {
							callToCheck.addTrusted(trustedPD);
						}
					}

					// Add depended on method calls
					for (Map.Entry<ResultDrop, UniquePromiseDrop> entry : pr.calledUniqueReturns.entrySet()) {
					  final IRNode methodCall = entry.getKey().getNode();
					  callToCheck.addInformationHint(methodCall,
							  Messages.UNIQUE_RETURN_VALUE,
					          DebugUnparser.toString(methodCall));
					  callToCheck.addTrusted(entry.getValue());
					}

          // Add depended on contructors with borrowed("this") or unique("return")
          for (Map.Entry<ResultDrop, BorrowedPromiseDrop> entry : pr.calledBorrowedConstructors.entrySet()) {
            final IRNode constructorCall = entry.getKey().getNode();
            callToCheck.addInformationHint(constructorCall,
            		Messages.BORROWED_CONSTRUCTOR,
                    DebugUnparser.toString(constructorCall));
            callToCheck.addTrusted(entry.getValue());
          }
          for (Map.Entry<ResultDrop, UniquePromiseDrop> entry : pr.calledUniqueConstructors.entrySet()) {
            final IRNode constructorCall = entry.getKey().getNode();
            callToCheck.addInformationHint(constructorCall,
            		Messages.BORROWED_CONSTRUCTOR,
                    DebugUnparser.toString(constructorCall));
            callToCheck.addTrusted(entry.getValue());
          }
				}
			}
		}

		// Return the promise record
		return pr;
	}

	private void populatePromiseRecord(final PromiseRecord pr, final IRNode root) {
    final Iterator<IRNode> nodes = JJNode.tree.topDown(root);
    while (nodes.hasNext()) {
      final IRNode currentNode = nodes.next();
      final Operator op = JJNode.tree.getOperator(currentNode);

      // is it a unique field access?
      if (FieldRef.prototype.equals(op)) {
        final IRNode fdecl = getBinder().getBinding(currentNode);
        if (UniquenessUtils.isUnique(fdecl)) {
          pr.uniqueFields.add(UniquenessUtils.getUnique(fdecl).getDrop());
        }
        if (UniquenessUtils.isFieldBorrowed(fdecl)) {
          pr.uniqueFields.add(UniquenessUtils.getFieldBorrowed(fdecl));
        }
        final ImmutableRefPromiseDrop iDrop = LockRules.getImmutableRef(fdecl);
        final ReadOnlyPromiseDrop roDrop = UniquenessRules.getReadOnly(fdecl);
        if (iDrop != null) pr.usedImmutableFields.add(iDrop);
        if (roDrop != null) pr.usedReadOnlyFields.add(roDrop);
      }
      
      if (QualifiedThisExpression.prototype.includes(op)) {
        /* See if any of the qualified receiver fields that we implicitly
         * access are borrowed.  If so, add the containing method as interesting.
         */
        final IRNode ifqr = getBinder().getBinding(currentNode);
        /* Make sure
         * (1) it's not the regular receiver ("C.this" used inside of class "C")
         * (2) it's an IFQR and not an IPQR
         */
        if (!ReceiverDeclaration.prototype.includes(ifqr) &&
            !ConstructorDeclaration.prototype.includes(
                JavaPromise.getPromisedFor(ifqr))) {
          /* Loop up the nested class hierarchy until we find the class whose
           * qualified receiver declaration equals 'decl'. 
           */
          IRNode currentClass = VisitUtil.getEnclosingType(currentNode);
          IRNode currentQualifiedReceiverField;
          do {
            currentQualifiedReceiverField = JavaPromise.getQualifiedReceiverNodeOrNull(currentClass);
            // check for @Borrowed
            final BorrowedPromiseDrop bDrop = 
                UniquenessRules.getBorrowed(currentQualifiedReceiverField);
            if (bDrop != null) pr.uniqueFields.add(bDrop);
            currentClass = VisitUtil.getEnclosingType(currentClass);
          } while (currentQualifiedReceiverField != ifqr);
        }
      }

      // Is it a method call
      if (op instanceof CallInterface) {
        final IRNode declNode = getBinder().getBinding(currentNode);
        if (declNode == null) {
          LOG.warning("No binding for "+DebugUnparser.toString(currentNode));
          continue;
        }

        /* Look at the call site and see if there are actual expressions whose
         * type is @Immutable.
         */
        // check the receiver, if any (non-static method calls)
        if (MethodCall.prototype.includes(currentNode) && !TypeUtil.isStatic(declNode)) {
          final IRNode rcvr = MethodCall.getObject(currentNode);
          final IJavaType type = getBinder().getJavaType(rcvr);
          if (type instanceof IJavaSourceRefType) {
            final IJavaSourceRefType srcRefType = (IJavaSourceRefType) type;
            final IRNode typeDeclarationNode = srcRefType.getDeclaration();
            final ImmutablePromiseDrop iDrop = LockRules.getImmutableType(typeDeclarationNode);
            if (iDrop != null) pr.immutableActuals.add(iDrop);
          }
        }
        // check the params
        try {
          final IRNode actuals = ((CallInterface) JJNode.tree.getOperator(currentNode)).get_Args(currentNode);
          for (final IRNode arg : Arguments.getArgIterator(actuals)) {
            final IJavaType type = getBinder().getJavaType(arg);
            if (type instanceof IJavaSourceRefType) {
              final IJavaSourceRefType srcRefType = (IJavaSourceRefType) type;
              final IRNode typeDeclarationNode = srcRefType.getDeclaration();
              final ImmutablePromiseDrop iDrop = LockRules.getImmutableType(typeDeclarationNode);
              if (iDrop != null) pr.immutableActuals.add(iDrop);
            }
          }
        } catch (final CallInterface.NoArgs e) {
          // do nothing
        }

        final boolean isConstructorCall = 
          ConstructorDeclaration.prototype.includes(declNode);
        
        // get the info for the called method
        final Set<UniquePromiseDrop> uniqueReturns = new HashSet<UniquePromiseDrop>();
        final Set<BorrowedPromiseDrop> borrowedParams = new HashSet<BorrowedPromiseDrop>();
        final Set<BorrowedPromiseDrop> borrowedReceiver = new HashSet<BorrowedPromiseDrop>();
        final Set<UniquePromiseDrop> uniqueParams = new HashSet<UniquePromiseDrop>();
        final Set<RegionEffectsPromiseDrop> effects = new HashSet<RegionEffectsPromiseDrop>();
        getPromisesFromMethodDecl(
            declNode, uniqueReturns, pr.calledImmtuableReturns, pr.calledReadOnlyReturns,
            borrowedParams, borrowedReceiver, pr.calledImmutable, pr.calledReadOnly,
            uniqueParams, effects);

        // Create the method call drops
        final Set<ResultDrop> allCallDrops = new HashSet<ResultDrop>();
        final String label = DebugUnparser.toString(currentNode);
        if (!uniqueReturns.isEmpty()) {
          final ResultDrop callDrop = getMethodCallDrop(currentNode, uniqueReturns, Messages.UNIQUE_RETURN, label);
          if (!isConstructorCall) {
            allCallDrops.add(callDrop);
            // Unique returns is a singleton set
            pr.calledUniqueReturns.put(callDrop, uniqueReturns.iterator().next());
          } else {
            /* Unique return on constructor should be treated like a borrowed
             * receiver 
             */
            allCallDrops.add(callDrop);
            pr.calledBorrowedReceiverAsUniqueReturn.add(callDrop);
            pr.calledUniqueConstructors.put(callDrop, uniqueReturns.iterator().next());
          }
        }
        if (!borrowedParams.isEmpty()) {
          final ResultDrop callDrop = getMethodCallDrop(currentNode, borrowedParams, Messages.BORROWED_PARAMETERS, label);
          allCallDrops.add(callDrop);
          pr.calledBorrowedParams.add(callDrop);
          
          if (isConstructorCall && !borrowedReceiver.isEmpty()) {
            // Borrowed receivers is a singleton set
            pr.calledBorrowedConstructors.put(callDrop, borrowedReceiver.iterator().next());
          }
          
          for (final BorrowedPromiseDrop bDrop : borrowedParams) {
            if (bDrop.allowReturn()) {
              pr.calledBorrowedAllowReturn.add(bDrop);
            }
          }
        }
        if (!uniqueParams.isEmpty()) {
          /* Here we hold off setting the message and category until the 
           * call is actually assured in checkMethodCall()
           */
          final ResultDrop callDrop = new ResultDrop(currentNode);
          callDrop.setConsistent();
          // This result checks the uniqueness promises of the parameters
          for (final UniquePromiseDrop uniqueParam : uniqueParams) {
            callDrop.addChecked(uniqueParam);
            
            /* The uniqueness of the parameter also depends on the control flow
             * of the calling method.
             */
            pr.controlFlow.addChecked(uniqueParam);
          }
          allCallDrops.add(callDrop);
          pr.calledUniqueParams.add(callDrop);          
        }
        if (!effects.isEmpty()) {
          final ResultDrop callDrop = getMethodCallDrop(currentNode, effects, Messages.CALL_EFFECT, label);
          allCallDrops.add(callDrop);
          pr.calledEffects.add(callDrop);
        }

        // Add to the map of calls to drops
        pr.callsToDrops.put(currentNode, allCallDrops);
      }
    }
	}
	
	/**
	 * Collect the promise drops from the given method/constructor declaration.
	 * Also, if <code>callRD</code> is non-null, it adds each unique parameter
	 * as a promise checked by <code>callRD</code>; <code>calLRD</code> is a
	 * drop that represents a particular call to the method defined by
	 * <code>mdecl</code>.
	 *
	 * @param mdecl
	 *          Method or constructor declaration
	 * @param uniqueReturns
	 *          Set to be mutated
	 * @param borrowedParams
	 *          Set to be mutated
	 * @param uniqueParams
	 *          Set to be mutated
	 */
	private void getPromisesFromMethodDecl(final IRNode mdecl,
			final Set<UniquePromiseDrop> uniqueReturns,
			final Set<ImmutableRefPromiseDrop> immutableReturns,
			final Set<ReadOnlyPromiseDrop> readOnlyReturns,
			final Set<BorrowedPromiseDrop> borrowedParams,
			final Set<BorrowedPromiseDrop> borrowedReceiver,
			final Set<ImmutableRefPromiseDrop> immutableParams,
			final Set<ReadOnlyPromiseDrop> readOnlyParams,
			final Set<UniquePromiseDrop> uniqueParams,
			final Set<RegionEffectsPromiseDrop> effects) {
		// Try to get the @unique returns drop if any
		final IRNode retDecl = JavaPromise.getReturnNodeOrNull(mdecl);
		if (retDecl != null) {
      final UniquePromiseDrop returnsUniqueDrop =
          UniquenessRules.getUnique(retDecl);
      if (returnsUniqueDrop != null) uniqueReturns.add(returnsUniqueDrop);

      final ImmutableRefPromiseDrop returnsImmutableDrop =
          LockRules.getImmutableRef(retDecl);
      if (returnsImmutableDrop != null) immutableReturns.add(returnsImmutableDrop);

      final ReadOnlyPromiseDrop returnsReadOnlyDrop =
          UniquenessRules.getReadOnly(retDecl);
      if (returnsReadOnlyDrop != null) readOnlyReturns.add(returnsReadOnlyDrop);
		}

		// Get the @borrowed and @unique params drops, if any
		if (!TypeUtil.isStatic(mdecl)) { // don't forget the receiver
			final IRNode self = JavaPromise.getReceiverNodeOrNull(mdecl);
			if (self != null) {
				final BorrowedPromiseDrop borrowedRcvrDrop = UniquenessRules.getBorrowed(self);
				final UniquePromiseDrop uniqueRcvrDrop = UniquenessRules.getUnique(self);
				final ImmutableRefPromiseDrop immutableRcvrDrop = LockRules.getImmutableRef(self);
				final ReadOnlyPromiseDrop readOnlyRcvrDrop = UniquenessRules.getReadOnly(self);
				if (borrowedRcvrDrop != null) {
					borrowedParams.add(borrowedRcvrDrop);
					borrowedReceiver.add(borrowedRcvrDrop);
				}
				if (uniqueRcvrDrop != null) uniqueParams.add(uniqueRcvrDrop);
				if (immutableRcvrDrop != null) immutableParams.add(immutableRcvrDrop);
				if (readOnlyRcvrDrop != null) readOnlyParams.add(readOnlyRcvrDrop);
			}
			
			// Try for the IPQR (only present on constructors)
			final IRNode ipqr = JavaPromise.getQualifiedReceiverNodeOrNull(mdecl);
			if (ipqr != null) {
			  final BorrowedPromiseDrop borrowedQRcvrDrop = UniquenessRules.getBorrowed(ipqr);
			  if (borrowedQRcvrDrop != null) {
			    borrowedParams.add(borrowedQRcvrDrop);
			  }
			}
		}
		
    final LocalVariableDeclarations declsInScope = LocalVariableDeclarations.getDeclarationsFor(mdecl);
    for (final IRNode decl : declsInScope.getAllParameterDeclarations()) {
      final BorrowedPromiseDrop borrowedDrop = UniquenessRules.getBorrowed(decl);
      final UniquePromiseDrop uniqueDrop = UniquenessRules.getUnique(decl);
      final ImmutableRefPromiseDrop immutableDrop = LockRules.getImmutableRef(decl);
      final ReadOnlyPromiseDrop readOnlyDrop = UniquenessRules.getReadOnly(decl);
      if (borrowedDrop != null) borrowedParams.add(borrowedDrop);
      if (uniqueDrop != null) uniqueParams.add(uniqueDrop);
      if (immutableDrop != null) immutableParams.add(immutableDrop);
      if (readOnlyDrop != null) readOnlyParams.add(readOnlyDrop);
    }

//		final IRNode myParams = isConstructor ? ConstructorDeclaration
//				.getParams(mdecl) : MethodDeclaration.getParams(mdecl);
//		for (int i = 0; i < JJNode.tree.numChildren(myParams); i++) {
//			final IRNode param = JJNode.tree.getChild(myParams, i);
//			final BorrowedPromiseDrop borrowedDrop = UniquenessRules.getBorrowed(param);
//			final UniquePromiseDrop uniqueDrop = UniquenessRules.getUnique(param);
//			final ImmutableRefPromiseDrop immutableDrop = LockRules.getImmutableRef(param);
//			final ReadOnlyPromiseDrop readOnlyDrop = UniquenessRules.getReadOnly(param);
//			if (borrowedDrop != null) borrowedParams.add(borrowedDrop);
//			if (uniqueDrop != null) uniqueParams.add(uniqueDrop);
//			if (immutableDrop != null) immutableParams.add(immutableDrop);
//			if (readOnlyDrop != null) readOnlyParams.add(readOnlyDrop);
//		}

		// get effects
		if (true) {
			final RegionEffectsPromiseDrop fx = MethodEffectsRules.getRegionEffectsDrop(mdecl);
			// We don't depend on undeclared effects
			if (fx != null)
				effects.add(fx);
		}
	}

  private <D extends PromiseDrop<? extends IAASTRootNode>> ResultDrop getMethodCallDrop(final IRNode n,
      final Set<D> promises, int num, Object... args) {
    final ResultDrop rd = new ResultDrop(n);
    rd.setConsistent();
    rd.setMessage(num, args);
    for (final D pd : promises) {
      rd.addTrusted(pd);
    }
    return rd;
  }

  private <PD1 extends PromiseDrop<? extends IAASTRootNode>> void addDependencies(final Set<PD1> promises,
      final Set<ResultDrop> dependsOnResults) {
    if (!dependsOnResults.isEmpty()) {
      for (final PD1 promiseToCheck : promises) {
        // Add depended on method calls, etc.
        if (!dependsOnResults.isEmpty()) {
          for (ResultDrop rd : dependsOnResults) {
            rd.addChecked(promiseToCheck);
          }
        }
      }
    }
	}
	
  /*
   * We are searching for
   * 
   *   (1) the declaration of an unique, borrowed, or immutable field
   *   
   *   (2) the use of a unique, borrowed, read-only, or immutable field
   *   
   *   (3) the declaration of a method that has a borrowed, unique, read-only,
   *   or immutable parameter, or a unique or immutable return value
   *   
   *   (4) the invocation of a method that has unique or immutable parameter requirements or
   *   returns a read-only or immutable reference
   */
	private static final class NewShouldAnalyzeVisitor extends JavaSemanticsVisitor {
    private final IBinder binder;
    
    /**
     * The output of the visitation: the set of method/constructor declarations
     * that should receive additional scrutiny by the uniqueness analysyis.
     */
    private final Set<TypeAndMethod> results = new HashSet<TypeAndMethod>();
    
    
    
    public NewShouldAnalyzeVisitor(final IBinder binder) {
      super(true, false);
      this.binder = binder;
    }

    
    
    public Set<TypeAndMethod> getResults() {
      return results;
    }
    
    
    
    // Don't go inside annotations, they aren't interesting, and cause problems.
    @Override
    public Void visitAnnotation(final IRNode annotation) {
      return null;
    }
    
    
    
    /* Case (4): invoking method with UNIQUE parameter or IMMUTABLE parameters,
     * or a READ-ONLY or IMMUTABLE return value
     * 
     * Also interesting if the called method has a @Borrowed(allowReturn=true)
     * parameter.
     */
    @Override
    protected void handleAsMethodCall(final IRNode call) {
      final IRNode declNode = binder.getBinding(call);

      if (declNode != null) {
        final Operator declOp = JJNode.tree.getOperator(declNode);
        IRNode formals = null;
        boolean hasUnique = false;
        boolean hasImmutable = false;
        boolean hasBorrowedAllowReturn = false;
        
        if (declOp instanceof ConstructorDeclaration) {
          formals = ConstructorDeclaration.getParams(declNode);
        } else if (declOp instanceof MethodDeclaration) {
          formals = MethodDeclaration.getParams(declNode);
          if (!TypeUtil.isStatic(declNode)) {
            final IRNode self = JavaPromise.getReceiverNode(declNode);
            hasUnique = UniquenessRules.isUnique(self);
            hasImmutable = LockRules.isImmutableRef(self);
            hasBorrowedAllowReturn = UniquenessRules.isBorrowed(self) && 
                UniquenessRules.getBorrowed(self).allowReturn();
          }
        }
        if (formals != null) {
          for (int i = 0; !hasUnique && (i < JJNode.tree.numChildren(formals)); i++) {
            final IRNode param = JJNode.tree.getChild(formals, i);
            hasUnique |= UniquenessRules.isUnique(param);
            hasImmutable |= LockRules.isImmutableRef(param);
            hasBorrowedAllowReturn |= UniquenessRules.isBorrowed(param) && 
                UniquenessRules.getBorrowed(param).allowReturn();
          }
        }
        
        final IRNode returnNode = JavaPromise.getReturnNodeOrNull(declNode);
        boolean readOnlyReturn = false;
        boolean immutableReturn = false;
        if (returnNode != null) {
          readOnlyReturn = UniquenessRules.isReadOnly(returnNode);
          immutableReturn = LockRules.isImmutableRef(returnNode);
        }
        
        if (hasUnique || hasImmutable || hasBorrowedAllowReturn || readOnlyReturn || immutableReturn) {
          results.add(new TypeAndMethod(getEnclosingType(), getEnclosingDecl()));
        }
      }
      
      /* Look at the call site and see if there are actual expressions whose
       * type is @Immutable.
       */
      // check the receiver, if any (non-static method calls)
      if (MethodCall.prototype.includes(call) && !TypeUtil.isStatic(declNode)) {
        final IRNode rcvr = MethodCall.getObject(call);
        if (isValueNode(rcvr)) {
          results.add(new TypeAndMethod(getEnclosingType(), getEnclosingDecl()));
        }
      }
      // check the params
      try {
        final IRNode actuals = ((CallInterface) JJNode.tree.getOperator(call)).get_Args(call);
        for (final IRNode arg : Arguments.getArgIterator(actuals)) {
          if (isValueNode(arg)) {
            results.add(new TypeAndMethod(getEnclosingType(), getEnclosingDecl()));
          }
        }
      } catch (final CallInterface.NoArgs e) {
        // do nothing
      }
    }
    
    private boolean isValueNode(final IRNode node) {
      IJavaType type = binder.getJavaType(node);
      if (type instanceof IJavaSourceRefType) {
        final IJavaSourceRefType srcRefType = (IJavaSourceRefType) type;
        final IRNode typeDeclarationNode = srcRefType.getDeclaration();
        return LockRules.isImmutableType(typeDeclarationNode);
      }
      return false;
    }
    
    
    @Override
    protected void handleConstructorDeclaration(final IRNode cdecl) {
      // Case 3b: borrowed/unique/immutable/read-only parameter
      boolean hasBorrowedParam = false;
      boolean hasUniqueParam = false;
      boolean hasImmutableParam = false;
      boolean hasReadOnlyParam = false;
      hasBorrowedParam |= UniquenessRules.isBorrowedReceiver(cdecl);
      hasBorrowedParam |= UniquenessRules.isBorrowed(
          JavaPromise.getQualifiedReceiverNodeOrNull(cdecl));
      // Cannot have a unique receiver
      final IRNode self = JavaPromise.getReceiverNodeOrNull(cdecl);
      hasImmutableParam = LockRules.isImmutableRef(self);
      hasReadOnlyParam = UniquenessRules.isReadOnly(self);
      
      final LocalVariableDeclarations declsInScope = LocalVariableDeclarations.getDeclarationsFor(cdecl);
      for (final IRNode decl : declsInScope.getAllParameterDeclarations()) {
        hasBorrowedParam |= UniquenessRules.isBorrowed(decl);
        hasUniqueParam |= UniquenessRules.isUnique(decl);
        hasImmutableParam |= LockRules.isImmutableRef(decl);
        hasReadOnlyParam |= UniquenessRules.isReadOnly(decl);
      }
//      final IRNode formals = ConstructorDeclaration.getParams(cdecl);
//      for (int i = 0; i < JJNode.tree.numChildren(formals); i++) {
//        final IRNode param = JJNode.tree.getChild(formals, i);
//        hasBorrowedParam |= UniquenessRules.isBorrowed(param);
//        hasUniqueParam |= UniquenessRules.isUnique(param);
//        hasImmutableParam |= LockRules.isImmutableRef(param);
//        hasReadOnlyParam |= UniquenessRules.isReadOnly(param);
//      }
      
      /* Also check to see if the class containing the constructor has a 
       * @Borrowed annotation on the IFQR.
       */
      final boolean hasBorrowedIFQR = UniquenessRules.isBorrowed(
          JavaPromise.getQualifiedReceiverNodeOrNull(getEnclosingType()));
      
      if (hasBorrowedParam || hasUniqueParam || hasBorrowedIFQR
          || hasImmutableParam || hasReadOnlyParam) {
        results.add(new TypeAndMethod(getEnclosingType(), cdecl));
      }
            
      // Check the rest of the constructor
      doAcceptForChildren(cdecl);
    }

    @Override
    public Void visitFieldRef(final IRNode fieldRef) {
      /* Case (2): A use of a UNIQUE, BORROWED, READ-ONLY, or IMMUTABLE field. */
      final IRNode fdecl = binder.getBinding(fieldRef);
      if (
          UniquenessUtils.isUnique(fdecl)
          || UniquenessUtils.isFieldBorrowed(fdecl)
          || UniquenessRules.isReadOnly(fdecl)
          || LockRules.isImmutableRef(fdecl)) {
        results.add(new TypeAndMethod(getEnclosingType(), getEnclosingDecl()));
      }
      doAcceptForChildren(fieldRef);
      return null;
    }
    
    @Override
    public Void visitQualifiedThisExpression(final IRNode qthis) {
      /* See if any of the qualified receiver fields that we implicitly
       * access are borrowed.  If so, add the containing method as interesting.
       */
      final IRNode ifqr = binder.getBinding(qthis);
      /* Make sure
       * (1) it's not the regular receiver ("C.this" used inside of class "C")
       * (2) it's an IFQR and not an IPQR
       */
      if (!ReceiverDeclaration.prototype.includes(ifqr) &&
          !ConstructorDeclaration.prototype.includes(
              JavaPromise.getPromisedFor(ifqr))) {
        /* Loop up the nested class hierarchy until we find the class whose
         * qualified receiver declaration equals 'decl'. 
         */
        IRNode currentClass = VisitUtil.getEnclosingType(qthis);
        IRNode currentQualifiedReceiverField;
        do {
          currentQualifiedReceiverField = JavaPromise.getQualifiedReceiverNodeOrNull(currentClass);
          // check for @Borrowed
          if (UniquenessRules.isBorrowed(currentQualifiedReceiverField)) {
            results.add(new TypeAndMethod(getEnclosingType(), getEnclosingDecl()));
            // quit early
            break;
          }
          currentClass = VisitUtil.getEnclosingType(currentClass);
        } while (currentQualifiedReceiverField != ifqr);
      }
      doAcceptForChildren(qthis);
      return null;
    }
    
    @Override
    protected void handleMethodDeclaration(final IRNode mdecl) {
      // Case 3a: returns unique
      final IRNode retDecl = JavaPromise.getReturnNodeOrNull(mdecl);
      final boolean returnsUnique =
          (retDecl == null) ? false : UniquenessRules.isUnique(retDecl);
      final boolean returnsImmutable = 
          (retDecl == null) ? false : LockRules.isImmutableRef(retDecl);
      
      // Case 3b: borrowed/unique parameter
      boolean hasBorrowedParam = false;
      boolean hasUniqueParam = false;
      boolean hasImmutableParam = false;
      boolean hasReadOnlyParam = false;
      if (!TypeUtil.isStatic(mdecl)) { // non-static method
        final IRNode self = JavaPromise.getReceiverNode(mdecl);
        hasBorrowedParam |= UniquenessRules.isBorrowed(self);
        hasUniqueParam |= UniquenessRules.isUnique(self);
        hasImmutableParam = LockRules.isImmutableRef(self);
        hasReadOnlyParam = UniquenessRules.isReadOnly(self);
      }
      final LocalVariableDeclarations declsInScope = LocalVariableDeclarations.getDeclarationsFor(mdecl);
      for (final IRNode decl : declsInScope.getAllParameterDeclarations()) {
        hasBorrowedParam |= UniquenessRules.isBorrowed(decl);
        hasUniqueParam |= UniquenessRules.isUnique(decl);
        hasImmutableParam |= LockRules.isImmutableRef(decl);
        hasReadOnlyParam |= UniquenessRules.isReadOnly(decl);
      }
//      final IRNode formals = MethodDeclaration.getParams(mdecl);
//      for (int i = 0; i < JJNode.tree.numChildren(formals); i++) {
//        final IRNode param = JJNode.tree.getChild(formals, i);
//        hasBorrowedParam |= UniquenessRules.isBorrowed(param);
//        hasUniqueParam |= UniquenessRules.isUnique(param);
//        hasImmutableParam |= LockRules.isImmutableRef(param);
//        hasReadOnlyParam |= UniquenessRules.isReadOnly(param);
//      }
      if (returnsUnique || returnsImmutable
          || hasBorrowedParam || hasUniqueParam
          || hasImmutableParam || hasReadOnlyParam) {
        results.add(new TypeAndMethod(getEnclosingType(), mdecl));
      }
    
      doAcceptForChildren(mdecl);
    }
    
    @Override
    protected void handleFieldInitialization(final IRNode varDecl, final boolean isStatic) {
      /* CASE (1): If the field is UNIQUE, BORROWED, or IMMUTABLE then we
       * add the current enclosing declaration to the results.
       */
      if (UniquenessUtils.isUnique(varDecl)
          || UniquenessUtils.isFieldBorrowed(varDecl)
          || UniquenessRules.isReadOnly(varDecl)
          || LockRules.isImmutableRef(varDecl)) {
        results.add(new TypeAndMethod(getEnclosingType(), getEnclosingDecl()));
      }
      // analyze the the RHS of the initialization
      doAcceptForChildren(varDecl);
    }
	}
}