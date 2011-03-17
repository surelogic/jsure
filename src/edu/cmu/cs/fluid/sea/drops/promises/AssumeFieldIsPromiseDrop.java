package edu.cmu.cs.fluid.sea.drops.promises;

import com.surelogic.aast.promise.AssumeFieldIsNode;

import edu.cmu.cs.fluid.java.JavaGlobals;
import edu.cmu.cs.fluid.java.JavaNames;
import edu.cmu.cs.fluid.java.bind.Messages;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public class AssumeFieldIsPromiseDrop extends PromiseDrop<AssumeFieldIsNode> {
	public AssumeFieldIsPromiseDrop(AssumeFieldIsNode n) {
		super(n);
	    setCategory(JavaGlobals.LOCK_ASSURANCE_CAT);
	}
	
	@Override
	protected void computeBasedOnAST() {
		String name = JavaNames.getFieldDecl(getNode());
		setResultMessage(Messages.LockAnnotation_assumeFieldIsDrop, getAST().getKind(), name);
	}
}
