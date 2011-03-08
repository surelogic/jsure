package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.*;

/**
 * Promise drop for "NotThreadSafe" promises.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ImmutablePromiseDrop extends ModifiedBooleanPromiseDrop<ImmutableNode> {
  public ImmutablePromiseDrop(ImmutableNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
  
  @Override
  protected void computeBasedOnAST() {
    String name = JavaNames.getTypeName(getNode());
    setResultMessage(Messages.LockAnnotation_immutableDrop, name);
  }
}