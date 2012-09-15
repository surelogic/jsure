package com.surelogic.dropsea.ir.drops.uniqueness;

import com.surelogic.aast.promise.ReadOnlyNode;
import com.surelogic.dropsea.ir.UiShowAtTopLevel;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

public final class ReadOnlyPromiseDrop extends BooleanPromiseDrop<ReadOnlyNode> implements UiShowAtTopLevel {

  public ReadOnlyPromiseDrop(ReadOnlyNode n) {
    super(n);
    setCategory(JavaGlobals.UNIQUENESS_CAT);
    setMessage(12, getAAST().toString());
  }
}