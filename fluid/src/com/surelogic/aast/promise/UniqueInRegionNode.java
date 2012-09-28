
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNode;

public class UniqueInRegionNode extends AASTRootNode 
{ 
  // Fields
  private final RegionSpecificationNode spec;
  private final boolean allowRead;
  
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("UniqueInRegion") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        RegionSpecificationNode spec =  (RegionSpecificationNode) _kids.get(0);
        return new UniqueInRegionNode (_start, spec, JavaNode.getModifier(_mods, JavaNode.ALLOW_READ));
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public UniqueInRegionNode(int offset,
                     RegionSpecificationNode spec, boolean allow) {
    super(offset);
    if (spec == null) { throw new IllegalArgumentException("spec is null"); }
    ((AASTNode) spec).setParent(this);
    this.spec = spec;
    this.allowRead = allow;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append("UniqueInRegionNode\n");
    	indent(sb, indent+2);
    	sb.append(getSpec().unparse(debug, indent+2));
    	if (allowRead) {
    		indent(sb, indent+2);
    		sb.append("allowRead=true");
    	}
    } else {
    	sb.append("UniqueInRegion(\"");
    	sb.append(getSpec());
    	if (allowRead) {
    		sb.append(", allowRead=true");
    	}
    	sb.append("\")");
    }
    return sb.toString();
  }

  public String unparseForPromise() {
	  return unparse(false);
  }
  
  /**
   * @return A non-null node
   */
  public RegionSpecificationNode getSpec() {
    return spec;
  }
  
  public boolean allowRead() {
	  return allowRead;
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTRootNode cloneTree() {
    RegionSpecificationNode s = (RegionSpecificationNode)spec.cloneTree();
    return new UniqueInRegionNode(offset, s, allowRead);
  }
}

