package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.SelfProtectedNode;
import com.surelogic.sea.drops.*;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

/**
 * Promise drop for "selfProtected" promises.
 * 
 * @see edu.cmu.cs.fluid.java.analysis.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class SelfProtectedPromiseDrop extends BooleanPromiseDrop<SelfProtectedNode> {
  public SelfProtectedPromiseDrop(SelfProtectedNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
  
  @Override
  protected void computeBasedOnAST() {
    String name = JavaNames.getTypeName(getNode());
    setMessage(Messages.LockAnnotation_selfProtectedDrop, name);
  }
}