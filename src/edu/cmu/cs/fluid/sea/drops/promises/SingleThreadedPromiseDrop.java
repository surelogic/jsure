package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.SingleThreadedNode;
import com.surelogic.sea.drops.*;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Promise drop for "singleThreaded" promises.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class SingleThreadedPromiseDrop extends BooleanPromiseDrop<SingleThreadedNode> {
  public SingleThreadedPromiseDrop(SingleThreadedNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
  
  @Override
  protected void computeBasedOnAST() {
    String name = JavaNames.genMethodConstructorName(getNode());
    setMessage(Messages.LockAnnotation_singleThreadedDrop, name);
  }
}