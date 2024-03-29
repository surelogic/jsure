/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/BooleanPromiseDrop.java,v 1.2 2007/06/27 14:37:40 chance Exp $*/
package com.surelogic.dropsea.ir.drops;

import com.surelogic.Part;
import com.surelogic.aast.promise.AbstractModifiedBooleanNode;

public class ModifiedBooleanPromiseDrop<A extends AbstractModifiedBooleanNode> extends BooleanPromiseDrop<A> {
  public ModifiedBooleanPromiseDrop(A a) {
    super(a);
  }
  
  public final String getToken() {
    return getAAST().getToken();
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
}
