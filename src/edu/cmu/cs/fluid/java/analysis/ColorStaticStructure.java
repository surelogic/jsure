/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/java/analysis/ColorStaticStructure.java,v 1.6 2008/06/24 19:13:13 thallora Exp $*/
package edu.cmu.cs.fluid.java.analysis;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;

import edu.cmu.cs.fluid.ir.IRNode;

@Deprecated
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
