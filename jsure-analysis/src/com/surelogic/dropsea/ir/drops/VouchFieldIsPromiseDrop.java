package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.aast.promise.VouchFieldIsNode.FieldKind;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

public final class VouchFieldIsPromiseDrop extends PromiseDrop<VouchFieldIsNode> {

  public VouchFieldIsPromiseDrop(final VouchFieldIsNode n) {
    super(n);
    setCategorizingMessage(JavaGlobals.VOUCH_CAT);
  }

  public boolean isFinal() {
    return getAAST().getKind() == FieldKind.Final;
  }

  public boolean isContainable() {
    return getAAST().getKind() == FieldKind.Containable;
  }

  public boolean isImmutable() {
    return getAAST().getKind() == FieldKind.Immutable;
  }

  public boolean isThreadSafe() {
    return getAAST().getKind() == FieldKind.ThreadSafe;
  }

  public boolean isAnnotationBounds() {
	return getAAST().getKind() == FieldKind.AnnotationBounds;
  }

  public String getReason() {
    return getAAST().getReason();
  }
}
