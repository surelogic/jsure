
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;

public class AggregateNode extends AASTRootNode 
{ 
  // Fields
  private final MappedRegionSpecificationNode spec;

  public static final AbstractSingleNodeFactory factory =
    new AbstractSingleNodeFactory("Aggregate") {
      @Override
      @SuppressWarnings("unchecked")      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        MappedRegionSpecificationNode spec =  (MappedRegionSpecificationNode) _kids.get(0);
        return new AggregateNode (_start, spec        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public AggregateNode(int offset,
                     MappedRegionSpecificationNode spec) {
    super(offset);
    if (spec == null) { throw new IllegalArgumentException("spec is null"); }
    ((AASTNode) spec).setParent(this);
    this.spec = spec;
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("AggregateNode\n");
    indent(sb, indent+2);
    sb.append(getSpec().unparse(debug, indent+2));
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public MappedRegionSpecificationNode getSpec() {
    return spec;
  }
  
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
    return visitor.visit(this);
  }
  
  @Override
  public IAASTNode cloneTree(){
  	return new AggregateNode(offset, (MappedRegionSpecificationNode)spec.cloneTree());
  }
}

