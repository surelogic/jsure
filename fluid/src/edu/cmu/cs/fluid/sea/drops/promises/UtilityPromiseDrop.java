package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.UtilityNode;
import com.surelogic.dropsea.ir.drops.promises.BooleanPromiseDrop;

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
