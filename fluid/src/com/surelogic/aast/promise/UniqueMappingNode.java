package com.surelogic.aast.promise;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNode;

public class UniqueMappingNode extends AbstractSingleRegionNode<MappedRegionSpecificationNode> 
{ 
  // Fields
  private final boolean allowRead;
  
  public static final Factory<MappedRegionSpecificationNode> factory =
    new Factory<MappedRegionSpecificationNode>("UniqueMapping") {
	  @Override
	  protected AASTRootNode create(int offset, 
			  MappedRegionSpecificationNode spec, int mods) {
        return new UniqueMappingNode (offset, spec, 
        		JavaNode.getModifier(mods, JavaNode.ALLOW_READ));       
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public UniqueMappingNode(int offset, 
                     MappedRegionSpecificationNode spec, boolean allow) {
    super(offset, spec);
    allowRead = allow;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append("UniqueMappingNode\n");
    	indent(sb, indent+2);
    	sb.append(getSpec().unparse(debug, indent+2));
    	if (allowRead) {
    		indent(sb, indent+2);
    		sb.append("allowRead=true");
    	}
    } else {
    	sb.append("UniqueInRegion(\"");
    	sb.append(getSpec().unparse(debug, indent+2)).append('"');
    	if (allowRead) {
    		sb.append(", allowRead=true");
    	}
    	sb.append(')');
    }
    return sb.toString();
  }
  
  public boolean allowRead() {
	  return allowRead;
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
	return cloneTree(factory, allowRead ? JavaNode.ALLOW_READ : JavaNode.ALL_FALSE);    
  }
}

