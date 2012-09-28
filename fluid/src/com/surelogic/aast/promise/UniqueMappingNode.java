
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

import edu.cmu.cs.fluid.java.JavaNode;

public class UniqueMappingNode extends AASTRootNode 
{ 
  // Fields
  private final MappedRegionSpecificationNode spec;
  private final boolean allowRead;
  
  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("UniqueMapping") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        MappedRegionSpecificationNode spec =  (MappedRegionSpecificationNode) _kids.get(0);
        return new UniqueMappingNode (_start, spec, JavaNode.getModifier(_mods, JavaNode.ALLOW_READ));
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public UniqueMappingNode(int offset,
                     MappedRegionSpecificationNode spec, boolean allow) {
    super(offset);
    if (spec == null) { throw new IllegalArgumentException("spec is null"); }
    ((AASTNode) spec).setParent(this);
    this.spec = spec;
    allowRead = allow;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { 
    	indent(sb, indent); 
    	sb.append("UniqueMappingNode\n");
    	indent(sb, indent+2);
    	sb.append(spec.unparse(debug, indent+2));
    	if (allowRead) {
    		indent(sb, indent+2);
    		sb.append("allowRead=true");
    	}
    } else {
    	sb.append("UniqueInRegion(\"");
    	sb.append(spec.unparse(debug, indent+2)).append('"');
    	if (allowRead) {
    		sb.append(", allowRead=true");
    	}
    	sb.append(')');
    }
    return sb.toString();
  }

  public String unparseForPromise() {
	  return unparse(false);
  }
  
  /**
   * @return A non-null node
   */
  public MappedRegionSpecificationNode getMapping() {
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
  public IAASTNode cloneTree(){
  	return new UniqueMappingNode(offset, (MappedRegionSpecificationNode)spec.cloneTree(), allowRead);
  }
}

