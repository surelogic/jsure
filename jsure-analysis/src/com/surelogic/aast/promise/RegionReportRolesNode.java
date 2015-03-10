
package com.surelogic.aast.promise;


import java.util.*;

import com.surelogic.aast.*;
import com.surelogic.parse.TempListNode;

public class RegionReportRolesNode extends DataThreadRoleAnnotationNode { 
  // Fields
  private final List<RegionSpecificationNode> trRegions;

  public static final AbstractAASTNodeFactory factory =
    new AbstractAASTNodeFactory("RegionReportRoles") {
      @Override
      public AASTNode create(String _token, int _start, int _stop,
                                      int _mods, String _id, int _dims, List<AASTNode> _kids) {
        @SuppressWarnings("unchecked")
        List<RegionSpecificationNode> trRegions =  ((TempListNode) _kids.get(0)).toList();
        return new RegionReportRolesNode (_start, trRegions);
      }
    };

  // Constructors
  /**
   * Lists passed in as arguments must be @unique
   */
  public RegionReportRolesNode(int offset,
                             List<RegionSpecificationNode> trRegions) {
    super(offset);
    if (trRegions == null) { throw new IllegalArgumentException("cRegions is null"); }
    for (RegionSpecificationNode tr : trRegions) {
      ((AASTNode) tr).setParent(this);
    }
    this.trRegions = Collections.unmodifiableList(trRegions);
  }

  @Override
  public String unparse(boolean debug, int indent) {
    StringBuilder sb = new StringBuilder();
    if (debug) { indent(sb, indent); }
    sb.append("RegionReportRoles\n");
    for(AASTNode _n : getTRRegionsList()) {
      sb.append(_n.unparse(debug, indent+2));
    }
    return sb.toString();
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
		List<RegionSpecificationNode> cRegionsCopy = new ArrayList<RegionSpecificationNode>(trRegions.size());
		for (RegionSpecificationNode regionSpecificationNode : trRegions) {
			cRegionsCopy.add((RegionSpecificationNode)regionSpecificationNode.cloneTree());
		}	
		return new RegionReportRolesNode(getOffset(), cRegionsCopy);
	}
}

