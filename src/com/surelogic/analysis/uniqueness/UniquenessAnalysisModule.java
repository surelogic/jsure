package com.surelogic.analysis.uniqueness;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.Effects;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.control.FlowAnalysis;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.promise.ClassInitDeclaration;
import edu.cmu.cs.fluid.java.promise.InitDeclaration;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;

public class UniquenessAnalysisModule extends AbstractWholeIRAnalysis<UniqueAnalysis,Void> {
  private static final Category DSC_UNIQUE_PARAMS_SATISFIED =
    Category.getInstance(Messages.Category_uniqueParametersSatisfied);
  
  private static final Category DSC_UNIQUE_PARAMS_UNSATISFIED =
    Category.getInstance(Messages.Category_uniqueParametersUnsatisfied);
  
  
  
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
  private final Set<ResultDropBuilder> controlFlowDrops = new HashSet<ResultDropBuilder>();
  
  
  
  public UniquenessAnalysisModule() {
		super(true && !singleThreaded, null, "UniqueAnalysis");
	}
  
	public void init(IIRAnalysisEnvironment env) {
		// Nothing to do
	}

	@Override
	protected void startAnalyzeBegin(IIRProject p, IBinder binder) {
		// Nothing to do
	}

	@Override
	protected UniqueAnalysis constructIRAnalysis(IBinder binder) {
		//System.out.println(Thread.currentThread()+" : Constructed Unique for "+
		//           binder.getTypeEnvironment().getProject());
		return new UniqueAnalysis(binder,
				new Effects(binder, new BindingContextAnalysis(binder)));
	}
	
	/*
	@Override
	protected void finishAnalyzeBegin(IIRProject p, IBinder binder) {
		if (runInParallel()) {
			runInParallel(Void.class, nulls, new Procedure<Void>() {
				public void op(Void v) {
					UniqueAnalysis a = getAnalysis();
					System.out.println(Thread.currentThread()+" : Analysis for "+
				           a.binder.getTypeEnvironment().getProject());
				}
			});
		} else {
			UniqueAnalysis a = getAnalysis();
			System.out.println(Thread.currentThread()+" : Analysis for "+
		           a.binder.getTypeEnvironment().getProject());
		}
	}
	*/
	
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
	protected boolean doAnalysisOnAFile(CUDrop cud, IRNode compUnit, IAnalysisMonitor monitor) {
		return checkUniquenessForFile(compUnit, monitor);
	}

