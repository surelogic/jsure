package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.SelfProtectedNode;
import com.surelogic.sea.drops.*;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

/**
 * Promise drop for "ThreadSafe" promises.
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
    setResultMessage(Messages.LockAnnotation_selfProtectedDrop, name);
  }
}