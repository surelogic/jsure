package com.surelogic.aast.layers;

import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.aast.bind.ILayerBinding;
import com.surelogic.aast.promise.*;
import com.surelogic.aast.visitor.DescendingVisitor;
import com.surelogic.dropsea.ir.drops.layers.LayerPromiseDrop;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.util.EmptyIterator;

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
	  LayerRefVisitor v = new LayerRefVisitor();
	  target.accept(v);
	  return v.getRefs();
  }
  
  class LayerRefVisitor extends DescendingVisitor<Void> {
	  public LayerRefVisitor() {
		  super(null);
	  }

	  final List<LayerPromiseDrop> layers = new ArrayList<LayerPromiseDrop>();

	  public Iterable<LayerPromiseDrop> getRefs() {
		  return layers;
	  }

	  @Override
	  public Void visit(UnidentifiedTargetNode n) {
		  ILayerBinding b = n.resolveBinding();		  
		  if (b != null && b.getKind() == LayerBindingKind.LAYER) {
			  layers.add((LayerPromiseDrop) b.getOther());
		  }
		  return null;  
	  }
  }
}
