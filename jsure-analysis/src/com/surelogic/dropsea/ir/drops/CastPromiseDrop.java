package com.surelogic.dropsea.ir.drops;

import com.surelogic.aast.promise.*;
import com.surelogic.aast.promise.CastNode.CastKind;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.util.VisitUtil;

public final class CastPromiseDrop extends PromiseDrop<CastNode> {

  public CastPromiseDrop(final CastNode n) {
    super(n);
    setCategorizingMessage(JavaGlobals.VOUCH_CAT);
  }

  @Override
  protected IRNode useAlternateDeclForUnparse() {
    return VisitUtil.getEnclosingClassBodyDecl(getNode());
  }
  
  public boolean isToNonNull() {
	return getAAST().getKind() == CastKind.toNonNull;
  }
  
  public boolean isToNullable() {
	return getAAST().getKind() == CastKind.toNullable;
  }
  
  public boolean isToUniqueReference() {
	return getAAST().getKind() == CastKind.toUniqueReference;
  }
}
