/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/BooleanPromiseDrop.java,v 1.2 2007/06/27 14:37:40 chance Exp $*/
package com.surelogic.dropsea.ir.drops;

import com.surelogic.Part;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode.State;

public class ModifiedBooleanPromiseDrop<A extends AbstractModifiedBooleanNode> extends BooleanPromiseDrop<A> {
  public ModifiedBooleanPromiseDrop(A a) {
    super(a);
  }

  public final boolean isImplementationOnly() {
	  return getAAST().isImplementationOnly();
  }
	
  public final boolean verify() {
    return getAAST().verify();  
  }
  
  public final Part getAppliesTo() {
    return getAAST().getAppliesTo();
  }
  
  @Deprecated
  public final State staticPart() {
    return getAAST().getStaticPart();
  }
}