	@Override
	public IRNode[] analyzeEnd(IIRProject p) {
    // Remove any control flow drops that aren't used for anything
    for (final ResultDropBuilder cfDrop : controlFlowDrops) {
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
		return JavaGlobals.noNodes;
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
        	final DebugUnparser unparser = new DebugUnparser(10, JJNode.tree);
            String s = unparser.unparseString(node.methodDecl);
            final long start = System.currentTimeMillis();
            analzyePseudoMethodDeclaration(node);
            final long end = System.currentTimeMillis();
            System.out.println("Parallel: " + methodName + " -- "+(end-start)+" ms");
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
      for (final IRNode bodyDecl : ClassBody.getDeclIterator(TypeDeclaration.getBody(node.typeDecl))) {
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
//    final ShouldAnalyzeVisitor visitor = new ShouldAnalyzeVisitor(getBinder());
    final NewShouldAnalyzeVisitor visitor = new NewShouldAnalyzeVisitor(getBinder());
	  visitor.doAccept(compUnit);
	  return visitor.getResults();
	}
	
//	/**
//	 * Returns <code>null</code> if the compilation unit contains the
//	 * declaration or use of a unique field. Otherwise returns the set of the
//	 * methods in the CU that need to be analyzed because they have borrowed or
//	 * unique annotations or because they invoke a method that has unique
//	 * parameter requirements.
//	 *
//	 * @param compUnit
//	 *          A compilation unit node.
//	 * @return <code>null</code> or a set of method/constructor declarations.
//	 */
//	private Set<IRNode> shouldAnalyzeCompilationUnit(final IRNode compUnit) {
//		final Set<IRNode> rootNodesForAnalysis = new HashSet<IRNode>();
//
//		final IRNode typeDecls =
//		  edu.cmu.cs.fluid.java.operator.CompilationUnit.getDecls(compUnit);
//		final Iterator<IRNode> decls = TypeDeclarations.getTypesIterator(typeDecls);
//		while (decls.hasNext()) {
//			final IRNode decl = decls.next();
//			final Iterator<IRNode> nodes = JJNode.tree.topDown(decl);
//			while (nodes.hasNext()) {
//				final IRNode currentNode = nodes.next();
//				final Operator op = JJNode.tree.getOperator(currentNode);
//
//				// Ignore Annotation elements
//				if (AnnotationElement.prototype.includes(op)) {
//					continue;
//				}
//
//				/*
//				 * We are searching for (1) the declarations of an unique field (2) the
//				 * use of an unique field, (3) the declaration of a method that has
//				 * borrowed parameters, unique parameters, or a unique return value, or
//				 * (4) the invocation of a method that has unique parameter requirements.
//				 */
//
//				/*
//				 * NEW Case 1: Initializer of an unique field declaration. This
//				 * represents an implicit assignment to the field, and needs to be
//				 * captured so we can get the control-flow dependency.
//				 */
//				if (Initialization.prototype.includes(op)) {
//					final IRNode variableDeclarator = JJNode.tree.getParent(currentNode);
//					final IRNode variableDeclarators = JJNode.tree.getParent(variableDeclarator);
//					final IRNode possibleFieldDeclaration = JJNode.tree.getParent(variableDeclarators);
//					if (FieldDeclaration.prototype.includes(
//							JJNode.tree.getOperator(possibleFieldDeclaration))) {
//						if (UniquenessRules.isUnique(variableDeclarator)) {
//							rootNodesForAnalysis.add(possibleFieldDeclaration);
//						}
//					}
//				}
//
//				/*
//				 * Case 2
//				 */
//				if (FieldRef.prototype.equals(op)) {
//					if (UniquenessRules.isUnique(getBinder().getBinding(currentNode))) {
//						rootNodesForAnalysis.add(getContainingBlock(currentNode));
//					}
//				}
//
//				if (ConstructorDeclaration.prototype.equals(op)
//						|| MethodDeclaration.prototype.equals(op)) {
//					boolean hasBorrowedParam = false;
//					boolean hasUniqueParam = false;
//					boolean returnsUnique = false;
//
//					// Case 3a: returns unique
//					if (MethodDeclaration.prototype.equals(op)) {
//						final IRNode retDecl = JavaPromise.getReturnNodeOrNull(currentNode);
//						returnsUnique = (retDecl == null) ? false : UniquenessRules
//								.isUnique(retDecl);
//					}
//
//					// Case 3b: borrowed/unique parameter
//          if (ConstructorDeclaration.prototype.includes(op)) {
//            hasBorrowedParam |= UniquenessRules.constructorYieldsUnaliasedObject(currentNode);
//            // Cannot have a unique receiver
//          } else {
//            if (!TypeUtil.isStatic(currentNode)) { // non-static method
//              final IRNode self = JavaPromise.getReceiverNode(currentNode);
//              hasBorrowedParam |= UniquenessRules.isBorrowed(self);
//              hasUniqueParam |= UniquenessRules.isUnique(self);
//            }
//          }
//					IRNode formals = null;
//					if (ConstructorDeclaration.prototype.includes(op)) {
//						formals = ConstructorDeclaration.getParams(currentNode);
//					} else {
//						formals = MethodDeclaration.getParams(currentNode);
//					}
//					for (int i = 0; i < JJNode.tree.numChildren(formals); i++) {
//            final IRNode param = JJNode.tree.getChild(formals, i);
//            hasBorrowedParam |= UniquenessRules.isBorrowed(param);
//            hasUniqueParam |= UniquenessRules.isUnique(param);
//					}
//					if (returnsUnique || hasBorrowedParam || hasUniqueParam)
//						rootNodesForAnalysis.add(currentNode);
//				}
//
//				// Case 4: invoking method with unique parameter
//				if (op instanceof CallInterface) {
//					final IRNode declNode = getBinder().getBinding(currentNode);
//
//					if (declNode != null) {
//						final Operator declOp = JJNode.tree.getOperator(declNode);
//						IRNode formals = null;
//
//						if (declOp instanceof ConstructorDeclaration) {
//							formals = ConstructorDeclaration.getParams(declNode);
//						} else if (declOp instanceof MethodDeclaration) {
//							formals = MethodDeclaration.getParams(declNode);
//						}
//						if (formals != null) {
//							boolean hasUniqueParam = false;
//
//							if (!TypeUtil.isStatic(declNode)) {
//								final IRNode self = JavaPromise.getReceiverNode(declNode);
//								hasUniqueParam |= UniquenessRules.isUnique(self);
//							}
//							for (int i = 0; !hasUniqueParam
//							&& (i < JJNode.tree.numChildren(formals)); i++) {
//								hasUniqueParam = UniquenessRules.isUnique(JJNode.tree
//										.getChild(formals, i));
//							}
//							if (hasUniqueParam) {
//								final IRNode currentMethod = getContainingBlock(currentNode);
//								if (currentMethod != null) {
//									rootNodesForAnalysis.add(currentMethod);
//								} else {
//									// method call not in a method! analyze everything!
//									return null;
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//
//		return rootNodesForAnalysis;
//	}

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
			
			if (getAnalysis().isPositivelyAssured(node, insideDecl)) {
				for (ResultDropBuilder callDrop : callDrops) {
					callDrop.setConsistent();
					if (pr.calledUniqueParams.contains(callDrop)) {
					  callDrop.setResultMessage(Messages.uniqueParametersSatisfied, DebugUnparser.toString(node));
					  callDrop.setCategory(DSC_UNIQUE_PARAMS_SATISFIED);
					}
				}
			} else {
				for (ResultDropBuilder callDrop : callDrops) {
					callDrop.setInconsistent();
          callDrop.addSupportingInformation(getErrorMessage(insideDecl, node), node);
					if (pr.calledUniqueParams.contains(callDrop)) {
					  callDrop.setResultMessage(Messages.uniqueParametersUnsatisfied, DebugUnparser.toString(node));
					  callDrop.setCategory(DSC_UNIQUE_PARAMS_UNSATISFIED);
					}
				}
			}
		}
	}

