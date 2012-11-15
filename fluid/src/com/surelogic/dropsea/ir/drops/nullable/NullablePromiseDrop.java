package com.surelogic.dropsea.ir.drops.nullable;

import com.surelogic.aast.promise.NullableNode;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;

public final class NullablePromiseDrop extends BooleanPromiseDrop<NullableNode> {

  public NullablePromiseDrop(NullableNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.NULL_CAT);
  }
  
  @Override
  protected IRNode useAlternateDeclForUnparse() {
	  return BooleanPromiseDrop.computeAlternateDeclForUnparse(getNode());
  }
}
