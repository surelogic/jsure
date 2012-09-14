package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.VouchSpecificationNode;
import com.surelogic.dropsea.ir.PromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;

/**
 */
public class VouchPromiseDrop extends PromiseDrop<VouchSpecificationNode> {

  public VouchPromiseDrop(VouchSpecificationNode a) {
    super(a);
    setCategory(JavaGlobals.VOUCH_CAT);
    setMessage(16, getAAST());
  }
}