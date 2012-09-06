package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.PromiseDrop;

/**
 */
public class VouchPromiseDrop extends PromiseDrop<VouchSpecificationNode> {
  public VouchPromiseDrop(VouchSpecificationNode a) {
    super(a);
    setCategory(JavaGlobals.VOUCH_CAT); 
  }
  
  @Override
  protected void computeBasedOnAST() {  
    setMessage("Vouch "+getAAST());    
  }
}