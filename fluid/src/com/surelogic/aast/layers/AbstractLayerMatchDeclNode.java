/*$Header: /cvs/fluid/fluid/.settings/org.eclipse.jdt.ui.prefs,v 1.2 2006/03/27 21:35:50 boyland Exp $*/
package com.surelogic.aast.layers;

import com.surelogic.aast.promise.PromiseTargetNode;

/**
 * Superclass for @Layer, @TypeSet
 * @author Edwin
 */
public abstract class AbstractLayerMatchDeclNode extends AbstractLayerMatchRootNode {
	private final String id;
	
	AbstractLayerMatchDeclNode(int offset, String id, PromiseTargetNode t) {
		super(offset, t);
		this.id = id;
	}

	public String getId() {
		return id;
	}	
	
	@Override
	protected String unparse(boolean debug, int indent, String name) {
		return unparse(debug, indent, name, " = ");
	}
	
	protected String unparse(boolean debug, int indent, String name, String connector) {
		  StringBuilder sb = new StringBuilder();
		  if (debug) {
			  indent(sb, indent);		    
			  sb.append(name).append("Node\n");
			  indent(sb, indent + 2);
			  sb.append("id=").append(getId());
			  sb.append("\n");
			  if (getTarget() != null) {
				  indent(sb, indent + 2);
				  sb.append("name=").append(getTarget().unparse(debug, indent));
				  sb.append("\n");
			  }
		  } else {
			  sb.append(name).append("(\"");
			  sb.append(getId());
			  if (getTarget() != null) {
				  sb.append(connector).append(getTarget().unparse(debug, indent));
			  }
			  sb.append("\")");
		  }
		  return sb.toString();
	}
}
