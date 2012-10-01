package com.surelogic.dropsea.ir.drops.method.constraints;

import com.surelogic.aast.promise.AnnotationBoundsNode;
import com.surelogic.annotation.scrub.ValidatedDropCallback;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

public final class AnnotationBoundsPromiseDrop extends BooleanPromiseDrop<AnnotationBoundsNode> implements
    ValidatedDropCallback<AnnotationBoundsPromiseDrop> {

  public AnnotationBoundsPromiseDrop(AnnotationBoundsNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.ANNO_BOUNDS_CAT);
    if (!XUtil.useExperimental()) {
    setMessage(Messages.AnnotationBounds, getAAST(), JavaNames.getTypeName(getNode()));
    }
  }

  public void validated(final AnnotationBoundsPromiseDrop pd) {
    pd.setVirtual(true);
    pd.setSourceDrop(this);
  }
}
