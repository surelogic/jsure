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
	}
	
	@Override
	protected void computeBasedOnAST() {
		String name = JavaNames.getFieldDecl(getNode());
		setResultMessage(Messages.LockAnnotation_vouchFieldIsDrop, getAST().getKind(), name);
	}
  
  public boolean isFinal() {
    return getAST().getKind() == FieldKind.Final;
  }
  
  public boolean isContainable() {
    return getAST().getKind() == FieldKind.Containable;
  }
  
  public boolean isImmutable() {
    return getAST().getKind() == FieldKind.Immutable;
  }
  
  public boolean isThreadSafe() {
    return getAST().getKind() == FieldKind.ThreadSafe;
  }
  
  public String getReason() {
    return getAST().getReason();
  }
}
