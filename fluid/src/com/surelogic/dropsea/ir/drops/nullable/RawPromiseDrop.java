package com.surelogic.dropsea.ir.drops.nullable;

import com.surelogic.aast.promise.RawNode;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "Raw" promises established by the null value analysis.
 */
public final class RawPromiseDrop extends BooleanPromiseDrop<RawNode> {

  public RawPromiseDrop(RawNode a) {
    super(a);
    setCategorizingString(JavaGlobals.LOCK_ASSURANCE_CAT);
    setMessage(20, getAAST(), DebugUnparser.toString(getNode()));
  }
}
