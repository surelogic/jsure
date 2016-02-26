package com.surelogic.aast.layers;

import java.util.List;

import com.surelogic.aast.*;

public class InLayerNode extends AASTRootNode {
  private final AbstractLayerMatchTarget layerNames;
	
  public static final AbstractAASTNodeFactory factory =
	  new AbstractAASTNodeFactory("InLayer") {
	  @Override
	  public AASTNode create(String _token, int _start, int _stop,
			  int _mods, String _id, int _dims, List<AASTNode> _kids) {			
		  return new InLayerNode(_start, (AbstractLayerMatchTarget) _kids.get(0));
	  }
  };
	
  // Constructors
  public InLayerNode(int offset, AbstractLayerMatchTarget target) {
    super(offset);
    layerNames = target;
    target.setParent(this);
  }

  public AbstractLayerMatchTarget getLayers() {
	  return layerNames;
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
	  return new InLayerNode(offset, (AbstractLayerMatchTarget) layerNames.cloneOrModifyTree(mod));
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
		  sb.append("name=").append(layerNames.unparse(debug, indent+2));
		  sb.append("\n");
	  } else {
		  sb.append("InLayer(\"").append(layerNames.unparse(debug, indent)).append("\")");
	  }
	  return sb.toString();
  }
  
  @Override
  public final String unparseForPromise() {
	  return unparse(false);
  }
}
