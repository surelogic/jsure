/*
 * $header$
 * Created on Jul 7, 2005
 */
package edu.cmu.cs.fluid.java.analysis;

import java.util.Iterator;

import edu.cmu.cs.fluid.FluidRuntimeException;
import edu.cmu.cs.fluid.control.Component;
import edu.cmu.cs.fluid.control.ControlEdgeIterator;
import edu.cmu.cs.fluid.control.FlowAnalysis;
import edu.cmu.cs.fluid.control.ForwardAnalysis;
import edu.cmu.cs.fluid.control.LabelList;
import edu.cmu.cs.fluid.control.UnknownLabel;
import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaComponentFactory;
import edu.cmu.cs.fluid.java.operator.FlowUnit;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.util.FlatLattice;
import edu.cmu.cs.fluid.util.Lattice;

/**
 * Test to see if the stack is kept the right height for the
 * CFG and {@link JavaEvaluationTransfer}
 * @author boyland
 */
@SuppressWarnings("unchecked")
public class TestEvaluationTransfer {

  public static void runAll(IRNode root) {
    for (Iterator<IRNode> it = JJNode.tree.bottomUp(root); it.hasNext();) {
      IRNode n = it.next();
      if (JJNode.tree.getOperator(n) instanceof FlowUnit) {
        run(n);
      }
    }
  }
  
  public static void run(IRNode flowUnit) {
    System.out.println("Testing CFG for " + DebugUnparser.toString(flowUnit));
    FlowUnit op = (FlowUnit) JJNode.tree.getOperator(flowUnit);
    FlowAnalysis fa = new ForwardAnalysis("TestCFG", FlatLattice.topValue,
        new Transfer(), DebugUnparser.viewer);
    fa.initialize(op.getSource(flowUnit).getOutput(), 
                        FlatLattice.newInteger(0));
    fa.initialize(op.getNormalSink(flowUnit).getInput());
    LabelList defaultLL = LabelList.empty.addLabel(UnknownLabel.prototype);
    fa.initialize(op.getAbruptSink(flowUnit).getInput(), defaultLL, fa.getLattice().top());
    fa.performAnalysis();
    for (Iterator<IRNode> it = JJNode.tree.bottomUp(flowUnit); it.hasNext();) {
      IRNode node = it.next();
      Component c;
      if ((c = JavaComponentFactory.getComponent(node, true)) != null) {
        ControlEdgeIterator outputs = c.getEntryPort().getOutputs();
        if (!outputs.hasNext()) {
          // System.out.println("No input? " + DebugUnparser.toString(node));
          continue;
        }
        FlatLattice entryVal = ((FlatLattice) fa.getInfo(outputs.nextControlEdge()));
        if (entryVal.inDomain()) {
          FlatLattice normalVal = (FlatLattice) fa.getInfo(c
              .getNormalExitPort().getOutputs().nextControlEdge());
          FlatLattice abruptVal = (FlatLattice) fa.getInfo(c
              .getAbruptExitPort().getOutputs().nextControlEdge(),defaultLL);
          if (normalVal.equals(FlatLattice.bottomValue) || 
              abruptVal.equals(FlatLattice.bottomValue)) {
            System.out
                .println("Stack depth at " + DebugUnparser.toString(node));
            System.out.println("  Before: " + entryVal);
            System.out.println("  After: " + normalVal);
            System.out.println("  Abrupt: " + abruptVal);
          }
        }
      }
    }
  }
  
  static class Transfer extends JavaEvaluationTransfer {
    static FlatLattice zero = FlatLattice.newInteger(0);

    Transfer() {
      super(null,null);
    }

    @Override
    protected Lattice pop(Lattice val) {
      FlatLattice fl = (FlatLattice)val;
      if (!fl.inDomain()) return val;
      int depth = fl.intValue();
      if (depth == 0) {
        throw new FluidRuntimeException("Popped empty stack");
      }
      return FlatLattice.newInteger(depth-1);
    }

    @Override
    protected Lattice push(Lattice val) {
      FlatLattice fl = (FlatLattice)val;
      if (!fl.inDomain()) return val;
      int depth = fl.intValue();
      return FlatLattice.newInteger(depth+1);
    }

    @Override
    protected Lattice popAllPending(Lattice val) {
      return zero;
    }
  }
}
