package com.surelogic.dropsea.ir.drops.uniqueness;

import com.surelogic.aast.promise.ReadOnlyNode;
import com.surelogic.dropsea.UiShowAtTopLevel;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public final class ReadOnlyPromiseDrop extends BooleanPromiseDrop<ReadOnlyNode> implements UiShowAtTopLevel {

  public ReadOnlyPromiseDrop(ReadOnlyNode n) {
    super(n);
    setCategorizingMessage(JavaGlobals.UNIQUENESS_CAT);
  }
  
  @Override
  protected IRNode useAlternateDeclForUnparse() {
	  return VisitUtil.getEnclosingClassBodyDecl(getNode());
  }
}