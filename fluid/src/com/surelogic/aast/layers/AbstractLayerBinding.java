/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import java.util.*;

import com.surelogic.aast.bind.ILayerBinding;
import com.surelogic.dropsea.ir.drops.promises.layers.IReferenceCheckDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.util.EmptyIterator;

public abstract class AbstractLayerBinding implements ILayerBinding, Iterable<IRNode> {
	private final LayerBindingKind kind;

	protected AbstractLayerBinding(LayerBindingKind k) {
		kind = k;
	}
	
	public LayerBindingKind getKind() {
		return kind;
	}
	
	public IReferenceCheckDrop getOther() {
		return null;
	}

	public Iterable<IRNode> getPackages() {
		return this;
	}
	
	public Iterator<IRNode> iterator() {
		return new EmptyIterator<IRNode>();
	}
	
	public IRNode getType() {
		return null;
	}
}
