package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.SingletonNode;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

public class SingletonPromiseDrop extends BooleanPromiseDrop<SingletonNode> {

  public SingletonPromiseDrop(SingletonNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.SINGLETON_CAT);
  }
}
