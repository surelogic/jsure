package com.surelogic.analysis.uniqueness.sideeffecting;

import java.util.*;
import java.util.logging.Level;

import jsr166y.forkjoin.Ops.Procedure;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.analysis.*;
import com.surelogic.analysis.bca.BindingContextAnalysis;
import com.surelogic.analysis.uniqueness.Messages;
import com.surelogic.analysis.uniqueness.UniquenessUtils;
import com.surelogic.annotation.rules.UniquenessRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.java.util.TypeUtil;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.sea.PromiseDrop;
import edu.cmu.cs.fluid.sea.drops.CUDrop;
import edu.cmu.cs.fluid.sea.drops.promises.*;
import edu.cmu.cs.fluid.sea.proxy.ResultDropBuilder;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ImmutableHashOrderSet;
import edu.cmu.cs.fluid.util.Pair;

public class UniquenessAnalysisModule extends AbstractAnalysisSharingAnalysis<BindingContextAnalysis, UniquenessAnalysis,Void> {
  private final Map<PromiseDrop<? extends IAASTRootNode>, Set<UniquenessControlFlowDrop>> uniqueDropsToUses = 
    new HashMap<PromiseDrop<? extends IAASTRootNode>, Set<UniquenessControlFlowDrop>>();
  
  
  
  public UniquenessAnalysisModule() {
		super(true && !singleThreaded, null, "UniqueAnalysis", BindingContextAnalysis.factory);
	}


  
	@Override
	protected UniquenessAnalysis constructIRAnalysis(IBinder binder) {
		return new UniquenessAnalysis(this, binder,	false, getSharedAnalysis());
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
	public Iterable<IRNode> analyzeEnd(IIRAnalysisEnvironment env, IIRProject p) {
    // Link control flow PROMISE drops to unique fields
    for (final Map.Entry<PromiseDrop<? extends IAASTRootNode>, Set<UniquenessControlFlowDrop>> entry : uniqueDropsToUses.entrySet()) {
      final ResultDropBuilder middleDrop = ResultDropBuilder.create(
          this, Messages.toString(Messages.CONTROL_FLOW_ROOT));
      middleDrop.addCheckedPromise(entry.getKey());
      middleDrop.setConsistent();
      middleDrop.setNode(entry.getKey().getNode());
      setResultDependUponDrop(middleDrop, entry.getKey().getNode());
      middleDrop.setResultMessage(Messages.CONTROL_FLOW_ROOT, p.getName());
      for (final UniquenessControlFlowDrop cfDrop : entry.getValue()) {
        middleDrop.addTrustedPromise(cfDrop);
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
			final Set<IRNode> methods = shouldAnalyzeCompilationUnit(compUnit);

      if (runInParallel() == ConcurrencyType.INTERNALLY) {
        runInParallel(IRNode.class, methods, new Procedure<IRNode>() {
          public void op(IRNode methodDecl) {
            final String methodName = JavaNames.genRelativeFunctionName(methodDecl);
            if (monitor != null) {
              monitor.subTask("Checking [ Uniqueness Assurance ] " + methodName);
            }
            analzyePseudoMethodDeclaration(methodDecl);
            ImmutableHashOrderSet.clearCaches();
          }
        });
        return !methods.isEmpty();
      } else {
        // Analyze the given nodes
        for (Iterator<IRNode> iter = methods.iterator(); iter.hasNext();) {
          final IRNode methodDecl = iter.next();
          final String methodName = JavaNames.genQualifiedMethodConstructorName(methodDecl);
          if (monitor != null) {
            monitor.subTask("Checking [ Uniqueness Assurance ] " + methodName);
          }
          System.out.println("Sequential: " + methodName);
          analzyePseudoMethodDeclaration(methodDecl);
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
	private void analzyePseudoMethodDeclaration(final IRNode methodDecl) {
	  /* Invoking getResultFor() on the query will kick off the control-flow
	   * analysis of the method.  We use the results of the query later on
	   * to link the control-flow result drop of this method to the 
	   * unique fields read by the method body.
	   */
    final Pair<UniquenessControlFlowDrop, Set<PromiseDrop<? extends IAASTRootNode>>> links =
      getAnalysis().getUsedUnique(methodDecl).getResultFor(methodDecl);
    for (final PromiseDrop<? extends IAASTRootNode> pd : links.second()) {
      Set<UniquenessControlFlowDrop> value = uniqueDropsToUses.get(pd);
      if (value == null) {
        value = new HashSet<UniquenessControlFlowDrop>();
        uniqueDropsToUses.put(pd, value);
      }
      value.add(links.first());
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
	private Set<IRNode> shouldAnalyzeCompilationUnit(final IRNode compUnit) {
    final NewShouldAnalyzeVisitor visitor = new NewShouldAnalyzeVisitor(getBinder());
	  visitor.doAccept(compUnit);
	  return visitor.getResults();
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
    private final Set<IRNode> results = new HashSet<IRNode>();
    
    
    
    public NewShouldAnalyzeVisitor(final IBinder binder) {
      super(true);
      this.binder = binder;
    }

    
    
    public Set<IRNode> getResults() {
      return results;
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
            results.add(getEnclosingDecl());
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
        results.add(cdecl);
      }
            
      // Check the rest of the constructor
      doAcceptForChildren(cdecl);
    }

    @Override
    public Void visitFieldRef(final IRNode fieldRef) {
      /* Case (2): A use of a unique field. */
      if (UniquenessUtils.isFieldUnique(binder.getBinding(fieldRef))) {
        results.add(getEnclosingDecl());
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
        results.add(mdecl);
      }
    
      doAcceptForChildren(mdecl);
    }
    
    @Override
    protected void handleFieldInitialization(final IRNode varDecl, final boolean isStatic) {
      /* CASE (1): If the field is UNIQUE then we
       * add the current enclosing declaration to the results.
       */
      if (UniquenessUtils.isFieldUnique(varDecl)) {
        results.add(getEnclosingDecl());
      }
      // analyze the the RHS of the initialization
      doAcceptForChildren(varDecl);
    }
	}
}
