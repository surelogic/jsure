package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

public class TestTree extends TestDigraph {
  public static void main(String[] args) {
    new TestTree().test(args);
  }
  @Override
  protected MutableDigraphInterface createStored(SlotFactory sf) {
    try {
      return new Tree(null,sf);
    } catch (SlotAlreadyRegisteredException ex) {
      System.out.println("panic: " + ex);
      ex.printStackTrace();
      System.exit(1);
      return null;
    }
  }
  @Override
  @SuppressWarnings("unchecked")
  protected MutableDigraphInterface createCopy(MutableDigraphInterface dig) {
    return new Tree(dig.getAttribute(Digraph.CHILDREN),
		    dig.getAttribute(Tree.PARENTS),
		    dig.getAttribute(Tree.LOCATION));
  }
  @Override
  protected boolean getDagOK() { return false; }
}
