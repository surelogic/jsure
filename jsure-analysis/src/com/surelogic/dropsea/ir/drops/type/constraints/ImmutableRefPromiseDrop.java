package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.ImmutableRefNode;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;

public final class ImmutableRefPromiseDrop extends BooleanPromiseDrop<ImmutableRefNode> {

  public ImmutableRefPromiseDrop(ImmutableRefNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.LOCK_ASSURANCE_CAT);
  }
  
  @Override
  protected IRNode useAlternateDeclForUnparse() {
	  final IRNode n = getNode();
	  /*
	  final String qname = JavaNames.getRelativeName(n);
	  //if (qname.contains("Test.")) {
		  System.out.println("Got Immutable promise: "+qname);
	  //}
	   */
	  return computeAlternateDeclForUnparse(n);
  }
}
