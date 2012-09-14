package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

/**
 * Promise drop for "Mutable" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class MutablePromiseDrop extends BooleanPromiseDrop<MutableNode> {

  public MutablePromiseDrop(MutableNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    final String name = JavaNames.getTypeName(getNode());
    setResultMessage(Messages.LockAnnotation_mutableDrop, name);
  }
}