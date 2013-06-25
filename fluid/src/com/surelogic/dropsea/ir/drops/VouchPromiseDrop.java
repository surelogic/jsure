package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.promise.VouchSpecificationNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 */
public class VouchPromiseDrop extends PromiseDrop<VouchSpecificationNode> {

  public VouchPromiseDrop(VouchSpecificationNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.VOUCH_CAT);
  }

  @Override
  protected IRNode useAlternateDeclForUnparse() {
	  return BooleanPromiseDrop.computeAlternateDeclForUnparse(getNode());
  }
}