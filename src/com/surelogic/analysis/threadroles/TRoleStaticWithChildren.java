/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticWithChildren.java,v 1.2 2007/07/09 13:39:26 chance Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import edu.cmu.cs.fluid.ir.IRNode;


public abstract class TRoleStaticWithChildren extends TRoleStaticStructure {

  private Collection<TRoleStaticStructure> children;
  
  /**
   * Add a child to the current node
   * @param theChild
   */
  public void addChild(TRoleStaticStructure theChild) {
    if (theChild == null) return;
    if (children==null) {
      children = new ArrayList<TRoleStaticStructure>(1);
    }

    children.add(theChild);
  }
  
  /**
   * Remove a child from the current node. 
   * @param theChild The child node to remove. Never null, but need not be a
   * current child.
   */
  public void removeChild(TRoleStaticStructure theChild) {
   if (theChild == null) return;
    
    children.remove(theChild);
  }
  
  /**
   * Get the children of the current node.
   * @return A copy of the children of the current node. 
   * Never null, but may be empty.
   */
  public Collection<TRoleStaticStructure> getChildren() {
    if (children==null) {
      return Collections.emptyList();
    }
    
    Collection<TRoleStaticStructure> res = new HashSet<TRoleStaticStructure>(children.size());
    res.addAll(children);
    return res;
  }

  public TRoleStaticWithChildren(final IRNode node,
                                 final TRoleStaticWithChildren parent) {
    super(node, parent);
    children = null;
  }

}
