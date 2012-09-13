package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.StartsSpecificationNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.PromiseDrop;

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
    setResultMessage(Messages.StartsAnnotation_startNothingDrop, JavaNames.genMethodConstructorName(getNode()));
  }

  public boolean startsNothing() {
    return getNode() != null;
  }
}