package edu.cmu.cs.fluid.java.analysis;

import edu.cmu.cs.fluid.control.BackwardAnalysis;
import edu.cmu.cs.fluid.control.FlowAnalysis;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.bind.IBinder;
import edu.cmu.cs.fluid.java.operator.AssignmentInterface;
import edu.cmu.cs.fluid.java.operator.VariableUseExpression;
import edu.cmu.cs.fluid.java.operator.VariableDeclarator;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.*;

/** Compute the set of live local variables at each program point.
 *  Currently it does <b>not</b> work for anonymous class instance creation.
 */
public class LiveVariables<V> extends IntraproceduralAnalysis<IRNode,V> {
  public LiveVariables(IBinder b) { super(b); }
  @Override
  public FlowAnalysis<IRNode> createAnalysis(IRNode methodDecl) {
    SetLattice<IRNode> s = new UnionLattice<IRNode>();
    FlowAnalysis<IRNode> analysis =
        new BackwardAnalysis<IRNode>("live variable analysis",
			     s,new LiveVariableTransfer<V>(this,binder), DebugUnparser.viewer);
    return analysis;
  }
}

class LiveVariableTransfer<V> extends JavaBackwardTransfer<IRNode,V> {
  public LiveVariableTransfer(LiveVariables<V> ba, IBinder b) {
    super(ba,b);
  }
  @Override protected Lattice<IRNode> transferUse(IRNode node, Operator op, Lattice <IRNode>usedAfter) {
    if (op instanceof VariableUseExpression) {
      IRNode decl = binder.getBinding(node);
      return (Lattice<IRNode>)((SetLattice<IRNode>)usedAfter).addElement(decl);
    } else {
      return usedAfter;
    }
  }
  @Override protected Lattice<IRNode> transferAssignment(IRNode assign, Lattice<IRNode> usedAfter) {
    IRNode target = ((AssignmentInterface)tree.getOperator(assign)).getTarget(assign);
    if (VariableUseExpression.prototype.includes(tree.getOperator(target))) {
      IRNode decl = binder.getBinding(target);
      return (Lattice<IRNode>)((SetLattice<IRNode>)usedAfter).removeElement(decl);
    } else {
      return usedAfter;
    }
  }
  @Override protected Lattice<IRNode> transferInitialization(IRNode init, Lattice<IRNode> usedAfter) {
    if (tree.getOperator(init) instanceof VariableDeclarator) {
      return (Lattice<IRNode>)((SetLattice<IRNode>)usedAfter).removeElement(init);
    } else {
      return usedAfter;
    }
  }
}

