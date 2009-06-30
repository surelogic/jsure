/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticMeth.java,v 1.2 2007/07/09 13:39:26 chance Exp $*/
package com.surelogic.analysis.colors;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.fluid.ir.IRNode;


public class ColorStaticMeth extends ColorStaticBlockish {
  
  final static Map<IRNode, ColorStaticMeth> irnodeToCSM = 
    new HashMap<IRNode, ColorStaticMeth>();
  
  public boolean needsBodyTraversal = false;

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure#accept(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.IColorVisitor)
   */
  @Override
  public void accept(IColorVisitor visitor) {
    visitor.visitMeth(this);
  }

  public ColorStaticMeth(final IRNode node, 
                         final ColorStaticWithChildren parent) {
    super(node, parent);
    synchronized (ColorStaticMeth.class) {
      irnodeToCSM.put(node, this);
    }
  }
  
  public static synchronized ColorStaticMeth getStaticMeth(final IRNode forNode) {
    final ColorStaticMeth res = irnodeToCSM.get(forNode);
    return res;
  }

  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.IRReferenceDrop#deponentInvalidAction()
   */
  protected void invalidateAction() {
    synchronized (ColorStaticMeth.class) {
      irnodeToCSM.remove(this.getNode());
    }
  }
  
  
}
