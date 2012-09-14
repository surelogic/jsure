/*$Header: /cvs/fluid/fluid/src/com/surelogic/analysis/colors/ColorStaticBlock.java,v 1.2 2007/07/09 13:39:26 chance Exp $*/
package com.surelogic.analysis.threadroles;

import java.util.Set;

import com.surelogic.dropsea.ir.drops.promises.threadroles.TRoleGrantDrop;
import com.surelogic.dropsea.ir.drops.promises.threadroles.TRoleRevokeDrop;


import edu.cmu.cs.fluid.ir.IRNode;


public class TRoleStaticBlock extends TRoleStaticBlockish {

  public final Set<TRoleGrantDrop> grants;
  public final Set<TRoleRevokeDrop> revokes;
  
  @Override
  public void accept(ITRoleStaticVisitor visitor) {
    visitor.visitBlock(this);
  }
  
  public TRoleStaticBlock(
      final IRNode node,
      final TRoleStaticWithChildren parent,
      final Set<TRoleGrantDrop> grants, 
      final Set<TRoleRevokeDrop> revokes) {
    super(node, parent);
    this.grants = grants;
    this.revokes = revokes;
  }

}
