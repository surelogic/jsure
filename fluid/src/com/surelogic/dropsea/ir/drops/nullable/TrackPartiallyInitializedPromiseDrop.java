package com.surelogic.dropsea.ir.drops.nullable;

import com.surelogic.aast.promise.TrackPartiallyInitializedNode;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 * Promise drop for "TrackPartiallyInitialized" promise
 */
public final class TrackPartiallyInitializedPromiseDrop extends BooleanPromiseDrop<TrackPartiallyInitializedNode> {
  public TrackPartiallyInitializedPromiseDrop(TrackPartiallyInitializedNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.NULL_CAT);
  }
  
  public final boolean verifyParent() {
	  return getAAST().verifyParent();
  }
}