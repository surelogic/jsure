package com.surelogic.analysis.threads;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.analysis.AbstractWholeIRAnalysis;
import com.surelogic.analysis.IBinderClient;
import com.surelogic.annotation.rules.ThreadEffectsRules;
import com.surelogic.common.logging.SLLogger;
import com.surelogic.persistence.*;

import edu.cmu.cs.fluid.ide.IDE;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.*;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.ProposedPromiseDrop.Origin;
import edu.cmu.cs.fluid.sea.drops.promises.StartsPromiseDrop;
import edu.cmu.cs.fluid.sea.proxy.ProposedPromiseBuilder;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;

public final class ThreadEffectsAnalysis implements IBinderClient {

	private static final Logger LOG = SLLogger
			.getLogger("FLUID.analysis.ThreadEffects"); //$NON-NLS-1$

	public static final boolean createDrops = true;
	
	private final IBinder binder;
	private final ThreadEffectsModule analysis;
	
	private Drop resultDependUpon = null;

	private void setResultDependUponDrop(ResultDropBuilder drop, IRNode node,
			int resultNum, String arg) {
		drop.setNodeAndCompilationUnitDependency(node);
		drop.setResultMessage(resultNum, arg);
		if (AbstractWholeIRAnalysis.useDependencies) {
			return;
		}
		if (resultDependUpon != null && resultDependUpon.isValid()) {
			drop.addDependUponDrop(resultDependUpon);
			//resultDependUpon.addDependent(drop);
		} else {
			LOG
					.log(Level.SEVERE,
							"setLockResultDep found invalid or null resultDependUpon drop"); //$NON-NLS-1$
		}
	}

	private static Operator getOperator(final IRNode n) {
		return JJNode.tree.getOperator(n);
	}

	private IRNode getBinding(final IRNode n) {
		return binder.getBinding(n);
	}

	public IBinder getBinder() {
		return binder;
	}

	public void clearCaches() {
		// Nothing to do
	}

	//
	// Saves the IRNode for "java.lang.Thread" which is lazily loaded
	// should always be accessed via the "getJavaLangThread()" call below
	//
	private IRNode javaLangThread;

	private IRNode getJavaLangThread() {
		if (javaLangThread == null) {
			final ITypeEnvironment typeEnv = binder.getTypeEnvironment();
			javaLangThread = typeEnv.findNamedType("java.lang.Thread"); //$NON-NLS-1$
			if (javaLangThread == null) {
				LOG
						.log(Level.SEVERE,
								"[ThreadEffects] failed to find java.lang.Thread in the IR"); //$NON-NLS-1$
			}
		}
		return javaLangThread;
	}

	/**
	 * Generates a model name from the promise.
	 * 
	 * @param node
	 *            a constructor or method declaration
	 * @param op
	 *            the operator for node
	 * @return a created model name for the thread effects declaration
	 */
	private String genModelName(final IRNode node, final Operator op) {
		return "nothing " + JavaNames.genMethodConstructorName(node); //$NON-NLS-1$
	}

	/**
	 * Utility method to check if the promise "starts nothing" is on a method or
	 * constructor declaration.
	 * 
	 * @return <code>true</code> if the promise exists, <code>false</code>
	 *         otherwise
	 */
	private boolean startsNothing(final IRNode node) {
	  /* No annotation means (could) start anything */ 
		return ThreadEffectsRules.startsNothing(node);
	}

