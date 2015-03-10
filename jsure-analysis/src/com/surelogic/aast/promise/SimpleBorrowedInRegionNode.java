package com.surelogic.aast.promise;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNode;

public class SimpleBorrowedInRegionNode extends AbstractSingleRegionNode<RegionSpecificationNode> 
{ 
  // Fields  
  public static final Factory<RegionSpecificationNode> factory =
    new Factory<RegionSpecificationNode>("SimpleBorrowedInRegion") {
	  @Override
	  protected AASTRootNode create(int offset, 
			  RegionSpecificationNode spec, int mods) {
        return new SimpleBorrowedInRegionNode (offset, spec);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public SimpleBorrowedInRegionNode(int offset, 
                     RegionSpecificationNode spec) {
    super(offset, spec);
  }

  @Override
  public String unparse(boolean debug, int indent) {
	return unparse(debug, indent, "BorrowedInRegion");
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTRootNode cloneTree() {
	  return cloneTree(factory, JavaNode.ALL_FALSE);
  }
}

