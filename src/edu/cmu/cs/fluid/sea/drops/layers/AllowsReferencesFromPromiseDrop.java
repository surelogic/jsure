/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.drops.layers;

import com.surelogic.aast.layers.*;

import edu.cmu.cs.fluid.sea.PromiseDrop;

public class AllowsReferencesFromPromiseDrop extends PromiseDrop<AllowsReferencesFromNode> {
	public AllowsReferencesFromPromiseDrop(AllowsReferencesFromNode a) {
		super(a);
	}
	@Override
	protected void computeBasedOnAST() {
		setMessage(getAST().toString());
	}
}
