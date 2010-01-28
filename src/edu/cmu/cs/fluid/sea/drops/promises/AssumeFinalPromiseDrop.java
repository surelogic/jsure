package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.AssumeFinalNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

/**
 * Promise drop for "assumeFinal" assumptions about fields which are not null
 * but need to be treated as if they were.
 */
public class AssumeFinalPromiseDrop extends BooleanPromiseDrop<AssumeFinalNode> {
  public AssumeFinalPromiseDrop(AssumeFinalNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    setResultMessage(Messages.AssumeFinalAnnotation_finalFieldDrop, JavaNames.getFieldDecl(getNode()));
  }
}
