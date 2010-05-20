/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/colors/TransparentPromiseDrop.java,v 1.3 2007/10/28 18:17:06 dfsuther Exp $*/
package edu.cmu.cs.fluid.sea.drops.threadroles;

import com.surelogic.aast.promise.ThreadRoleTransparentNode;
import com.surelogic.analysis.threadroles.TRoleMessages;

import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public class TRoleTransparentDrop extends BooleanPromiseDrop<ThreadRoleTransparentNode> {
  public TRoleTransparentDrop(ThreadRoleTransparentNode n) {
    super(n);
    setCategory(TRoleMessages.assuranceCategory);
    setMessage("@ThreadRoleTransparent");
  }
}
