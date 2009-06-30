/*
 * Created on Sep 13, 2004
 *
 */
package edu.cmu.cs.fluid.tree;

import edu.cmu.cs.fluid.ir.*;


/**
 * @author Edwin
 *
 */
public class StitchedTreeFactory {
  static <T> SlotInfo<T> wrapSlotInfo(SlotInfo<T> si) {
    return new MutableDelegatingSlotInfo<T>(si, SimpleSlotFactory.prototype);
  }
  
  public static Tree createTree(Tree t) {
    try {
      return new Tree(SimpleSlotFactory.prototype, t);
    } catch (SlotAlreadyRegisteredException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }
  
  public static SyntaxTree createTree(SyntaxTree t) {
    try {
      return new SyntaxTree(SimpleSlotFactory.prototype, t);
    } catch (SlotAlreadyRegisteredException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    }
  }
}
