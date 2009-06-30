package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.*;
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
public class ReachingDefAnalysis<V> extends IntraproceduralAnalysis<IRNode,V> {
  public ReachingDefAnalysis(final IBinder b) {
    super(b);
  }

  @Override
  public FlowAnalysis<IRNode> createAnalysis(final IRNode flowNode) {
    final FlowUnit op = (FlowUnit) tree.getOperator(flowNode);
    final IRNode[] locals = flowUnitLocals(flowNode);
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

    /*
        for( int i = 0; i < locals.length; i++ ) {
          SetLattice s = (SetLattice)init.getValue( i );
          System.out.println( "Elt " + i + ":" ); 
          try {
            for( int j = 0; j < s.size(); j++ ) {
              final IRNode n = (IRNode)s.elementAt( j ); 
              System.out.println( "    " + JavaNode.toString( n ) + ": " + DebugUnparser.toString( n ) );
            }
            System.out.println();
          } catch( SetException e ) { }
        }
    */

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
  public SetLattice getReachingDef(final IRNode use) {
    final Operator op = tree.getOperator(use);
    if (VariableUseExpression.prototype.includes(op)) {
      IRNode binding = binder.getBinding(use);
      IRNode loc = use;
      for (;;) {
        final ReachingDefs rd = (ReachingDefs) getAnalysisResultsBefore(loc);
        try {
          return rd.getReachingDefsFor(binding);
        } catch (IllegalArgumentException e) {
          LOG.info("Cannot find local " + JJNode.getInfo(use)
              + " locally, looking outward");
          loc = IntraproceduralAnalysis.getFlowUnit(loc);
          loc = JJNode.tree.getParent(loc);
          // try here now
        }
      }
    }
    throw new IllegalArgumentException("Node isn't a use expression");
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


