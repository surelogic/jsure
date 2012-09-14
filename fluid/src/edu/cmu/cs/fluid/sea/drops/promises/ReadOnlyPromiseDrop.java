package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ReadOnlyNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.UiShowAtTopLevel;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public final class ReadOnlyPromiseDrop extends BooleanPromiseDrop<ReadOnlyNode> implements UiShowAtTopLevel {

  public ReadOnlyPromiseDrop(ReadOnlyNode n) {
    super(n);
    setCategory(JavaGlobals.UNIQUENESS_CAT);
    setMessage(getAAST().toString());
  }
}