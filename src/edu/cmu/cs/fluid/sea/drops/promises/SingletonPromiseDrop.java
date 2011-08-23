package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public class SingletonPromiseDrop extends BooleanPromiseDrop<SingletonNode> {

	public SingletonPromiseDrop(SingletonNode a) {
		super(a);
		setCategory(JavaGlobals.SINGLETON_CAT);
	}

	@Override
	protected void computeBasedOnAST() {
		String name = JavaNames.getTypeName(getNode());
		setResultMessage(Messages.SingletonDrop, name);
	}
}
