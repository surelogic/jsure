/*$Header: /cvs/fluid/fluid/src/com/surelogic/tree/SyntaxTreeRegion.java,v 1.1 2007/11/08 21:18:46 chance Exp $*/
package com.surelogic.tree;

import edu.cmu.cs.fluid.ir.*;

public class SyntaxTreeRegion extends IRRegion {
  @Override
  protected IRNode newNode() {
    return new SyntaxTreeNode(null, null);
  }
}
