package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.*;
import edu.cmu.cs.fluid.control.Component.WhichPort;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.*;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/**
 * Analysis that determines what definitions reach each 
 * local variable use.  
 */
@SuppressWarnings("unchecked")
public class ReachingDefAnalysis<V> extends IntraproceduralAnalysis<IRNode,V> {
  public final class Query implements AnalysisQuery<SetLattice> {
    private final FlowAnalysis<IRNode> a;
    
    public Query(final IRNode flowUnit) {
      a = getAnalysis(flowUnit);
    }

    public SetLattice getResultFor(final IRNode use) {
      if (VariableUseExpression.prototype.includes(use)) {
        final IRNode binding = binder.getBinding(use);
        final ReachingDefs rd = (ReachingDefs) a.getAfter(use, WhichPort.ENTRY);
        return rd.getReachingDefsFor(binding);
      } else {
        throw new IllegalArgumentException("Node is not a variable use expression");
      }
    }

    public AnalysisQuery<SetLattice> getSubAnalysisQuery(final IRNode caller) {
      return null;
    }

    public boolean hasSubAnalysisQuery(final IRNode caller) {
      return false;
    }
  }
  
  public ReachingDefAnalysis(final IBinder b) {
    super(b);
  }

  @Override
  public FlowAnalysis<IRNode> createAnalysis(final IRNode flowNode) {
    final FlowUnit op = (FlowUnit) tree.getOperator(flowNode);
    final IRNode[] locals = flowUnitLocals(flowNode, true, binder);
    final ReachingDefs rd = new ReachingDefs(locals);
    final ForwardTransfer<IRNode> rdt = new ReachingDefsTransfer(this);
    final FlowAnalysis<IRNode> analysis =
      new ForwardAnalysis<IRNode>("reaching def analysis", rd, rdt, DebugUnparser.viewer);

    // Make sure that parameters have an initial value of 
    // the formal parameter itself
    ReachingDefs init = (ReachingDefs) rd.top();

    for (int i = 0; i < locals.length; i++) {
      if (ParameterDeclaration.prototype.includes(tree.getOperator(locals[i]))) {
        init = init.replaceValue(locals[i], locals[i]);
      }
    }

    analysis.initialize(op.getSource(flowNode).getOutput(), init);
    return analysis;
  }

  /**
   * Given a use expression, find the declarations that reach it.
   * @param use A variable use expression.
   * @return A {@link edu.cmu.cs.fluid.util.SetLattice}.  If the lattice
   * value is top, then <code>use</code> is in dead code.
   * Otherwise the lattice contains the definitions that reach
   * the use.  See {@link ReachingDefs#getReachingDefsFor} for
   * a description of what values may be in the set.
   * @exception IllegalArgumentException
   * Thrown if <code>use</code> is not a use expression.
   */
  public SetLattice getReachingDef(final IRNode use, final IRNode constructorContext) {
    final Operator op = tree.getOperator(use);
    if (VariableUseExpression.prototype.includes(op)) {
      IRNode binding = binder.getBinding(use);
      IRNode loc = use;
      for (;;) {
        final ReachingDefs rd = (ReachingDefs) getAnalysisResultsBefore(loc, constructorContext);
        try {
          return rd.getReachingDefsFor(binding);
        } catch (IllegalArgumentException e) {
          /* John added this stuff back in 2005 to try to deal with references
           * in Anonymous classes to external state.  Not sure if this works.
           * See bug 235.  Probably would be better to update the lattice to
           * handle this.
           */
          LOG.info("Cannot find local " + JJNode.getInfo(use)
              + " locally, looking outward");
          loc = IntraproceduralAnalysis.getFlowUnit(loc, constructorContext);
          loc = JJNode.tree.getParent(loc);
          // try here now
        }
      }
    }
    throw new IllegalArgumentException("Node isn't a use expression");
  }
  
  public Query getReachingDefQuery(final IRNode flowUnit) {
    return new Query(flowUnit);
  }

  //---------------------------------------------------------

  /* Transfer Function */
  private class ReachingDefsTransfer extends JavaForwardTransfer<IRNode,V> {
    public ReachingDefsTransfer(ReachingDefAnalysis<V> rd) {
      super(rd, ReachingDefAnalysis.this.binder);
    }

    @Override protected Lattice<IRNode> transferAssignment(
      final IRNode assign,
      final Lattice<IRNode> value) {
      final AssignmentInterface op = (AssignmentInterface) tree.getOperator(assign);
      final IRNode target = op.getTarget(assign);
      final Operator targetOp = tree.getOperator(target);
      if (VariableUseExpression.prototype.includes(targetOp)) {
        final IRNode targetDecl = binder.getBinding(target);
        return ((ReachingDefs) value).replaceValue(targetDecl, assign);
      } else {
        return value;
      }
    }

    @Override protected Lattice<IRNode> transferInitialization(
      final IRNode root,
      final Lattice<IRNode> value) {
      final IRNode p = tree.getParent(root);
      final IRNode gp = tree.getParent(p);
      if (tree.getOperator(gp) == DeclStatement.prototype) {
        final ReachingDefs rd = (ReachingDefs) value;
        return rd.replaceValue(root, root);
      } else {
        return value;
      }
    }
  }
}


