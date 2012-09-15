/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticCall.java,v 1.5 2007/07/09 14:26:57 chance Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.*;

import com.surelogic.dropsea.ir.drops.threadroles.RegionTRoleModel;


import edu.cmu.cs.fluid.ir.IRNode;


public class TRoleStaticCall extends TRoleStaticStructure {

  public List<RegionTRoleModel> colorCRMsHere;
  
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure#accept(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.IColorVisitor)
   */
  @Override
  public void accept(ITRoleStaticVisitor visitor) {
     visitor.visitCall(this);
  }
  
  public TRoleStaticCall(final IRNode node, final TRoleStaticWithChildren parent) {
    super(node, parent);
  }

}
