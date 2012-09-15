/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.bind;

import com.surelogic.aast.layers.LayerBindingKind;
import com.surelogic.dropsea.ir.drops.layers.IReferenceCheckDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Binding for UnidentifiedTargetNode
 * 
 * @author Edwin
 */
public interface ILayerBinding extends IBinding {
	LayerBindingKind getKind();
	IRNode getType();
	Iterable<IRNode> getPackages();
	IReferenceCheckDrop getOther();
}
