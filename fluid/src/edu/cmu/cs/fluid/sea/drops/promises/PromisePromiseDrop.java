package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.ScopedPromiseNode;

import edu.cmu.cs.fluid.java.*;

/**
 * Promise drop for "promise" scoped promises.
 */
public final class PromisePromiseDrop extends ScopedPromiseDrop {
  public PromisePromiseDrop(ScopedPromiseNode a) {
    super(a);
    setCategory(JavaGlobals.PROMISE_CAT);
    /*
    if (a.getSrcType().isFromSource()) {
    	System.out.println("Creating promise: "+a);
    }
    */
  }
  
  @Override
  protected void computeBasedOnAST() {  
    setMessage("Promise "+getAAST());    
  }
  /*
  @Override
  protected final void invalidate_internal() {
	  System.out.println("Invalidating scoped promise: "+getMessage());
  }
*/
}