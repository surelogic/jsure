/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.dropsea.ir.drops.layers;

import com.surelogic.aast.layers.*;
import com.surelogic.analysis.layers.Messages;
import com.surelogic.dropsea.ir.PromiseDrop;


public class InLayerPromiseDrop extends PromiseDrop<InLayerNode> {
	public InLayerPromiseDrop(InLayerNode a) {
		super(a);
		setCategory(Messages.DSC_LAYERS_ISSUES);
		setMessage(12, getAAST().toString());
	}
}
