package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.UtilityNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public class UtilityPromiseDrop extends BooleanPromiseDrop<UtilityNode> {

	public UtilityPromiseDrop(UtilityNode a) {
		super(a);
		setCategory(JavaGlobals.UTILITY_CAT);
	}

	@Override
	protected void computeBasedOnAST() {
		String name = JavaNames.getTypeName(getNode());
		setResultMessage(125, name);
	}
}
