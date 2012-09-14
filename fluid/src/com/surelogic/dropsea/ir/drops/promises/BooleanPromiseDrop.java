/*$Header: /cvs/fluid/fluid/src/com/surelogic/sea/drops/BooleanPromiseDrop.java,v 1.2 2007/06/27 14:37:40 chance Exp $*/
package com.surelogic.dropsea.ir.drops.promises;

import com.surelogic.aast.IAASTRootNode;
import com.surelogic.dropsea.ir.PromiseDrop;


public class BooleanPromiseDrop<A extends IAASTRootNode> extends PromiseDrop<A> {
  public BooleanPromiseDrop(A a) {
    super(a);
  }
}
