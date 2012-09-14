package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.NonNullNode;
import com.surelogic.annotation.rules.NonNullRules;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public final class NonNullPromiseDrop extends BooleanPromiseDrop<NonNullNode> {

  public NonNullPromiseDrop(NonNullNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    setResultMessage(20, NonNullRules.NONNULL, DebugUnparser.toString(getNode()));
  }
}
