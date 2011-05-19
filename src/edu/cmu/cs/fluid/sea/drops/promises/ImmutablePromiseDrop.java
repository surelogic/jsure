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
 * @see edu.cmu.cs.fluid.java.analysis.LockVisitor
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
    final boolean isImplementationOnly = getAST().isImplementationOnly();
    final boolean isVerify = getAST().verify();
    if (!isImplementationOnly && isVerify) {
      setResultMessage(Messages.LockAnnotation_immutableDrop, name);
    } else {
      setResultMessage(Messages.LockAnnotation_immutableAttributedDrop,
          isImplementationOnly, isVerify, name);
    }
  }
  
  public void validated(final ImmutablePromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}