/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticStructure.java,v 1.7 2008/06/24 19:13:11 thallora Exp $*/
package com.surelogic.analysis.colors;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;


import edu.cmu.cs.fluid.ir.IRNode;

public abstract class ColorStaticStructure implements IColorAcceptor {
  

  private final IRNode node;
  private final ColorStaticWithChildren parent;

  private static final Logger LOG = 
	  SLLogger.getLogger("analysis.colors.colorstaticstructure");

  protected ColorStaticStructure(IRNode node, ColorStaticWithChildren parent) {
    this.node = node;
    this.parent = parent;
    if (parent != null) {
      parent.addChild(this);
    } 
  }

  
  public abstract void accept(
      IColorVisitor visitor);

  
  /**
   * @return Returns the node.
   */
  public IRNode getNode() {
    return node;
  }


  public ColorStaticWithChildren getParent() {
    return parent;
  }
}
