/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import java.util.*;

import com.surelogic.aast.bind.ILayerBinding;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.drops.layers.IReferenceCheckDrop;

import edu.cmu.cs.fluid.ir.IRNode;


public abstract class AbstractLayerBinding implements ILayerBinding, Iterable<IRNode> {
	private final LayerBindingKind kind;

	protected AbstractLayerBinding(LayerBindingKind k) {
		kind = k;
	}
	
	@Override
  public LayerBindingKind getKind() {
		return kind;
	}
	
	@Override
  public IReferenceCheckDrop getOther() {
		return null;
	}

	@Override
  public Iterable<IRNode> getPackages() {
		return this;
	}
	
	@Override
  public Iterator<IRNode> iterator() {
		return new EmptyIterator<IRNode>();
	}
	
	@Override
  public IRNode getType() {
		return null;
	}
}
