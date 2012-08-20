package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public final class AnnotationBoundsPromiseDrop extends
    BooleanPromiseDrop<AnnotationBoundsNode> implements
    ValidatedDropCallback<AnnotationBoundsPromiseDrop> {
  public AnnotationBoundsPromiseDrop(AnnotationBoundsNode a) {
    super(a); 
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
  }

  public void validated(final AnnotationBoundsPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}