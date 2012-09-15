package com.surelogic.dropsea.ir.drops.method.constraints;

import com.surelogic.aast.promise.StartsSpecificationNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Promise drop for "starts nothing" promises established by the thread effects
 * analysis.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.ThreadEffectsAnalysis
 * @see edu.cmu.cs.fluid.java.bind.StartsAnnotation
 */
public final class StartsPromiseDrop extends PromiseDrop<StartsSpecificationNode> {
  public StartsPromiseDrop(StartsSpecificationNode a) {
    super(a);
    this.setCategory(JavaGlobals.THREAD_EFFECTS_CAT);
    setMessage(Messages.StartsAnnotation_startNothingDrop, JavaNames.genMethodConstructorName(getNode()));
  }

  public boolean startsNothing() {
    return getNode() != null;
  }
}