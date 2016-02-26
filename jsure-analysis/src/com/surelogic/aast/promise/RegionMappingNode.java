
package com.surelogic.aast.promise;


import java.util.List;

import com.surelogic.aast.*;

public class RegionMappingNode extends AASTNode { 
  // Fields
  private final RegionNameNode from;
  private final RegionSpecificationNode to;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("RegionMapping") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        RegionNameNode from =  (RegionNameNode) _kids.get(0);
        RegionSpecificationNode to =  (RegionSpecificationNode) _kids.get(1);
        return new RegionMappingNode (_start,
          from,
          to        );
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public RegionMappingNode(int offset,
                           RegionNameNode from,
                           RegionSpecificationNode to) {
    super(offset);
    if (from == null) { throw new IllegalArgumentException("from is null"); }
    ((AASTNode) from).setParent(this);
    this.from = from;
    if (to == null) { throw new IllegalArgumentException("to is null"); }
    ((AASTNode) to).setParent(this);
    this.to = to;
  }
  
  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) {
      if (debug) { indent(sb, indent); }
      sb.append("RegionMapping\n");
      sb.append(getFrom().unparse(debug, indent+2));
      sb.append(getTo().unparse(debug, indent+2));
    } else {
      sb.append(getFrom().unparse(false));
      sb.append(" into ");
      sb.append(getTo().unparse(false));
    }
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public RegionNameNode getFrom() {
    return from;
  }
  /**
   * @return A non-null node
   */
  public RegionSpecificationNode getTo() {
    return to;
  }
  @Override
  public <T> T accept(INodeVisitor<T> visitor) {
   
    return visitor.visit(this);
  }
  
  @Override
  protected IAASTNode internalClone(final INodeModifier mod) {
  	return new RegionMappingNode(getOffset(), (RegionNameNode)getFrom().cloneOrModifyTree(mod), (RegionSpecificationNode)getTo().cloneOrModifyTree(mod));
  }
}

