package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.java.DebugUnparser;
import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

/**
 * Promise drop for "Raw" promises established by the null value analysis.
 */
public final class RawPromiseDrop extends BooleanPromiseDrop<RawNode> {
	public RawPromiseDrop(RawNode a) {
		super(a);
	    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
	}

	@Override
	protected void computeBasedOnAST() {
		setMessage(getAAST()+" on "+DebugUnparser.toString(getNode()));
	}
}
