package com.surelogic.analysis.uniqueness;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.uniqueness.Messages;
import com.surelogic.analysis.uniqueness.UniquenessAnalysis.AbruptErrorQuery;
import com.surelogic.analysis.uniqueness.UniquenessAnalysis.IsInvalidQuery;
import com.surelogic.analysis.uniqueness.UniquenessAnalysis.IsPositivelyAssuredQuery;
import com.surelogic.analysis.uniqueness.UniquenessAnalysis.NormalErrorQuery;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

public class UniquenessAnalysisModule extends AbstractWholeIRAnalysis<UniquenessAnalysis,Void> {
  /**
   * Map from promise drops to "intermediate result drops" that are used
   * to allow promises to depend on other promises.  There should be only
   * one intermediate result, so we cache it here.
   */
  private final Map<PromiseDrop<? extends IAASTRootNode>, ResultDropBuilder> intermediateResultDrops =
    new HashMap<PromiseDrop<? extends IAASTRootNode>, ResultDropBuilder>();

  /**
   * All the method control flow result drops we create.  We scan this at the
   * end to invalidate any drops that are not used.
   */
  private final Set<ResultDropBuilder> controlFlowDrops = 
	  Collections.synchronizedSet(new HashSet<ResultDropBuilder>());
  
  public UniquenessAnalysisModule() {
		super(true && !singleThreaded, null, "UniqueAnalysis");
	}

