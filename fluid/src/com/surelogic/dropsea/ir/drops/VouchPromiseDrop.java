package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.promise.VouchSpecificationNode;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 */
public class VouchPromiseDrop extends PromiseDrop<VouchSpecificationNode> {

  public VouchPromiseDrop(VouchSpecificationNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.VOUCH_CAT);
    if (!XUtil.useExperimental()) {
    setMessage(16, getAAST());
    }
  }
}