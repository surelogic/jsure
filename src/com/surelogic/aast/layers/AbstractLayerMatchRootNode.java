package com.surelogic.aast.layers;

import com.surelogic.aast.*;
import com.surelogic.aast.promise.*;

import edu.cmu.cs.fluid.ir.IRNode;

/**
 * Superclass for @MayReferTo, @AllowsReferencesTo, ...
 * 
 * @author Edwin
 */
public abstract class AbstractLayerMatchRootNode extends AASTRootNode {
  private final PromiseTargetNode target;
	
  // Constructors
  AbstractLayerMatchRootNode(int offset, PromiseTargetNode t) {
    super(offset);
    target = t;
    target.setParent(this);
  }

  public PromiseTargetNode getTarget() {
	  return target;
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
		  sb.append(name).append(' ').append(target.unparse(debug, indent));
	  }
	  return sb.toString();
  }
  
  public boolean check(IRNode type) {
	  return target.matches(type);
  }
}
