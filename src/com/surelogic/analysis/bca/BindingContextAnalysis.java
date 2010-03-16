/*
 * $Header:
 * /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/BindingContextAnalysis.java,v
 * 1.21 2003/09/22 20:49:54 chance Exp $
 */
package com.surelogic.analysis.bca;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;


import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.control.FlowAnalysis;
import edu.cmu.cs.fluid.control.ForwardAnalysis;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.analysis.AnalysisQuery;
import edu.cmu.cs.fluid.java.analysis.IntraproceduralAnalysis;
import edu.cmu.cs.fluid.java.analysis.JavaForwardTransfer;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.bind.IJavaReferenceType;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/**
 * This class tracks bindings of locals within a method. It associates a
 * binding context with each control-flow point. This context binds locals to a
 * set of IR nodes. the following kinds of nodes can appear in the sets:
 * <ul>
 * <li>parameter declarations, which represent their initial values.
 * <li>constructor or method calls that return unique objects, which represent
 * any instance of its evaluation during the execution of the method.
 * <li>accesses of unique fields which represents any evaluation of the
 * given FieldRef expression IRNode.
 * </ul>
 */

public class BindingContextAnalysis extends IntraproceduralAnalysis<IRNode,ImmutableHashOrderSet<IRNode>> {
  public final class Query implements AnalysisQuery<ImmutableHashOrderSet<IRNode>> {
    private final FlowAnalysis<IRNode> a;

    public Query(final IRNode flowUnit) {
      a = getAnalysis(flowUnit);
    }
    
    public ImmutableHashOrderSet<IRNode> getResultFor(final IRNode expr) {
      return getExpressionsFromLattice(
          a.getAfter(expr, WhichPort.NORMAL_EXIT), expr);
    }

    public AnalysisQuery<ImmutableHashOrderSet<IRNode>> getSubAnalysisQuery(final IRNode caller) {
      return null;
    }

    public boolean hasSubAnalysisQuery(final IRNode caller) {
      return false;
    }
  }
  
  public BindingContextAnalysis(IBinder b) {
    super(b);
  }
  
  @Override
  public FlowAnalysis<IRNode> createAnalysis(IRNode flowNode) {
    FlowUnit op = (FlowUnit) tree.getOperator(flowNode);
    // XXX This is suspicious.  Come back to this in the future
    IRNode methodDecl = getRawFlowUnit(flowNode);
    ImmutableHashOrderSet<IRNode> localset =
      methodDeclLocals(methodDecl, CachedSet.<IRNode>getEmpty());
    //localset = filterNonObjectTypedLocals(localset);
    try {
      int n = localset.size();
      IRNode[] locals = new IRNode[n];
      for (int i = 0; i < n; ++i) {
        locals[i] = (IRNode) localset.elementAt(i);
      }
      BindingContext bc = new BindingContext(methodDecl, locals, binder);
      FlowAnalysis<IRNode> analysis =
        new ForwardAnalysis<IRNode>(
          "binding context analysis",
          bc,
          new BindingContextTransfer(binder),
          DebugUnparser.viewer);
      BindingContext start = (BindingContext) bc.top();
      for (int i = 0; i < n; ++i) {
        IRNode local = locals[i];
        if (ParameterDeclaration.prototype.includes(tree.getOperator(local)))
          start =
            start.replaceValue(local, CachedSet.<IRNode>getEmpty().addElement(local));
      }
      analysis.initialize(op.getSource(flowNode).getOutput(), start);
      //analysis.debug();
      return analysis;
    } catch (SetException e) {
      throw new FluidRuntimeException("infinite number of locals?");
    }
  }

  /**
   * @param localset
   */
  /*
  private ImmutableHashOrderSet<IRNode> filterNonObjectTypedLocals(
      final ImmutableHashOrderSet<IRNode> localset) {
    ImmutableHashOrderSet<IRNode> locals = CachedSet.getEmpty();
    for (final IRNode n : localset) {
      if (binder.getJavaType(n) instanceof IJavaReferenceType) {
        locals = locals.addElement(n);
      }
    }
    return locals;
  }
  */

  /**
   * Changed to also filter out non-Object typed locals
   */
  private ImmutableHashOrderSet<IRNode> methodDeclLocals(
    IRNode methodDecl,
    ImmutableHashOrderSet<IRNode> s) {
    //!! does not work for class initialization methods:
	// TODO switch to using a Visitor?
    Iterator<IRNode> e = tree.bottomUp(methodDecl);
    try {
      while (true) {
        IRNode node = e.next();
        Operator op = tree.getOperator(node);
        if (VariableDeclaration.prototype.includes(op) &&
        	binder.getJavaType(node) instanceof IJavaReferenceType) {
          s = s.addElement(node);
        }
      }
    } catch (NoSuchElementException ex) {
      return s;
    }
  }

  /**
	 * Return a set of object identifiers for a particular expression. This is
	 * the purpose of binding context analysis.
   * 
   * @param constructorContext
   *          The constructor declaration, if any, that is currently being
   *          analyzed. if non-<code>null</code>, this is used as the flow unit
   *          if it turns out that the node <code>node</code> is part of an
   *          instance field initializer or instance initialization block.
	 */
  public ImmutableHashOrderSet<IRNode> expressionObjects(
      final IRNode expr, final IRNode constructorContext) {
    return getExpressionsFromLattice(
        getAnalysisResultsAfter(expr, constructorContext), expr);
  }
  
  /**
   * Get an query object tailored to a specific flow unit.
   */
  public Query getExpressionObjectsQuery(final IRNode flowUnit) {
    return new Query(flowUnit);
  }
  
  private static ImmutableHashOrderSet<IRNode> getExpressionsFromLattice(
      final Lattice<IRNode> lv, final IRNode expr) {
    return ((BindingContext) lv).expressionObjects(expr);
  }
}

class BindingContextTransfer extends JavaForwardTransfer<IRNode,ImmutableHashOrderSet<IRNode>> {
  private static final Logger LOG = SLLogger.getLogger("FLUID.analysis.BCA");

  public BindingContextTransfer(IBinder b) {
    super(null, b);
  }
  @Override
  public Lattice<IRNode> transferAssignment(IRNode node, Lattice<IRNode> value) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine(DebugUnparser.toString(node) + " " + value);
    }
    AssignmentInterface op = (AssignmentInterface) tree.getOperator(node);
    IRNode lhs = op.getTarget(node);
    Operator lhsOp = tree.getOperator(lhs);
    BindingContext bc = (BindingContext) value;
    if (VariableUseExpression.prototype.includes(lhsOp)) {
      IRNode decl = binder.getBinding(lhs);
      ImmutableHashOrderSet<IRNode> rhsObjects =
        bc.expressionObjects(op.getSource(node));
      return bc.replaceValue(decl, rhsObjects);
    }
    return value;
  }
  
  @Override
  public Lattice<IRNode> transferInitialization(IRNode node, Lattice<IRNode> value) {
    BindingContext bc = (BindingContext) value;
    if (VariableDeclarator.prototype.includes(tree.getOperator(node))) {
      ImmutableHashOrderSet<IRNode> initObjects =
        bc.expressionObjects(VariableDeclarator.getInit(node));
      return bc.replaceValue(node, initObjects);
    }
    return value;
  }
}
