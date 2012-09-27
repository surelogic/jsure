/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/colors/TransparentPromiseDrop.java,v 1.3 2007/10/28 18:17:06 dfsuther Exp $*/
package com.surelogic.dropsea.ir.drops.threadroles;

import com.surelogic.aast.promise.ThreadRoleTransparentNode;
import com.surelogic.analysis.threadroles.TRoleMessages;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;


public class TRoleTransparentDrop extends BooleanPromiseDrop<ThreadRoleTransparentNode> 
implements IThreadRoleDrop {
  public TRoleTransparentDrop(ThreadRoleTransparentNode n) {
    super(n);
    setCategorizingString(TRoleMessages.assuranceCategory);
    setMessage(12,"@ThreadRoleTransparent");
  }
}