	@Override
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do
	}

	@Override
	protected UniquenessAnalysis constructIRAnalysis(IBinder binder) {
		return new UniquenessAnalysis(binder,	false);
	}
	
	@Override
	protected void clearCaches() {
	  intermediateResultDrops.clear();
	  cachedControlFlow.clear();
	  controlFlowDrops.clear();
	  
	  if (!runInParallel()) {
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
	public Iterable<IRNode> analyzeEnd(IIRProject p) {
    // Remove any control flow drops that aren't used for anything
    for (final ResultDropBuilder cfDrop : controlFlowDrops) {
      //System.out.println("Looking at control flow drop: "+cfDrop);
      if (cfDrop.getChecks().isEmpty()) {
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
		// clear the cached information
		clearPromiseRecordCache();

		try {
			final Set<TypeAndMethod> methods = shouldAnalyzeCompilationUnit(compUnit);

      if (runInParallel()) {
        runInParallel(TypeAndMethod.class, methods, new Procedure<TypeAndMethod>() {
          public void op(TypeAndMethod node) {
            final String methodName = JavaNames.genRelativeFunctionName(node.methodDecl);
            if (monitor != null) {
              monitor.subTask("Checking [ Uniqueness Assurance ] " + methodName);
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
            ImmutableHashOrderSet.clearCaches();
          }
        });
        return !methods.isEmpty();
      } else {
        // Analyze the given nodes
        for (Iterator<TypeAndMethod> iter = methods.iterator(); iter.hasNext();) {
          final TypeAndMethod node = iter.next();
          final String methodName = JavaNames.genQualifiedMethodConstructorName(node.methodDecl);
          if (monitor != null) {
            monitor.subTask("Checking [ Uniqueness Assurance ] " + methodName);
          }
          System.out.println("Sequential: " + methodName);
          analzyePseudoMethodDeclaration(node);
        }
        ImmutableHashOrderSet.clearCaches();
        return !methods.isEmpty();
      }
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Exception in unique assurance", e); //$NON-NLS-1$
		}
		return false;
	}

	/**
	 * @param decl A constructor declaration, method declaration, init declaration,
	 * or class init declaration node.
	 */
	private void analzyePseudoMethodDeclaration(final TypeAndMethod node) {
    final PromiseRecord pr = getCachedPromiseRecord(node);
    final Operator blockOp = JJNode.tree.getOperator(node.methodDecl);
    final boolean isInit = InitDeclaration.prototype.includes(blockOp);
    final boolean isClassInit = ClassInitDeclaration.prototype.includes(blockOp);
    final boolean isConstructorDecl = ConstructorDeclaration.prototype.includes(blockOp);
    final boolean isMethodDecl = MethodDeclaration.prototype.includes(blockOp);
	  
    /* if decl is a constructor declaration or initializer declaration, we need to
     * scan the containing type and process the field declarations and
     * initializer blocks.
     */
    if (isInit || isClassInit || isConstructorDecl) {
      for (final IRNode bodyDecl : ClassBody.getDeclIterator(node.getClassBody())) {
        if (FieldDeclaration.prototype.includes(bodyDecl) || ClassInitializer.prototype.includes(bodyDecl)) {
          if (isClassInit == TypeUtil.isStatic(bodyDecl)) {
            analyzeSubtree(node.methodDecl, pr, bodyDecl);
          }
        }
      }
    }
    
    if (isConstructorDecl || isMethodDecl) {
      analyzeSubtree(node.methodDecl, pr, node.methodDecl);
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
	private Set<TypeAndMethod> shouldAnalyzeCompilationUnit(final IRNode compUnit) {
    final NewShouldAnalyzeVisitor visitor = new NewShouldAnalyzeVisitor(getBinder());
	  visitor.doAccept(compUnit);
	  return visitor.getResults();
	}
	
	private void checkForError(
	    final IRNode insideDecl, final IRNode node, final PromiseRecord pr) {
		if (isInvalid(insideDecl, node)) {
		  final ResultDropBuilder cfDrop = pr.controlFlow;
		  cfDrop.setInconsistent();
		  cfDrop.addSupportingInformation(getErrorMessage(insideDecl, node), node);
		}
	}

	public void checkMethodCall(
	    final IRNode insideDecl, final IRNode node, final PromiseRecord pr) {
		if (JJNode.tree.getOperator(node) instanceof CallInterface) {
			final Set<ResultDropBuilder> callDrops = pr.callsToDrops.get(node);
			
			final IsPositivelyAssuredQuery q =
			  getAnalysis().getIsPositivelyAssuredQuery(insideDecl);
			if (q.getResultFor(node).booleanValue()) {
				for (ResultDropBuilder callDrop : callDrops) {
					callDrop.setConsistent();
					if (pr.calledUniqueParams.contains(callDrop)) {
					  callDrop.setResultMessage(Messages.UNIQUE_PARAMETERS_SATISFIED, DebugUnparser.toString(node));
					  callDrop.setCategory(Messages.DSC_UNIQUE_PARAMS_SATISFIED);
					}
				}
			} else {
				for (ResultDropBuilder callDrop : callDrops) {
					callDrop.setInconsistent();
          callDrop.addSupportingInformation(getErrorMessage(insideDecl, node), node);
					if (pr.calledUniqueParams.contains(callDrop)) {
					  callDrop.setResultMessage(Messages.UNIQUE_PARAMETERS_UNSATISFIED, DebugUnparser.toString(node));
					  callDrop.setCategory(Messages.DSC_UNIQUE_PARAMS_UNSATISFIED);
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

	private Map<IRNode, PromiseRecord> cachedPromiseRecord = null;

	private void clearPromiseRecordCache() {
		cachedPromiseRecord = new HashMap<IRNode, PromiseRecord>();
	}

	/**
	 * @param block
	 *          A MethodDeclaration, ConstructorDeclaration, InitDeclaration, or ClassInitDeclaration.
	 */
	private PromiseRecord getCachedPromiseRecord(final TypeAndMethod block) {
		/*
		String name = DebugUnparser.toString(block);
		if (name.contains("GameMap getMap()")) {
			System.out.println("Found: "+name);
		}
		*/
		PromiseRecord pr = cachedPromiseRecord.get(block.methodDecl);
		if (pr == null) {
			pr = createPromiseRecordFor(block);
			cachedPromiseRecord.put(block.methodDecl, pr);
		}
		return pr;
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

		/** The unique return value declared by this method */
		public final Set<UniquePromiseDrop> myUniqueReturn;

		/** Method call drops for each invoked method that has unique parameters */
		public final Set<ResultDropBuilder> calledUniqueParams;

		/**
		 * Method call drops for each invoked method that has unique return;
		 * Map from the method call drop to the unique promise about the return value.
		 */
		public final Map<ResultDropBuilder, UniquePromiseDrop> calledUniqueReturns;

		/**
		 * Method call drops for each invoked constructor that has a borrowed
		 * receiver.  Map from the method call drop to the borrowed promise.
		 */
		public final Map<ResultDropBuilder, BorrowedPromiseDrop> calledBorrowedConstructors;
		public final Map<ResultDropBuilder, UniquePromiseDrop> calledUniqueConstructors;
		
		/** Method call drops for each invoked method that has borrowed parameters */
		public final Set<ResultDropBuilder> calledBorrowedParams;
		public final Set<ResultDropBuilder> calledBorrowedReceiverAsUniqueReturn;

		/** Method call drops for each invoked method that has effects */
		public final Set<ResultDropBuilder> calledEffects;

		/** The unique fields accessed */
		public final Set<UniquePromiseDrop> uniqueFields;

		/** Drop for control-flow within this block */
		public final ResultDropBuilder controlFlow;

    /**
     * Result drop that aggregates together all the unique field promise drops.
     * Created lazily.
     */
    private ResultDropBuilder aggregatedUniqueFields;
    
    /** Need a separate flag because aggregatedUniqueFields is allowed to be null;
     */
    private boolean isAggregatedUniqueFieldsSet = false;

    /**
     * Result drop that aggregates together all the unique parameter promise drops.
     * Created lazily.
     */
    private ResultDropBuilder aggregatedUniqueParams;
    
    /** Need a separate flag because aggregatedUniqueParams is allowed to be null;
     */
    private boolean isAggregatedUniqueParamsSet = false;
		
		/**
		 * Map from method/constructor calls to the set of result drops that
		 * represent the calls.
		 */
		public final Map<IRNode, Set<ResultDropBuilder>> callsToDrops;

		private String name;
		
		public PromiseRecord(final IRNode block) {
		  methodDecl = block;
			myUniqueParams = new HashSet<UniquePromiseDrop>();
			myBorrowedParams = new HashSet<BorrowedPromiseDrop>();
			myUniqueReturn = new HashSet<UniquePromiseDrop>();
			calledUniqueReturns = new HashMap<ResultDropBuilder, UniquePromiseDrop>();
      calledBorrowedConstructors = new HashMap<ResultDropBuilder, BorrowedPromiseDrop>();
      calledUniqueConstructors = new HashMap<ResultDropBuilder, UniquePromiseDrop>();
			calledUniqueParams = new HashSet<ResultDropBuilder>();
      calledBorrowedParams = new HashSet<ResultDropBuilder>();
      calledBorrowedReceiverAsUniqueReturn = new HashSet<ResultDropBuilder>();
			calledEffects = new HashSet<ResultDropBuilder>();
			uniqueFields = new HashSet<UniquePromiseDrop>();

			callsToDrops = new HashMap<IRNode, Set<ResultDropBuilder>>();
			
			// Create the control flow drop for the block
			controlFlow = getMethodControlFlowDrop(block);
			name = DebugUnparser.toString(block);
		}
		
		@Override
		public String toString() {
			return name;
		}
		
		// Can return null
		public synchronized ResultDropBuilder getAggregatedUniqueFields() {
		  if (!isAggregatedUniqueFieldsSet) {
		    if (uniqueFields.isEmpty()) {
		      aggregatedUniqueFields = null;
		    } else {
          final ResultDropBuilder middleDrop = ResultDropBuilder.create(
              UniquenessAnalysisModule.this, Messages.toString(Messages.AGGREGATED_UNIQUE_FIELDS));
          middleDrop.setConsistent();
          middleDrop.setNode(methodDecl);
          middleDrop.setResultMessage(Messages.AGGREGATED_UNIQUE_FIELDS, JavaNames.genQualifiedMethodConstructorName(methodDecl));
          setResultDependUponDrop(middleDrop, methodDecl);
          for (final UniquePromiseDrop ud : uniqueFields) {
            middleDrop.addTrustedPromise(ud);
          }       
          aggregatedUniqueFields = middleDrop;
		    }
		    isAggregatedUniqueFieldsSet = true;
		  }
		  return aggregatedUniqueFields;
		}
    
    // Can return null
    public synchronized ResultDropBuilder getAggregatedUniqueParams() {
      if (!isAggregatedUniqueParamsSet) {
        if (myUniqueParams.isEmpty()) {
          aggregatedUniqueParams = null;
        } else {
          final ResultDropBuilder middleDrop = ResultDropBuilder.create(
              UniquenessAnalysisModule.this, Messages.toString(Messages.AGGREGATED_UNIQUE_PARAMS));
          middleDrop.setConsistent();
          middleDrop.setNode(methodDecl);
          middleDrop.setResultMessage(Messages.AGGREGATED_UNIQUE_PARAMS, JavaNames.genQualifiedMethodConstructorName(methodDecl));
          setResultDependUponDrop(middleDrop, methodDecl);
          for (final UniquePromiseDrop ud : myUniqueParams) {
            middleDrop.addTrustedPromise(ud);
          }       
          aggregatedUniqueParams = middleDrop;
        }
        isAggregatedUniqueParamsSet = true;
      }
      return aggregatedUniqueParams;
    }
	}

	private final Map<IRNode, ResultDropBuilder> cachedControlFlow = new ConcurrentHashMap<IRNode, ResultDropBuilder>();
	
	ResultDropBuilder getMethodControlFlowDrop(final IRNode block) {
    ResultDropBuilder drop = cachedControlFlow.get(block);
    if (drop == null || !drop.isValid()) {
      drop = ResultDropBuilder.create(this, Messages.toString(Messages.METHOD_CONTROL_FLOW));
      drop.setConsistent();
      setResultDependUponDrop(drop, block);

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
      drop.setResultMessage(Messages.METHOD_CONTROL_FLOW, label, unparse);
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
		final PromiseRecord pr = new PromiseRecord(block.methodDecl);
		final Operator blockOp = JJNode.tree.getOperator(block.methodDecl);
    final boolean isInit = InitDeclaration.prototype.includes(blockOp);
    final boolean isClassInit = ClassInitDeclaration.prototype.includes(blockOp);
    final boolean isConstructorDecl = ConstructorDeclaration.prototype.includes(blockOp);
    final boolean isMethodDecl = MethodDeclaration.prototype.includes(blockOp);

    /*
     * If the block is a constructor declaration or InitDeclaration or
     * ClassInitDeclaration we need to gather up the unique field declarations.
     */		
    if (isConstructorDecl || isInit || isClassInit) {
		  for (final IRNode bodyDecl : ClassBody.getDeclIterator(block.getClassBody())) {
		    if (FieldDeclaration.prototype.includes(bodyDecl)) {
		      if (isClassInit == TypeUtil.isStatic(bodyDecl)) {
  		      final IRNode variableDeclarators = FieldDeclaration.getVars(bodyDecl);
  		      for (IRNode varDecl : VariableDeclarators.getVarIterator(variableDeclarators)) {
  		        if (UniquenessRules.isUnique(varDecl)) {
  		          pr.uniqueFields.add(UniquenessRules.getUniqueDrop(varDecl));
  		        }
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
      getPromisesFromMethodDecl(block.methodDecl, pr.myUniqueReturn,
          pr.myBorrowedParams, new HashSet<BorrowedPromiseDrop>(),
          pr.myUniqueParams, new HashSet<RegionEffectsPromiseDrop>());
    }

		/* Look at the guts of the method/constructor/initializer.  If the block
		 * is a constructor or instance init declaration, we also look at all the 
		 * field declarations and initializer blocks.
		 */
    if (isConstructorDecl || isMethodDecl) {
      populatePromiseRecord(pr, block.methodDecl);
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


    
    /*
     * Set up the borrowed dependencies. Each parameter of the method that is
     * declared to be borrowed trusts the @borrowed annotations (including
     * @Unique("return") annotations on constructors) of any methods called by
     * the body of this method.
     */
		if (!pr.myBorrowedParams.isEmpty() ||
		    (isConstructorDecl && !pr.myUniqueReturn.isEmpty())) {
			final Set<ResultDropBuilder> dependsOnResults = new HashSet<ResultDropBuilder>(pr.calledBorrowedParams);
			dependsOnResults.addAll(pr.calledBorrowedReceiverAsUniqueReturn);
			dependsOnResults.add(pr.controlFlow);
      addDependencies(
          pr.myBorrowedParams, intermediateResultDrops, dependsOnResults);
      /* If we are a constructor, we treat unique("return") like @borrowed("this")
       */
      if (isConstructorDecl) {
        addDependencies(
            pr.myUniqueReturn, intermediateResultDrops, dependsOnResults);
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
      final Set<ResultDropBuilder> dependsOnResults = new HashSet<ResultDropBuilder>();
      final ResultDropBuilder aggregatedUniqueParams = pr.getAggregatedUniqueParams();
      if (aggregatedUniqueParams != null) dependsOnResults.add(aggregatedUniqueParams);
			final ResultDropBuilder aggregatedUniqueFields = pr.getAggregatedUniqueFields();
			if (aggregatedUniqueFields != null) dependsOnResults.add(aggregatedUniqueFields);
			dependsOnResults.add(pr.controlFlow);
      dependsOnResults.addAll(pr.calledUniqueReturns.keySet());
      dependsOnResults.addAll(pr.calledBorrowedConstructors.keySet());
      dependsOnResults.addAll(pr.calledUniqueConstructors.keySet());
			dependsOnResults.addAll(pr.calledBorrowedParams);
			dependsOnResults.addAll(pr.calledBorrowedReceiverAsUniqueReturn);
			dependsOnResults.addAll(pr.calledEffects);
			addDependencies(pr.uniqueFields, intermediateResultDrops, dependsOnResults);
		}

		/*
		 * Set up the dependencies for this method's unique return value. Depends on
		 * the unique parameters of the method, the unique return values of called
		 * methods, the unique fields accessed by this method, and the control-flow
		 * of the method itself.
		 */
		if (!isConstructorDecl && !pr.myUniqueReturn.isEmpty()) {
      final Set<ResultDropBuilder> dependsOnResults = new HashSet<ResultDropBuilder>();
      final ResultDropBuilder aggregatedUniqueParams = pr.getAggregatedUniqueParams();
      if (aggregatedUniqueParams != null) dependsOnResults.add(aggregatedUniqueParams);
      final ResultDropBuilder aggregatedUniqueFields = pr.getAggregatedUniqueFields();
      if (aggregatedUniqueFields != null) dependsOnResults.add(aggregatedUniqueFields);
	    dependsOnResults.add(pr.controlFlow);
  		dependsOnResults.addAll(pr.calledUniqueReturns.keySet());
      dependsOnResults.addAll(pr.calledBorrowedConstructors.keySet());
      dependsOnResults.addAll(pr.calledUniqueConstructors.keySet());
      addDependencies(
          pr.myUniqueReturn, intermediateResultDrops, dependsOnResults);
		}

		/* Set up the dependencies for this method's unique parameters.  They can
		 * be compromised and turned non-unique during the execution of the method.
		 * Depends on the the control-flow of the method.
		 */
		if (!pr.myUniqueParams.isEmpty()) {
      final Set<ResultDropBuilder> dependsOnResults = new HashSet<ResultDropBuilder>();
      dependsOnResults.add(pr.controlFlow);
		  addDependencies(
		      pr.myUniqueParams, intermediateResultDrops, dependsOnResults);
		}
		
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
			if (!(dependsOnPromises.isEmpty() && pr.calledUniqueReturns.isEmpty() && pr.calledBorrowedConstructors.isEmpty() && pr.calledUniqueConstructors.isEmpty())) {
				for (ResultDropBuilder callToCheck : pr.calledUniqueParams) {
					// Add depended upon promises
					for (final PromiseDrop<? extends IAASTRootNode> trustedPD : dependsOnPromises) {
						if (trustedPD != null) {
							callToCheck.addTrustedPromise(trustedPD);
						}
					}

					// Add depended on method calls
					for (Map.Entry<ResultDropBuilder, UniquePromiseDrop> entry : pr.calledUniqueReturns.entrySet()) {
					  final IRNode methodCall = entry.getKey().getNode();
					  callToCheck.addSupportingInformation(methodCall,
							  Messages.UNIQUE_RETURN_VALUE,
					          DebugUnparser.toString(methodCall));
					  callToCheck.addTrustedPromise(entry.getValue());
					}

          // Add depended on contructors with borrowed("this") or unique("return")
          for (Map.Entry<ResultDropBuilder, BorrowedPromiseDrop> entry : pr.calledBorrowedConstructors.entrySet()) {
            final IRNode constructorCall = entry.getKey().getNode();
            callToCheck.addSupportingInformation(constructorCall,
            		Messages.BORROWED_CONSTRUCTOR,
                    DebugUnparser.toString(constructorCall));
            callToCheck.addTrustedPromise(entry.getValue());
          }
          for (Map.Entry<ResultDropBuilder, UniquePromiseDrop> entry : pr.calledUniqueConstructors.entrySet()) {
            final IRNode constructorCall = entry.getKey().getNode();
            callToCheck.addSupportingInformation(constructorCall,
            		Messages.BORROWED_CONSTRUCTOR,
                    DebugUnparser.toString(constructorCall));
            callToCheck.addTrustedPromise(entry.getValue());
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
        if (UniquenessRules.isUnique(fdecl)) {
          pr.uniqueFields.add(UniquenessRules.getUniqueDrop(fdecl));
        }
      }

      // Is it a method call
      if (op instanceof CallInterface) {
        final IRNode declNode = getBinder().getBinding(currentNode);
        if (declNode == null) {
          LOG.warning("No binding for "+DebugUnparser.toString(currentNode));
          continue;
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
            declNode, uniqueReturns, borrowedParams, borrowedReceiver,
            uniqueParams, effects);

        // Create the method call drops
        final Set<ResultDropBuilder> allCallDrops = new HashSet<ResultDropBuilder>();
        final String label = DebugUnparser.toString(currentNode);
        if (!uniqueReturns.isEmpty()) {
          final ResultDropBuilder callDrop = getMethodCallDrop(Messages.toString(Messages.UNIQUE_RETURN),
              currentNode, uniqueReturns, Messages.UNIQUE_RETURN, label);
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
          final ResultDropBuilder callDrop = getMethodCallDrop(Messages.toString(Messages.BORROWED_PARAMETERS),
              currentNode, borrowedParams, Messages.BORROWED_PARAMETERS, label);
          allCallDrops.add(callDrop);
          pr.calledBorrowedParams.add(callDrop);
          
          if (isConstructorCall && !borrowedReceiver.isEmpty()) {
            // Borrowed receivers is a singleton set
            pr.calledBorrowedConstructors.put(callDrop, borrowedReceiver.iterator().next());
          }
        }
        if (!uniqueParams.isEmpty()) {
          /* Here we hold off setting the message and category until the 
           * call is actually assured in checkMethodCall()
           */
          final ResultDropBuilder callDrop = ResultDropBuilder.create(this, Messages.toString(Messages.UNIQUE_PARAMETERS_SATISFIED));
          callDrop.setConsistent();
          setResultDependUponDrop(callDrop, currentNode);
          // This result checks the uniqueness promises of the parameters
          for (final UniquePromiseDrop uniqueParam : uniqueParams) {
            callDrop.addCheckedPromise(uniqueParam);
          }
          allCallDrops.add(callDrop);
          pr.calledUniqueParams.add(callDrop);
        }
        if (!effects.isEmpty()) {
          final ResultDropBuilder callDrop = getMethodCallDrop(Messages.toString(Messages.CALL_EFFECT),
              currentNode, effects, Messages.CALL_EFFECT, label);
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
			final Set<BorrowedPromiseDrop> borrowedParams,
			final Set<BorrowedPromiseDrop> borrowedReceiver,
			final Set<UniquePromiseDrop> uniqueParams,
			final Set<RegionEffectsPromiseDrop> effects) {
		final Operator op = JJNode.tree.getOperator(mdecl);
		final boolean isConstructor = ConstructorDeclaration.prototype.includes(op);

		// Try to get the @unique returns drop if any
		final IRNode retDecl = JavaPromise.getReturnNodeOrNull(mdecl);
		if (retDecl != null) {
		  final UniquePromiseDrop returnsUniqueDrop =
		    UniquenessRules.getUniqueDrop(retDecl);
		  if (returnsUniqueDrop != null)
		    uniqueReturns.add(returnsUniqueDrop);
		}

		// Get the @borrowed and @unique params drops, if any
		if (!TypeUtil.isStatic(mdecl)) { // don't forget the receiver
			final IRNode self = JavaPromise.getReceiverNode(mdecl);
			final BorrowedPromiseDrop borrowedRcvrDrop = UniquenessRules.getBorrowedDrop(self);
			final UniquePromiseDrop uniqueRcvrDrop = UniquenessRules.getUniqueDrop(self);
			if (borrowedRcvrDrop != null) {
				borrowedParams.add(borrowedRcvrDrop);
				borrowedReceiver.add(borrowedRcvrDrop);
			}
			if (uniqueRcvrDrop != null) {
				uniqueParams.add(uniqueRcvrDrop);
			}
		}
		final IRNode myParams = isConstructor ? ConstructorDeclaration
				.getParams(mdecl) : MethodDeclaration.getParams(mdecl);
		for (int i = 0; i < JJNode.tree.numChildren(myParams); i++) {
			final IRNode param = JJNode.tree.getChild(myParams, i);
			final BorrowedPromiseDrop borrowedDrop = UniquenessRules.getBorrowedDrop(param);
			final UniquePromiseDrop uniqueDrop = UniquenessRules.getUniqueDrop(param);
			if (borrowedDrop != null) {
				borrowedParams.add(borrowedDrop);
			}
			if (uniqueDrop != null) {
				uniqueParams.add(uniqueDrop);
			}
		}

		// get effects
		if (true) {
			final RegionEffectsPromiseDrop fx = MethodEffectsRules.getRegionEffectsDrop(mdecl);
			if (fx != null)
				effects.add(fx);
		}
	}

	private <D extends PromiseDrop<? extends IAASTNode>> ResultDropBuilder
	getMethodCallDrop(final String type, final IRNode n, final Set<D> promises, int num, Object... args) {
		final ResultDropBuilder rd = ResultDropBuilder.create(this, type);
		rd.setConsistent();
		rd.setResultMessage(num, args);
		setResultDependUponDrop(rd, n);
		for (final D pd : promises) {
			rd.addTrustedPromise(pd);
		}
		return rd;
	}

	private <PD1 extends PromiseDrop<? extends IAASTRootNode>>
	void addDependencies(final Set<PD1> promises,
      final Map<PromiseDrop<? extends IAASTRootNode>, ResultDropBuilder> intermediateDrops,
			final Set<ResultDropBuilder> dependsOnResults) {
		if (!dependsOnResults.isEmpty()) {
			for (final PD1 promiseToCheck : promises) {
				// Add depended on method calls, etc.
				if (!dependsOnResults.isEmpty()) {
					for (ResultDropBuilder rd : dependsOnResults) {
						rd.addCheckedPromise(promiseToCheck);
					}
				}
			}
		}
	}
	
	
	
	private static final class TypeAndMethod {
	  public final IRNode typeDecl;
	  public final IRNode methodDecl;
	  
	  public TypeAndMethod(final IRNode td, final IRNode md) {
	    typeDecl = td;
	    methodDecl = md;
	  }
	  
    public IRNode getClassBody() {
      return AnonClassExpression.prototype.includes(typeDecl) ?
          AnonClassExpression.getBody(typeDecl) : TypeDeclaration.getBody(typeDecl);

    }
	  
	  @Override
    public boolean equals(final Object other) {
	    if (other instanceof TypeAndMethod) {
	      final TypeAndMethod tan = (TypeAndMethod) other;
	      return typeDecl == tan.typeDecl && methodDecl == tan.methodDecl;
	    } else {
	      return false;
	    }
	  }
	  
	  @Override
	  public int hashCode() {
	    int result = 17;
	    result = 31 * result + typeDecl.hashCode();
	    result = 31 * result + methodDecl.hashCode();
	    return result;
	  }
	}
	
  /*
   * We are searching for (1) the declarations of an unique field (2) the
   * use of an unique field, (3) the declaration of a method that has
   * borrowed parameters, unique parameters, or a unique return value, or
   * (4) the invocation of a method that has unique parameter requirements.
   */
	private static final class NewShouldAnalyzeVisitor extends JavaSemanticsVisitor {
    private final IBinder binder;
    
    /**
     * The output of the visitation: the set of method/constructor declarations
     * that should receive additional scrutiny by the uniqueness analysyis.
     */
    private final Set<TypeAndMethod> results = new HashSet<TypeAndMethod>();
    
    
    
    public NewShouldAnalyzeVisitor(final IBinder binder) {
      super(true);
      this.binder = binder;
    }

    
    
    public Set<TypeAndMethod> getResults() {
      return results;
    }
    
    
    
    /* Case 4: invoking method with unique parameter or borrowed parameters.
     * We care about borrowed parameters because they can affect the 
     * validity of unique fields passed to them.
     */
//    private void visitCallInterface(final IRNode call) {
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
            results.add(new TypeAndMethod(getEnclosingType(), getEnclosingDecl()));
          }
        }
      }
    }
    
    
    
//    @Override
//    public Void visitAllocationCallExpression(final IRNode call) {
//      visitCallInterface(call);
//      return null;
//    }
//    
//    @Override
//    protected void handleAnonClassExpression(final IRNode expr) {
//      doAccept(AnonClassExpression.getArgs(expr));
//      // Handle as a AllocationCallExpression (CallInterface really)
//      visitCallInterface(expr);
//    }
//
//    @Override
//    protected void handleEnumConstantClassDeclaration(final IRNode expr) {
//      doAccept(EnumConstantClassDeclaration.getArgs(expr));
//      // Handle as a AllocationCallExpression (CallInterface really)
//      visitCallInterface(expr);
//    }
//    
//    @Override
//    public Void visitCall(final IRNode call) {
//      visitCallInterface(call);
//      doAcceptForChildren(call);
//      return null;
//    }

    @Override
    protected void handleConstructorDeclaration(final IRNode cdecl) {
      // Case 3b: borrowed/unique parameter
      boolean hasBorrowedParam = false;
      boolean hasUniqueParam = false;
      hasBorrowedParam |= UniquenessRules.constructorYieldsUnaliasedObject(cdecl);
      // Cannot have a unique receiver

      final IRNode formals = ConstructorDeclaration.getParams(cdecl);
      for (int i = 0; i < JJNode.tree.numChildren(formals); i++) {
        final IRNode param = JJNode.tree.getChild(formals, i);
        hasBorrowedParam |= UniquenessRules.isBorrowed(param);
        hasUniqueParam |= UniquenessRules.isUnique(param);
      }
      if (hasBorrowedParam || hasUniqueParam) {
        results.add(new TypeAndMethod(getEnclosingType(), cdecl));
      }
            
      // Check the rest of the constructor
      doAcceptForChildren(cdecl);
    }

    @Override
    public Void visitFieldRef(final IRNode fieldRef) {
      /* Case (2): A use of a unique field. */
      if (UniquenessRules.isUnique(binder.getBinding(fieldRef))) {
        results.add(new TypeAndMethod(getEnclosingType(), getEnclosingDecl()));
      }
      return null;
    }
    
    @Override
    protected void handleMethodDeclaration(final IRNode mdecl) {
      /*
      System.out.println("Looking at: "+DebugUnparser.toString(mdecl));
      if ("getLength".equals(JJNode.getInfoOrNull(mdecl))) {
    	  for(IRNode n : JJNode.tree.bottomUp(mdecl)) {
    		  System.out.println("Node: "+JJNode.tree.getOperator(n));
    	  }
      }
      */

      // Case 3a: returns unique
      final IRNode retDecl = JavaPromise.getReturnNodeOrNull(mdecl);
      final boolean returnsUnique =
        (retDecl == null) ? false : UniquenessRules.isUnique(retDecl);

      // Case 3b: borrowed/unique parameter
      boolean hasBorrowedParam = false;
      boolean hasUniqueParam = false;
      if (!TypeUtil.isStatic(mdecl)) { // non-static method
        final IRNode self = JavaPromise.getReceiverNode(mdecl);
        hasBorrowedParam |= UniquenessRules.isBorrowed(self);
        hasUniqueParam |= UniquenessRules.isUnique(self);
      }
      final IRNode formals = MethodDeclaration.getParams(mdecl);
      for (int i = 0; i < JJNode.tree.numChildren(formals); i++) {
        final IRNode param = JJNode.tree.getChild(formals, i);
        hasBorrowedParam |= UniquenessRules.isBorrowed(param);
        hasUniqueParam |= UniquenessRules.isUnique(param);
      }
      if (returnsUnique || hasBorrowedParam || hasUniqueParam) {
        results.add(new TypeAndMethod(getEnclosingType(), mdecl));
      }
    
      doAcceptForChildren(mdecl);
    }
    
//    @Override
//    public Void visitSomeFunctionCall(final IRNode call) {
//      visitCallInterface(call);
//      doAcceptForChildren(call);
//      return null;
//    }
    
    @Override
    protected void handleFieldInitialization(final IRNode varDecl, final boolean isStatic) {
      /* CASE (1): If the field is UNIQUE then we
       * add the current enclosing declaration to the results.
       */
      if (UniquenessRules.isUnique(varDecl)) {
        results.add(new TypeAndMethod(getEnclosingType(), getEnclosingDecl()));
      }
      // analyze the the RHS of the initialization
      doAcceptForChildren(varDecl);
    }
	}
}
