package com.surelogic.aast.promise;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNode;

public class ExplicitBorrowedInRegionNode extends AbstractSingleRegionNode<MappedRegionSpecificationNode>  
{ 
  // Fields
  public static final Factory<MappedRegionSpecificationNode> factory =
    new Factory<MappedRegionSpecificationNode>("ExplicitBorrowedInRegion") {
	  @Override
	  protected AASTRootNode create(int offset, 
			  MappedRegionSpecificationNode spec, int mods) {
      	return new ExplicitBorrowedInRegionNode(offset, spec);        
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ExplicitBorrowedInRegionNode(int offset,
                     MappedRegionSpecificationNode spec) {
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
  public IAASTNode cloneTree(){
	  return cloneTree(factory, JavaNode.ALL_FALSE);
  }
}

