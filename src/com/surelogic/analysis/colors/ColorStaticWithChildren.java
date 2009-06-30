/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticWithChildren.java,v 1.2 2007/07/09 13:39:26 chance Exp $*/
package com.surelogic.analysis.colors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import edu.cmu.cs.fluid.ir.IRNode;


public abstract class ColorStaticWithChildren extends ColorStaticStructure {

  private Collection<ColorStaticStructure> children;
  
  /**
   * Add a child to the current node
   * @param theChild
   */
  public void addChild(ColorStaticStructure theChild) {
    if (theChild == null) return;
    if (children==null) {
      children = new ArrayList<ColorStaticStructure>(1);
    }

    children.add(theChild);
  }
  
  /**
   * Remove a child from the current node. 
   * @param theChild The child node to remove. Never null, but need not be a
   * current child.
   */
  public void removeChild(ColorStaticStructure theChild) {
   if (theChild == null) return;
    
    children.remove(theChild);
  }
  
  /**
   * Get the children of the current node.
   * @return A copy of the children of the current node. 
   * Never null, but may be empty.
   */
  public Collection<ColorStaticStructure> getChildren() {
    if (children==null) {
      return Collections.emptyList();
    }
    
    Collection<ColorStaticStructure> res = new HashSet<ColorStaticStructure>(children.size());
    res.addAll(children);
    return res;
  }

  public ColorStaticWithChildren(final IRNode node,
                                 final ColorStaticWithChildren parent) {
    super(node, parent);
    children = null;
  }

}
