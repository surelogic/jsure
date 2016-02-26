package com.surelogic.aast.promise;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNode;

public class InRegionNode extends AbstractSingleRegionNode<RegionSpecificationNode> 
{ 
  // Fields
  public static final Factory<RegionSpecificationNode> factory =
	  new Factory<RegionSpecificationNode>("InRegion") {
	  @Override
	  protected AASTRootNode create(int offset, 
			  RegionSpecificationNode spec, int mods) {
		  return new InRegionNode (offset, spec);
	  }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public InRegionNode(int offset, RegionSpecificationNode spec) {
    super(offset, spec);
  }

  @Override
  public String unparse(boolean debug, int indent) {
	return unparse(debug, indent, "InRegion");
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

