/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticMeth.java,v 1.2 2007/07/09 13:39:26 chance Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.ir.IRNode;


public class TRoleStaticMeth extends TRoleStaticBlockish {
  
  final static Map<IRNode, TRoleStaticMeth> irnodeToTRSM = 
    new HashMap<IRNode, TRoleStaticMeth>();
  
  public boolean needsBodyTraversal = false;

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure#accept(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.IColorVisitor)
   */
  @Override
  public void accept(ITRoleStaticVisitor visitor) {
    visitor.visitMeth(this);
  }

  public TRoleStaticMeth(final IRNode node, 
                         final TRoleStaticWithChildren parent) {
    super(node, parent);
    synchronized (TRoleStaticMeth.class) {
      irnodeToTRSM.put(node, this);
    }
  }
  
  public static synchronized TRoleStaticMeth getStaticMeth(final IRNode forNode) {
    final TRoleStaticMeth res = irnodeToTRSM.get(forNode);
    return res;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  protected void invalidateAction() {
    synchronized (TRoleStaticMeth.class) {
      irnodeToTRSM.remove(this.getNode());
    }
  }
  
  
}
