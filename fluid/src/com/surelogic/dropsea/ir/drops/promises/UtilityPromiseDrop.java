package com.surelogic.dropsea.ir.drops.promises;

import com.surelogic.aast.promise.UtilityNode;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

public class UtilityPromiseDrop extends BooleanPromiseDrop<UtilityNode> {

  public UtilityPromiseDrop(UtilityNode a) {
    super(a);
    setCategory(JavaGlobals.UTILITY_CAT);
    String name = JavaNames.getTypeName(getNode());
    setMessage(Messages.UtilityDrop, name);
  }
}
