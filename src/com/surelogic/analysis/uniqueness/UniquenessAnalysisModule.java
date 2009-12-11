/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/uniqueness/UniquenessAnalysisModule.java,v 1.2 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis.uniqueness;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.EffectsVisitor;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.effects.RegionEffectsPromiseDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;

public class UniquenessAnalysisModule extends AbstractWholeIRAnalysis<UniqueAnalysis> {
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
		super("UniqueAnalysis");
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
		return new UniqueAnalysis(binder,
				new EffectsVisitor(binder, new BindingContextAnalysis(binder)));
	}

	@Override
	protected void clearCaches() {
	  intermediateResultDrops.clear();
	  
	  // Remove any control flow drops that aren't used for anything
	  for (final ResultDropBuilder cfDrop : controlFlowDrops) {
	    if (cfDrop.getChecks().isEmpty()) {
	      cfDrop.invalidate();
	    }
	  }
	  controlFlowDrops.clear();
	  
		getAnalysis().clearCaches();
	}
	
	@Override
	protected boolean doAnalysisOnAFile(CUDrop cud, IRNode compUnit, IAnalysisMonitor monitor) {
		return checkUniquenessForFile(compUnit, monitor);
	}

	@Override
	public IRNode[] analyzeEnd(IIRProject p) {
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
			final Set<IRNode> methods = shouldAnalyzeCompilationUnit(compUnit);
			if (methods == null) {
				// Analyze everything!
				final IRNode typeDecls =
					edu.cmu.cs.fluid.java.operator.CompilationUnit.getDecls(compUnit);
				final Iterator<IRNode> decls =
					TypeDeclarations.getTypesIterator(typeDecls);
				while (decls.hasNext()) {
					final IRNode typeDecl = decls.next();
					if (monitor != null) {
						monitor.subTask("Checking [ Uniqueness Assurance ] "+
								        JavaNames.getFullTypeName(typeDecl));
					}
					analyzeSubtree(typeDecl);
				}
				return true;
			} else if (!singleThreaded) {
				runInParallel(IRNode.class, methods, new Procedure<IRNode>() {
					public void op(IRNode node) {
						if (monitor != null) {
							monitor.subTask("Checking [ Uniqueness Assurance ] "+
									        JavaNames.genRelativeFunctionName(node));
						}
						analyzeSubtree(node);
					}
				});
				return !methods.isEmpty();				
			} else {
				// Analyze the given nodes
				for (Iterator<IRNode> iter = methods.iterator(); iter.hasNext();) {
					final IRNode node = iter.next();
					
					if (monitor != null) {
						monitor.subTask("Checking [ Uniqueness Assurance ] "+
								        JavaNames.genRelativeFunctionName(node));
					}
					analyzeSubtree(node);
				}
				return !methods.isEmpty();
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Exception in unique assurance", e); //$NON-NLS-1$
		}
		return false;
	}

	/**
	 * @param reporter
	 * @param decl
	 */
	private void analyzeSubtree(final IRNode decl) {
		final Iterator<IRNode> nodes = JJNode.tree.topDown(decl);
		while (nodes.hasNext()) {
			final IRNode currentNode = nodes.next();
			final IRNode insideBlock = getContainingBlock(currentNode);
			if (insideBlock != null) {
				final PromiseRecord pr = getCachedPromiseRecord(insideBlock);
				checkMethodCall(currentNode, pr);
				checkForError(currentNode, pr);
			}
		}
	}

	/**
	 * Returns <code>null</code> if the compilation unit contains the
	 * declaration or use of a unique field. Otherwise returns the set of the
	 * methods in the CU that need to be analyzed because they have borrowed or
	 * unique annotations or because they invoke a method that has unique
	 * parameter requirements.
	 *
	 * @param compUnit
	 *          A compilation unit node.
	 * @return <code>null</code> or a set of method/constructor declarations.
	 */
	private Set<IRNode> shouldAnalyzeCompilationUnit(final IRNode compUnit) {
		final Set<IRNode> rootNodesForAnalysis = new HashSet<IRNode>();

		final IRNode typeDecls = edu.cmu.cs.fluid.java.operator.CompilationUnit
		.getDecls(compUnit);
		final Iterator<IRNode> decls = TypeDeclarations.getTypesIterator(typeDecls);
		while (decls.hasNext()) {
			final IRNode decl = decls.next();
			final Iterator<IRNode> nodes = JJNode.tree.topDown(decl);
			while (nodes.hasNext()) {
				final IRNode currentNode = nodes.next();
				final Operator op = JJNode.tree.getOperator(currentNode);

				// Ignore Annotation elements
				if (AnnotationElement.prototype.includes(op)) {
					continue;
				}

				/*
				 * We are searching for (1) the declarations of an unique field (2) the
				 * use of an unique field, (3) the declaration of a method that has
				 * borrowed parameters, unique parameters, or a unique return value, or
				 * (4) the invocation of a method that has unique parameter requirements.
				 */

				/*
				 * NEW Case 1: Initializer of an unique field declaration. This
				 * represents an implicit assignment to the field, and needs to be
				 * captured so we can get the control-flow dependency.
				 */
				if (Initialization.prototype.includes(op)) {
					final IRNode variableDeclarator = JJNode.tree.getParent(currentNode);
					final IRNode variableDeclarators = JJNode.tree.getParent(variableDeclarator);
					final IRNode possibleFieldDeclaration = JJNode.tree.getParent(variableDeclarators);
					if (FieldDeclaration.prototype.includes(
							JJNode.tree.getOperator(possibleFieldDeclaration))) {
						if (UniquenessRules.isUnique(variableDeclarator)) {
							rootNodesForAnalysis.add(possibleFieldDeclaration);
						}
					}
				}

				/*
				 * Case 2
				 */
				if (FieldRef.prototype.equals(op)) {
					if (UniquenessRules.isUnique(getBinder().getBinding(currentNode))) {
						rootNodesForAnalysis.add(getContainingBlock(currentNode));
					}
				}

				if (ConstructorDeclaration.prototype.equals(op)
						|| MethodDeclaration.prototype.equals(op)) {
					boolean hasBorrowedParam = false;
					boolean hasUniqueParam = false;
					boolean returnsUnique = false;

					// Case 3a: returns unique
					if (MethodDeclaration.prototype.equals(op)) {
						final IRNode retDecl = JavaPromise.getReturnNodeOrNull(currentNode);
						returnsUnique = (retDecl == null) ? false : UniquenessRules
								.isUnique(retDecl);
					}

					// Case 3b: borrowed/unique parameter
          if (ConstructorDeclaration.prototype.includes(op)) {
            hasBorrowedParam |= UniquenessRules.constructorYieldsUnaliasedObject(currentNode);
            // Cannot have a unique receiver
          } else {
            if (!TypeUtil.isStatic(currentNode)) { // non-static method
              final IRNode self = JavaPromise.getReceiverNode(currentNode);
              hasBorrowedParam |= UniquenessRules.isBorrowed(self);
              hasUniqueParam |= UniquenessRules.isUnique(self);
            }
          }
					IRNode formals = null;
					if (ConstructorDeclaration.prototype.includes(op)) {
						formals = ConstructorDeclaration.getParams(currentNode);
					} else {
						formals = MethodDeclaration.getParams(currentNode);
					}
					for (int i = 0; i < JJNode.tree.numChildren(formals); i++) {
            final IRNode param = JJNode.tree.getChild(formals, i);
            hasBorrowedParam |= UniquenessRules.isBorrowed(param);
            hasUniqueParam |= UniquenessRules.isUnique(param);
					}
					if (returnsUnique || hasBorrowedParam || hasUniqueParam)
						rootNodesForAnalysis.add(currentNode);
				}

				// Case 4: invoking method with unique parameter
				if (op instanceof CallInterface) {
					final IRNode declNode = getBinder().getBinding(currentNode);

					if (declNode != null) {
						final Operator declOp = JJNode.tree.getOperator(declNode);
						IRNode formals = null;

						if (declOp instanceof ConstructorDeclaration) {
							formals = ConstructorDeclaration.getParams(declNode);
						} else if (declOp instanceof MethodDeclaration) {
							formals = MethodDeclaration.getParams(declNode);
						}
						if (formals != null) {
							boolean hasUniqueParam = false;

							if (!TypeUtil.isStatic(declNode)) {
								final IRNode self = JavaPromise.getReceiverNode(declNode);
								hasUniqueParam |= UniquenessRules.isUnique(self);
							}
							for (int i = 0; !hasUniqueParam
							&& (i < JJNode.tree.numChildren(formals)); i++) {
								hasUniqueParam = UniquenessRules.isUnique(JJNode.tree
										.getChild(formals, i));
							}
							if (hasUniqueParam) {
								final IRNode currentMethod = getContainingBlock(currentNode);
								if (currentMethod != null) {
									rootNodesForAnalysis.add(currentMethod);
								} else {
									// method call not in a method! analyze everything!
									return null;
								}
							}
						}
					}
				}
			}
		}

		return rootNodesForAnalysis;
	}

	private void checkForError(final IRNode node, final PromiseRecord pr) {
		if (isInvalid(node)) {
		  final ResultDropBuilder cfDrop = pr.controlFlow;
		  cfDrop.setInconsistent();
		  cfDrop.addSupportingInformation(getErrorMessage(node), node);
		}
	}

	public void checkMethodCall(final IRNode node, final PromiseRecord pr) {
		if (JJNode.tree.getOperator(node) instanceof CallInterface) {
			final Set<ResultDropBuilder> callDrops = pr.callsToDrops.get(node);
			
			if (getAnalysis().isPositivelyAssured(node)) {
				for (ResultDropBuilder callDrop : callDrops) {
					callDrop.setConsistent();
					if (pr.calledUniqueParams.contains(callDrop)) {
					  callDrop.setMessage(Messages.uniqueParametersSatisfied, DebugUnparser.toString(node));
					  callDrop.setCategory(DSC_UNIQUE_PARAMS_SATISFIED);
					}
				}
			} else {
				for (ResultDropBuilder callDrop : callDrops) {
					callDrop.setInconsistent();
          callDrop.addSupportingInformation(getErrorMessage(node), node);
					if (pr.calledUniqueParams.contains(callDrop)) {
					  callDrop.setMessage(Messages.uniqueParametersUnsatisfied, DebugUnparser.toString(node));
					  callDrop.setCategory(DSC_UNIQUE_PARAMS_UNSATISFIED);
					}
				}
			}
		}
	}

	/**
	 * Is the node the source of a uniqueness problem?
	 */
	private boolean isInvalid(final IRNode node) {
		/* Definitely not erroneous */
		if (!getAnalysis().isInvalid(node))
			return false;

		/* Node is erroneous, but does the error come from a child? */
		for (Iterator<IRNode> ch = JJNode.tree.children(node); ch.hasNext();) {
			final IRNode n = ch.next();
			/* Problem comes from a child, so parent is not to blame */
			if (getAnalysis().isInvalid(n))
				return false;
		}
		/* Not a problem from a child. */
		return true;
	}

	/**
	 * Assumes that isInvalid( n ) is true
	 */
	private String getErrorMessage(final IRNode n) {
		final String normErr = getAnalysis().getNormalErrorMessage(n);
		final String abruptErr = getAnalysis().getAbruptErrorMessage(n);

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

	/**
	 * If node is inside a method/constructor body, then return the
	 * MethodDeclaration or ConstructorDeclaration node. If node is inside an
	 * instance or static initializer block, return the ClassInitializer node. if
	 * it's inside a field initializer, return the FieldDeclaration node. Otherwise
	 * return <code>null</code>.
	 */
	private IRNode getContainingBlock(IRNode node) {
		while (node != null) {
			final Operator op = JJNode.tree.getOperator(node);
			if (ConstructorDeclaration.prototype.includes(op)
					|| MethodDeclaration.prototype.includes(op)
					|| ClassInitializer.prototype.includes(op)
					|| FieldDeclaration.prototype.includes(op)) {
				return node;
			}
			node = JJNode.tree.getParentOrNull(node);
		}
		return null;
	}

	private Map<IRNode, PromiseRecord> cachedPromiseRecord = null;

	private void clearPromiseRecordCache() {
		cachedPromiseRecord = new HashMap<IRNode, PromiseRecord>();
	}

	/**
	 * @param block
	 *          A MethodDeclaration, ConstructorDeclaration, or ClassInitializer.
	 */
	private PromiseRecord getCachedPromiseRecord(final IRNode block) {
		PromiseRecord pr = cachedPromiseRecord.get(block);
		if (pr == null) {
			pr = createPromiseRecordFor(block);
			cachedPromiseRecord.put(block, pr);
		}
		return pr;
	}

	/**
	 * A record of the annotations that are interesting for a particular
	 * MethodDeclaration, ConstructorDeclaration, ClassInitializer, or Initialzer.
	 */
	private class PromiseRecord {
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
		 * Map from method/constructor calls to the set of result drops that
		 * represent the calls.
		 */
		public final Map<IRNode, Set<ResultDropBuilder>> callsToDrops;

		public PromiseRecord(final IRNode block) {
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
		}
	}

	private final Map<IRNode, ResultDropBuilder> cachedControlFlow = new HashMap<IRNode, ResultDropBuilder>();
	
	private ResultDropBuilder getMethodControlFlowDrop(final IRNode block) {
    ResultDropBuilder drop = cachedControlFlow.get(block);
    if (drop == null || !drop.isValid()) {
      drop = ResultDropBuilder.create(this, "methodControlFlow");
      drop.setConsistent();
      setResultDependUponDrop(drop, block);

      final String message;
      final Operator op = JJNode.tree.getOperator(block);
      if (ConstructorDeclaration.prototype.includes(op)) {
        message = MessageFormat.format(
            Messages.methodControlFlow, "constructor",
            JavaNames.genMethodConstructorName(block));
      } else if (MethodDeclaration.prototype.includes(op)) {
        message = MessageFormat.format(
            Messages.methodControlFlow, "method",
            JavaNames.genMethodConstructorName(block));
      } else if (ClassInitializer.prototype.includes(op)) {
        message = MessageFormat.format(
            Messages.methodControlFlow, "initializer",
            DebugUnparser.toString(block));
      } else { // Field declaration
        message = MessageFormat.format(
            Messages.methodControlFlow, "field initializer",
            DebugUnparser.toString(block));
      }
      drop.setMessage(message);
      cachedControlFlow.put(block, drop);
      controlFlowDrops.add(drop);
    }
    return drop;
	}
	
	@SuppressWarnings("unchecked")
  private PromiseRecord createPromiseRecordFor(final IRNode block) {
		final PromiseRecord pr = new PromiseRecord(block);
		final Operator blockOp = JJNode.tree.getOperator(block);

		/*
		 * If the block is a FieldDeclaration, see if it contains any unique fields.
		 */
		if (FieldDeclaration.prototype.includes(blockOp)) {
			final IRNode variableDeclarators = FieldDeclaration.getVars(block);
			for (IRNode varDecl : VariableDeclarators.getVarIterator(variableDeclarators)) {
				if (UniquenessRules.isUnique(varDecl)) {
					pr.uniqueFields.add(UniquenessRules.getUniqueDrop(varDecl));
				}
			}
		}

		/* If the block is a method or constructor declaration, get promise
		 * information from it.
		 */
		final boolean isConDecl = ConstructorDeclaration.prototype.includes(blockOp);
    if (isConDecl || MethodDeclaration.prototype.includes(blockOp)) {
      // don't care about my effects, use a throw-away set here
      getPromisesFromMethodDecl(block, pr.myUniqueReturn,
          pr.myBorrowedParams, new HashSet<BorrowedPromiseDrop>(),
          pr.myUniqueParams, new HashSet<RegionEffectsPromiseDrop>());
    }

		// Look at the guts of the method/constructor/initializer
		final Iterator<IRNode> nodes = JJNode.tree.topDown(block);
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
					    MessageFormat.format(Messages.uniqueReturnDrop, label),
					    currentNode, uniqueReturns);
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
					    MessageFormat.format(Messages.borrowedParametersDrop, label),
							currentNode, borrowedParams);
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
					    MessageFormat.format(Messages.effectOfCallDrop, label),
					    currentNode, effects);
					allCallDrops.add(callDrop);
					pr.calledEffects.add(callDrop);
				}

				// Add to the map of calls to drops
				pr.callsToDrops.put(currentNode, allCallDrops);
			}
		}

    /*
     * Set up the borrowed dependencies. Each parameter of the method that is
     * declared to be borrowed trusts the @borrowed annotations (including
     * @Unique("return") annotations on constructors) of any methods called by
     * the body of this method.
     */
		{
			final Set<ResultDropBuilder> dependsOnResults = new HashSet<ResultDropBuilder>(pr.calledBorrowedParams);
			dependsOnResults.addAll(pr.calledBorrowedReceiverAsUniqueReturn);
			dependsOnResults.add(pr.controlFlow);
      addDependencies(pr.myBorrowedParams, intermediateResultDrops,
          Collections.<PromiseDrop>emptySet(), dependsOnResults);
      /* If we are a constructor, we treat unique("return") like @borrowed("this")
       */
      if (isConDecl) {
        addDependencies(pr.myUniqueReturn, intermediateResultDrops,
            Collections.<PromiseDrop>emptySet(), dependsOnResults);
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
		{
			final Set<PromiseDrop<? extends IAASTRootNode>> dependsOnPromises =
				new HashSet<PromiseDrop<? extends IAASTRootNode>>();
			dependsOnPromises.addAll(pr.myUniqueParams);
			dependsOnPromises.addAll(pr.uniqueFields);
			final Set<ResultDropBuilder> dependsOnResults = new HashSet<ResultDropBuilder>();
			dependsOnResults.add(pr.controlFlow);
      dependsOnResults.addAll(pr.calledUniqueReturns.keySet());
      dependsOnResults.addAll(pr.calledBorrowedConstructors.keySet());
      dependsOnResults.addAll(pr.calledUniqueConstructors.keySet());
			dependsOnResults.addAll(pr.calledBorrowedParams);
			dependsOnResults.addAll(pr.calledBorrowedReceiverAsUniqueReturn);
			dependsOnResults.addAll(pr.calledEffects);
			addDependencies(pr.uniqueFields, intermediateResultDrops, dependsOnPromises, dependsOnResults);
		}

		/*
		 * Set up the dependencies for this method's unique return value. Depends on
		 * the unique parameters of the method, the unique return values of called
		 * methods, the unique fields accessed by this method, and the control-flow
		 * of the method itself.
		 */
		{
			final Set<PromiseDrop<? extends IAASTRootNode>> dependsOnPromises =
				new HashSet<PromiseDrop<? extends IAASTRootNode>>();
			dependsOnPromises.addAll(pr.myUniqueParams);
			dependsOnPromises.addAll(pr.uniqueFields);
			final Set<ResultDropBuilder> dependsOnResults = new HashSet<ResultDropBuilder>();
	    dependsOnResults.add(pr.controlFlow);
  		dependsOnResults.addAll(pr.calledUniqueReturns.keySet());
      dependsOnResults.addAll(pr.calledBorrowedConstructors.keySet());
      dependsOnResults.addAll(pr.calledUniqueConstructors.keySet());
  		/* If this is from a constructor than unique("return") should be 
  		 * treated as borrowed("this")
  		 */
  		if (!isConDecl) {
  		  addDependencies(pr.myUniqueReturn, intermediateResultDrops,
  		      dependsOnPromises, dependsOnResults);
  		}
		}

		/* Set up the dependencies for this method's unique parameters.  They can
		 * be compromised and turned non-unique during the execution of the method.
		 * Depends on the the control-flow of the method.
		 */
		{
      final Set<PromiseDrop<? extends IAASTRootNode>> dependsOnPromises =
        new HashSet<PromiseDrop<? extends IAASTRootNode>>();
      final Set<ResultDropBuilder> dependsOnResults = new HashSet<ResultDropBuilder>();
      dependsOnResults.add(pr.controlFlow);
		  addDependencies(pr.myUniqueParams, intermediateResultDrops, dependsOnPromises, dependsOnResults);
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
					  callToCheck.addSupportingInformation(
					      MessageFormat.format(Messages.uniqueReturnValue,
					          DebugUnparser.toString(methodCall)), methodCall);
					  callToCheck.addTrustedPromise(entry.getValue());
					}

          // Add depended on contructors with borrowed("this") or unique("return")
          for (Map.Entry<ResultDropBuilder, BorrowedPromiseDrop> entry : pr.calledBorrowedConstructors.entrySet()) {
            final IRNode constructorCall = entry.getKey().getNode();
            callToCheck.addSupportingInformation(
                MessageFormat.format(Messages.borrowedConstructor,
                    DebugUnparser.toString(constructorCall)), constructorCall);
            callToCheck.addTrustedPromise(entry.getValue());
          }
          for (Map.Entry<ResultDropBuilder, UniquePromiseDrop> entry : pr.calledUniqueConstructors.entrySet()) {
            final IRNode constructorCall = entry.getKey().getNode();
            callToCheck.addSupportingInformation(
                MessageFormat.format(Messages.borrowedConstructor,
                    DebugUnparser.toString(constructorCall)), constructorCall);
            callToCheck.addTrustedPromise(entry.getValue());
          }
				}
			}
		}

		// Return the promise record
		return pr;
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
	getMethodCallDrop(final String type, final String label, final IRNode n, final Set<D> promises) {
		final ResultDropBuilder rd = ResultDropBuilder.create(this, type);
		rd.setConsistent();
		rd.setMessage(label);
		setResultDependUponDrop(rd, n);
		for (final D pd : promises) {
			rd.addTrustedPromise(pd);
		}
		return rd;
	}

	private <PD1 extends PromiseDrop<? extends IAASTRootNode>,
	PD2 extends PromiseDrop<? extends IAASTRootNode>>
	void addDependencies(final Set<PD1> promises,
      final Map<PromiseDrop<? extends IAASTRootNode>, ResultDropBuilder> intermediateDrops,
			final Set<PD2> dependsOnPromises,
			final Set<ResultDropBuilder> dependsOnResults) {
		if (!dependsOnPromises.isEmpty() || !dependsOnResults.isEmpty()) {
			for (final PD1 promiseToCheck : promises) {
				/* Add depended upon promises, skipping ourself (avoid direct
				 * self-dependency).  So we proceed if dependsOnPromises contains
				 * promiseToCheck but has size >= 2, or if dependsOnPromises has size >= 1.
				 */
			  if (dependsOnPromises.contains(promiseToCheck) ? dependsOnPromises.size() >= 2 : dependsOnPromises.size() >= 1) {
				  ResultDropBuilder middleDrop = intermediateDrops.get(promiseToCheck);
				  if (middleDrop == null) {
				    middleDrop = ResultDropBuilder.create(this, "dependencyDrop");
	          middleDrop.setNode(promiseToCheck.getNode());
	          middleDrop.setConsistent();
	          middleDrop.setMessage(Messages.dependencyDrop);
	          middleDrop.addCheckedPromise(promiseToCheck);
	          intermediateDrops.put(promiseToCheck, middleDrop);
				  }

					for (final PD2 trustedPD : dependsOnPromises) {
						// Avoid self-dependency
						if ((trustedPD != null) && (promiseToCheck != trustedPD)) {
							middleDrop.addTrustedPromise(trustedPD);
							setResultDependUponDrop(middleDrop);
						}
					}
				}

				// Add depended on method calls, etc.
				if (!dependsOnResults.isEmpty()) {
					for (ResultDropBuilder rd : dependsOnResults) {
						rd.addCheckedPromise(promiseToCheck);
					}
				}
			}
		}
	}
}
