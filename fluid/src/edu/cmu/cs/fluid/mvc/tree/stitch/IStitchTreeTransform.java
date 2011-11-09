/*
 * Created on Sep 14, 2004
 *
 */
package edu.cmu.cs.fluid.mvc.tree.stitch;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.mvc.tree.*;
import edu.cmu.cs.fluid.mvc.tree.syntax.*;


/**
 * @author Edwin
 *
 */
public interface IStitchTreeTransform {
  void init(AttributeHandler am);
  void rewriteTree(IRNode n);
  
  
  public static interface AttributeHandler {
    /**
     * 
     * @param name
     * @param type
     * @param sf
     * @param mutable
     * @return The unwrapped SlotInfo
     */
    SlotInfo addNodeAttribute(String name, IRType type, SlotFactory sf, boolean mutable);
  }
  
  public static interface Factory {
    IStitchTreeTransform create(ForestModel forest, ForestModelCore core, SyntaxForestModelCore score);
  }
}