	/**
	 * Checks if a method is or subsumes Thread.start().
	 * 
	 * @param methodDeclaration
	 *            the method to check
	 * @return <code>true</code> if the method is Thread.start() or a
	 *         subsumption, <code>false</code> otherwise
	 */
	private boolean doesMethodSubsumeThreadStart(final IRNode methodDeclaration) {
		// add the type we found the method within (could be the promised type)
		IRNode enclosingType = VisitUtil.getEnclosingType(methodDeclaration);
		//
		// check if the method is Thread.start() -- if so we are OK
		//
		if (enclosingType.equals(getJavaLangThread())) {
			return true;
		}
		//
		// otherwise see if this method subsumes Thread.start()
		//
		for (IBinding m : binder.findOverriddenMethods(methodDeclaration)) {
			IRNode subsumedType = VisitUtil.getEnclosingType(m.getNode());
			if (subsumedType.equals(getJavaLangThread())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Examines the block of a constructor or method which has declared thread
	 * effects and reports if it is consistent or not.
	 */
	private List<IAnalysisResult> examineBlock(IRNode block, Operator blockOp,
			StartsPromiseDrop pd, String modelName) {
		final List<IAnalysisResult> results = new ArrayList<IAnalysisResult>(1);
		boolean noThreadsStarted = true;
		
		/*
    String unparse = JavaNames.genQualifiedMethodConstructorName(block);
		if (unparse.endsWith("CyclicBarrier(int)")) {
			System.out.println("Block: "+unparse);
		}
		*/
		
		// TODO: this is busted we do NOT want to go into anonymous classes and
		// declared classes within a method or constructor
		final Iterator<IRNode> nodes = JJNode.tree.topDown(block);
		while (nodes.hasNext()) {
			final IRNode node = nodes.next();
			Result result = checkNodeThreadEffects(pd, node, modelName);
			if (result != null) {
				/*
				if (!result.success) {
					checkNodeThreadEffects(pd, node, modelName);
				}
				*/
				noThreadsStarted &= result.success;
				
				IAnalysisResult issue = result.result;
				if (issue != null) {
					results.add(issue);
				}
			}
		}

		if (noThreadsStarted && createDrops) {
			ResultDropBuilder r = ResultDropBuilder.create(analysis, Messages.toString(Messages.NO_THREADS_STARTED));
			r.setConsistent();
			r.addCheckedPromise(pd);
			setResultDependUponDrop(r, block, Messages.NO_THREADS_STARTED, JavaNames
					.genMethodConstructorName(block));
		}
		// TODO how to create an equivalent "no threads started" message?
		// results.add(new SimpleAnalysisResult(pd, block, Messages.NO_THREADS_STARTED, JavaNames.genMethodConstructorName(block)));					
		return results;
	}

	static class Result {
		// Needed for the new results infrastructure
		final IAnalysisResult result;
		// Needed for the old results infrastructure
		final boolean success;
		
		Result(IAnalysisResult r, boolean ok) {
			result = r;
			success = ok;
		}
	}
	
	/**
	 * Examines a single node in the fAST to check if it is consistent with
	 * declared thread effects.
	 * 
	 * @param reporter
	 *            feedback to the UI
	 * @param node
	 *            the IRNode to examine
	 * @param pd
	 *            the promise drop we are checking
	 * @return <code>true</code> if a problem was found (i.e., a thread was
	 *         started), <code>false</code> otherwise.
	 */
	private Result checkNodeThreadEffects(StartsPromiseDrop pd,
			final IRNode node, String modelName) {
		final Operator op = getOperator(node);
		if (MethodCall.prototype.includes(op)) {
			MethodCall call = (MethodCall) op;

			// 2 cases of interest here: (1) is this a ".start()" call
			// (2) otherwise check thread effects
			// CASE (1) : look for a ".start()"
			if (MethodCall.getMethod(node).equals("start")) { //$NON-NLS-1$
				// check that there are no arguments to the call
				// (seems like this should be easier...sigh)
				IRNode args = call.get_Args(node);
				if (!Arguments.getArgIterator(args).hasNext()) {
					// might be a "Thread.start()" but we need to be sure so
					// lets
					// bind and check the the method
					IRNode methodDec = getBinding(node);
					if (methodDec == null) {
						LOG
								.log(Level.SEVERE,
										"[ThreadEffects] binding failed on start() method call"); //$NON-NLS-1$
					} else {
						if (doesMethodSubsumeThreadStart(methodDec)) {
							System.out.println("Found start() for "+DebugUnparser.toString(node)+" -- "+node);
							//
							// THREAD STARTED WITHIN THE BLOCK
							// (Note: this is simply a problem now as the only
							// understood
							// effect is "nothing" -- in the future a better
							// check would
							// be needed)
							//
							// System.out.println("[ThreadEffects] Thread.start() "
							// + DebugUnparser.toString(node));
							if (createDrops) {
						    ResultDropBuilder rd = ResultDropBuilder.create(analysis, Messages.toString(Messages.PROHIBITED));
							rd.setInconsistent();
							rd.addCheckedPromise(pd);
							setResultDependUponDrop(rd, node, Messages.PROHIBITED, DebugUnparser
									.toString(node));
							}
							return new Result(new SimpleAnalysisResult(pd, node, Messages.PROHIBITED, DebugUnparser.toString(node)), false);
						}
					}
				}
			} else {
				return checkCallThreadEffects(pd, node, modelName);
			}
		} else if (ConstructorCall.prototype.includes(op)) {
			// check thread effects
			return checkCallThreadEffects(pd, node, modelName);
		} else if (NewExpression.prototype.includes(op)) {
			// check thread effects
			return checkCallThreadEffects(pd, node, modelName);
		}
		return null;
	}

	/**
	 * Checks thread effects for a method call, a constructor call or a new
	 * expression.
	 * 
	 * @param reporter
	 * @param node
	 *            the call
	 * @param modelName
	 */
	private Result checkCallThreadEffects(StartsPromiseDrop pd,
			final IRNode node, String modelName) {
		// CASE (2) check for compatible thread effects
		IRNode declaration = getBinding(node);
		if (declaration == null) {
			return null;
		}

		// does it promise to start nothing?
		boolean success;
		if (createDrops) {
			if (ThreadEffectsRules.startsNothing(declaration)) {
				// get the promise drop
				StartsPromiseDrop callp = ThreadEffectsRules
				.getStartsSpec(declaration);
				ResultDropBuilder rd = ResultDropBuilder.create(analysis, Messages.toString(Messages.CALLED_METHOD_DOES_PROMISE));
				rd.addCheckedPromise(pd);
				rd.addTrustedPromise(callp);
				setResultDependUponDrop(rd, node, Messages.CALLED_METHOD_DOES_PROMISE, DebugUnparser.toString(node));
				rd.setConsistent();
				success = true;
			} else {
			  // No annotation: called method could start anything
				ResultDropBuilder rd = ResultDropBuilder.create(analysis, Messages.toString(Messages.CALLED_METHOD_DOES_NOT_PROMISE));
				rd.addCheckedPromise(pd);
				rd.setInconsistent();
				setResultDependUponDrop(rd, node, Messages.CALLED_METHOD_DOES_NOT_PROMISE, DebugUnparser.toString(node));
				rd.addProposal(new ProposedPromiseBuilder("Starts", "nothing",
						declaration, node, Origin.MODEL));
				success = false;
			}		
		} else {
			success = true;
		}
		final PromiseRef depend  = new PromiseRef("Starts", "nothing", declaration);		
		return new Result(new AndAnalysisResult(pd, node, depend), success);
	}

	public ThreadEffectsAnalysis(ThreadEffectsModule ted, final IBinder b) {
		binder = b;
		analysis = ted;
	}

	public List<IAnalysisResult> analyzeCompilationUnit(final IRNode compUnit,
			final Drop resultDependUpon) {
		List<IAnalysisResult> results = null;
		this.resultDependUpon = resultDependUpon;
		final Iterator<IRNode> nodes = JJNode.tree.topDown(compUnit);
		while (nodes.hasNext()) {
			final IRNode node = nodes.next();
			final Operator op = getOperator(node);
			if (MethodDeclaration.prototype.includes(op)) {
				try {
				  /* startsNothing(node) == false implies the method could start
				   * anything (via default annotation). There is nothing to check
				   * in that case: all code is always consistent with that.
				   */
					if (startsNothing(node)) {
						String modelName = genModelName(node, op);
						// System.out.println("[ThreadEffects] method @starts nothing "
						// + DebugUnparser.toString(node));
						StartsPromiseDrop p = ThreadEffectsRules
								.getStartsSpec(node);
						if (p == null) {
							// DROP DOESN'T EXIST
							LOG
									.log(
											Level.SEVERE,
											"ThreadEffects encountered a problem" //$NON-NLS-1$
													+ " extracting the promise drop from the MethodDeclaration " //$NON-NLS-1$
													+ DebugUnparser
															.toString(node)
													+ " within compilation unit:\n" //$NON-NLS-1$
													+ DebugUnparser
															.toString(compUnit));
						} else {
							// FIX just testing
							IDE.getInstance().getReporter().reportInfo(node,
									"@Starts nothing");
						}

						List<IAnalysisResult> results2 = examineBlock(node, op, p, modelName);
						if (results == null) {
							results = results2;
						} else {
							results.addAll(results2);
						}
					}
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "ThreadEffects encountered a problem" //$NON-NLS-1$
							+ " examining the MethodDeclaration " //$NON-NLS-1$
							+ DebugUnparser.toString(node)
							+ " within compilation unit:\n" //$NON-NLS-1$
							+ DebugUnparser.toString(compUnit) + "\n", e); //$NON-NLS-1$
				}
			} else if (ConstructorDeclaration.prototype.includes(op)) {
				try {
          /* startsNothing(node) == false implies the method could start
           * anything (via default annotation). There is nothing to check
           * in that case: all code is always consistent with that.
           */
					if (startsNothing(node)) {
						String modelName = genModelName(node, op);
						// System.out.println("[ThreadEffects] constructor @starts nothing "
						// + DebugUnparser.toString(node));
						StartsPromiseDrop p = ThreadEffectsRules
								.getStartsSpec(node);
						if (p == null) {
							// DROP DOESN'T EXIST
							LOG
									.log(
											Level.SEVERE,
											"ThreadEffects encountered a problem" //$NON-NLS-1$
													+ " extracting the promise drop from the ConstructorDeclaration " //$NON-NLS-1$
													+ DebugUnparser
															.toString(node)
													+ " within compilation unit:\n" //$NON-NLS-1$
													+ DebugUnparser
															.toString(compUnit));
						}
						List<IAnalysisResult> results2 = examineBlock(node, op, p, modelName);
						if (results == null) {
							results = results2;
						} else {
							results.addAll(results2);
						}
					}
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "ThreadEffects encountered a problem" //$NON-NLS-1$
							+ " examining the ConstructorDeclaration " //$NON-NLS-1$
							+ DebugUnparser.toString(node)
							+ " within compilation unit:\n" //$NON-NLS-1$
							+ DebugUnparser.toString(compUnit) + "\n", e); //$NON-NLS-1$
				}
			}
		}
		if (results == null) {
			return Collections.emptyList();
		}
		return results;
	}
}