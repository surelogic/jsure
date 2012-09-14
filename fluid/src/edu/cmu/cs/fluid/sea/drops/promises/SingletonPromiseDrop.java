package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.SingletonNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public class SingletonPromiseDrop extends BooleanPromiseDrop<SingletonNode> {

  public SingletonPromiseDrop(SingletonNode a) {
    super(a);
    setCategory(JavaGlobals.SINGLETON_CAT);
    final String name = JavaNames.getTypeName(getNode());
    setResultMessage(Messages.SingletonDrop, name);
  }
}
