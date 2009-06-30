/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/uniqueness/UniquenessAnalysisModule.java,v 1.2 2008/09/08 17:43:38 chance Exp $*/
package com.surelogic.analysis.uniqueness;

import java.util.*;
import java.util.logging.Level;

import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.effects.EffectsVisitor;
import com.surelogic.annotation.rules.MethodEffectsRules;
import com.surelogic.annotation.rules.UniquenessRules;
import com.surelogic.sea.drops.effects.RegionEffectsPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.ResultDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.tree.Operator;

public class UniquenessAnalysisModule extends AbstractWholeIRAnalysis<UniqueAnalysis> {
	public UniquenessAnalysisModule() {
		super("UniqueAnalysis");
	}

	public void init(IIRAnalysisEnvironment env) {
		// TODO Auto-generated method stub
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
	protected void doAnalysisOnAFile(CUDrop cud, IRNode compUnit) {
		checkUniquenessForFile(compUnit);
	}

	@Override
	public IRNode[] analyzeEnd(IIRProject p) {
		// FIX only clearing some of the threads?
		if (getAnalysis() != null) {
			getAnalysis().clear();
		}
		return JavaGlobals.noNodes;
	}

	protected void checkUniquenessForFile(IRNode compUnit) {
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
					analyzeSubtree(typeDecl);
				}
			} else {
				// Analyze the given nodes
				for (Iterator<IRNode> iter = methods.iterator(); iter.hasNext();) {
					final IRNode node = iter.next();
					analyzeSubtree(node);
				}
			}
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Exception in unique assruance", e); //$NON-NLS-1$
		}
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
				checkMethodBody(currentNode, pr);
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
				 * borrowed parameters or a unique return value, or (4) the invocation
				 * of a method that has unique parameter requirements.
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
					boolean returnsUnique = false;

					// Case 3a: returns unique
					if (MethodDeclaration.prototype.equals(op)) {
						final IRNode retDecl = JavaPromise.getReturnNodeOrNull(currentNode);
						returnsUnique = (retDecl == null) ? false : UniquenessRules
								.isUnique(retDecl);
					}

					// Case 3b: borrowed parameter
					if (!TypeUtil.isStatic(currentNode)) {
						final IRNode self = JavaPromise.getReceiverNode(currentNode);
						hasBorrowedParam |= UniquenessRules.isBorrowed(self);
					}
					IRNode formals = null;
					if (op instanceof ConstructorDeclaration) {
						formals = ConstructorDeclaration.getParams(currentNode);
					} else {
						formals = MethodDeclaration.getParams(currentNode);
					}
					for (int i = 0; !hasBorrowedParam
					&& (i < JJNode.tree.numChildren(formals)); i++) {
						hasBorrowedParam = UniquenessRules.isBorrowed(JJNode.tree
								.getChild(formals, i));
					}
					if (returnsUnique || hasBorrowedParam)
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
			final ResultDrop rd = new ResultDrop("UniquenessAssurance_error");
			setResultDependUponDrop(rd, node);
			rd.setInconsistent();
			rd.setMessage(getErrorMessage(node) + " " + DebugUnparser.toString(node)); //$NON-NLS-1$
			rd.addCheckedPromise(pr.controlFlow);
		}
	}

	public void checkMethodBody(final IRNode node, final PromiseRecord pr) {
		final Operator op = JJNode.tree.getOperator(node);
		if (MethodBody.prototype.includes(op)) {
			if (getAnalysis().isPositivelyAssured(node)) {
				ResultDrop rd = new ResultDrop("UniquenessAssurance_uniquenessContraints1");
				rd.addCheckedPromise(pr.controlFlow);
				rd.setConsistent();
				setResultDependUponDrop(rd, node);
				rd.setMessage(Messages.UniquenessAssurance_uniquenessContraints1); //$NON-NLS-1$
			} else {
				ResultDrop rd = new ResultDrop("UniquenessAssurance_uniquenessContraints2");
				rd.addCheckedPromise(pr.controlFlow);
				rd.setInconsistent();
				setResultDependUponDrop(rd, node);
				rd.setMessage(Messages.UniquenessAssurance_uniquenessContraints2); //$NON-NLS-1$
			}
		}
	}

	public void checkMethodCall(final IRNode node, final PromiseRecord pr) {
		if (JJNode.tree.getOperator(node) instanceof CallInterface) {
			final Set<ResultDrop> callDrops = pr.callsToDrops.get(node);

			if (getAnalysis().isPositivelyAssured(node)) {
				for (ResultDrop callDrop : callDrops) {
					callDrop.setConsistent();
				}
			} else {
				for (ResultDrop callDrop : callDrops) {
					callDrop.setInconsistent();
//					callDrop.setMessage(Messages.UniquenessAssurance_checkMethodCallDrop,
//					callDrop.getMessage(), analysisContext.uniqueAnalysis
//					.isInvalid(node), getErrorMessage(node)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
//			if (VariableDeclarator.prototype.includes(op)) {
//			// found an initializer, is it a field initializer?
//			if (FieldDeclaration.prototype.includes(
//			JJNode.tree.getOperator(
//			JJNode.tree.getParent(
//			JJNode.tree.getParent(node))))) {
//			return node;
//			}
//			}

//			if (Initialization.prototype.includes(op)) {
//			// found an initializer, is it a field initializer?
//			if (FieldDeclaration.prototype.includes(
//			JJNode.tree.getOperator(
//			JJNode.tree.getParent(
//			JJNode.tree.getParent(
//			JJNode.tree.getParent(node)))))) {
//			return node;
//			}
//			}
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
	private static class PromiseRecord {
		/** The unique parameters declared by this method/constructor */
		public final Set<UniquePromiseDrop> myUniqueParams;

		/** The borrowed parameters declared by this method/constructor */
		public final Set<BorrowedPromiseDrop> myBorrowedParams;

		/** The unique return value declared by this method */
		public final Set<UniquePromiseDrop> myUniqueReturn;

		/** Method call drops for each invoked method that has unique parameters */
		public final Set<ResultDrop> calledUniqueParams;

		/** Method call drops for each invoked method that has unique return */
		public final Set<ResultDrop> calledUniqueReturns;

		/** Method call drops for each invoked method that has borrowed parameters */
		public final Set<ResultDrop> calledBorrowedParams;

		/** Method call drops for each invoked method that has effects */
		public final Set<ResultDrop> calledEffects;

		/** The unique fields accessed */
		public final Set<UniquePromiseDrop> uniqueFields;

		/** Drop for control-flow within this block */
		public final MethodControlFlow controlFlow;

		/**
		 * Map from method/constructor calls to the set of result drops that
		 * represent the calls.
		 */
		public final Map<IRNode, Set<ResultDrop>> callsToDrops;

		public PromiseRecord(final IRNode block) {
			myUniqueParams = new HashSet<UniquePromiseDrop>();
			myBorrowedParams = new HashSet<BorrowedPromiseDrop>();
			myUniqueReturn = new HashSet<UniquePromiseDrop>();
			calledUniqueReturns = new HashSet<ResultDrop>();
			calledUniqueParams = new HashSet<ResultDrop>();
			calledBorrowedParams = new HashSet<ResultDrop>();
			calledEffects = new HashSet<ResultDrop>();
			uniqueFields = new HashSet<UniquePromiseDrop>();

			callsToDrops = new HashMap<IRNode, Set<ResultDrop>>();

			// Create the control flow drop for the block
			controlFlow = MethodControlFlow.getDropFor(block);
		}
	}

	private PromiseRecord createPromiseRecordFor(final IRNode block) {
		final PromiseRecord pr = new PromiseRecord(block);
		final Operator blockOp = JJNode.tree.getOperator(block);

		/*
		 * If the block is an FieldDeclaration, see if it contains any unique fields.
		 */
		if (FieldDeclaration.prototype.includes(blockOp)) {
			final IRNode variableDeclarators = FieldDeclaration.getVars(block);
			for (IRNode varDecl : VariableDeclarators.getVarIterator(variableDeclarators)) {
				if (UniquenessRules.isUnique(varDecl)) {
					pr.uniqueFields.add(UniquenessRules.getUniqueDrop(varDecl));
				}
			}
		}

//		if (VariableDeclarator.prototype.includes(blockOp)) {
//		final IRNode variableDeclarators = JJNode.tree.getParent(block);
//		final IRNode possibleFieldDeclaration = JJNode.tree.getParent(variableDeclarators);
//		if (FieldDeclaration.prototype.includes(
//		JJNode.tree.getOperator(possibleFieldDeclaration))) {
//		if (UniquenessRules.isUnique(block)) {
//		pr.uniqueFields.add(UniquenessRules.getUniqueDrop(block));
//		}
//		}
//		}

		// If the block is a method declaration, get promise information from it
		if (ConstructorDeclaration.prototype.includes(blockOp)
				|| MethodDeclaration.prototype.includes(blockOp)) {
			// don't care about my effects, use a throw-away set here
			getPromisesFromMethodDecl(block, pr.myUniqueReturn, pr.myBorrowedParams,
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

				// get the info for the called method
				final Set<UniquePromiseDrop> uniqueReturns = new HashSet<UniquePromiseDrop>();
				final Set<BorrowedPromiseDrop> borrowedParams = new HashSet<BorrowedPromiseDrop>();
				final Set<UniquePromiseDrop> uniqueParams = new HashSet<UniquePromiseDrop>();
				final Set<RegionEffectsPromiseDrop> effects = new HashSet<RegionEffectsPromiseDrop>();
				getPromisesFromMethodDecl(declNode, uniqueReturns, borrowedParams,
						uniqueParams, effects);

				// Create the method call drops
				final Set<ResultDrop> allCallDrops = new HashSet<ResultDrop>();
				final String label = DebugUnparser.toString(currentNode);
				if (!uniqueReturns.isEmpty()) {
					final ResultDrop callDrop = getMethodCallDrop("UniquenessAssurance_uniqueReturnDrop",
							Messages.UniquenessAssurance_uniqueReturnDrop + label + "\"", currentNode, uniqueReturns); //$NON-NLS-1$ //$NON-NLS-2$
					allCallDrops.add(callDrop);
					pr.calledUniqueReturns.add(callDrop);
				}
				if (!borrowedParams.isEmpty()) {
					final ResultDrop callDrop = getMethodCallDrop("UniquenessAssurance_borrowedParametersDrop",
							Messages.UniquenessAssurance_borrowedParametersDrop + label
							+ "\"", currentNode, borrowedParams); //$NON-NLS-1$ //$NON-NLS-2$
					allCallDrops.add(callDrop);
					pr.calledBorrowedParams.add(callDrop);
				}
				if (!uniqueParams.isEmpty()) {
					final ResultDrop callDrop = getMethodCallDrop("UniquenessAssurance_uniqueParametersDrop",
							Messages.UniquenessAssurance_uniqueParametersDrop + label + "\"", currentNode, uniqueParams); //$NON-NLS-1$ //$NON-NLS-2$
					final CalledMethodsWithUniqueParams pd =
						CalledMethodsWithUniqueParams.getDropFor(currentNode);
					callDrop.addCheckedPromise(pd);
					allCallDrops.add(callDrop);
					pr.calledUniqueParams.add(callDrop);
				}
				if (!effects.isEmpty()) {
					final ResultDrop callDrop = getMethodCallDrop("UniquenessAssurance_effectOfCallDrop",
							Messages.UniquenessAssurance_effectOfCallDrop + label + "\"", currentNode, effects); //$NON-NLS-1$ //$NON-NLS-2$
					allCallDrops.add(callDrop);
					pr.calledEffects.add(callDrop);
				}

				// Add to the map of calls to drops
				pr.callsToDrops.put(currentNode, allCallDrops);
			}
		}

		/*
		 * TODO: Find a better place to put this. This method should get run exactly
		 * once for each method in the CU that has anything to do with uniqueness.
		 * So we set up all the cross dependences now.
		 */

		/*
		 * Set up the borrowed dependencies. Each parameter of the method that is
		 * declared to be borrowed trusts the @borrowed annotations of any methods
		 * called by the body of this method.
		 */
		{
			final Set<MethodControlFlow> dependsOnPromises = Collections.singleton(pr.controlFlow);
			final Set<ResultDrop> dependsOnResults = new HashSet<ResultDrop>(pr.calledBorrowedParams);
			addDependencies(pr.myBorrowedParams, dependsOnPromises, dependsOnResults);
		}

		/*
		 * Set up the dependencies for this method's accessed unique fields. Depends
		 * on the unique parameters of the method, the unique return values of
		 * called methods, the borrowed parameters of called methods, the unique
		 * fields accessed by this method, the effects of methods w/borrowed
		 * parameters, and the control-flow of the method itself.
		 */
		{
			final Set<PromiseDrop<? extends IAASTRootNode>> dependsOnPromises =
				new HashSet<PromiseDrop<? extends IAASTRootNode>>();
			dependsOnPromises.addAll(pr.myUniqueParams);
			dependsOnPromises.addAll(pr.uniqueFields);
			dependsOnPromises.add(pr.controlFlow);
			final Set<ResultDrop> dependsOnResults = new HashSet<ResultDrop>();
			dependsOnResults.addAll(pr.calledUniqueReturns);
			dependsOnResults.addAll(pr.calledBorrowedParams);
			dependsOnResults.addAll(pr.calledEffects);
			addDependencies(pr.uniqueFields, dependsOnPromises, dependsOnResults);
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
			dependsOnPromises.add(pr.controlFlow);
			final Set<ResultDrop> dependsOnResults = new HashSet<ResultDrop>();
			dependsOnResults.addAll(pr.calledUniqueReturns);
			addDependencies(pr.myUniqueReturn, dependsOnPromises, dependsOnResults);
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
			dependsOnPromises.add(pr.controlFlow);
			final Set<ResultDrop> dependsOnResults = new HashSet<ResultDrop>();
			dependsOnResults.addAll(pr.calledUniqueReturns);
			if (!dependsOnPromises.isEmpty() && !dependsOnResults.isEmpty()) {
				for (ResultDrop callToCheck : pr.calledUniqueParams) {
					// Add depended upon promises
					for (final PromiseDrop<? extends IAASTRootNode> trustedPD : dependsOnPromises) {
						if (trustedPD != null) {
							callToCheck.addTrustedPromise(trustedPD);
							setResultDependUponDrop(callToCheck, trustedPD.getNode());
						}
					}

					// Add depended on method calls, etc.
					for (ResultDrop rd : dependsOnResults) {
						callToCheck.addDependent(rd);
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
			final Set<UniquePromiseDrop> uniqueParams,
			final Set<RegionEffectsPromiseDrop> effects) {
		final Operator op = JJNode.tree.getOperator(mdecl);
		final boolean isConstructor = ConstructorDeclaration.prototype.includes(op);

		// Try to get the @unique returns drop if any
		if (!isConstructor) {
			final IRNode retDecl = JavaPromise.getReturnNodeOrNull(mdecl);
			if (retDecl != null) {
				final UniquePromiseDrop returnsUniqueDrop =
					UniquenessRules.getUniqueDrop(retDecl);
				if (returnsUniqueDrop != null)
					uniqueReturns.add(returnsUniqueDrop);
			}
		}

		// Get the @borrowed and @unique params drops, if any
		if (!TypeUtil.isStatic(mdecl)) { // don't forget the receiver
			final IRNode self = JavaPromise.getReceiverNode(mdecl);
			final BorrowedPromiseDrop borrowedRcvrDrop =
				UniquenessRules.getBorrowedDrop(self);
			final UniquePromiseDrop uniqueRcvrDrop =
				UniquenessRules.getUniqueDrop(self);
			if (borrowedRcvrDrop != null) {
				borrowedParams.add(borrowedRcvrDrop);
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

	private <D extends PromiseDrop<? extends IAASTNode>> ResultDrop
	getMethodCallDrop(final String type, final String label, final IRNode n, final Set<D> promises) {
		final ResultDrop rd = new ResultDrop(type);
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
			final Set<PD2> dependsOnPromises,
			final Set<ResultDrop> dependsOnResults) {
		if (!dependsOnPromises.isEmpty() || !dependsOnResults.isEmpty()) {
			for (final PD1 promiseToCheck : promises) {
				// Add depended upon promises
				if (!dependsOnPromises.isEmpty()) {
					final ResultDrop middleDrop = new ResultDrop("UniquenessAssurance_dependencyDrop");
					middleDrop.addCheckedPromise(promiseToCheck);
					middleDrop.setConsistent();
					middleDrop.setMessage(Messages.UniquenessAssurance_dependencyDrop); //$NON-NLS-1$
					middleDrop.setNode(promiseToCheck.getNode());

					boolean addedTrustedDrop = false;
					for (final PD2 trustedPD : dependsOnPromises) {
						// Avoid self-dependency
						if ((trustedPD != null) && (promiseToCheck != trustedPD)) {
							addedTrustedDrop = true;
							middleDrop.addTrustedPromise(trustedPD);
							setResultDependUponDrop(middleDrop);
						}
					}
					if (!addedTrustedDrop)
						middleDrop.invalidate();
				}

				// Add depended on method calls, etc.
				if (!dependsOnResults.isEmpty()) {
					for (ResultDrop rd : dependsOnResults) {
						rd.addCheckedPromise(promiseToCheck);
					}
				}
			}
		}
	}
}
