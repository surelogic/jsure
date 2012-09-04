package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.*;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.annotation.rules.*;

import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.sea.drops.BooleanPromiseDrop;

public final class ValueObjectPromiseDrop extends BooleanPromiseDrop<ValueObjectNode> {
	public ValueObjectPromiseDrop(ValueObjectNode a) {
		super(a);
	    setCategory(Messages.DSC_LAYERS_ISSUES);
	}

	@Override
	protected void computeBasedOnAST() {
		setMessage(EqualityRules.VALUE_OBJECT+" on "+JavaNames.getFullName(getNode()));
	}
}
