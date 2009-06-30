
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class InRegionNode extends AASTRootNode 
{ 
  // Fields
  private final RegionSpecificationNode spec;

  public static final AbstractSingleNodeFactory factory =
    new AbstractSingleNodeFactory("InRegion") {
      @Override
      @SuppressWarnings("unchecked")      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        RegionSpecificationNode spec =  (RegionSpecificationNode) _kids.get(0);
        return new InRegionNode (_start, spec        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public InRegionNode(int offset,
                     RegionSpecificationNode spec) {
    super(offset);
    if (spec == null) { throw new IllegalArgumentException("spec is null"); }
    ((AASTNode) spec).setParent(this);
    this.spec = spec;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("InRegionNode\n");
    indent(sb, indent+2);
    sb.append(getSpec().unparse(debug, indent+2));
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public RegionSpecificationNode getSpec() {
    return spec;
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTRootNode cloneTree() {
    RegionSpecificationNode s = (RegionSpecificationNode)spec.cloneTree();
    return new InRegionNode(offset, s);
  }
}

