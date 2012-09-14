/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/colors/TransparentPromiseDrop.java,v 1.3 2007/10/28 18:17:06 dfsuther Exp $*/
package edu.cmu.cs.fluid.sea.drops.threadroles;

import com.surelogic.aast.promise.ThreadRoleTransparentNode;
import com.surelogic.analysis.threadroles.TRoleMessages;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;


public class TRoleTransparentDrop extends BooleanPromiseDrop<ThreadRoleTransparentNode> 
implements IThreadRoleDrop {
  public TRoleTransparentDrop(ThreadRoleTransparentNode n) {
    super(n);
    setCategory(TRoleMessages.assuranceCategory);
    setMessage(12,"@ThreadRoleTransparent");
  }
}
