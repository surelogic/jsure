package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ImmutableRefNode;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public final class ImmutableRefPromiseDrop extends BooleanPromiseDrop<ImmutableRefNode> {

  public ImmutableRefPromiseDrop(ImmutableRefNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    setResultMessage(14, DebugUnparser.toString(getNode()));
  }
}
