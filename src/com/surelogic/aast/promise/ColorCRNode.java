/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorCRNode.java,v 1.1 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.parse.AbstractSingleNodeFactory;
import com.surelogic.parse.TempListNode;

public class ColorCRNode extends DataColoringAnnotationNode {
  // Fields
  private final ColorExprNode cExpr;

  private final List<RegionSpecificationNode> cRegions;

  public static final AbstractSingleNodeFactory factory = new AbstractSingleNodeFactory(
      "ColorCR") {
    @Override
    @SuppressWarnings("unchecked")
    public AASTNode create(String _token, int _start, int _stop, int _mods,
        String _id, int _dims, List<AASTNode> _kids) {
      ColorExprNode cExpr = (ColorExprNode) _kids.get(0);
      List<RegionSpecificationNode> cRegions = ((TempListNode) _kids.get(1))
          .toList();
      return new ColorCRNode(_start, cExpr, cRegions);
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ColorCRNode(int offset, ColorExprNode cExpr,
      List<RegionSpecificationNode> cRegions) {
    super(offset);
    if (cExpr == null) {
      throw new IllegalArgumentException("cExpr is null");
    }
    ((AASTNode) cExpr).setParent(this);
    this.cExpr = cExpr;
    if (cRegions == null) {
      throw new IllegalArgumentException("cRegions is null");
    }
    for (RegionSpecificationNode _c : cRegions) {
      ((AASTNode) _c).setParent(this);
    }
    this.cRegions = Collections.unmodifiableList(cRegions);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent);
    }
    sb.append("ColorConstrainedRegions\n");
    sb.append(getCExpr().unparse(debug, indent + 2));
    for (AASTNode _n : getCRegionsList()) {
      sb.append(_n.unparse(debug, indent + 2));
    }
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ColorExprNode getCExpr() {
    return cExpr;
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<RegionSpecificationNode> getCRegionsList() {
    return cRegions;
  }

  @Override
  public <T> T accept(INodeVisitor<T> visitor) {

    return visitor.visit(this);
  }

  /* (non-Javadoc)
   * @see com.surelogic.aast.AASTNode#cloneTree()
   */
  @Override
  public IAASTNode cloneTree() {
    List<RegionSpecificationNode> cRegionsCopy = new ArrayList<RegionSpecificationNode>(
        cRegions.size());
    for (RegionSpecificationNode regionSpecificationNode : cRegions) {
      cRegionsCopy.add((RegionSpecificationNode) regionSpecificationNode
          .cloneTree());
    }
    return new ColorCRNode(getOffset(),
        (ColorExprNode) getCExpr().cloneTree(), cRegionsCopy);
  }
}
