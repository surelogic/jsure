package com.surelogic.aast.promise;

import com.surelogic.aast.AASTRootNode;
import com.surelogic.aast.IAASTNode;
import com.surelogic.aast.IAASTRootNode;
import com.surelogic.aast.INodeModifier;
import com.surelogic.aast.INodeVisitor;

import edu.cmu.cs.fluid.java.JavaNode;

public class UniqueInRegionNode extends AbstractSingleRegionNode<RegionSpecificationNode> 
{ 
  public static final Factory<RegionSpecificationNode> factory =
    new Factory<RegionSpecificationNode>("UniqueInRegion") {
	  @Override
	  protected AASTRootNode create(int offset, 
			  RegionSpecificationNode spec, int mods) {
        return new UniqueInRegionNode(offset, spec);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public UniqueInRegionNode(int offset, 
                     RegionSpecificationNode spec) {
    super(offset, spec);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append("UniqueInRegionNode\n");
    	indent(sb, indent+2);
    	sb.append(getSpec().unparse(debug, indent+2));
    } else {
    	sb.append("UniqueInRegion(\"");
    	sb.append(getSpec());
    	sb.append("\")");
    }
    return sb.toString();
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
	return cloneTree(mod, factory, JavaNode.ALL_FALSE);    
  }
}

