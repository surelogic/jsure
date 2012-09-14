package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.VouchFieldIsNode;
import com.surelogic.analysis.concurrency.heldlocks.FieldKind;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public final class VouchFieldIsPromiseDrop extends PromiseDrop<VouchFieldIsNode> {

  public VouchFieldIsPromiseDrop(final VouchFieldIsNode n) {
    super(n);
    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
    final String name = JavaNames.getFieldDecl(getNode());
    setResultMessage(Messages.LockAnnotation_vouchFieldIsDrop, getAAST().getKind(), name);
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

  public String getReason() {
    return getAAST().getReason();
  }
}
