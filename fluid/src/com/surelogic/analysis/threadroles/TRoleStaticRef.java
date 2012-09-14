/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticRef.java,v 1.6 2007/07/10 22:16:34 aarong Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.*;

import com.surelogic.dropsea.ir.drops.promises.threadroles.RegionTRoleModel;


import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.parse.JJNode;
import edu.cmu.cs.fluid.tree.Operator;


public class TRoleStaticRef extends TRoleStaticStructure {

  public List<RegionTRoleModel> trTargetsHere;
  /* (non-Javadoc)
   * @see edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure#accept(edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.ColorStaticStructure, edu.cmu.cs.fluid.sea.drops.promises.IColorVisitor)
   */
  @Override
  public void accept(ITRoleStaticVisitor visitor) {
    visitor.visitReference(this);
  }
  
  public TRoleStaticRef(final IRNode node, TRoleStaticBlockish parent, final List<RegionTRoleModel> tgts) {
    super(node, parent);
    final Operator op = JJNode.tree.getOperator(node);
    parent.allRefs.add(this);
    if (tgts != null && !tgts.isEmpty()) {
      parent.interestingRefs.add(this);
    }
    trTargetsHere = tgts;
  }

}
