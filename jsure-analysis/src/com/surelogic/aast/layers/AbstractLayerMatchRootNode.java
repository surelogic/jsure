package com.surelogic.aast.layers;

import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.ILayerBinding;
import com.surelogic.aast.promise.*;
import com.surelogic.aast.visitor.DescendingVisitor;
import com.surelogic.common.util.*;
import com.surelogic.dropsea.ir.drops.layers.AbstractReferenceCheckDrop;
import com.surelogic.dropsea.ir.drops.layers.LayerPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Superclass for @MayReferTo, @AllowsReferencesTo, ...
 * 
 * @author Edwin
 */
public abstract class AbstractLayerMatchRootNode extends AASTRootNode {
  private final PromiseTargetNode target; // Can be null
	
  // Constructors
  AbstractLayerMatchRootNode(int offset, PromiseTargetNode t) {
    super(offset);
    target = t;
    if (t != null) {
    	target.setParent(this);
    }
  }

  public PromiseTargetNode getTarget() {
	  return target;
  }

  @Override
  public final String unparseForPromise() {
	  return unparse(false);
  }
  
  protected String unparse(boolean debug, int indent, String name) {
	  StringBuilder sb = new StringBuilder();
	  if (debug) {
		  indent(sb, indent);		    
		  sb.append(name).append("Node\n");
		  indent(sb, indent + 2);
		  sb.append("name=").append(target.unparse(debug, indent));
		  sb.append("\n");
	  } else {
		  sb.append(name).append("(\"").append(target.unparse(debug, indent)).append("\")");
	  }
	  return sb.toString();
  }
  
  public boolean check(IRNode type) {
	  if (target == null) {
		  return false;
	  }
	  return target.matches(type);
  }
  
  public Iterable<LayerPromiseDrop> getReferencedLayers() {
	  if (target == null) {
		  return new EmptyIterator<LayerPromiseDrop>();
	  }
	  LayerRefVisitor<LayerPromiseDrop> v = new LayerRefVisitor<LayerPromiseDrop>(false);
	  target.accept(v);
	  return v.getRefs();
  }
  
  public Iterable<AbstractReferenceCheckDrop<?>> getReferences() {
	  if (target == null) {
		  return new EmptyIterator<AbstractReferenceCheckDrop<?>>();
	  }
	  LayerRefVisitor<AbstractReferenceCheckDrop<?>> v = new LayerRefVisitor<AbstractReferenceCheckDrop<?>>(true);
	  target.accept(v);
	  return v.getRefs();
  }
  
  class LayerRefVisitor<T extends AbstractReferenceCheckDrop<?>> extends DescendingVisitor<Void> {
	private final boolean includeTypeSets;

	public LayerRefVisitor(boolean includeTypeSets) {
		  super(null);
		  this.includeTypeSets = includeTypeSets;
	  }

	  final List<T> layers = new ArrayList<T>();

	  public Iterable<T> getRefs() {
		  return layers;
	  }

	  @SuppressWarnings("unchecked")
	  @Override
	  public Void visit(UnidentifiedTargetNode n) {
		  ILayerBinding b = n.resolveBinding();		  
		  if (b != null) {
			  if (b.getKind() == LayerBindingKind.LAYER) {		  
				  layers.add((T) b.getOther());
			  }
			  else if (includeTypeSets && b.getKind() == LayerBindingKind.TYPESET) {
				  layers.add((T) b.getOther());
			  }
		  }
		  return null;  
	  }
  }
}
