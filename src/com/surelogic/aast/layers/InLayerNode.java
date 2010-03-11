package com.surelogic.aast.layers;

import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class InLayerNode extends AASTRootNode {
  private final String layerName;
	
  public static final AbstractSingleNodeFactory factory =
	  new AbstractSingleNodeFactory("InLayer") {
	  @Override
	  public AASTNode create(String _token, int _start, int _stop,
			  int _mods, String _id, int _dims, List<AASTNode> _kids) {			
		  return new InLayerNode(_start, _id);
	  }
  };
	
  // Constructors
  public InLayerNode(int offset, String id) {
    super(offset);
    layerName = id;
  }

  public String getLayer() {
	  return layerName;
  }
  
  @Override
  public IAASTNode cloneTree() {
	  return new InLayerNode(offset, layerName);
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
	  return visitor.visit(this);
  }

  @Override
  public String unparse(boolean debug, int indent) {
	  StringBuilder sb = new StringBuilder();
	  if (debug) {
		  indent(sb, indent);		    
		  sb.append("InLayerNode\n");
		  indent(sb, indent + 2);
		  sb.append("name=").append(layerName);
		  sb.append("\n");
	  } else {
		  sb.append("InLayer ").append(layerName);
	  }
	  return sb.toString();
  }
}
