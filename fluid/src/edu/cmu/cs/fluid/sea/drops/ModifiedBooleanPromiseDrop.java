/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/BooleanPromiseDrop.java,v 1.2 2007/06/27 14:37:40 chance Exp $*/
package edu.cmu.cs.fluid.sea.drops;

import com.surelogic.aast.promise.AbstractModifiedBooleanNode;

/**
 * Using the OLD drop-sea
 * 
 * @author edwin
 */
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
}
