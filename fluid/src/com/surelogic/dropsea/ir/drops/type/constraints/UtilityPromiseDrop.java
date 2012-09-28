package com.surelogic.dropsea.ir.drops.type.constraints;

import com.surelogic.aast.promise.UtilityNode;
import com.surelogic.common.XUtil;
import com.surelogic.dropsea.ir.drops.BooleanPromiseDrop;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;

public class UtilityPromiseDrop extends BooleanPromiseDrop<UtilityNode> {

  public UtilityPromiseDrop(UtilityNode a) {
    super(a);
    setCategorizingMessage(JavaGlobals.UTILITY_CAT);
    final String name = XUtil.useExperimental() ? JavaNames.getRelativeTypeName(getNode()) : JavaNames.getTypeName(getNode());
    setMessage(Messages.UtilityDrop, name);
  }
}
