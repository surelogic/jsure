package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.VouchSpecificationNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 */
public class VouchPromiseDrop extends PromiseDrop<VouchSpecificationNode> {

  public VouchPromiseDrop(VouchSpecificationNode a) {
    super(a);
    setCategory(JavaGlobals.VOUCH_CAT);
    setResultMessage(16, getAAST());
  }
}