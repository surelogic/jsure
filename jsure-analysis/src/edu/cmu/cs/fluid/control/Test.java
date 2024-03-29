/* $Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/control/Test.java,v 1.9 2006/03/28 20:19:03 chance Exp $ */
package edu.cmu.cs.fluid.control;

import java.util.NoSuchElementException;

import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotInfo;
import edu.cmu.cs.fluid.ir.SlotUndefinedException;

public class Test {
  private int level = 0;
  private int edges = 0;
  public static int traverse(ControlNode node) {
    SlotInfo<Boolean> si = SimpleSlotFactory.prototype.<Boolean>newAttribute(Boolean.FALSE);
    Test t = new Test();
    t.traverseNode(node,si);
    return t.edges;
  }
  void traverseNode(ControlNode node, SlotInfo<Boolean> si) {
    System.out.print(level + ": touched a " + node.getClass().getName());
    Component comp = null;
    if (node instanceof ComponentPort) {
      comp = ((ComponentPort)node).getComponent();
    } else if (node instanceof SubcomponentPort) {
      comp = ((SubcomponentPort)node).getSubcomponent().getComponentInChild();
    } else if (node instanceof ComponentFlow) {
      comp = ((ComponentFlow)node).getComponent();
    } else if (node instanceof ComponentChoice) {
      comp = ((ComponentChoice)node).getComponent();
    } 
    if (comp == null) {
      System.out.println();
    } else {
      System.out.println(" for " + Component.tree.getOperator(comp.getSyntax()).name());
    }
    try {
      ++level;
      try {
	ControlEdgeIterator outs;
	try {
	  outs = node.getOutputs();
	} catch (SlotUndefinedException e) {
	  return;
	}
	while (true) {
	  traverseEdge(outs.nextControlEdge(),si);
	}
      } catch (NoSuchElementException e) {
      }
    } finally {
      --level;
    }
  }
  void traverseEdge(ControlEdge edge, SlotInfo<Boolean> si) {
    Boolean b = edge.getSlotValue(si);
    if (!b.booleanValue()) {
      edge.setSlotValue(si,Boolean.TRUE);
      ++edges;
      traverseNode(edge.getSink(),si);
    }
  }
}