	/**
	 * Is the node the source of a uniqueness problem?
	 */
	private boolean isInvalid(final IRNode insideDecl, final IRNode node) {
		final UniqueAnalysis a = getAnalysis();
		//System.out.println("Unique using "+a.binder.getTypeEnvironment().getProject());
		
		/* Definitely not erroneous */
		if (!a.isInvalid(node, insideDecl))
			return false;

		/* Node is erroneous, but does the error come from a child? */
		for (Iterator<IRNode> ch = JJNode.tree.children(node); ch.hasNext();) {
			final IRNode n = ch.next();
			/* Problem comes from a child, so parent is not to blame */
			if (a.isInvalid(n, insideDecl))
				return false;
		}
		/* Not a problem from a child. */
		return true;
	}

	/**
	 * Assumes that isInvalid( n ) is true
	 */
	@SuppressWarnings("unchecked")
  private String getErrorMessage(final IRNode insideDecl, final IRNode n) {
	  final FlowAnalysis a = getAnalysis().getAnalysis(insideDecl);
		final String normErr = getAnalysis().getNormalErrorMessage(a, n);
		final String abruptErr = getAnalysis().getAbruptErrorMessage(a, n);

		if (normErr != UniqueAnalysis.NOT_AN_ERROR) {
			if (abruptErr != UniqueAnalysis.NOT_AN_ERROR) {
				return "(" + normErr + ", " + abruptErr + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			return normErr;
		}
		if (abruptErr != UniqueAnalysis.NOT_AN_ERROR) {
			return abruptErr + " (abrupt)"; //$NON-NLS-1$
		}
		// Shouldn't ever get here unless we are misused
		return UniqueAnalysis.NOT_AN_ERROR;
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
              UniquenessAnalysisModule.this, "aggregatedUniqueFields");
          middleDrop.setConsistent();
          middleDrop.setNode(methodDecl);
          middleDrop.setResultMessage(Messages.aggregatedUniqueFields, JavaNames.genQualifiedMethodConstructorName(methodDecl));
          setResultDependUponDrop(middleDrop);
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
              UniquenessAnalysisModule.this, "aggregatedUniqueParams");
          middleDrop.setConsistent();
          middleDrop.setNode(methodDecl);
          middleDrop.setResultMessage(Messages.aggregatedUniqueParams, JavaNames.genQualifiedMethodConstructorName(methodDecl));
          setResultDependUponDrop(middleDrop);
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

