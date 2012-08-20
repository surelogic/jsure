package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.*;

public final class AnnotationBoundsPromiseDrop extends
    BooleanPromiseDrop<AnnotationBoundsNode> implements
    ValidatedDropCallback<AnnotationBoundsPromiseDrop> {
  public AnnotationBoundsPromiseDrop(AnnotationBoundsNode a) {
    super(a); 
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
  /*
  @Override
  protected void computeBasedOnAST() {
    final String name = JavaNames.getTypeName(getNode());
    final boolean isImplementationOnly = getAST().isImplementationOnly();
    final boolean isVerify = getAST().verify();
    if (isVerify) {
      if (!isImplementationOnly) { // default case
        setResultMessage(Messages.LockAnnotation_threadSafeDrop, name);
      } else {
        setResultMessage(Messages.LockAnnotation_threadSafe_implOnly, name);
      }
    } else {
      if (isImplementationOnly) {
        setResultMessage(Messages.LockAnnotation_threadSafe_implOnly_noVerify, name);
      } else {
        setResultMessage(Messages.LockAnnotation_threadSafe_noVerify, name);
      }
    }
  }
  */
  public void validated(final AnnotationBoundsPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}