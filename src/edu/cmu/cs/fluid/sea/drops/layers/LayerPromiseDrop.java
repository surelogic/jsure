/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package edu.cmu.cs.fluid.sea.drops.layers;

import com.surelogic.aast.layers.*;
import com.surelogic.annotation.rules.LayerRules;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.util.VisitUtil;
import edu.cmu.cs.fluid.sea.PromiseDrop;

public class LayerPromiseDrop extends PromiseDrop<LayerNode> implements IReferenceCheckDrop {
	public LayerPromiseDrop(LayerNode a) {
		super(a);
	}
	@Override
	protected void computeBasedOnAST() {
		setMessage(getAST().toString());
	}
	public String getId() {
		return getAST().getId();
	}
	public boolean check(IRNode type) {
		return getAST().check(type);
	}
	public int getResultMessageKind() {
		return 350;
	}
	public Object[] getArgs(IRNode binding, IRNode type, IRNode context) {
		return new Object[] { binding, VisitUtil.getClosestType(context) };
	}
	public boolean isPartOf(IRNode type) {
		// Compute qualified name for this layer
		final IRNode cu    = VisitUtil.getEnclosingCompilationUnit(getNode());
		final String qname = VisitUtil.getPackageName(cu)+'.'+getAST().getId();
		// Check if matches the layer for the type 
		final InLayerPromiseDrop bindInLayer = LayerRules.getInLayerDrop(type);
		if (bindInLayer == null) {
			return false;
		}
		return qname.equals(bindInLayer.getAST().getLayer());
	}	
}


