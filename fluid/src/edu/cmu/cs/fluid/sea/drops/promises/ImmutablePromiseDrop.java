package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;
import com.surelogic.annotation.scrub.ValidatedDropCallback;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.*;

/**
 * Promise drop for "NotThreadSafe" promises.
 * 
 * @see edu.cmu.com.surelogic.analysis.locks.held.LockVisitor
 * @see edu.cmu.cs.fluid.java.bind.LockAnnotation
 */
public final class ImmutablePromiseDrop extends
    ModifiedBooleanPromiseDrop<ImmutableNode> implements
    ValidatedDropCallback<ImmutablePromiseDrop> {
  public ImmutablePromiseDrop(ImmutableNode a) {
    super(a);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
  
  @Override
  protected void computeBasedOnAST() {
    String name = JavaNames.getTypeName(getNode());
    final boolean isImplementationOnly = getAAST().isImplementationOnly();
    final boolean isVerify = getAAST().verify();    
    if (isVerify) {
      if (!isImplementationOnly) { // default case
        setResultMessage(Messages.LockAnnotation_immutableDrop, name);
      } else {
        setResultMessage(Messages.LockAnnotation_immutable_implOnly, name);
      }
    } else {
      if (isImplementationOnly) {
        setResultMessage(Messages.LockAnnotation_immutable_implOnly_noVerify, name);
      } else {
        setResultMessage(Messages.LockAnnotation_immutable_noVerify, name);
      }
    }
  }
  
  public void validated(final ImmutablePromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}