package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.SimpleExplicitSlotFactory;
import edu.cmu.cs.fluid.ir.SimpleSlotFactory;
import edu.cmu.cs.fluid.ir.SlotAlreadyRegisteredException;
import edu.cmu.cs.fluid.ir.SlotFactory;

public class TestSymmetricDigraph extends TestDigraph {
  public static void main(String[] args) {
    new TestSymmetricDigraph().test(args);
  }
  @Override
  protected MutableDigraphInterface createStored(SlotFactory sf) {
    try {
      return new SymmetricDigraph(null,sf);
    } catch (SlotAlreadyRegisteredException ex) {
      System.out.println("panic: " + ex);
      ex.printStackTrace();
      System.exit(1);
      return null;
    }
  }
  @SuppressWarnings("unchecked")
  protected MutableDigraphInterface createCopy(Digraph dig) {
    return new SymmetricDigraph(dig.getAttribute("children"),
				dig.getAttribute("parents"));
  }
  @SuppressWarnings("unchecked") MutableDigraphInterface createBackwardCopy(MutableDigraphInterface dig) {
    return new SymmetricDigraph(dig.getAttribute("parents"),
				dig.getAttribute("children"));
  }

  @Override
  public void test(String[] args) {
    verbose = args.length != 0;
    // immutable SymmetricDigraphs are basically useless.
    test(SimpleSlotFactory.prototype);
    test(SimpleExplicitSlotFactory.prototype);
 }
  /**
   * @param slotFactory
   */
  public void test(SlotFactory slotFactory) {
    MutableDigraphInterface dig = createStored(slotFactory);
    test("mutable",dig,createCopy(dig),true,getDagOK());
    MutableDigraphInterface backward = createBackwardCopy(dig);
    test("backward",backward,backward,true,getDagOK());
  }
}
