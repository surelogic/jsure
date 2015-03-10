/*$Header: /cvs/fluid/fluid/src/com/surelogic/tree/SyntaxTreeRegion.java,v 1.1 2007/11/08 21:18:46 chance Exp $*/
package com.surelogic.tree;

import edu.cmu.cs.fluid.ir.*;
import edu.cmu.cs.fluid.util.UniqueID;

public class SyntaxTreeRegion extends IRRegion {
  public SyntaxTreeRegion() {
	  super();
  }
  
  private SyntaxTreeRegion(UniqueID id) {
	  super(id);
  }
	
  @Override
  protected IRNode newNode() {
    return new SyntaxTreeNode();
  }
  
  public static IRRegion getRegion(UniqueID id) {
	  IRRegion reg = (IRRegion)find(id);
	  if (reg == null) reg = new SyntaxTreeRegion(id);
	  return reg;
  }
}
