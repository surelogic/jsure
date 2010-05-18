/*$Header: /cvs/fluid/fluid/src/com/surelogic/aast/promise/ColorCRNode.java,v 1.1 2007/10/24 15:18:09 dfsuther Exp $*/
package com.surelogic.aast.promise;

import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.aast.AbstractAASTNodeFactory;
import com.surelogic.parse.TempListNode;

public class ThreadRoleCRNode extends DataThreadRoleAnnotationNode {
  // Fields
  private final ThreadRoleExprNode trExpr;

  private final List<RegionSpecificationNode> trRegions;

  public static final AbstractAASTNodeFactory factory = new AbstractAASTNodeFactory(
      "ThreadRoleCR") {
    @Override
    @SuppressWarnings("unchecked")
    public AASTNode create(String _token, int _start, int _stop, int _mods,
        String _id, int _dims, List<AASTNode> _kids) {
      ThreadRoleExprNode trExpr = (ThreadRoleExprNode) _kids.get(0);
      List<RegionSpecificationNode> cRegions = ((TempListNode) _kids.get(1))
          .toList();
      return new ThreadRoleCRNode(_start, trExpr, cRegions);
    }
  };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public ThreadRoleCRNode(int offset, ThreadRoleExprNode trExpr,
      List<RegionSpecificationNode> trRegions) {
    super(offset);
    if (trExpr == null) {
      throw new IllegalArgumentException("trExpr is null");
    }
    ((AASTNode) trExpr).setParent(this);
    this.trExpr = trExpr;
    if (trRegions == null) {
      throw new IllegalArgumentException("trRegions is null");
    }
    for (RegionSpecificationNode _tr : trRegions) {
      ((AASTNode) _tr).setParent(this);
    }
    this.trRegions = Collections.unmodifiableList(trRegions);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) {
      indent(sb, indent);
    }
    sb.append("ColorConstrainedRegions\n");
    sb.append(getTRExpr().unparse(debug, indent + 2));
    for (AASTNode _n : getTRRegionsList()) {
      sb.append(_n.unparse(debug, indent + 2));
    }
    return sb.toString();
  }

  /**
   * @return A non-null node
   */
  public ThreadRoleExprNode getTRExpr() {
    return trExpr;
  }

  /**
   * @return A non-null, but possibly empty list of nodes
   */
  public List<RegionSpecificationNode> getTRRegionsList() {
    return trRegions;
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
        trRegions.size());
    for (RegionSpecificationNode regionSpecificationNode : trRegions) {
      cRegionsCopy.add((RegionSpecificationNode) regionSpecificationNode
          .cloneTree());
    }
    return new ThreadRoleCRNode(getOffset(),
        (ThreadRoleExprNode) getTRExpr().cloneTree(), cRegionsCopy);
  }
}
