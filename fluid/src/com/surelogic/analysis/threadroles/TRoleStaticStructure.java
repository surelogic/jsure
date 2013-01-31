/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticStructure.java,v 1.7 2008/06/24 19:13:11 thallora Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.logging.Logger;

import com.surelogic.common.logging.SLLogger;


import edu.cmu.cs.fluid.ir.IRNode;

public abstract class TRoleStaticStructure implements ITRoleStaticAcceptor {
  

  private final IRNode node;
  private final TRoleStaticWithChildren parent;

  private static final Logger LOG = 
	  SLLogger.getLogger("analysis.troles.trolestaticstructure");

  protected TRoleStaticStructure(IRNode node, TRoleStaticWithChildren parent) {
    this.node = node;
    this.parent = parent;
    if (parent != null) {
      parent.addChild(this);
    } 
  }

  
  @Override
  public abstract void accept(
      ITRoleStaticVisitor visitor);

  
  /**
   * @return Returns the node.
   */
  public IRNode getNode() {
    return node;
  }


  public TRoleStaticWithChildren getParent() {
    return parent;
  }
}
