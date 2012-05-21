package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;
import com.surelogic.annotation.rules.NonNullRules;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public final class NullablePromiseDrop extends BooleanPromiseDrop<NullableNode> {
	public NullablePromiseDrop(NullableNode a) {
		super(a);
	    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
	}

	@Override
	protected void computeBasedOnAST() {
		setMessage(NonNullRules.NULLABLE+" on "+DebugUnparser.toString(getNode()));
	}
}