	private final Map<IRNode, ResultDropBuilder> cachedControlFlow = new HashMap<IRNode, ResultDropBuilder>();
	
	ResultDropBuilder getMethodControlFlowDrop(final IRNode block) {
    ResultDropBuilder drop = cachedControlFlow.get(block);
    if (drop == null || !drop.isValid()) {
      drop = ResultDropBuilder.create(this, "methodControlFlow");
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
      drop.setResultMessage(Messages.methodControlFlow, label, unparse);
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
		  for (final IRNode bodyDecl : ClassBody.getDeclIterator(TypeDeclaration.getBody(block.typeDecl))) {
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
      for (final IRNode bodyDecl : ClassBody.getDeclIterator(TypeDeclaration.getBody(block.typeDecl))) {
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
							  Messages.uniqueReturnValue,
					          DebugUnparser.toString(methodCall));
					  callToCheck.addTrustedPromise(entry.getValue());
					}

          // Add depended on contructors with borrowed("this") or unique("return")
          for (Map.Entry<ResultDropBuilder, BorrowedPromiseDrop> entry : pr.calledBorrowedConstructors.entrySet()) {
            final IRNode constructorCall = entry.getKey().getNode();
            callToCheck.addSupportingInformation(constructorCall,
            		Messages.borrowedConstructor,
                    DebugUnparser.toString(constructorCall));
            callToCheck.addTrustedPromise(entry.getValue());
          }
          for (Map.Entry<ResultDropBuilder, UniquePromiseDrop> entry : pr.calledUniqueConstructors.entrySet()) {
            final IRNode constructorCall = entry.getKey().getNode();
            callToCheck.addSupportingInformation(constructorCall,
            		Messages.borrowedConstructor,
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
          final ResultDropBuilder callDrop = getMethodCallDrop("uniqueReturnDrop",
              currentNode, uniqueReturns, Messages.uniqueReturnDrop, label);
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
          final ResultDropBuilder callDrop = getMethodCallDrop("borrowedParametersDrop",
              currentNode, borrowedParams, Messages.borrowedParametersDrop, label);
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
          final ResultDropBuilder callDrop = ResultDropBuilder.create(this, "uniqueParametersDrop");
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
          final ResultDropBuilder callDrop = getMethodCallDrop("effectOfCallDrop",
              currentNode, effects, Messages.effectOfCallDrop, label);
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
     * We care about borrowed parameters because then can affect the 
     * validity of unique fields passed to them.
     */
    private void visitCallInterface(final IRNode call) {
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
    
    
    
    @Override
    public Void visitAllocationCallExpression(final IRNode call) {
      visitCallInterface(call);
      return null;
    }
    
    @Override
    protected void handleAnonClassExpression(final IRNode expr) {
      doAccept(AnonClassExpression.getArgs(expr));
      // Handle as a AllocationCallExpression (CallInterface really)
      visitCallInterface(expr);
    }

    @Override
    protected InstanceInitAction getAnonClassInitAction(final IRNode expr) {
      // We want to visit the initializers, but there is nothing 
      // interesting to do with the action
      return NULL_ACTION;
    }

    @Override
    public Void visitCall(final IRNode call) {
      visitCallInterface(call);
      doAcceptForChildren(call);
      return null;
    }

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
    
    @Override
    public Void visitSomeFunctionCall(final IRNode call) {
      visitCallInterface(call);
      doAcceptForChildren(call);
      return null;
    }
    
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
	
//  /*
//   * We are searching for (1) the declarations of an unique field (2) the
//   * use of an unique field, (3) the declaration of a method that has
//   * borrowed parameters, unique parameters, or a unique return value, or
//   * (4) the invocation of a method that has unique parameter requirements.
//   */
//	private static final class ShouldAnalyzeVisitor extends VoidTreeWalkVisitor {
//	  private final IBinder binder;
//	  
//	  private boolean isAnonClassExpression = false;
//	  
//	  /**
//	   * The current type declaration we are inside of.
//	   */
//	  private IRNode enclosingType = null;
//	  
//	  /**
//	   * The current method/constructor declaration that we are inside of.
//	   */
//	  private IRNode enclosingDecl = null;
//	  
//	  /**
//	   * Whether we are inside a constructor declaration.  Also true if we 
//	   * are analyzing the InitDeclaration associated with the construction of
//	   * an anonymous class.
//	   */
//	  private boolean insideConstructor = false;
//	  
//	  /**
//	   * The output of the visitation: the set of method/constructor declarations
//	   * that should receive additional scrutiny by the uniqueness analysyis.
//	   */
//	  private final Set<TypeAndMethod> results = new HashSet<TypeAndMethod>();
//	  
//	  
//	  
//	  public ShouldAnalyzeVisitor(final IBinder binder) {
//	    this.binder = binder;
//	  }
//
//	  
//	  
//	  public Set<TypeAndMethod> getResults() {
//	    return results;
//	  }
//	  
//	  
//	  
//	  private void visitNonAnnotationTypeDeclaration(final IRNode typeDecl) {
//      final IRNode prevEnclosingType = enclosingType;
//      final IRNode prevEnclosingDecl = enclosingDecl;
//      final boolean prevInsideConstructor = insideConstructor;
//      try {
//        enclosingType = typeDecl;
//        enclosingDecl = null;
//        insideConstructor = false;
//        doAcceptForChildren(typeDecl);
//      } finally {
//        enclosingType = prevEnclosingType;
//        enclosingDecl = prevEnclosingDecl;
//        insideConstructor = prevInsideConstructor;
//      }
//	  }
//	  
//    /* Case 4: invoking method with unique parameter or borrowed parameters.
//	   * We care about borrowed parameters because then can affect the 
//	   * validity of unique fields passed to them.
//	   */
//    private void visitCallInterface(final IRNode call) {
//      final IRNode declNode = binder.getBinding(call);
//
//      if (declNode != null) {
//        final Operator declOp = JJNode.tree.getOperator(declNode);
//        IRNode formals = null;
//        boolean hasUnique = false;
//        if (declOp instanceof ConstructorDeclaration) {
//          formals = ConstructorDeclaration.getParams(declNode);
//        } else if (declOp instanceof MethodDeclaration) {
//          formals = MethodDeclaration.getParams(declNode);
//          if (!TypeUtil.isStatic(declNode)) {
//            final IRNode self = JavaPromise.getReceiverNode(declNode);
//            hasUnique = UniquenessRules.isUnique(self);
//          }
//        }
//        if (formals != null) {
//          for (int i = 0; !hasUnique && (i < JJNode.tree.numChildren(formals)); i++) {
//            final IRNode param = JJNode.tree.getChild(formals, i);
//            hasUnique = UniquenessRules.isUnique(param);
//          }
//          if (hasUnique) {
//            results.add(new TypeAndMethod(enclosingType, enclosingDecl));
//          }
//        }
//      }
//    }
//	  
//    
//	  
//	  @Override
//	  public Void visitAllocationCallExpression(final IRNode call) {
//	    visitCallInterface(call);
//	    /* The guts of an anonymous class expression are visited specially
//	     * by visitAnonClassExpression().  If we do this traversal here
//	     * with an anonymous class expression, we will get a NullPointerException.
//	     */
//	    if (!isAnonClassExpression) {
//	      doAcceptForChildren(call);
//	    }
//	    return null;
//	  }
//	  
//	  @Override
//	  public Void visitAnonClassExpression(final IRNode expr) {
//	    // Traverse into the arguments, but *not* the body
//	    doAccept(AnonClassExpression.getArgs(expr));
//	    
//	    /* We are going to recursively re-enter this class via the use of an
//	     * InstanceInitVisitor instance.
//	     */
//	    final IRNode prevEnclosingType = enclosingType;
//      final boolean prevInsideConstructor = insideConstructor;
//      final IRNode prevEnclosingDecl = enclosingDecl;
//	    try {
//        enclosingType = expr; // Now inside the anonymous type declaration
//        insideConstructor = false; // start by assuming we are not in the constructor
//        
//        InstanceInitializationVisitor.processAnonClassExpression(expr, this,
//            new Action() {
//              public void tryBefore() {
//                enclosingDecl = JavaPromise.getInitMethodOrNull(expr); // Inside the <init> method
//                insideConstructor = true; // We are inside the constructor of the anonymous class
//              }
//              
//              public void finallyAfter() {
//                enclosingDecl = prevEnclosingDecl; // Restore to the original value
//                insideConstructor = false;
//              }
//            });
//        	      
//        /* Now visit the rest of the anonymous class looking for additional
//         * classes to analyze, so we reset the enclosing declaration to null.
//         */
//	      try {
//	        enclosingDecl = null; // We are not inside of any method or constructor
//	        doAccept(AnonClassExpression.getBody(expr));
//	      } finally {
//	        enclosingDecl = prevEnclosingDecl; // finally restore to the original value
//	      }
//	    } finally {
//	      // restore the global state
//	      enclosingType = prevEnclosingType;
//	      insideConstructor = prevInsideConstructor;
//	    }
//	    
//	    /* Call super implementation so we also process this as an allocation call
//	     * expression. 
//	     */
//	    try {
//	      isAnonClassExpression = true;
//	      return super.visitAnonClassExpression(expr);
//	    } finally {
//	      isAnonClassExpression = false;
//	    }
//	  }
//
//    @Override
//    public Void visitCall(final IRNode call) {
//      visitCallInterface(call);
//      doAcceptForChildren(call);
//      return null;
//    }
//
//    @Override
//	  public Void visitClassDeclaration(final IRNode classDecl) {
//	    visitNonAnnotationTypeDeclaration(classDecl);
//      return null;
//	  }
//	  
//	  @Override
//	  public Void visitClassInitializer(final IRNode expr) {
//	    if (TypeUtil.isStatic(expr)) {
//	      enclosingDecl = ClassInitDeclaration.getClassInitMethod(enclosingType);
//	      try {
//	        doAcceptForChildren(expr);
//	      } finally {
//	        enclosingDecl = null;
//	      }
//	    } else {
//	      /* Only go inside of instance initializers if we are being called by the
//	       * InstanceInitVisitor! In this case, the InstanceInitVisitor directly
//	       * traverses into the children of the ClassInitializer, so we don't even
//	       * get here.
//	       */
//	    }
//	    return null;
//	  }
//
//	  @Override
//	  public Void visitConstructorCall(final IRNode expr) {
//	    // continue into the expression
//	    doAcceptForChildren(expr);
//
//	    // Make sure we account for the super class's field inits, etc
//      InstanceInitializationVisitor.processConstructorCall(expr, TypeDeclaration.getBody(enclosingType), this);
//
//	    return null;
//	  }
//	  
//	  @Override
//	  public Void visitConstructorDeclaration(final IRNode cdecl) {
//      enclosingDecl = cdecl;
//      insideConstructor = true;
//      try {
//        // Case 3b: borrowed/unique parameter
//        boolean hasBorrowedParam = false;
//        boolean hasUniqueParam = false;
//        hasBorrowedParam |= UniquenessRules.constructorYieldsUnaliasedObject(cdecl);
//        // Cannot have a unique receiver
//
//        final IRNode formals = ConstructorDeclaration.getParams(cdecl);
//        for (int i = 0; i < JJNode.tree.numChildren(formals); i++) {
//          final IRNode param = JJNode.tree.getChild(formals, i);
//          hasBorrowedParam |= UniquenessRules.isBorrowed(param);
//          hasUniqueParam |= UniquenessRules.isUnique(param);
//        }
//        if (hasBorrowedParam || hasUniqueParam) {
//          results.add(new TypeAndMethod(enclosingType, cdecl));
//        }
//              
//        // Check the rest of the constructor
//        doAcceptForChildren(cdecl);
//      } finally {
//        enclosingDecl = null;
//        insideConstructor = false;
//      }
//      return null;
//	  }
//
//    @Override
//    public Void visitEnumDeclaration(final IRNode enumDecl) {
//      visitNonAnnotationTypeDeclaration(enumDecl);
//      return null;
//    }
//
//    @Override
//    public Void visitFieldRef(final IRNode fieldRef) {
//      /* Case (2): A use of a unique field. */
//      if (UniquenessRules.isUnique(binder.getBinding(fieldRef))) {
//        results.add(new TypeAndMethod(enclosingType, enclosingDecl));
//      }
//      return null;
//    }
//    
//    @Override
//    public Void visitInterfaceDeclaration(final IRNode interfaceDecl) {
//      visitNonAnnotationTypeDeclaration(interfaceDecl);
//      return null;
//    }
//    
//    @Override
//    public Void visitMethodDeclaration(final IRNode mdecl) {
//      enclosingDecl = mdecl;
//      try {
//        // Case 3a: returns unique
//        final IRNode retDecl = JavaPromise.getReturnNodeOrNull(mdecl);
//        final boolean returnsUnique =
//          (retDecl == null) ? false : UniquenessRules.isUnique(retDecl);
//
//        // Case 3b: borrowed/unique parameter
//        boolean hasBorrowedParam = false;
//        boolean hasUniqueParam = false;
//        if (!TypeUtil.isStatic(mdecl)) { // non-static method
//          final IRNode self = JavaPromise.getReceiverNode(mdecl);
//          hasBorrowedParam |= UniquenessRules.isBorrowed(self);
//          hasUniqueParam |= UniquenessRules.isUnique(self);
//        }
//        final IRNode formals = MethodDeclaration.getParams(mdecl);
//        for (int i = 0; i < JJNode.tree.numChildren(formals); i++) {
//          final IRNode param = JJNode.tree.getChild(formals, i);
//          hasBorrowedParam |= UniquenessRules.isBorrowed(param);
//          hasUniqueParam |= UniquenessRules.isUnique(param);
//        }
//        if (returnsUnique || hasBorrowedParam || hasUniqueParam) {
//          results.add(new TypeAndMethod(enclosingType, mdecl));
//        }
//      
//        doAcceptForChildren(mdecl);
//      } finally {
//        enclosingDecl = null;
//      }
//      return null;
//    }
//    
//    @Override
//    public Void visitSomeFunctionCall(final IRNode call) {
//      visitCallInterface(call);
//      doAcceptForChildren(call);
//      return null;
//    }
//
//	  @Override
//	  public Void visitVariableDeclarator(final IRNode varDecl) {
//	    /* If this is inside a FieldDeclaration, then we only want to run if we are
//	     * being executed on behalf of the InstanceInitHelper or if we are part of a
//	     * static field declaration.
//	     * 
//	     * If this inside a DeclStatement, then we always want to run, and we don't
//	     * do anything special at all. (I would like to avoid having to climb up the
//	     * parse tree, but I don't have a choice because InstanceInitHelper does not
//	     * call back into FieldDeclaration, but into the children of
//	     * FieldDeclaration.)
//	     */
//	    if (FieldDeclaration.prototype.includes(
//	        JJNode.tree.getOperator(
//	            JJNode.tree.getParentOrNull(
//	                JJNode.tree.getParentOrNull(varDecl))))) {      
//        /* Analyze the field initialization if we are inside a constructor or
//         * visiting a static field.
//         */
//	      final boolean isStaticDeclaration = TypeUtil.isStatic(varDecl);
//	      if (insideConstructor || isStaticDeclaration) {
//	        /* At this point we know we are inside a field declaration that is
//	         * being analyzed on behalf of a constructor or a static initializer.
//	         */
//	        final IRNode init = VariableDeclarator.getInit(varDecl);
//	        // Don't worry about uninitialized fields
//	        if (!NoInitialization.prototype.includes(JJNode.tree.getOperator(init))) {
//	          /* If the initialization is static, we have to update the enclosing 
//	           * method to the class init declaration. 
//	           */
//	          if (isStaticDeclaration) {
//	            enclosingDecl = ClassInitDeclaration.getClassInitMethod(enclosingType);
//	          }
//	          try {
//	            /* We have a non-empty initialization of a field inside on
//	             * behalf of a constructor or class initializer.  This counts as
//	             * a use of the field.  CASE (1): If the field is UNIQUE then we
//	             * add the current enclosing declaration to the results.
//	             */
//	            if (UniquenessRules.isUnique(varDecl)) {
//	              results.add(new TypeAndMethod(enclosingType, enclosingDecl));
//	            }
//	            // analyze the the RHS of the initialization
//	            doAcceptForChildren(varDecl);
//	          } finally {
//	            if (isStaticDeclaration) {
//	              enclosingDecl = null;
//	            }
//	          }
//	        }
//	      }
//	    } else {
//	      /* Not a field declaration: so we are in a local variable declaration.
//	       * Always analyze its contents.
//	       */
//	      doAcceptForChildren(varDecl);
//	    }
//	    return null;
//	  }
//	}
}
