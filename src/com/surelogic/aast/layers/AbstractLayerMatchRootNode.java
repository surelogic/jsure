package com.surelogic.aast.layers;

import com.surelogic.aast.*;

/**
 * Superclass for @MayReferTo, @AllowsReferencesTo, ...
 * 
 * @author Edwin
 */
public abstract class AbstractLayerMatchRootNode extends AASTRootNode {
  private final AbstractLayerMatchTarget target;
	
  // Constructors
  AbstractLayerMatchRootNode(int offset, AbstractLayerMatchTarget t) {
    super(offset);
    target = t;
  }

  public AbstractLayerMatchTarget getTarget() {
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
}
